#!/usr/bin/env python3
import importlib.util
import json
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
EXPORT_SCRIPT = ROOT_DIR / "scripts" / "graphhopper" / "export_postgis_to_osm.py"
CUSTOM_MODEL_DIR = ROOT_DIR / "INF" / "graphhopper" / "custom_models"


def load_export_module():
    spec = importlib.util.spec_from_file_location("export_postgis_to_osm", EXPORT_SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class GraphhopperExportTest(unittest.TestCase):

    def sample_nodes(self):
        return [
            {"vertex_id": 10, "lon": 128.1, "lat": 35.1},
            {"vertex_id": 20, "lon": 128.2, "lat": 35.2},
            {"vertex_id": 30, "lon": 128.3, "lat": 35.3},
        ]

    def sample_segments(self):
        return [
            {
                "edge_id": 1,
                "from_node_id": 10,
                "to_node_id": 20,
                "geom_wkt": "LINESTRING(128.1 35.1, 128.15 35.15, 128.2 35.2)",
                "walk_access": "NO",
                "avg_slope_percent": None,
                "width_meter": "",
                "braille_block_state": "UNKNOWN",
                "audio_signal_state": "UNKNOWN",
                "slope_state": "UNKNOWN",
                "width_state": "UNKNOWN",
                "surface_state": "UNKNOWN",
                "stairs_state": "UNKNOWN",
                "signal_state": "UNKNOWN",
                "segment_type": "SIDE_LINE",
            },
            {
                "edge_id": 2,
                "from_node_id": 20,
                "to_node_id": 30,
                "geom_wkt": "LINESTRING(128.2 35.2, 128.3 35.3)",
                "walk_access": "YES",
                "avg_slope_percent": "1.2",
                "width_meter": "3.0",
                "braille_block_state": "UNKNOWN",
                "audio_signal_state": "UNKNOWN",
                "slope_state": "RISK",
                "width_state": "ADEQUATE_120",
                "surface_state": "PAVED",
                "stairs_state": "NO",
                "signal_state": "YES",
                "segment_type": "CROSS_WALK",
            },
        ]

    def test_write_osm_exports_ieum_tags_for_all_segments(self):
        module = load_export_module()
        nodes = self.sample_nodes()
        segments = self.sample_segments()

        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "road-network.osm"
            module.write_osm(nodes, segments, output)
            root = ET.parse(output).getroot()

        ways = root.findall("way")
        self.assertEqual(len(ways), 2)
        first_way_tags = {tag.attrib["k"]: tag.attrib["v"] for tag in ways[0].findall("tag")}
        second_way_tags = {tag.attrib["k"]: tag.attrib["v"] for tag in ways[1].findall("tag")}

        self.assertEqual(first_way_tags["highway"], "footway")
        self.assertEqual(first_way_tags["foot"], "yes")
        self.assertEqual(first_way_tags["oneway"], "no")
        self.assertEqual(first_way_tags["ieum:walk_access"], "NO")
        self.assertEqual(first_way_tags["ieum:avg_slope_percent"], "0.0")
        self.assertEqual(first_way_tags["ieum:width_meter"], "0.0")
        self.assertEqual(first_way_tags["ieum:segment_type"], "SIDE_LINE")
        self.assertEqual(second_way_tags["ieum:segment_type"], "CROSS_WALK")
        self.assertEqual(second_way_tags["ieum:slope_state"], "RISK")
        self.assertEqual(second_way_tags["ieum:width_state"], "ADEQUATE_120")
        self.assertEqual(second_way_tags["ieum:surface_state"], "PAVED")
        self.assertNotIn("e102:edge_id", first_way_tags)
        self.assertNotIn("ieum:crossing_state", first_way_tags)

    def test_validate_graph_reports_pass_with_unknown_warnings(self):
        module = load_export_module()

        report = module.validate_graph(self.sample_nodes(), self.sample_segments(), "road-network.osm")

        self.assertEqual(report["status"], "PASS")
        self.assertEqual(report["summary"]["nodeCount"], 3)
        self.assertEqual(report["summary"]["segmentCount"], 2)
        self.assertEqual(report["summary"]["routeableEdgeCount"], 1)
        self.assertEqual(report["summary"]["blockerCount"], 0)
        self.assertEqual(report["enumCounts"]["segment_type"]["CROSS_WALK"], 1)
        self.assertTrue(any(warning["kind"] == "high_unknown_ratio" for warning in report["warnings"]))

    def test_validate_graph_blocks_bad_topology_and_enum(self):
        module = load_export_module()
        bad_segments = self.sample_segments()
        bad_segments[1] = {
            **bad_segments[1],
            "to_node_id": 999,
            "geom_wkt": "LINESTRING(128.2 35.2, 128.21 35.21)",
            "surface_state": "YES",
        }

        report = module.validate_graph(self.sample_nodes(), bad_segments, "road-network.osm")

        blocker_kinds = {blocker["kind"] for blocker in report["blockers"]}
        self.assertEqual(report["status"], "FAIL")
        self.assertIn("missing_node_reference", blocker_kinds)
        self.assertIn("enum_violation", blocker_kinds)

    def test_write_json_report_creates_parent_directory(self):
        module = load_export_module()
        report = module.validate_graph(self.sample_nodes(), self.sample_segments(), "road-network.osm")

        with tempfile.TemporaryDirectory() as directory:
            report_path = Path(directory) / "validation" / "report.json"
            module.write_json_report(report_path, report)
            parsed = json.loads(report_path.read_text(encoding="utf-8"))

        self.assertEqual(parsed["status"], "PASS")

    def test_custom_models_use_canonical_accessibility_enums(self):
        allowed_width_conditions = {
            "width_state == ADEQUATE_120",
            "width_state == NARROW",
            "width_state == UNKNOWN",
        }
        for model_path in CUSTOM_MODEL_DIR.glob("*.json"):
            model = json.loads(model_path.read_text(encoding="utf-8"))
            conditions = [
                priority_rule.get("if", "")
                for priority_rule in model.get("priority", [])
            ]
            joined_conditions = "\n".join(conditions)

            self.assertIn("slope_state == RISK", joined_conditions, model_path.name)
            width_conditions = {
                condition for condition in conditions
                if condition.startswith("width_state == ")
            }
            self.assertTrue(width_conditions <= allowed_width_conditions, model_path.name)

            if "wheelchair" in model_path.name or "visual_safe" in model_path.name:
                self.assertIn("width_state == ADEQUATE_120", joined_conditions, model_path.name)
            self.assertIn("width_state == NARROW", joined_conditions, model_path.name)


if __name__ == "__main__":
    unittest.main()
