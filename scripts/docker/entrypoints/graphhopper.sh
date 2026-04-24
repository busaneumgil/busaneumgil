#!/bin/sh
set -eu

DATA_FILE="${GRAPHHOPPER_DATA_FILE:-/data/raw/busan.osm.pbf}"
CONFIG_FILE="${GRAPHHOPPER_CONFIG_FILE:-/opt/graphhopper/config-local.yml}"
GRAPH_LOCATION="${GRAPHHOPPER_GRAPH_LOCATION:-/graphhopper/data}"

if [ ! -f "$DATA_FILE" ]; then
  echo "GraphHopper data file not found: $DATA_FILE" >&2
  exit 1
fi

exec java ${JAVA_OPTS:-} \
  -Ddw.graphhopper.datareader.file="$DATA_FILE" \
  -Ddw.graphhopper.graph.location="$GRAPH_LOCATION" \
  -jar /opt/graphhopper/graphhopper-web.jar \
  server "$CONFIG_FILE"
