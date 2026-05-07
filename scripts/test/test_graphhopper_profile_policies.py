#!/usr/bin/env python3
import json
import unittest
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
CUSTOM_MODEL_DIR = ROOT_DIR / "INF" / "graphhopper" / "custom_models"


def load_custom_models():
    return {
        model_path.stem: json.loads(model_path.read_text(encoding="utf-8"))
        for model_path in CUSTOM_MODEL_DIR.glob("*.json")
    }


def priority_multiplier(model, condition):
    for rule in model.get("priority", []):
        if rule.get("if") == condition:
            return str(rule.get("multiply_by"))
    return None


class GraphhopperProfilePolicyTest(unittest.TestCase):

    def test_walk_access_no_is_blocked_for_all_profiles(self):
        for profile_name, model in load_custom_models().items():
            with self.subTest(profile=profile_name):
                self.assertEqual(priority_multiplier(model, "walk_access == NO"), "0")

    def test_wheelchair_profiles_block_stairs(self):
        models = load_custom_models()
        wheelchair_profiles = [
            "wheelchair_auto_fast",
            "wheelchair_auto_safe",
            "wheelchair_manual_fast",
            "wheelchair_manual_safe",
        ]
        for profile_name in wheelchair_profiles:
            with self.subTest(profile=profile_name):
                self.assertEqual(priority_multiplier(models[profile_name], "stairs_state == YES"), "0.00")


if __name__ == "__main__":
    unittest.main()
