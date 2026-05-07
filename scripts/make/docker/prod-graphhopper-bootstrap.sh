#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

cat <<'EOF'
prod GraphHopper bootstrap starts.
  1. prod road schema 확인/생성
  2. LOCAL nodes/segments CSV를 prod DB에 재적재
  3. prod DB에서 graph-cache 생성
  4. prod GraphHopper/backend/ai runtime 기동

주의: road-network-prod-load는 road_nodes/road_segments를 비운 뒤 CSV 기준으로 다시 적재한다.
EOF

"$ROOT_DIR/scripts/db/load_road_network_prod.sh"

echo "building prod graph-cache from the loaded prod road network"
GRAPHHOPPER_PROD_SKIP_SCHEMA_UPDATE=true \
  "$ROOT_DIR/scripts/make/docker/graphhopper-prod-build.sh"

echo "starting prod GraphHopper runtime and dependent services"
"$ROOT_DIR/scripts/make/docker/prod-up-graphhopper.sh"
