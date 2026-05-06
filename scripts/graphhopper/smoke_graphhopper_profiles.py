#!/usr/bin/env python3
import argparse
import importlib.util
import json
import os
import time
from datetime import datetime, timezone
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError


EXPORTER_PATH = "/usr/local/bin/export-postgis-to-osm.py"
DEFAULT_PROFILES = [
    "pedestrian_safe",
    "pedestrian_fast",
    "visual_safe",
    "visual_fast",
    "wheelchair_manual_safe",
    "wheelchair_manual_fast",
    "wheelchair_auto_safe",
    "wheelchair_auto_fast",
]


def load_exporter():
    spec = importlib.util.spec_from_file_location("export_postgis_to_osm", EXPORTER_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def fetch_candidates(conn, limit):
    sql = """
SELECT
  "edgeId" AS edge_id,
  "walkAccess"::text AS walk_access,
  "stairsState"::text AS stairs_state,
  COALESCE("lengthMeter", ST_Length("geom"::geography)) AS length_meter,
  ST_X(ST_StartPoint("geom"::geometry)) AS from_lon,
  ST_Y(ST_StartPoint("geom"::geometry)) AS from_lat,
  ST_X(ST_EndPoint("geom"::geometry)) AS to_lon,
  ST_Y(ST_EndPoint("geom"::geometry)) AS to_lat
FROM road_segments
WHERE COALESCE("walkAccess"::text, 'UNKNOWN') <> 'NO'
  AND "geom" IS NOT NULL
  AND NOT ST_IsEmpty("geom"::geometry)
  AND ST_NPoints("geom"::geometry) >= 2
  AND NOT (
    ST_X(ST_StartPoint("geom"::geometry)) = ST_X(ST_EndPoint("geom"::geometry))
    AND ST_Y(ST_StartPoint("geom"::geometry)) = ST_Y(ST_EndPoint("geom"::geometry))
  )
ORDER BY COALESCE("lengthMeter", ST_Length("geom"::geography)) DESC
LIMIT %s
"""
    with conn.cursor() as cur:
        cur.execute(sql, [limit])
        columns = [desc[0] for desc in cur.description]
        return [dict(zip(columns, row)) for row in cur.fetchall()]


def request_json(url, timeout):
    request = Request(url, headers={"Accept": "application/json"})
    with urlopen(request, timeout=timeout) as response:
        body = response.read().decode("utf-8")
        return response.status, json.loads(body)


def route_url(base_url, candidate, profile):
    query = urlencode(
        [
            ("profile", profile),
            ("point", f'{candidate["from_lat"]},{candidate["from_lon"]}'),
            ("point", f'{candidate["to_lat"]},{candidate["to_lon"]}'),
            ("points_encoded", "false"),
            ("locale", "ko-KR"),
        ]
    )
    return f"{base_url.rstrip('/')}/route?{query}"


def smoke_profile(base_url, candidate, profile, timeout):
    url = route_url(base_url, candidate, profile)
    started = time.monotonic()
    try:
        status, payload = request_json(url, timeout)
    except HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        return {
            "profile": profile,
            "status": "FAIL",
            "httpStatus": error.code,
            "error": body[:1000],
            "url": url,
        }
    except (TimeoutError, URLError) as error:
        return {
            "profile": profile,
            "status": "FAIL",
            "error": str(error),
            "url": url,
        }

    elapsed_ms = round((time.monotonic() - started) * 1000)
    paths = payload.get("paths") or []
    if status != 200 or not paths:
        return {
            "profile": profile,
            "status": "FAIL",
            "httpStatus": status,
            "elapsedMs": elapsed_ms,
            "error": "route response has no paths",
            "url": url,
        }

    path = paths[0]
    return {
        "profile": profile,
        "status": "PASS",
        "httpStatus": status,
        "elapsedMs": elapsed_ms,
        "distanceMeter": round(float(path.get("distance", 0.0)), 3),
        "timeMs": int(path.get("time", 0)),
        "url": url,
    }


def write_report(path, report):
    if not path:
        return
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as report_file:
        json.dump(report, report_file, ensure_ascii=False, indent=2, default=str)
        report_file.write("\n")


def profile_list(raw):
    if not raw:
        return DEFAULT_PROFILES
    return [item.strip() for item in raw.split(",") if item.strip()]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=os.getenv("GRAPHHOPPER_PROFILE_SMOKE_BASE_URL", "http://graphhopper:8989"))
    parser.add_argument("--candidate-limit", type=int, default=int(os.getenv("GRAPHHOPPER_PROFILE_SMOKE_CANDIDATE_LIMIT", "30")))
    parser.add_argument("--timeout-seconds", type=int, default=int(os.getenv("GRAPHHOPPER_PROFILE_SMOKE_TIMEOUT_SECONDS", "10")))
    parser.add_argument("--profiles", default=os.getenv("GRAPHHOPPER_PROFILE_SMOKE_PROFILES", ",".join(DEFAULT_PROFILES)))
    parser.add_argument("--report-json", default=os.getenv("GRAPHHOPPER_PROFILE_SMOKE_REPORT_FILE"))
    args = parser.parse_args()

    exporter = load_exporter()
    profiles = profile_list(args.profiles)
    with exporter.connect() as conn:
        candidates = fetch_candidates(conn, args.candidate_limit)

    if not candidates:
        report = {
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "status": "FAIL",
            "error": "no routeable road_segments candidate found",
            "profiles": profiles,
        }
        write_report(args.report_json, report)
        print("GraphHopper profile smoke failed: no routeable candidate")
        return 1

    attempts = []
    selected = None
    for candidate in candidates:
        results = [smoke_profile(args.base_url, candidate, profile, args.timeout_seconds) for profile in profiles]
        attempt = {
            "candidate": candidate,
            "results": results,
        }
        attempts.append(attempt)
        if all(result["status"] == "PASS" for result in results):
            selected = attempt
            break

    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "status": "PASS" if selected else "FAIL",
        "baseUrl": args.base_url,
        "profileCount": len(profiles),
        "candidateLimit": args.candidate_limit,
        "selectedCandidate": selected["candidate"] if selected else None,
        "results": selected["results"] if selected else attempts[-1]["results"],
        "attempts": attempts,
    }
    write_report(args.report_json, report)

    if not selected:
        print(f"GraphHopper profile smoke failed: profiles={','.join(profiles)}")
        return 1

    print(
        "GraphHopper profile smoke ok: "
        f"profiles={len(profiles)}, edgeId={selected['candidate']['edge_id']}, "
        f"lengthMeter={round(float(selected['candidate']['length_meter']), 3)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
