#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
source "$ROOT_DIR/scripts/make/lib/be-dev.sh"

ensure_env_file
ensure_docker_daemon
ensure_dev_tunnel

db_name="$(dev_db_name)"
app_port="$(env_value GRAPHHOPPER_PORT)"
admin_port="$(env_value GRAPHHOPPER_ADMIN_PORT)"
app_port="${app_port:-8998}"
admin_port="${admin_port:-8999}"

DB_URL="jdbc:postgresql://host.docker.internal:$BE_DEV_DB_LOCAL_PORT/$db_name" \
"${DEV_COMPOSE[@]}" up -d --build graphhopper

echo "waiting for dev GraphHopper healthcheck: http://127.0.0.1:$admin_port/healthcheck"
ready=0
for _ in $(seq 1 180); do
  if curl -fsS "http://127.0.0.1:$admin_port/healthcheck" >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done

if [ "$ready" -ne 1 ]; then
  echo "dev GraphHopper runtime did not become healthy on admin port $admin_port" >&2
  exit 1
fi

DB_URL="jdbc:postgresql://host.docker.internal:$BE_DEV_DB_LOCAL_PORT/$db_name" \
GRAPHHOPPER_PROFILE_SMOKE_BASE_URL="http://host.docker.internal:$app_port" \
"${DEV_COMPOSE[@]}" --profile graphhopper-build run --rm --no-deps --build --entrypoint python3 \
  graphhopper-build \
  /usr/local/bin/smoke-graphhopper-profiles.py \
    --report-json /graphhopper/import/road-network-profile-smoke-report.json
