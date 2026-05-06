#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

DEPLOY_STATE_DIR="${DEPLOY_STATE_DIR:-.deploy-state}"
DEPLOY_GRAPHHOPPER="${DEPLOY_GRAPHHOPPER:-false}"

export DEPLOY_GRAPHHOPPER
if [ ! -f "$DEPLOY_STATE_DIR/previous-app-image" ]; then
  echo "No previous app image tag recorded in $DEPLOY_STATE_DIR/previous-app-image" >&2
  exit 1
fi

export APP_IMAGE_TAG="$(cat "$DEPLOY_STATE_DIR/previous-app-image")"

if [ -f "$DEPLOY_STATE_DIR/previous-graphhopper-image" ]; then
  export GRAPHHOPPER_IMAGE_TAG="$(cat "$DEPLOY_STATE_DIR/previous-graphhopper-image")"
fi

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d backend ai

if [ "$DEPLOY_GRAPHHOPPER" = "true" ]; then
  docker compose --env-file .env.prod -f docker-compose.prod.yml --profile graphhopper run --rm --entrypoint sh graphhopper -c '
    if [ ! -d /graphhopper/previous-cache ] || [ -z "$(find /graphhopper/previous-cache -mindepth 1 -maxdepth 1 2>/dev/null)" ]; then
      echo "No previous GraphHopper graph-cache found." >&2
      exit 1
    fi
    find /graphhopper/data -mindepth 1 -maxdepth 1 -exec rm -rf {} +
    cp -a /graphhopper/previous-cache/. /graphhopper/data/
  '
  docker compose --env-file .env.prod -f docker-compose.prod.yml --profile graphhopper up -d graphhopper
fi

cp "$DEPLOY_STATE_DIR/previous-app-image" "$DEPLOY_STATE_DIR/current-app-image"
if [ -f "$DEPLOY_STATE_DIR/previous-graphhopper-image" ]; then
  cp "$DEPLOY_STATE_DIR/previous-graphhopper-image" "$DEPLOY_STATE_DIR/current-graphhopper-image"
fi

"$ROOT_DIR/scripts/deploy/prod-smoke.sh"
