#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
ENV_FILE="$ROOT_DIR/.env.prod"
PROD_GRAPHHOPPER_TUNNEL_AUTO="${PROD_GRAPHHOPPER_TUNNEL_AUTO:-true}"
PROD_GRAPHHOPPER_SSH_HOST="${PROD_GRAPHHOPPER_SSH_HOST:-ubuntu@api.busaneumgil.com}"
PROD_GRAPHHOPPER_SSH_KEY="${PROD_GRAPHHOPPER_SSH_KEY:-$ROOT_DIR/K14E102T.pem}"
PROD_GRAPHHOPPER_DB_LOCAL_PORT="${PROD_GRAPHHOPPER_DB_LOCAL_PORT:-25432}"
PROD_GRAPHHOPPER_TUNNEL_PID_FILE="${PROD_GRAPHHOPPER_TUNNEL_PID_FILE:-$ROOT_DIR/.tmp/graphhopper-prod-tunnel.pid}"
PROD_GRAPHHOPPER_DB_URL=""

env_value() {
  local key="$1"
  if [ -f "$ENV_FILE" ]; then
    awk -F= -v key="$key" '$1 == key {print substr($0, length(key) + 2)}' "$ENV_FILE" \
      | tail -n 1 \
      | sed 's/\r$//' \
      | sed 's/^"//; s/"$//' \
      | sed "s/^'//; s/'$//"
  fi
}

configured_prod_db_url() {
  env_value DB_URL
}

prod_db_host_port_name() {
  local db_url="$1"
  local without_prefix="${db_url#jdbc:postgresql://}"
  local without_query="${without_prefix%%\?*}"
  local host_port="${without_query%%/*}"
  local db_name="${without_query#*/}"
  local host="${host_port%%:*}"
  local port="${host_port#*:}"

  if [ "$port" = "$host_port" ]; then
    port="5432"
  fi

  echo "$host" "$port" "$db_name"
}

port_open() {
  local port="$1"
  (echo >"/dev/tcp/127.0.0.1/$port") >/dev/null 2>&1
}

ensure_prod_graphhopper_tunnel() {
  local configured_db_url="$1"
  local db_host db_port db_name
  read -r db_host db_port db_name < <(prod_db_host_port_name "$configured_db_url")

  if port_open "$PROD_GRAPHHOPPER_DB_LOCAL_PORT"; then
    echo "prod GraphHopper DB tunnel already reachable on local port: $PROD_GRAPHHOPPER_DB_LOCAL_PORT"
    PROD_GRAPHHOPPER_DB_URL="jdbc:postgresql://host.docker.internal:$PROD_GRAPHHOPPER_DB_LOCAL_PORT/$db_name"
    return
  fi

  if [ ! -f "$PROD_GRAPHHOPPER_SSH_KEY" ]; then
    echo "Missing SSH key for prod GraphHopper DB tunnel: $PROD_GRAPHHOPPER_SSH_KEY" >&2
    echo "Using configured prod DB_URL directly." >&2
    PROD_GRAPHHOPPER_DB_URL="$configured_db_url"
    return
  fi

  local temp_ssh_key
  temp_ssh_key="$(mktemp)"
  cp "$PROD_GRAPHHOPPER_SSH_KEY" "$temp_ssh_key"
  chmod 600 "$temp_ssh_key"

  echo "opening prod GraphHopper DB tunnel: local $PROD_GRAPHHOPPER_DB_LOCAL_PORT->$db_host:$db_port"
  mkdir -p "$(dirname "$PROD_GRAPHHOPPER_TUNNEL_PID_FILE")"

  if ! ssh \
    -i "$temp_ssh_key" \
    -o BatchMode=yes \
    -o ConnectTimeout=10 \
    -o StrictHostKeyChecking=accept-new \
    "$PROD_GRAPHHOPPER_SSH_HOST" \
    "true" >/dev/null 2>&1; then
    rm -f "$temp_ssh_key"
    echo "Cannot connect to prod GraphHopper SSH host: $PROD_GRAPHHOPPER_SSH_HOST" >&2
    echo "Set PROD_GRAPHHOPPER_SSH_HOST/PROD_GRAPHHOPPER_SSH_KEY to the S2 prod host credentials, or run this build on S2 with PROD_GRAPHHOPPER_TUNNEL_AUTO=false." >&2
    exit 1
  fi

  if ! ssh \
    -i "$temp_ssh_key" \
    -o BatchMode=yes \
    -o ConnectTimeout=10 \
    -o StrictHostKeyChecking=accept-new \
    "$PROD_GRAPHHOPPER_SSH_HOST" \
    "timeout 7 bash -lc 'echo >/dev/tcp/$db_host/$db_port'" >/dev/null 2>&1; then
    rm -f "$temp_ssh_key"
    echo "Prod GraphHopper SSH host cannot reach DB: $PROD_GRAPHHOPPER_SSH_HOST -> $db_host:$db_port" >&2
    echo "Check the RDS security group/VPC route, or run this build on the S2 prod host with PROD_GRAPHHOPPER_TUNNEL_AUTO=false." >&2
    exit 1
  fi

  ssh -nN \
    -i "$temp_ssh_key" \
    -o StrictHostKeyChecking=accept-new \
    -o ExitOnForwardFailure=yes \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -L "127.0.0.1:$PROD_GRAPHHOPPER_DB_LOCAL_PORT:$db_host:$db_port" \
    "$PROD_GRAPHHOPPER_SSH_HOST" &
  local tunnel_pid=$!
  echo "$tunnel_pid" >"$PROD_GRAPHHOPPER_TUNNEL_PID_FILE"

  local opened=false
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    if ! kill -0 "$tunnel_pid" >/dev/null 2>&1; then
      rm -f "$PROD_GRAPHHOPPER_TUNNEL_PID_FILE"
      rm -f "$temp_ssh_key"
      echo "Failed to start prod GraphHopper DB tunnel process" >&2
      exit 1
    fi
    if port_open "$PROD_GRAPHHOPPER_DB_LOCAL_PORT"; then
      opened=true
      break
    fi
    sleep 1
  done

  rm -f "$temp_ssh_key"

  if [ "$opened" != "true" ]; then
    kill "$tunnel_pid" >/dev/null 2>&1 || true
    rm -f "$PROD_GRAPHHOPPER_TUNNEL_PID_FILE"
    echo "Failed to open prod GraphHopper DB tunnel on local port: $PROD_GRAPHHOPPER_DB_LOCAL_PORT" >&2
    exit 1
  fi

  PROD_GRAPHHOPPER_DB_URL="jdbc:postgresql://host.docker.internal:$PROD_GRAPHHOPPER_DB_LOCAL_PORT/$db_name"
}

resolve_prod_graphhopper_db_url() {
  local configured_db_url
  configured_db_url="$(configured_prod_db_url)"

  if [ -z "$configured_db_url" ]; then
    echo "Missing DB_URL in $ENV_FILE" >&2
    exit 1
  fi

  if [ "$PROD_GRAPHHOPPER_TUNNEL_AUTO" = "true" ]; then
    ensure_prod_graphhopper_tunnel "$configured_db_url"
    return
  fi

  PROD_GRAPHHOPPER_DB_URL="$configured_db_url"
}

resolve_prod_graphhopper_db_url

DB_URL="$PROD_GRAPHHOPPER_DB_URL" \
docker compose --env-file "$ENV_FILE" \
  -f "$ROOT_DIR/docker-compose.prod.yml" \
  --profile graphhopper-build \
  run --rm graphhopper-build
