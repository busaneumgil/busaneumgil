#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

SERVER_PORT="${SERVER_PORT:-8080}"
AI_PORT="${AI_PORT:-5000}"
GRAPHHOPPER_ADMIN_PORT="${GRAPHHOPPER_ADMIN_PORT:-8990}"
DEPLOY_GRAPHHOPPER="${DEPLOY_GRAPHHOPPER:-false}"

curl -fsS "http://127.0.0.1:${SERVER_PORT}/v3/api-docs" >/dev/null
curl -fsS "http://127.0.0.1:${AI_PORT}/health" >/dev/null

if [ "$DEPLOY_GRAPHHOPPER" = "true" ]; then
  curl -fsS "http://127.0.0.1:${GRAPHHOPPER_ADMIN_PORT}/healthcheck" >/dev/null
fi

echo "prod smoke test passed"
