#!/usr/bin/env python3

import importlib.util
import os
from pathlib import Path
import unittest


ROOT_DIR = Path(__file__).resolve().parents[2]
CONFIG_PATH = ROOT_DIR / "AI" / "llm_test" / "server" / "config.py"


def load_config_module():
    spec = importlib.util.spec_from_file_location("ai_server_config_test", CONFIG_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class AiServerConfigTest(unittest.TestCase):
    def test_config_reads_runtime_values_from_env(self):
        old_env = os.environ.copy()
        try:
            os.environ["HOST"] = "127.0.0.1"
            os.environ["PORT"] = "8123"
            os.environ["DEBUG"] = "false"
            os.environ["DEFAULT_MODEL"] = "claude"
            module = load_config_module()

            self.assertEqual(module.Config.HOST, "127.0.0.1")
            self.assertEqual(module.Config.PORT, 8123)
            self.assertFalse(module.Config.DEBUG)
            self.assertEqual(module.Config.DEFAULT_MODEL, "claude")
        finally:
            os.environ.clear()
            os.environ.update(old_env)

    def test_config_falls_back_to_ai_host_and_ai_port(self):
        old_env = os.environ.copy()
        try:
            os.environ.pop("HOST", None)
            os.environ.pop("PORT", None)
            os.environ["AI_HOST"] = "0.0.0.0"
            os.environ["AI_PORT"] = "5001"
            module = load_config_module()

            self.assertEqual(module.Config.HOST, "0.0.0.0")
            self.assertEqual(module.Config.PORT, 5001)
        finally:
            os.environ.clear()
            os.environ.update(old_env)


if __name__ == "__main__":
    unittest.main()
