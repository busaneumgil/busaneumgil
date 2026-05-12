#!/usr/bin/env python3
"""모니터링 단순화 설정 회귀 테스트다.

이번 회귀 포인트는 두 가지다.
- S1 Prometheus가 dev backend를 host loopback 우회 경로가 아닌 dev network의 `backend:18080`으로 직접 scrape 해야 한다.
- prod 대시보드의 DB/Redis 카드는 실제 managed resource 상태가 아니라 backend dependency health라는 의미를 드러내야 한다.
"""

from pathlib import Path
import unittest


ROOT_DIR = Path(__file__).resolve().parents[2]
MONITORING_COMPOSE = ROOT_DIR / "INF" / "monitoring" / "s1" / "docker-compose.yml"
PROMETHEUS_CONFIG = ROOT_DIR / "INF" / "monitoring" / "s1" / "prometheus" / "prometheus.yml"
MONITORING_README = ROOT_DIR / "INF" / "monitoring" / "README.md"
PROD_DASHBOARD = ROOT_DIR / "INF" / "monitoring" / "s1" / "grafana" / "provisioning" / "dashboards" / "json" / "e102-prod-observability.json"


class MonitoringConfigsTest(unittest.TestCase):
    def test_prometheus_scrapes_dev_backend_on_dev_network(self):
        compose_content = MONITORING_COMPOSE.read_text(encoding="utf-8")
        prometheus_content = PROMETHEUS_CONFIG.read_text(encoding="utf-8")
        readme_content = MONITORING_README.read_text(encoding="utf-8")

        self.assertIn("dev-stack:", compose_content)
        self.assertIn("name: s14p31e102-dev_default", compose_content)
        self.assertIn('targets: ["backend:18080"]', prometheus_content)
        self.assertNotIn('targets: ["host.docker.internal:18080"]', prometheus_content)
        self.assertIn("backend:18080", readme_content)

    def test_redis_exporter_uses_dev_network_redis_by_default(self):
        compose_content = MONITORING_COMPOSE.read_text(encoding="utf-8")

        self.assertIn("REDIS_ADDR: ${DEV_REDIS_EXPORTER_ADDR:-redis://redis:6379}", compose_content)
        self.assertNotIn("redis://host.docker.internal:6379", compose_content)

    def test_prod_dashboard_explicitly_marks_dependency_health_cards(self):
        dashboard_content = PROD_DASHBOARD.read_text(encoding="utf-8")

        self.assertIn("DB 연결 상태", dashboard_content)
        self.assertIn("Redis 연결 상태", dashboard_content)
        self.assertIn("dependency health", dashboard_content)
        self.assertNotIn('"title": "DB 상태"', dashboard_content)
        self.assertNotIn('"title": "Redis 상태"', dashboard_content)


if __name__ == "__main__":
    unittest.main()
