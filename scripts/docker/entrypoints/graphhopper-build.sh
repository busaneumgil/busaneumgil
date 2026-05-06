#!/bin/sh
set -eu

CONFIG_FILE="${GRAPHHOPPER_BUILD_CONFIG_FILE:-/opt/graphhopper/config-build.yml}"
IMPORT_FILE="${GRAPHHOPPER_IMPORT_FILE:-/graphhopper/import/road-network.osm}"
VALIDATION_REPORT_FILE="${GRAPHHOPPER_VALIDATION_REPORT_FILE:-/graphhopper/import/road-network-validation-report.json}"
BUILD_LOCATION="${GRAPHHOPPER_BUILD_LOCATION:-/graphhopper/build-cache}"
GRAPH_LOCATION="${GRAPHHOPPER_GRAPH_LOCATION:-/graphhopper/data}"
IMPORT_TIMEOUT_SECONDS="${GRAPHHOPPER_IMPORT_TIMEOUT_SECONDS:-1800}"

mkdir -p "$(dirname "$IMPORT_FILE")" "$GRAPH_LOCATION"
mkdir -p "$(dirname "$VALIDATION_REPORT_FILE")"
mkdir -p "$BUILD_LOCATION"
find "$BUILD_LOCATION" -mindepth 1 -maxdepth 1 -exec rm -rf {} +

echo "Exporting PostgreSQL road network to temporary OSM: $IMPORT_FILE"
python3 /usr/local/bin/export-postgis-to-osm.py \
  --output "$IMPORT_FILE" \
  --report-json "$VALIDATION_REPORT_FILE"

if [ ! -s "$IMPORT_FILE" ]; then
  echo "GraphHopper import file is empty: $IMPORT_FILE" >&2
  exit 1
fi

echo "Building GraphHopper graph-cache at: $BUILD_LOCATION"
set +e
timeout "$IMPORT_TIMEOUT_SECONDS" java ${JAVA_OPTS:-} \
  -Ddw.graphhopper.datareader.file="$IMPORT_FILE" \
  -Ddw.graphhopper.graph.location="$BUILD_LOCATION" \
  -jar /opt/graphhopper/graphhopper-web.jar \
  server "$CONFIG_FILE" &
java_pid="$!"

ready=0
for _ in $(seq 1 "$IMPORT_TIMEOUT_SECONDS"); do
  if curl -fsS http://127.0.0.1:8990/healthcheck >/dev/null 2>&1; then
    ready=1
    break
  fi
  if ! kill -0 "$java_pid" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

kill "$java_pid" >/dev/null 2>&1 || true
wait "$java_pid" >/dev/null 2>&1
java_status="$?"
set -e

if [ "$ready" -ne 1 ] && [ "$java_status" -ne 124 ]; then
  echo "GraphHopper import process did not become healthy. status=$java_status" >&2
  exit 1
fi

if [ ! -d "$BUILD_LOCATION" ] || [ -z "$(find "$BUILD_LOCATION" -mindepth 1 -maxdepth 1 2>/dev/null)" ]; then
  echo "GraphHopper graph-cache build produced no files: $BUILD_LOCATION" >&2
  exit 1
fi

echo "Publishing graph-cache to runtime location: $GRAPH_LOCATION"
PREVIOUS_LOCATION="${GRAPHHOPPER_PREVIOUS_GRAPH_LOCATION:-/graphhopper/previous-cache}"
mkdir -p "$PREVIOUS_LOCATION"
find "$PREVIOUS_LOCATION" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
if [ -d "$GRAPH_LOCATION" ] && [ -n "$(find "$GRAPH_LOCATION" -mindepth 1 -maxdepth 1 2>/dev/null)" ]; then
  cp -a "$GRAPH_LOCATION"/. "$PREVIOUS_LOCATION"/
fi
find "$GRAPH_LOCATION" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
cp -a "$BUILD_LOCATION"/. "$GRAPH_LOCATION"/

echo "GraphHopper graph-cache build completed."
