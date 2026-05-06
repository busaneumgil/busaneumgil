#!/usr/bin/env python3
import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET
from urllib.parse import parse_qs, urlparse

import psycopg2


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
  "avgSlopePercent" AS avg_slope_percent,
  COALESCE("widthState", 'UNKNOWN') AS width_state,
  COALESCE("slopeState", 'UNKNOWN') AS slope_state,
  COALESCE("stairsState", 'UNKNOWN') AS stairs_state,
  COALESCE("crossingState", 'UNKNOWN') AS crossing_state
FROM road_segments
WHERE COALESCE("walkAccess"::text, 'UNKNOWN') <> 'NO'
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


def write_osm(nodes, segments, output):
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
        tag(osm_node, "e102:vertex_id", node["vertex_id"])

    segment_refs = []
    for segment in segments:
        refs = [node_id_map[int(segment["from_node_id"])]]
        coords = parse_linestring_wkt(segment.get("geom_wkt"))
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
        tag(way, "e102:edge_id", segment["edge_id"])
        tag(way, "e102:walk_access", segment["walk_access"])
        tag(way, "e102:avg_slope_percent", segment["avg_slope_percent"])
        tag(way, "e102:width_state", segment["width_state"])
        tag(way, "e102:slope_state", segment["slope_state"])
        tag(way, "e102:stairs_state", segment["stairs_state"])
        tag(way, "e102:crossing_state", segment["crossing_state"])

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
