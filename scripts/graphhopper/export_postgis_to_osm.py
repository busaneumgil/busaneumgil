#!/usr/bin/env python3
import argparse
import os
import sys
import xml.etree.ElementTree as ET
from urllib.parse import parse_qs, urlparse


DEFAULT_NODES_SQL = '''
SELECT
  "vertexId" AS vertex_id,
  ST_X("point"::geometry) AS lon,
  ST_Y("point"::geometry) AS lat
FROM road_nodes
ORDER BY "vertexId"
'''

DEFAULT_SEGMENTS_SQL = '''
SELECT
  "edgeId" AS edge_id,
  "fromNodeId" AS from_node_id,
  "toNodeId" AS to_node_id,
  ST_AsText("geom"::geometry) AS geom_wkt,
  COALESCE("walkAccess"::text, 'UNKNOWN') AS walk_access,
  COALESCE("avgSlopePercent", 0.0) AS avg_slope_percent,
  COALESCE("widthMeter", 0.0) AS width_meter,
  COALESCE("brailleBlockState"::text, 'UNKNOWN') AS braille_block_state,
  COALESCE("audioSignalState"::text, 'UNKNOWN') AS audio_signal_state,
  COALESCE("slopeState"::text, 'UNKNOWN') AS slope_state,
  COALESCE("widthState"::text, 'UNKNOWN') AS width_state,
  COALESCE("surfaceState"::text, 'UNKNOWN') AS surface_state,
  COALESCE("stairsState"::text, 'UNKNOWN') AS stairs_state,
  COALESCE("signalState"::text, 'UNKNOWN') AS signal_state,
  COALESCE("segmentType"::text, 'SIDE_LINE') AS segment_type
FROM road_segments
ORDER BY "edgeId"
'''


def jdbc_to_dsn(jdbc_url: str) -> dict:
    if not jdbc_url.startswith("jdbc:postgresql://"):
        raise ValueError("DB_URL must use jdbc:postgresql://host:port/database")
    parsed = urlparse(jdbc_url.replace("jdbc:postgresql://", "postgresql://", 1))
    if not parsed.hostname or not parsed.path:
        raise ValueError("DB_URL must include host and database name")
    query = parse_qs(parsed.query)
    return {
        "host": parsed.hostname,
        "port": parsed.port or 5432,
        "dbname": parsed.path.lstrip("/"),
        "user": os.getenv("DB_USERNAME") or os.getenv("POSTGRES_USER"),
        "password": os.getenv("DB_PASSWORD") or os.getenv("POSTGRES_PASSWORD"),
        "sslmode": query.get("sslmode", [os.getenv("DB_SSLMODE", "prefer")])[0],
    }


def connect():
    import psycopg2

    db_url = os.getenv("DB_URL")
    if db_url:
        dsn = jdbc_to_dsn(db_url)
    else:
        dsn = {
            "host": os.getenv("PGHOST", "postgres"),
            "port": int(os.getenv("PGPORT", "5432")),
            "dbname": os.getenv("PGDATABASE", os.getenv("POSTGRES_DB", "e102")),
            "user": os.getenv("PGUSER", os.getenv("DB_USERNAME", "e102")),
            "password": os.getenv("PGPASSWORD", os.getenv("DB_PASSWORD", "e102")),
            "sslmode": os.getenv("DB_SSLMODE", "prefer"),
        }
    return psycopg2.connect(**dsn)


def tag(parent, key, value):
    if value is None:
        return
    ET.SubElement(parent, "tag", {"k": key, "v": str(value)})


def safe_osm_way_id(edge_id):
    numeric = int(edge_id)
    return numeric if numeric > 0 else abs(numeric) + 1_000_000_000


def parse_linestring_wkt(value):
    if not value:
        return []
    text = value.strip()
    upper = text.upper()
    if not upper.startswith("LINESTRING"):
        return []
    body = text[text.find("(") + 1 : text.rfind(")")]
    points = []
    for token in body.split(","):
        parts = token.strip().split()
        if len(parts) < 2:
            continue
        lon, lat = float(parts[0]), float(parts[1])
        points.append((lon, lat))
    return points


def normalize_export_value(value, fallback):
    if value is None:
        return fallback
    text = str(value)
    if text == "":
        return fallback
    return text


def write_osm(nodes, segments, output):
    # The exporter is the boundary between PostGIS camelCase columns and the GraphHopper
    # custom parser. Keep tag names aligned with Graphhopper_pipeline.md's ieum:* contract.
    root = ET.Element("osm", {"version": "0.6", "generator": "e102-postgis-graphhopper-export"})
    node_id_map = {int(node["vertex_id"]): index + 1 for index, node in enumerate(nodes)}
    next_synthetic_node_id = len(node_id_map) + 1

    for node in nodes:
        osm_node = ET.SubElement(
            root,
            "node",
            {
                "id": str(node_id_map[int(node["vertex_id"])]),
                "lat": f'{float(node["lat"]):.8f}',
                "lon": f'{float(node["lon"]):.8f}',
            },
        )
        tag(osm_node, "ieum:vertex_id", node["vertex_id"])

    segment_refs = []
    for segment in segments:
        refs = [node_id_map[int(segment["from_node_id"])]]
        coords = parse_linestring_wkt(segment.get("geom_wkt"))
        if len(coords) < 2:
            raise ValueError(f'road_segment edge_id={segment["edge_id"]} has invalid LINESTRING geometry')
        for lon, lat in coords[1:-1]:
            synthetic_id = next_synthetic_node_id
            next_synthetic_node_id += 1
            ET.SubElement(
                root,
                "node",
                {
                    "id": str(synthetic_id),
                    "lat": f"{lat:.8f}",
                    "lon": f"{lon:.8f}",
                },
            )
            refs.append(synthetic_id)
        refs.append(node_id_map[int(segment["to_node_id"])])
        segment_refs.append((segment, refs))

    for segment, refs in segment_refs:
        way = ET.SubElement(root, "way", {"id": str(safe_osm_way_id(segment["edge_id"]))})
        for ref in refs:
            ET.SubElement(way, "nd", {"ref": str(ref)})
        tag(way, "highway", "footway")
        tag(way, "foot", "yes")
        tag(way, "oneway", "no")
        tag(way, "ieum:edge_id", segment["edge_id"])
        tag(way, "ieum:walk_access", normalize_export_value(segment.get("walk_access"), "UNKNOWN"))
        tag(way, "ieum:avg_slope_percent", normalize_export_value(segment.get("avg_slope_percent"), "0.0"))
        tag(way, "ieum:width_meter", normalize_export_value(segment.get("width_meter"), "0.0"))
        tag(way, "ieum:braille_block_state", normalize_export_value(segment.get("braille_block_state"), "UNKNOWN"))
        tag(way, "ieum:audio_signal_state", normalize_export_value(segment.get("audio_signal_state"), "UNKNOWN"))
        tag(way, "ieum:slope_state", normalize_export_value(segment.get("slope_state"), "UNKNOWN"))
        tag(way, "ieum:width_state", normalize_export_value(segment.get("width_state"), "UNKNOWN"))
        tag(way, "ieum:surface_state", normalize_export_value(segment.get("surface_state"), "UNKNOWN"))
        tag(way, "ieum:stairs_state", normalize_export_value(segment.get("stairs_state"), "UNKNOWN"))
        tag(way, "ieum:signal_state", normalize_export_value(segment.get("signal_state"), "UNKNOWN"))
        tag(way, "ieum:segment_type", normalize_export_value(segment.get("segment_type"), "SIDE_LINE"))

    tree = ET.ElementTree(root)
    ET.indent(tree, space="  ")
    tree.write(output, encoding="utf-8", xml_declaration=True)


def fetch_dicts(conn, sql):
    with conn.cursor() as cur:
        cur.execute(sql)
        columns = [desc[0] for desc in cur.description]
        return [dict(zip(columns, row)) for row in cur.fetchall()]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    nodes_sql = os.getenv("GRAPHHOPPER_ROAD_NODES_SQL") or DEFAULT_NODES_SQL
    segments_sql = os.getenv("GRAPHHOPPER_ROAD_SEGMENTS_SQL") or DEFAULT_SEGMENTS_SQL

    with connect() as conn:
        nodes = fetch_dicts(conn, nodes_sql)
        segments = fetch_dicts(conn, segments_sql)

    if not nodes:
        print("No road_nodes rows found for GraphHopper export.", file=sys.stderr)
        return 1
    if not segments:
        print("No road_segments rows found for GraphHopper export.", file=sys.stderr)
        return 1

    node_ids = {int(row["vertex_id"]) for row in nodes}
    dangling = [
        row["edge_id"]
        for row in segments
        if int(row["from_node_id"]) not in node_ids or int(row["to_node_id"]) not in node_ids
    ]
    if dangling:
        sample = ", ".join(map(str, dangling[:10]))
        print(f"road_segments reference missing road_nodes. edge sample: {sample}", file=sys.stderr)
        return 1

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    write_osm(nodes, segments, args.output)
    print(f"Exported {len(nodes)} nodes and {len(segments)} segments to {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
