#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
source "$ROOT_DIR/scripts/make/lib/prod-db-tunnel.sh"

if [ "${GRAPHHOPPER_PROD_SKIP_SCHEMA_UPDATE:-false}" != "true" ]; then
  echo "ensuring prod JPA road schema before GraphHopper build"
  "$ROOT_DIR/scripts/make/docker/prod-schema-update.sh"
fi

resolve_prod_db_url

DB_URL="$PROD_DB_URL" \
docker compose --env-file "$ENV_FILE" \
  -f "$ROOT_DIR/docker-compose.prod.yml" \
  --profile graphhopper-build \
  run --rm graphhopper-build
