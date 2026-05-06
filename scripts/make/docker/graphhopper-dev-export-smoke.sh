#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
source "$ROOT_DIR/scripts/make/lib/be-dev.sh"

ensure_env_file
ensure_docker_daemon
ensure_dev_tunnel

db_name="$(dev_db_name)"

DB_URL="jdbc:postgresql://host.docker.internal:$BE_DEV_DB_LOCAL_PORT/$db_name" \
"${DEV_COMPOSE[@]}" --profile graphhopper-build run --rm --no-deps --build --entrypoint python3 \
  graphhopper-build \
  /usr/local/bin/smoke-postgis-export.py \
    --report-json /graphhopper/import/road-network-export-smoke-report.json
