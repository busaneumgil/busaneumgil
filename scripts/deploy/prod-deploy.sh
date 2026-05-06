#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

DEPLOY_STATE_DIR="${DEPLOY_STATE_DIR:-.deploy-state}"
DEPLOY_GRAPHHOPPER="${DEPLOY_GRAPHHOPPER:-false}"
BUILD_GRAPHHOPPER="${BUILD_GRAPHHOPPER:-false}"
APP_IMAGE_TAG="${APP_IMAGE_TAG:-$(git rev-parse --short=12 HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"
GRAPHHOPPER_IMAGE_TAG="${GRAPHHOPPER_IMAGE_TAG:-$APP_IMAGE_TAG}"

export DEPLOY_GRAPHHOPPER
mkdir -p "$DEPLOY_STATE_DIR"
if [ -f "$DEPLOY_STATE_DIR/current-app-image" ]; then
  cp "$DEPLOY_STATE_DIR/current-app-image" "$DEPLOY_STATE_DIR/previous-app-image"
fi
if { [ "$BUILD_GRAPHHOPPER" = "true" ] || [ "$DEPLOY_GRAPHHOPPER" = "true" ]; } && [ -f "$DEPLOY_STATE_DIR/current-graphhopper-image" ]; then
  cp "$DEPLOY_STATE_DIR/current-graphhopper-image" "$DEPLOY_STATE_DIR/previous-graphhopper-image"
fi

export APP_IMAGE_TAG GRAPHHOPPER_IMAGE_TAG

docker compose --env-file .env.prod -f docker-compose.prod.yml config --quiet

docker compose --env-file .env.prod -f docker-compose.prod.yml build backend ai
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d backend ai

if [ "$BUILD_GRAPHHOPPER" = "true" ]; then
  docker compose --env-file .env.prod -f docker-compose.prod.yml --profile graphhopper-build build graphhopper-build
  docker compose --env-file .env.prod -f docker-compose.prod.yml --profile graphhopper-build run --rm graphhopper-build
fi

if [ "$DEPLOY_GRAPHHOPPER" = "true" ]; then
  docker compose --env-file .env.prod -f docker-compose.prod.yml --profile graphhopper up -d graphhopper
fi

"$ROOT_DIR/scripts/deploy/prod-smoke.sh"

echo "$APP_IMAGE_TAG" > "$DEPLOY_STATE_DIR/current-app-image"
if [ "$BUILD_GRAPHHOPPER" = "true" ] || [ "$DEPLOY_GRAPHHOPPER" = "true" ]; then
  echo "$GRAPHHOPPER_IMAGE_TAG" > "$DEPLOY_STATE_DIR/current-graphhopper-image"
fi
