#!/usr/bin/env python3
import importlib.util
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
EXPORT_SCRIPT = ROOT_DIR / "scripts" / "graphhopper" / "export_postgis_to_osm.py"


def load_export_module():
    spec = importlib.util.spec_from_file_location("export_postgis_to_osm", EXPORT_SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class GraphhopperExportTest(unittest.TestCase):

    def test_write_osm_exports_ieum_tags_for_all_segments(self):
        module = load_export_module()
        nodes = [
            {"vertex_id": 10, "lon": 128.1, "lat": 35.1},
            {"vertex_id": 20, "lon": 128.2, "lat": 35.2},
            {"vertex_id": 30, "lon": 128.3, "lat": 35.3},
        ]
        segments = [
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
                "slope_state": "FLAT",
                "width_state": "ADEQUATE_150",
                "surface_state": "PAVED",
                "stairs_state": "NO",
                "signal_state": "YES",
                "segment_type": "CROSS_WALK",
            },
        ]

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
        self.assertEqual(second_way_tags["ieum:surface_state"], "PAVED")
        self.assertNotIn("e102:edge_id", first_way_tags)
        self.assertNotIn("ieum:crossing_state", first_way_tags)


if __name__ == "__main__":
    unittest.main()
