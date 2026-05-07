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


def priority_multiplier_number(model, condition):
    value = priority_multiplier(model, condition)
    return None if value is None else float(value)


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

    def test_safe_profiles_avoid_accessibility_risks_more_than_fast_profiles(self):
        models = load_custom_models()
        profile_pairs = [
            ("pedestrian_safe", "pedestrian_fast"),
            ("visual_safe", "visual_fast"),
            ("wheelchair_auto_safe", "wheelchair_auto_fast"),
            ("wheelchair_manual_safe", "wheelchair_manual_fast"),
        ]
        risk_conditions = [
            "width_state == NARROW",
            "surface_state == UNPAVED",
            "slope_state == RISK",
        ]

        for safe_profile, fast_profile in profile_pairs:
            for condition in risk_conditions:
                with self.subTest(safe=safe_profile, fast=fast_profile, condition=condition):
                    self.assertLess(
                        priority_multiplier_number(models[safe_profile], condition),
                        priority_multiplier_number(models[fast_profile], condition),
                    )


if __name__ == "__main__":
    unittest.main()
