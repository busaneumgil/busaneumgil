#!/usr/bin/env python3
import argparse
from collections import Counter, defaultdict, deque
from datetime import datetime, timezone
import json
import math
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

REQUIRED_NODE_FIELDS = {"vertex_id", "lon", "lat"}
REQUIRED_SEGMENT_FIELDS = {
    "edge_id",
    "from_node_id",
    "to_node_id",
    "geom_wkt",
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
}

ENUM_VALUES = {
    "walk_access": {"YES", "NO", "UNKNOWN"},
    "braille_block_state": {"YES", "NO", "UNKNOWN"},
    "audio_signal_state": {"YES", "NO", "UNKNOWN"},
    "slope_state": {"FLAT", "MODERATE", "STEEP", "RISK", "UNKNOWN"},
    "width_state": {"ADEQUATE_150", "ADEQUATE_120", "NARROW", "UNKNOWN"},
    "surface_state": {"PAVED", "UNPAVED", "UNKNOWN"},
    "stairs_state": {"YES", "NO", "UNKNOWN"},
    "signal_state": {"YES", "NO", "UNKNOWN"},
    "segment_type": {"CROSS_WALK", "SIDE_LINE"},
}

UNKNOWN_WARNING_THRESHOLD = 0.90
ENDPOINT_TOLERANCE = 0.000001


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


def is_finite_coordinate(value):
    try:
        number = float(value)
    except (TypeError, ValueError):
        return False
    return math.isfinite(number)


def is_valid_lon_lat(lon, lat):
    if not is_finite_coordinate(lon) or not is_finite_coordinate(lat):
        return False
    return -180.0 <= float(lon) <= 180.0 and -90.0 <= float(lat) <= 90.0


def same_coordinate(left, right):
    return (
        abs(float(left[0]) - float(right[0])) <= ENDPOINT_TOLERANCE
        and abs(float(left[1]) - float(right[1])) <= ENDPOINT_TOLERANCE
    )


def normalize_export_value(value, fallback):
    if value is None:
        return fallback
    text = str(value)
    if text == "":
        return fallback
    return text


def new_issue(kind, level, message, samples=None):
    issue = {"kind": kind, "level": level, "message": message}
    if samples:
        issue["samples"] = [str(sample) for sample in samples[:10]]
    return issue


def add_issue(target, kind, level, message, samples=None):
    target.append(new_issue(kind, level, message, samples))


def count_components(segments):
    graph = defaultdict(set)
    routeable_edges = []
    for segment in segments:
        if normalize_export_value(segment.get("walk_access"), "UNKNOWN") == "NO":
            continue
        try:
            from_node = int(segment["from_node_id"])
            to_node = int(segment["to_node_id"])
        except (KeyError, TypeError, ValueError):
            continue
        graph[from_node].add(to_node)
        graph[to_node].add(from_node)
        routeable_edges.append((from_node, to_node))

    seen = set()
    sizes = []
    for node_id in graph:
        if node_id in seen:
            continue
        queue = deque([node_id])
        seen.add(node_id)
        size = 0
        while queue:
            current = queue.popleft()
            size += 1
            for neighbor in graph[current]:
                if neighbor not in seen:
                    seen.add(neighbor)
                    queue.append(neighbor)
        sizes.append(size)
    return sorted(sizes, reverse=True), len(routeable_edges)


def validate_graph(nodes, segments, output=None):
    blockers = []
    warnings = []
    node_count = len(nodes)
    segment_count = len(segments)

    node_fields = set(nodes[0].keys()) if nodes else set()
    segment_fields = set(segments[0].keys()) if segments else set()
    missing_node_fields = sorted(REQUIRED_NODE_FIELDS - node_fields)
    missing_segment_fields = sorted(REQUIRED_SEGMENT_FIELDS - segment_fields)
    if missing_node_fields:
        add_issue(blockers, "missing_node_fields", "blocker", "road_nodes export is missing required fields", missing_node_fields)
    if missing_segment_fields:
        add_issue(
            blockers,
            "missing_segment_fields",
            "blocker",
            "road_segments export is missing required fields",
            missing_segment_fields,
        )

    vertex_counter = Counter()
    node_lookup = {}
    invalid_nodes = []
    for node in nodes:
        try:
            vertex_id = int(node["vertex_id"])
        except (KeyError, TypeError, ValueError):
            invalid_nodes.append(node.get("vertex_id"))
            continue
        vertex_counter[vertex_id] += 1
        if not is_valid_lon_lat(node.get("lon"), node.get("lat")):
            invalid_nodes.append(vertex_id)
            continue
        node_lookup[vertex_id] = (float(node["lon"]), float(node["lat"]))

    duplicate_vertices = [vertex_id for vertex_id, count in vertex_counter.items() if count > 1]
    if duplicate_vertices:
        add_issue(blockers, "duplicate_vertex_id", "blocker", "duplicate road_nodes vertex_id values found", duplicate_vertices)
    if invalid_nodes:
        add_issue(blockers, "invalid_node_geometry", "blocker", "node coordinates are missing or outside lon/lat bounds", invalid_nodes)

    edge_counter = Counter()
    dangling_edges = []
    invalid_geometry_edges = []
    endpoint_mismatch_edges = []
    self_loop_edges = []
    enum_violations = defaultdict(list)
    routeable_edges = 0
    unknown_counts = Counter()
    enum_counts = {field: Counter() for field in ENUM_VALUES}

    for segment in segments:
        edge_id = segment.get("edge_id")
        edge_counter[edge_id] += 1
        try:
            from_node_id = int(segment["from_node_id"])
            to_node_id = int(segment["to_node_id"])
        except (KeyError, TypeError, ValueError):
            dangling_edges.append(edge_id)
            continue

        if from_node_id == to_node_id:
            self_loop_edges.append(edge_id)
        if from_node_id not in node_lookup or to_node_id not in node_lookup:
            dangling_edges.append(edge_id)

        coords = parse_linestring_wkt(segment.get("geom_wkt"))
        if len(coords) < 2 or coords[0] == coords[-1] or not all(is_valid_lon_lat(lon, lat) for lon, lat in coords):
            invalid_geometry_edges.append(edge_id)
        elif from_node_id in node_lookup and to_node_id in node_lookup:
            if not same_coordinate(coords[0], node_lookup[from_node_id]) or not same_coordinate(coords[-1], node_lookup[to_node_id]):
                endpoint_mismatch_edges.append(edge_id)

        for field, allowed_values in ENUM_VALUES.items():
            fallback = "SIDE_LINE" if field == "segment_type" else "UNKNOWN"
            value = normalize_export_value(segment.get(field), fallback)
            enum_counts[field][value] += 1
            if value == "UNKNOWN":
                unknown_counts[field] += 1
            if value not in allowed_values:
                enum_violations[field].append(edge_id)

        if normalize_export_value(segment.get("walk_access"), "UNKNOWN") != "NO":
            routeable_edges += 1

    duplicate_edges = [edge_id for edge_id, count in edge_counter.items() if count > 1]
    if duplicate_edges:
        add_issue(blockers, "duplicate_edge_id", "blocker", "duplicate road_segments edge_id values found", duplicate_edges)
    if dangling_edges:
        add_issue(blockers, "missing_node_reference", "blocker", "road_segments reference missing road_nodes", dangling_edges)
    if invalid_geometry_edges:
        add_issue(blockers, "invalid_segment_geometry", "blocker", "segment geometry must be LINESTRING with at least two valid points", invalid_geometry_edges)
    if endpoint_mismatch_edges:
        add_issue(blockers, "endpoint_mismatch", "blocker", "segment endpoints do not match from/to node coordinates", endpoint_mismatch_edges)
    if self_loop_edges:
        add_issue(blockers, "self_loop", "blocker", "segment from_node_id and to_node_id must differ", self_loop_edges)
    for field, edge_ids in enum_violations.items():
        add_issue(blockers, "enum_violation", "blocker", f"{field} contains values outside the GraphHopper contract", edge_ids)
    if routeable_edges == 0:
        add_issue(blockers, "no_routeable_edge", "blocker", "at least one segment must have walk_access other than NO")

    component_sizes, routeable_edge_count = count_components(segments)
    if len(component_sizes) > 1:
        add_issue(
            warnings,
            "small_component",
            "warning",
            "routeable road network has disconnected components",
            component_sizes[1:6],
        )

    for field, count in unknown_counts.items():
        ratio = count / segment_count if segment_count else 0
        if ratio >= UNKNOWN_WARNING_THRESHOLD:
            add_issue(
                warnings,
                "high_unknown_ratio",
                "warning",
                f"{field} UNKNOWN ratio is {ratio:.2%}",
                [f"{count}/{segment_count}"],
            )

    return {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "status": "PASS" if not blockers else "FAIL",
        "output": output,
        "summary": {
            "nodeCount": node_count,
            "segmentCount": segment_count,
            "routeableEdgeCount": routeable_edges,
            "routeableComponentCount": len(component_sizes),
            "largestRouteableComponentNodeCount": component_sizes[0] if component_sizes else 0,
            "routeableEdgeCountForComponentScan": routeable_edge_count,
            "blockerCount": len(blockers),
            "warningCount": len(warnings),
        },
        "enumCounts": {field: dict(counter) for field, counter in enum_counts.items()},
        "blockers": blockers,
        "warnings": warnings,
    }


def write_json_report(path, report):
    if not path:
        return
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as report_file:
        json.dump(report, report_file, ensure_ascii=False, indent=2)
        report_file.write("\n")


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
    parser.add_argument("--report-json")
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

    report = validate_graph(nodes, segments, args.output)
    write_json_report(args.report_json, report)
    if report["blockers"]:
        sample = "; ".join(f'{item["kind"]}: {item["message"]}' for item in report["blockers"][:5])
        print(f"GraphHopper export validation failed. {sample}", file=sys.stderr)
        return 1

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    write_osm(nodes, segments, args.output)
    print(
        f'Exported {len(nodes)} nodes and {len(segments)} segments to {args.output}. '
        f'validation_status={report["status"]}'
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
