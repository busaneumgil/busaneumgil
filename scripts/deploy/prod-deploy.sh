#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

DEPLOY_STATE_DIR="${DEPLOY_STATE_DIR:-.deploy-state}"
DEPLOY_GRAPHHOPPER="${DEPLOY_GRAPHHOPPER:-false}"
BUILD_GRAPHHOPPER="${BUILD_GRAPHHOPPER:-false}"
APP_IMAGE_TAG="${APP_IMAGE_TAG:-$(git rev-parse --short=12 HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"
GRAPHHOPPER_IMAGE_TAG="${GRAPHHOPPER_IMAGE_TAG:-$APP_IMAGE_TAG}"
GRAPHHOPPER_CACHE_VOLUME="${GRAPHHOPPER_CACHE_VOLUME:-s14p31e102-prod_graphhopper-prod-data}"

export DEPLOY_GRAPHHOPPER

require_env_value() {
  local key="$1"
  local raw

  raw="$(grep -E "^${key}=" .env.prod | tail -n1 || true)"
  if [ -z "$raw" ] || [ "${raw#*=}" = "" ]; then
    echo "${key} must be set in .env.prod" >&2
    exit 1
  fi
}

require_env_value JWT_SECRET

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

graphhopper_cache_fingerprint="$(bash "$ROOT_DIR/scripts/graphhopper/cache_fingerprint.sh" "$ROOT_DIR")"
needs_graphhopper_build="$BUILD_GRAPHHOPPER"

if [ "$DEPLOY_GRAPHHOPPER" = "true" ]; then
  docker compose --env-file .env.prod -f docker-compose.prod.yml --profile graphhopper build graphhopper
  if [ "$needs_graphhopper_build" != "true" ] \
    && ! bash "$ROOT_DIR/scripts/graphhopper/cache_matches_volume.sh" "$graphhopper_cache_fingerprint" "$GRAPHHOPPER_CACHE_VOLUME"; then
    needs_graphhopper_build="true"
    echo "GraphHopper cache fingerprint mismatch detected. Rebuilding graph-cache before runtime deploy."
  fi
fi

if [ "$needs_graphhopper_build" = "true" ]; then
  docker compose --env-file .env.prod -f docker-compose.prod.yml --profile graphhopper-build build graphhopper-build
  docker compose --env-file .env.prod -f docker-compose.prod.yml --profile graphhopper-build run --rm \
    -e GRAPHHOPPER_CACHE_FINGERPRINT="$graphhopper_cache_fingerprint" \
    graphhopper-build
fi

if [ "$DEPLOY_GRAPHHOPPER" = "true" ]; then
  docker compose --env-file .env.prod -f docker-compose.prod.yml --profile graphhopper up -d graphhopper
fi

bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"

echo "$APP_IMAGE_TAG" > "$DEPLOY_STATE_DIR/current-app-image"
if [ "$needs_graphhopper_build" = "true" ] || [ "$DEPLOY_GRAPHHOPPER" = "true" ]; then
  echo "$GRAPHHOPPER_IMAGE_TAG" > "$DEPLOY_STATE_DIR/current-graphhopper-image"
fi
