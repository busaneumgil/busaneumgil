#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
source "$ROOT_DIR/scripts/make/lib/prod-db-tunnel.sh"

PROD_COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.prod.yml")

run_db_python() {
  DB_URL="$PROD_DB_URL" \
  "${PROD_COMPOSE[@]}" --profile graphhopper-build run --rm --no-deps --build --entrypoint python3 \
    graphhopper-build -
}

ensure_postgis_extension() {
  run_db_python <<'PY'
import os
import psycopg2

url = os.environ["DB_URL"].replace("jdbc:postgresql://", "")
host_port, db_name = url.split("/", 1)
host, port = host_port.split(":", 1)

with psycopg2.connect(
    host=host,
    port=port,
    dbname=db_name,
    user=os.environ["DB_USERNAME"],
    password=os.environ["DB_PASSWORD"],
    sslmode=os.environ.get("DB_SSLMODE", "require"),
) as conn:
    conn.autocommit = True
    with conn.cursor() as cursor:
        cursor.execute("CREATE EXTENSION IF NOT EXISTS postgis")
        cursor.execute("SELECT extversion FROM pg_extension WHERE extname = 'postgis'")
        row = cursor.fetchone()
        print(f"PostGIS extension ready: {row[0] if row else 'unknown'}")
PY
}

drop_empty_incompatible_road_tables() {
  run_db_python <<'PY'
import os
import psycopg2

url = os.environ["DB_URL"].replace("jdbc:postgresql://", "")
host_port, db_name = url.split("/", 1)
host, port = host_port.split(":", 1)

required_columns = {
    "road_nodes": ("vertex_id", "source_node_key", "point"),
    "road_segments": (
        "edge_id",
        "from_node_id",
        "to_node_id",
        "geom",
        "length_meter",
        "walk_access",
        "avg_slope_percent",
        "width_meter",
        "braille_block_state",
        "audio_signal_state",
        "slope_state",
        "width_state",
        "surface_state",
        "stairs_state",
        "signal_state",
        "segment_type",
    ),
    "segment_features": (
        "feature_id",
        "edge_id",
        "feature_type",
        "geom",
        "state",
        "value_number",
    ),
}

with psycopg2.connect(
    host=host,
    port=port,
    dbname=db_name,
    user=os.environ["DB_USERNAME"],
    password=os.environ["DB_PASSWORD"],
    sslmode=os.environ.get("DB_SSLMODE", "require"),
) as conn:
    conn.autocommit = True
    with conn.cursor() as cursor:
        incompatible = []
        for table, columns in required_columns.items():
            cursor.execute("SELECT to_regclass(%s)", (f"public.{table}",))
            if cursor.fetchone()[0] is None:
                continue
            cursor.execute(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = %s
                """,
                (table,),
            )
            existing_columns = {row[0] for row in cursor.fetchall()}
            missing_columns = [column for column in columns if column not in existing_columns]
            if missing_columns:
                cursor.execute(f'SELECT COUNT(*) FROM "{table}"')
                row_count = cursor.fetchone()[0]
                if row_count != 0:
                    raise SystemExit(
                        f"{table} has incompatible columns and rows={row_count}; refusing to drop it"
                    )
                incompatible.append(table)

        if incompatible:
            if "road_nodes" in incompatible or "road_segments" in incompatible:
                cursor.execute('DROP TABLE IF EXISTS "segment_features"')
                cursor.execute('DROP TABLE IF EXISTS "road_segments"')
                cursor.execute('DROP TABLE IF EXISTS "road_nodes"')
                print("Dropped empty incompatible road network schema tables for snake_case JPA recreation.")
            elif "segment_features" in incompatible:
                cursor.execute('DROP TABLE IF EXISTS "segment_features"')
                print("Dropped empty incompatible segment_features table for snake_case JPA recreation.")
PY
}

run_jpa_schema_update() {
  local timeout_seconds="${PROD_SCHEMA_UPDATE_TIMEOUT_SECONDS:-90}"
  local container_name="${PROD_SCHEMA_UPDATE_CONTAINER_NAME:-s14p31e102-prod-backend-schema-update}"

  docker rm -f "$container_name" >/dev/null 2>&1 || true
  set +e
  DB_URL="$PROD_DB_URL" \
  JPA_DDL_AUTO=update \
  SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  SPRING_JPA_PROPERTIES_HIBERNATE_GLOBALLY_QUOTED_IDENTIFIERS=true \
  SPRING_JPA_PROPERTIES_HIBERNATE_GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS=true \
  SERVER_PORT=0 \
  timeout "$timeout_seconds" \
  "${PROD_COMPOSE[@]}" run --rm --no-deps --build \
    --name "$container_name" \
    backend \
    --server.port=0 \
    --spring.jpa.hibernate.ddl-auto=update \
    --spring.jpa.properties.hibernate.globally_quoted_identifiers=true \
    --spring.jpa.properties.hibernate.globally_quoted_identifiers_skip_column_definitions=true
  local status=$?
  set -e
  docker rm -f "$container_name" >/dev/null 2>&1 || true

  if [ "$status" -eq 124 ]; then
    echo "backend JPA schema update reached timeout; verifying schema state."
    return
  fi

  if [ "$status" -ne 0 ]; then
    echo "backend JPA schema update failed before schema verification: exit $status" >&2
    return "$status"
  fi
}

verify_road_schema() {
  run_db_python <<'PY'
import os
import psycopg2

url = os.environ["DB_URL"].replace("jdbc:postgresql://", "")
host_port, db_name = url.split("/", 1)
host, port = host_port.split(":", 1)

required_tables = ("road_nodes", "road_segments", "segment_features")
required_columns = {
    "road_nodes": ("vertex_id", "source_node_key", "point"),
    "road_segments": (
        "edge_id",
        "from_node_id",
        "to_node_id",
        "geom",
        "length_meter",
        "walk_access",
        "avg_slope_percent",
        "width_meter",
        "braille_block_state",
        "audio_signal_state",
        "slope_state",
        "width_state",
        "surface_state",
        "stairs_state",
        "signal_state",
        "segment_type",
    ),
    "segment_features": (
        "feature_id",
        "edge_id",
        "feature_type",
        "geom",
        "state",
        "value_number",
    ),
}

with psycopg2.connect(
    host=host,
    port=port,
    dbname=db_name,
    user=os.environ["DB_USERNAME"],
    password=os.environ["DB_PASSWORD"],
    sslmode=os.environ.get("DB_SSLMODE", "require"),
) as conn:
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT to_regclass('public.road_nodes'), "
            "to_regclass('public.road_segments'), "
            "to_regclass('public.segment_features')"
        )
        existing = cursor.fetchone()
        missing_tables = [table for table, regclass in zip(required_tables, existing) if regclass is None]
        if missing_tables:
            raise SystemExit(f"missing road schema tables: {', '.join(missing_tables)}")

        for table, columns in required_columns.items():
            cursor.execute(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = %s
                """,
                (table,),
            )
            existing_columns = {row[0] for row in cursor.fetchall()}
            missing_columns = [column for column in columns if column not in existing_columns]
            if missing_columns:
                raise SystemExit(f"missing {table} columns: {', '.join(missing_columns)}")

        for table in required_tables:
            cursor.execute(f'SELECT COUNT(*) FROM "{table}"')
            count = cursor.fetchone()[0]
            print(f"{table} ready: rows={count}")
PY
}

resolve_prod_db_url
ensure_postgis_extension
drop_empty_incompatible_road_tables
run_jpa_schema_update
verify_road_schema
