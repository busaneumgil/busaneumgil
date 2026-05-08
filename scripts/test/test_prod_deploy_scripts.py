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
PROD_DEPLOY = ROOT_DIR / "scripts" / "deploy" / "prod-deploy.sh"
PROD_ROLLBACK = ROOT_DIR / "scripts" / "deploy" / "prod-rollback.sh"


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


if __name__ == "__main__":
    unittest.main()
