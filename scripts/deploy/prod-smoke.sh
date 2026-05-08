#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

SERVER_PORT="${SERVER_PORT:-8080}"
AI_PORT="${AI_PORT:-5000}"
GRAPHHOPPER_ADMIN_PORT="${GRAPHHOPPER_ADMIN_PORT:-8990}"
DEPLOY_GRAPHHOPPER="${DEPLOY_GRAPHHOPPER:-false}"
SMOKE_RETRIES="${SMOKE_RETRIES:-24}"
SMOKE_DELAY_SECONDS="${SMOKE_DELAY_SECONDS:-5}"

wait_for_url() {
  local url="$1"
  local name="$2"
  local attempt

  for attempt in $(seq 1 "$SMOKE_RETRIES"); do
    if curl -fsS "$url" >/dev/null; then
      return 0
    fi
    sleep "$SMOKE_DELAY_SECONDS"
  done

  echo "${name} smoke check failed: ${url}" >&2
  return 1
}

wait_for_url "http://127.0.0.1:${AI_PORT}/health" "AI"
wait_for_url "http://127.0.0.1:${SERVER_PORT}/v3/api-docs" "Backend"

if [ "$DEPLOY_GRAPHHOPPER" = "true" ]; then
  wait_for_url "http://127.0.0.1:${GRAPHHOPPER_ADMIN_PORT}/healthcheck" "GraphHopper"
fi

echo "prod smoke test passed"
