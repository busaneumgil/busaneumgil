#!/usr/bin/env python3
"""Verify that GraphHopper walk_access hot patch changes routing immediately.

This smoke test runs the same origin/destination route twice:
1. Before patch: the route must include the target `db_edge_id`.
2. After patch: the route must either avoid that `db_edge_id` or become unroutable.

The script restores the original walk_access value for the target edge unless
`--skip-restore` is used.
"""

from __future__ import annotations

import argparse
import json
import os
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


NO_ROUTE_MARKERS = (
    "connectionnotfoundexception",
    "connection between locations not found",
)


@dataclass
class HttpRequestError(Exception):
    status_code: int
    body: str
    payload: dict[str, Any]

    def __str__(self) -> str:
        return f"http {self.status_code}: {self.body or self.payload}"


def parse_json_body(body: str) -> dict[str, Any]:
    if not body:
        return {}
    try:
        parsed = json.loads(body)
    except json.JSONDecodeError:
        return {}
    return parsed if isinstance(parsed, dict) else {}


def request_json(method: str, url: str, payload: dict[str, Any] | None, timeout: int) -> tuple[int, dict[str, Any]]:
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    request = Request(
        url,
        data=body,
        method=method,
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
        },
    )
    try:
        with urlopen(request, timeout=timeout) as response:
            response_body = response.read().decode("utf-8")
            return response.status, json.loads(response_body) if response_body else {}
    except HTTPError as error:
        response_body = error.read().decode("utf-8", errors="replace")
        raise HttpRequestError(error.code, response_body, parse_json_body(response_body)) from error


def route_url(base_url: str, profile: str, from_lat: float, from_lng: float, to_lat: float, to_lng: float) -> str:
    query = urlencode(
        [
            ("profile", profile),
            ("point", f"{from_lat},{from_lng}"),
            ("point", f"{to_lat},{to_lng}"),
            ("points_encoded", "false"),
            ("locale", "ko-KR"),
            ("details", "db_edge_id"),
            ("details", "walk_access"),
        ]
    )
    return f"{base_url.rstrip('/')}/route?{query}"


def patch_url(base_url: str, edge_id: int) -> str:
    return f"{base_url.rstrip('/')}/ieum/admin/edges/{edge_id}/walk-access"


def extract_paths(payload: dict[str, Any]) -> list[dict[str, Any]]:
    return payload.get("paths") or []


def detail_rows(path: dict[str, Any], detail_name: str) -> list[list[Any]]:
    details = path.get("details") or {}
    rows = details.get(detail_name) or []
    return [row for row in rows if isinstance(row, list) and len(row) >= 3]


def detail_values(path: dict[str, Any], detail_name: str) -> list[str]:
    return [str(row[2]) for row in detail_rows(path, detail_name)]


def ranges_overlap(start_a: int, end_a: int, start_b: int, end_b: int) -> bool:
    return max(start_a, start_b) < min(end_a, end_b)


def resolve_original_walk_access(path: dict[str, Any], edge_id: int) -> str:
    target_ranges = [
        (int(row[0]), int(row[1]))
        for row in detail_rows(path, "db_edge_id")
        if str(row[2]) == str(edge_id)
    ]
    if not target_ranges:
        raise ValueError("before route does not include the target db_edge_id")

    overlapping_walk_access = {
        str(row[2])
        for row in detail_rows(path, "walk_access")
        for target_start, target_end in target_ranges
        if ranges_overlap(target_start, target_end, int(row[0]), int(row[1]))
    }
    if len(overlapping_walk_access) != 1:
        raise ValueError(
            "failed to resolve a single original walk_access value for the target db_edge_id: "
            + ", ".join(sorted(overlapping_walk_access))
        )
    return overlapping_walk_access.pop()


def is_no_route_error(error: HttpRequestError) -> bool:
    normalized_body = error.body.lower()
    if any(marker in normalized_body for marker in NO_ROUTE_MARKERS):
        return True
    payload_dump = json.dumps(error.payload).lower() if error.payload else ""
    return any(marker in payload_dump for marker in NO_ROUTE_MARKERS)


def write_report(report_json: str | None, report: dict[str, Any]) -> None:
    if not report_json:
        return
    os.makedirs(os.path.dirname(os.path.abspath(report_json)), exist_ok=True)
    with open(report_json, "w", encoding="utf-8") as file:
        json.dump(report, file, ensure_ascii=False, indent=2)
        file.write("\n")


def fail(message: str, report_json: str | None, report: dict[str, Any]) -> int:
    report["status"] = "FAIL"
    report["message"] = message
    write_report(report_json, report)
    print(message)
    return 1


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=os.getenv("GRAPHHOPPER_HOT_PATCH_SMOKE_BASE_URL", "http://graphhopper:8989"))
    parser.add_argument("--profile", default=os.getenv("GRAPHHOPPER_HOT_PATCH_SMOKE_PROFILE", "wheelchair_manual_safe"))
    parser.add_argument("--edge-id", type=int, required=True)
    parser.add_argument("--from-lat", type=float, required=True)
    parser.add_argument("--from-lng", type=float, required=True)
    parser.add_argument("--to-lat", type=float, required=True)
    parser.add_argument("--to-lng", type=float, required=True)
    parser.add_argument("--blocked-walk-access", default=os.getenv("GRAPHHOPPER_HOT_PATCH_SMOKE_BLOCKED_ACCESS", "NO"))
    parser.add_argument("--timeout-seconds", type=int, default=int(os.getenv("GRAPHHOPPER_HOT_PATCH_SMOKE_TIMEOUT_SECONDS", "10")))
    parser.add_argument("--report-json", default=os.getenv("GRAPHHOPPER_HOT_PATCH_SMOKE_REPORT_FILE"))
    parser.add_argument("--skip-restore", action="store_true")
    args = parser.parse_args()

    report: dict[str, Any] = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "baseUrl": args.base_url,
        "profile": args.profile,
        "edgeId": args.edge_id,
        "route": {
            "from": {"lat": args.from_lat, "lng": args.from_lng},
            "to": {"lat": args.to_lat, "lng": args.to_lng},
        },
    }

    before_url = route_url(args.base_url, args.profile, args.from_lat, args.from_lng, args.to_lat, args.to_lng)
    try:
        before_status, before_payload = request_json("GET", before_url, None, args.timeout_seconds)
    except (HttpRequestError, URLError, TimeoutError) as error:
        return fail(f"hot patch smoke failed before route request: {error}", args.report_json, report)

    before_paths = extract_paths(before_payload)
    report["before"] = {"httpStatus": before_status, "pathCount": len(before_paths)}
    if before_status != 200 or not before_paths:
        return fail("hot patch smoke failed: before route has no paths", args.report_json, report)

    before_edge_values = detail_values(before_paths[0], "db_edge_id")
    before_walk_access_values = detail_values(before_paths[0], "walk_access")
    report["before"]["dbEdgeIds"] = before_edge_values
    report["before"]["walkAccessValues"] = before_walk_access_values
    if str(args.edge_id) not in before_edge_values:
        return fail(
            "hot patch smoke failed: before route does not include the target db_edge_id; choose an OD that traverses the edge",
            args.report_json,
            report,
        )

    try:
        original_walk_access = resolve_original_walk_access(before_paths[0], args.edge_id)
    except ValueError as error:
        return fail(f"hot patch smoke failed: {error}", args.report_json, report)
    report["before"]["originalWalkAccess"] = original_walk_access

    patch_request = {"walkAccess": args.blocked_walk_access}
    try:
        patch_status, patch_payload = request_json(
            "PATCH",
            patch_url(args.base_url, args.edge_id),
            patch_request,
            args.timeout_seconds,
        )
    except (HttpRequestError, URLError, TimeoutError) as error:
        return fail(f"hot patch smoke failed during patch request: {error}", args.report_json, report)

    report["patch"] = {
        "httpStatus": patch_status,
        "request": patch_request,
        "response": patch_payload,
    }
    if patch_status != 200:
        return fail("hot patch smoke failed: patch endpoint did not return 200", args.report_json, report)

    return_code = 1
    restore_error: str | None = None
    after_url = route_url(args.base_url, args.profile, args.from_lat, args.from_lng, args.to_lat, args.to_lng)
    try:
        after_status, after_payload = request_json("GET", after_url, None, args.timeout_seconds)
        after_paths = extract_paths(after_payload)
        report["after"] = {"httpStatus": after_status, "pathCount": len(after_paths)}

        if after_status != 200:
            return fail("hot patch smoke failed: after route endpoint did not return 200", args.report_json, report)

        if not after_paths:
            report["status"] = "PASS"
            report["message"] = "hot patch smoke ok: route became unroutable after blocking target edge"
            return_code = 0
        else:
            after_edge_values = detail_values(after_paths[0], "db_edge_id")
            after_walk_access_values = detail_values(after_paths[0], "walk_access")
            report["after"]["dbEdgeIds"] = after_edge_values
            report["after"]["walkAccessValues"] = after_walk_access_values

            if str(args.edge_id) in after_edge_values:
                return fail("hot patch smoke failed: target db_edge_id is still present after patch", args.report_json, report)

            report["status"] = "PASS"
            report["message"] = "hot patch smoke ok: target db_edge_id disappeared from the route after patch"
            return_code = 0
    except HttpRequestError as error:
        report["after"] = {
            "httpStatus": error.status_code,
            "pathCount": 0,
            "errorBody": error.body,
            "errorPayload": error.payload,
        }
        if is_no_route_error(error):
            report["status"] = "PASS"
            report["message"] = "hot patch smoke ok: route became unroutable after blocking target edge"
            return_code = 0
        else:
            return fail(f"hot patch smoke failed after route request: {error}", args.report_json, report)
    except (URLError, TimeoutError) as error:
        return fail(f"hot patch smoke failed after route request: {error}", args.report_json, report)
    finally:
        if not args.skip_restore:
            try:
                request_json(
                    "PATCH",
                    patch_url(args.base_url, args.edge_id),
                    {"walkAccess": original_walk_access},
                    args.timeout_seconds,
                )
                report["restore"] = {"walkAccess": original_walk_access}
            except (HttpRequestError, URLError, TimeoutError) as error:
                restore_error = str(error)
                report["restoreError"] = restore_error

    if restore_error is not None:
        return fail(
            f"hot patch smoke failed while restoring original walk_access={original_walk_access}: {restore_error}",
            args.report_json,
            report,
        )

    write_report(args.report_json, report)
    print(report["message"])
    return return_code


if __name__ == "__main__":
    raise SystemExit(main())
