#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
source "$ROOT_DIR/scripts/make/lib/be-dev.sh"

ensure_env_file
ensure_docker_daemon
resolve_dev_graphhopper_db_url

run_args=(--profile graphhopper-build run --rm)
if [ "$DEV_GRAPHHOPPER_BUILD_NO_DEPS" = "true" ]; then
  run_args+=(--no-deps)
fi

DB_URL="$DEV_GRAPHHOPPER_DB_URL" \
"${DEV_COMPOSE[@]}" "${run_args[@]}" graphhopper-build
