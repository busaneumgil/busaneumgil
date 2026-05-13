#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

SERVER_PORT="${SERVER_PORT:-8080}"
AI_PORT="${AI_PORT:-5000}"
ADMIN_PORT="${ADMIN_PORT:-3001}"
GRAPHHOPPER_ADMIN_PORT="${GRAPHHOPPER_ADMIN_PORT:-8990}"
GRAPHHOPPER_BLUE_ADMIN_PORT="${GRAPHHOPPER_BLUE_ADMIN_PORT:-18990}"
GRAPHHOPPER_GREEN_ADMIN_PORT="${GRAPHHOPPER_GREEN_ADMIN_PORT:-18992}"
DEPLOY_GRAPHHOPPER="${DEPLOY_GRAPHHOPPER:-true}"
SMOKE_ADMIN="${SMOKE_ADMIN:-true}"
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

wait_for_any_url() {
  local name="$1"
  shift
  local attempt
  local url

  for attempt in $(seq 1 "$SMOKE_RETRIES"); do
    for url in "$@"; do
      if curl -fsS "$url" >/dev/null; then
        return 0
      fi
    done
    sleep "$SMOKE_DELAY_SECONDS"
  done

  echo "${name} smoke check failed: $*" >&2
  return 1
}

wait_for_response() {
  local method="$1"
  local url="$2"
  local request_body="$3"

  if [ "$method" = "POST" ]; then
    curl -sS -o /tmp/prod-smoke-response.json -w "%{http_code}" \
      -H "Content-Type: application/json" \
      -d "$request_body" \
      "$url" || true
    return
  fi

  curl -sS -o /tmp/prod-smoke-response.json -w "%{http_code}" "$url" || true
}

wait_for_status_with_body() {
  local method="$1"
  local url="$2"
  local name="$3"
  local expected_status="$4"
  local request_body="$5"
  shift 5

  local attempt
  local status=""
  local pattern
  local matched

  for attempt in $(seq 1 "$SMOKE_RETRIES"); do
    status="$(wait_for_response "$method" "$url" "$request_body")"
    if [ "$status" = "$expected_status" ]; then
      matched="true"
      for pattern in "$@"; do
        if ! grep -Eq "$pattern" /tmp/prod-smoke-response.json; then
          matched="false"
          break
        fi
      done
      if [ "$matched" = "true" ]; then
        return 0
      fi
    fi
    sleep "$SMOKE_DELAY_SECONDS"
  done

  echo "${name} smoke check failed: ${url} (expected ${expected_status}, got ${status})" >&2
  if [ -f /tmp/prod-smoke-response.json ]; then
    cat /tmp/prod-smoke-response.json >&2
    echo >&2
  fi
  return 1
}

wait_for_status_with_body "GET" "http://127.0.0.1:${AI_PORT}/health" "AI health" "200" "" \
  '"providers"[[:space:]]*:' \
  '"POST /voice/analyze"'
wait_for_status_with_body "POST" "http://127.0.0.1:${AI_PORT}/voice/analyze" "AI voice analyze" "400" '{}' \
  '"success"[[:space:]]*:[[:space:]]*false' \
  '"intent"[[:space:]]*:[[:space:]]*"unknown"' \
  '"error"[[:space:]]*:'
wait_for_url "http://127.0.0.1:${SERVER_PORT}/v3/api-docs" "Backend"
if [ "$SMOKE_ADMIN" = "true" ]; then
  wait_for_status_with_body "GET" "http://127.0.0.1:${ADMIN_PORT}/health" "ADMIN health" "200" "" \
    'ok'
  wait_for_status_with_body "GET" "http://127.0.0.1:${ADMIN_PORT}/" "ADMIN web" "200" "" \
    '<div id="root"></div>'
fi

if [ "$DEPLOY_GRAPHHOPPER" = "true" ]; then
  wait_for_any_url "GraphHopper" \
    "http://127.0.0.1:${GRAPHHOPPER_BLUE_ADMIN_PORT}/healthcheck" \
    "http://127.0.0.1:${GRAPHHOPPER_GREEN_ADMIN_PORT}/healthcheck" \
    "http://127.0.0.1:${GRAPHHOPPER_ADMIN_PORT}/healthcheck"
fi

echo "prod smoke test passed"
