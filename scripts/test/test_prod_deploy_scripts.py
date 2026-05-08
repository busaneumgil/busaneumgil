#!/usr/bin/env python3
"""prod 배포 스크립트 회귀 테스트다.

Jenkins prod 배포에서 재현된 두 가지 회귀를 막는다.
- `.env.prod`를 직접 scp overwrite 하다 권한 오류가 나는 문제
- smoke 스크립트를 실행 비트에 의존해 직접 호출하다 Permission denied가 나는 문제
"""

from pathlib import Path
import unittest


ROOT_DIR = Path(__file__).resolve().parents[2]
JENKINSFILE = ROOT_DIR / "INF" / "jenkins" / "pipelines" / "e102-prod-deploy.Jenkinsfile"
DEV_JENKINSFILE = ROOT_DIR / "INF" / "jenkins" / "pipelines" / "e102-dev-deploy.Jenkinsfile"
PROD_DEPLOY = ROOT_DIR / "scripts" / "deploy" / "prod-deploy.sh"
PROD_ROLLBACK = ROOT_DIR / "scripts" / "deploy" / "prod-rollback.sh"
PROD_SMOKE = ROOT_DIR / "scripts" / "deploy" / "prod-smoke.sh"
PROD_UP = ROOT_DIR / "scripts" / "make" / "docker" / "prod-up.sh"


class ProdDeployScriptsTest(unittest.TestCase):
    def test_jenkinsfile_uploads_prod_env_via_temp_file_then_rename(self):
        content = JENKINSFILE.read_text(encoding="utf-8")

        self.assertIn('.env.prod.upload', content)
        self.assertIn('mv -f .env.prod.upload .env.prod', content)
        self.assertIn("chmod +x scripts/deploy/*.sh", content)
        self.assertNotIn('scp -i "$S2_KEY" -o StrictHostKeyChecking=accept-new "$PROD_ENV" "$S2_USER@$S2_HOST:$REMOTE_DIR/.env.prod"', content)

    def test_jenkinsfile_uses_pipeline_params_for_remote_flags(self):
        content = JENKINSFILE.read_text(encoding="utf-8")

        self.assertIn('params.ROLLBACK', content)
        self.assertIn('params.BUILD_GRAPHHOPPER.toString()', content)
        self.assertIn('params.DEPLOY_GRAPHHOPPER.toString()', content)

    def test_prod_deploy_runs_smoke_via_bash(self):
        content = PROD_DEPLOY.read_text(encoding="utf-8")

        self.assertIn('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"', content)
        self.assertIn('require_env_value JWT_SECRET', content)

    def test_prod_rollback_runs_smoke_via_bash(self):
        content = PROD_ROLLBACK.read_text(encoding="utf-8")

        self.assertIn('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"', content)
        self.assertIn('require_env_value JWT_SECRET', content)
        self.assertLess(
            content.index('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"'),
            content.index('cp "$DEPLOY_STATE_DIR/previous-app-image" "$DEPLOY_STATE_DIR/current-app-image"'),
        )

    def test_prod_smoke_retries_backend_and_ai_checks(self):
        content = PROD_SMOKE.read_text(encoding="utf-8")

        self.assertIn('SMOKE_RETRIES="${SMOKE_RETRIES:-24}"', content)
        self.assertIn('SMOKE_DELAY_SECONDS="${SMOKE_DELAY_SECONDS:-5}"', content)
        self.assertIn('wait_for_url "http://127.0.0.1:${AI_PORT}/health" "AI"', content)
        self.assertIn('wait_for_url "http://127.0.0.1:${SERVER_PORT}/v3/api-docs" "Backend"', content)

    def test_prod_up_runs_prod_smoke_after_start(self):
        content = PROD_UP.read_text(encoding="utf-8")

        self.assertIn('up -d backend ai', content)
        self.assertIn('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"', content)

    def test_prod_jenkinsfile_collects_remote_logs_on_failure(self):
        content = JENKINSFILE.read_text(encoding="utf-8")

        self.assertIn('docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=160 backend ai || true', content)

    def test_dev_jenkinsfile_notifies_success_and_failure(self):
        content = DEV_JENKINSFILE.read_text(encoding="utf-8")

        self.assertIn('##### ✅ DEV 배포가 완료되었습니다.', content)
        self.assertIn('##### ❌ DEV 배포가 실패했습니다.', content)


if __name__ == "__main__":
    unittest.main()
