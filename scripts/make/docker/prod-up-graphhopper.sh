#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
ENV_FILE="${ENV_FILE:-"$ROOT_DIR/.env.prod"}"
source "$ROOT_DIR/scripts/make/lib/prod-db-tunnel.sh"
PROD_COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.prod.yml")

graphhopper_admin_port() {
  local port
  port="$(env_value GRAPHHOPPER_ADMIN_PORT)"
  echo "${port:-8990}"
}

graphhopper_cache_ready() {
  "${PROD_COMPOSE[@]}" --profile graphhopper-build run --rm --no-deps --build --entrypoint sh \
    graphhopper-build \
    -c 'test -d "${GRAPHHOPPER_GRAPH_LOCATION:-/graphhopper/data}" && test -n "$(find "${GRAPHHOPPER_GRAPH_LOCATION:-/graphhopper/data}" -mindepth 1 -maxdepth 1 2>/dev/null)"'
}

prepare_prod_runtime_env() {
  resolve_prod_db_url
  export DB_URL="$PROD_DB_URL"
  echo "prod runtime DB_URL resolved for compose runtime."
}

wait_for_graphhopper_healthcheck() {
  local admin_port="$1"
  local url="http://127.0.0.1:$admin_port/healthcheck"

  echo "waiting for prod GraphHopper healthcheck: $url"
  for _ in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "prod GraphHopper is healthy: $url"
      return 0
    fi
    sleep 3
  done

  echo "prod GraphHopper did not become healthy: $url" >&2
  "${PROD_COMPOSE[@]}" --profile graphhopper logs --tail=120 graphhopper >&2 || true
  return 1
}

wait_for_backend_http() {
  local server_port
  server_port="$(env_value SERVER_PORT)"
  server_port="${server_port:-8080}"
  local url="http://127.0.0.1:$server_port/"

  echo "waiting for prod backend HTTP listener: $url"
  for _ in $(seq 1 60); do
    local http_code
    http_code="$(curl -sS -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || true)"
    if [ "$http_code" != "000" ]; then
      echo "prod backend accepted HTTP: $url status=$http_code"
      return 0
    fi

    local backend_container
    backend_container="$("${PROD_COMPOSE[@]}" ps -q backend 2>/dev/null || true)"
    if [ -n "$backend_container" ] && [ "$(docker inspect -f '{{.State.Running}}' "$backend_container" 2>/dev/null || echo false)" != "true" ]; then
      echo "prod backend container exited before HTTP listener became reachable." >&2
      "${PROD_COMPOSE[@]}" logs --tail=160 backend >&2 || true
      return 1
    fi

    sleep 3
  done

  echo "prod backend HTTP listener did not become reachable: $url" >&2
  "${PROD_COMPOSE[@]}" logs --tail=160 backend >&2 || true
  return 1
}

if graphhopper_cache_ready; then
  echo "prod GraphHopper graph-cache is already present."
else
  echo "prod GraphHopper graph-cache is missing or empty. Running graphhopper-prod-build first."
  "$ROOT_DIR/scripts/make/docker/graphhopper-prod-build.sh"
fi

prepare_prod_runtime_env
"${PROD_COMPOSE[@]}" --profile graphhopper up -d --build graphhopper
wait_for_graphhopper_healthcheck "$(graphhopper_admin_port)"
"${PROD_COMPOSE[@]}" --profile graphhopper up -d --force-recreate backend ai
wait_for_backend_http
