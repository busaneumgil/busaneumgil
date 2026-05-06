#!/usr/bin/env python3
import argparse
import importlib.util
import json
import os
from datetime import datetime, timezone


EXPORTER_PATH = "/usr/local/bin/export-postgis-to-osm.py"


def load_exporter():
    spec = importlib.util.spec_from_file_location("export_postgis_to_osm", EXPORTER_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def strip_sql(sql):
    return sql.strip().rstrip(";")


def fetch_dicts(conn, sql, params=None):
    with conn.cursor() as cur:
        cur.execute(sql, params or [])
        columns = [desc[0] for desc in cur.description]
        return [dict(zip(columns, row)) for row in cur.fetchall()]


def fetch_scalar(conn, sql):
    with conn.cursor() as cur:
        cur.execute(sql)
        return cur.fetchone()[0]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sample-size", type=int, default=int(os.getenv("GRAPHHOPPER_EXPORT_SMOKE_SAMPLE_SIZE", "100")))
    parser.add_argument("--report-json", default=os.getenv("GRAPHHOPPER_EXPORT_SMOKE_REPORT_FILE"))
    args = parser.parse_args()

    exporter = load_exporter()
    nodes_sql = strip_sql(os.getenv("GRAPHHOPPER_ROAD_NODES_SQL") or exporter.DEFAULT_NODES_SQL)
    segments_sql = strip_sql(os.getenv("GRAPHHOPPER_ROAD_SEGMENTS_SQL") or exporter.DEFAULT_SEGMENTS_SQL)

    with exporter.connect() as conn:
        node_count = fetch_scalar(conn, 'SELECT COUNT(*) FROM road_nodes')
        segment_count = fetch_scalar(conn, 'SELECT COUNT(*) FROM road_segments')
        segments = fetch_dicts(conn, f"SELECT * FROM ({segments_sql}) segments LIMIT %s", [args.sample_size])
        referenced_nodes = sorted(
            {
                int(segment["from_node_id"])
                for segment in segments
            }
            | {
                int(segment["to_node_id"])
                for segment in segments
            }
        )
        nodes = []
        if referenced_nodes:
            nodes = fetch_dicts(
                conn,
                f"SELECT * FROM ({nodes_sql}) nodes WHERE vertex_id = ANY(%s) ORDER BY vertex_id",
                [referenced_nodes],
            )

    report = exporter.validate_graph(nodes, segments, "graphhopper-db-export-smoke")
    report["smoke"] = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "sampleSize": args.sample_size,
        "databaseNodeCount": node_count,
        "databaseSegmentCount": segment_count,
        "sampleReferencedNodeCount": len(referenced_nodes),
        "sampleLoadedNodeCount": len(nodes),
        "sampleSegmentCount": len(segments),
    }
    exporter.write_json_report(args.report_json, report)

    if report["blockers"]:
        print(json.dumps(report["smoke"], ensure_ascii=False))
        print(f'GraphHopper DB export smoke failed: blockerCount={report["summary"]["blockerCount"]}')
        return 1

    print(
        "GraphHopper DB export smoke ok: "
        f"nodes={node_count}, segments={segment_count}, sampleSegments={len(segments)}, "
        f'warnings={report["summary"]["warningCount"]}'
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
