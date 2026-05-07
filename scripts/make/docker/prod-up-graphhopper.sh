#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PROD_COMPOSE=(docker compose --env-file "$ROOT_DIR/.env.prod" -f "$ROOT_DIR/docker-compose.prod.yml")

env_value() {
  local key="$1"
  if [ -f "$ROOT_DIR/.env.prod" ]; then
    awk -F= -v key="$key" '$1 == key {print substr($0, length(key) + 2)}' "$ROOT_DIR/.env.prod" \
      | tail -n 1 \
      | sed 's/\r$//' \
      | sed 's/^"//; s/"$//' \
      | sed "s/^'//; s/'$//"
  fi
}

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

if graphhopper_cache_ready; then
  echo "prod GraphHopper graph-cache is already present."
else
  echo "prod GraphHopper graph-cache is missing or empty. Running graphhopper-prod-build first."
  "$ROOT_DIR/scripts/make/docker/graphhopper-prod-build.sh"
fi

"${PROD_COMPOSE[@]}" --profile graphhopper up -d --build graphhopper
wait_for_graphhopper_healthcheck "$(graphhopper_admin_port)"
"${PROD_COMPOSE[@]}" --profile graphhopper up -d backend ai
