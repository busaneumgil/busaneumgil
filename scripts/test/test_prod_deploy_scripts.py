#!/usr/bin/env python3
"""prod 배포 스크립트 회귀 테스트다.

Jenkins prod 배포에서 재현된 두 가지 회귀를 막는다.
- `.env.prod`를 직접 scp overwrite 하다 권한 오류가 나는 문제
- smoke 스크립트를 실행 비트에 의존해 직접 호출하다 Permission denied가 나는 문제
"""

from pathlib import Path
import unittest


ROOT_DIR = Path(__file__).resolve().parents[2]
AI_DOCKERFILE = ROOT_DIR / "AI" / "Dockerfile"
AI_DOCKERIGNORE = ROOT_DIR / "AI" / ".dockerignore"
ADMIN_DOCKERFILE = ROOT_DIR / "ADMIN" / "Dockerfile"
ADMIN_DOCKERIGNORE = ROOT_DIR / "ADMIN" / ".dockerignore"
ADMIN_NGINX_CONF = ROOT_DIR / "ADMIN" / "nginx.conf"
DEV_COMPOSE = ROOT_DIR / "docker-compose.dev.yml"
PROD_COMPOSE = ROOT_DIR / "docker-compose.prod.yml"
LOCAL_COMPOSE = ROOT_DIR / "docker-compose.local.yml"
JENKINSFILE = ROOT_DIR / "INF" / "jenkins" / "pipelines" / "e102-prod-deploy.Jenkinsfile"
DEV_JENKINSFILE = ROOT_DIR / "INF" / "jenkins" / "pipelines" / "e102-dev-deploy.Jenkinsfile"
DEV_JOB_BOOTSTRAP = ROOT_DIR / "INF" / "jenkins" / "s1" / "init.groovy.d" / "02-dev-deploy-job.groovy"
PROD_JOB_BOOTSTRAP = ROOT_DIR / "INF" / "jenkins" / "s1" / "init.groovy.d" / "03-prod-deploy-job.groovy"
PROD_CREDENTIAL_BOOTSTRAP = ROOT_DIR / "INF" / "jenkins" / "s1" / "init.groovy.d" / "prod-deploy-credentials.groovy"
JENKINS_README = ROOT_DIR / "INF" / "jenkins" / "README.md"
PROD_DEPLOY = ROOT_DIR / "scripts" / "deploy" / "prod-deploy.sh"
PROD_ROLLBACK = ROOT_DIR / "scripts" / "deploy" / "prod-rollback.sh"
PROD_SMOKE = ROOT_DIR / "scripts" / "deploy" / "prod-smoke.sh"
PROD_ADMIN_INGRESS = ROOT_DIR / "scripts" / "deploy" / "prod-admin-ingress.sh"
PROD_UP = ROOT_DIR / "scripts" / "make" / "docker" / "prod-up.sh"
PROD_UP_GRAPHHOPPER = ROOT_DIR / "scripts" / "make" / "docker" / "prod-up-graphhopper.sh"
PROD_GRAPHHOPPER_BOOTSTRAP = ROOT_DIR / "scripts" / "make" / "docker" / "prod-graphhopper-bootstrap.sh"
JENKINS_COMPOSE = ROOT_DIR / "INF" / "jenkins" / "s1" / "docker-compose.yml"


class ProdDeployScriptsTest(unittest.TestCase):
    def test_ai_dockerfile_targets_llm_server_instead_of_placeholder_app(self):
        content = AI_DOCKERFILE.read_text(encoding="utf-8")

        self.assertIn("llm_test/server", content)
        self.assertNotIn("COPY app.py ./", content)

    def test_ai_compose_passes_required_llm_runtime_env(self):
        dev_content = DEV_COMPOSE.read_text(encoding="utf-8")
        prod_content = PROD_COMPOSE.read_text(encoding="utf-8")
        local_content = LOCAL_COMPOSE.read_text(encoding="utf-8")

        self.assertIn("GMS_KEY", dev_content)
        self.assertIn("DEFAULT_MODEL", dev_content)
        self.assertIn("APP_ENV: dev", dev_content)
        self.assertIn("GMS_KEY", prod_content)
        self.assertIn("DEFAULT_MODEL", prod_content)
        self.assertIn("APP_ENV: prod", prod_content)
        self.assertIn("ADMIN_BACKEND_API_URL: ${VITE_BACKEND_API_URL:-https://api.busaneumgil.com}", prod_content)
        self.assertIn("GMS_KEY", local_content)
        self.assertIn("DEFAULT_MODEL", local_content)
        self.assertIn("APP_ENV: dev", local_content)
        self.assertNotIn("AI_PORT: ${AI_PORT:-5000}", dev_content)
        self.assertNotIn("AI_PORT: ${AI_PORT:-5000}", prod_content)
        self.assertNotIn("AI_PORT: ${AI_PORT:-5000}", local_content)

    def test_ai_healthchecks_validate_llm_identity_in_all_compose_variants(self):
        dev_content = DEV_COMPOSE.read_text(encoding="utf-8")
        prod_content = PROD_COMPOSE.read_text(encoding="utf-8")
        local_content = LOCAL_COMPOSE.read_text(encoding="utf-8")

        for content in (dev_content, prod_content, local_content):
            self.assertIn("POST /voice/analyze", content)
            self.assertIn("providers", content)

    def test_ai_dockerignore_excludes_local_env_and_desktop_artifacts(self):
        content = AI_DOCKERIGNORE.read_text(encoding="utf-8")

        self.assertIn(".env", content)
        self.assertIn(".DS_Store", content)

    def test_admin_dockerfile_builds_static_vite_app(self):
        content = ADMIN_DOCKERFILE.read_text(encoding="utf-8")
        nginx_content = ADMIN_NGINX_CONF.read_text(encoding="utf-8")

        self.assertIn("npm ci", content)
        self.assertIn("npm run build:prod", content)
        self.assertIn("ARG ADMIN_BACKEND_API_URL=https://api.busaneumgil.com", content)
        self.assertIn('VITE_BACKEND_API_URL="$ADMIN_BACKEND_API_URL"', content)
        self.assertIn("COPY --from=build /app/dist /usr/share/nginx/html", content)
        self.assertIn("try_files $uri $uri/ /index.html", nginx_content)

    def test_admin_dockerignore_excludes_local_artifacts_and_env(self):
        content = ADMIN_DOCKERIGNORE.read_text(encoding="utf-8")

        self.assertIn("node_modules/", content)
        self.assertIn("dist/", content)
        self.assertIn(".env", content)

    def test_jenkinsfile_uploads_prod_env_via_temp_file_then_rename(self):
        content = JENKINSFILE.read_text(encoding="utf-8")

        self.assertIn("file(credentialsId: 'e102-prod-env-file'", content)
        self.assertIn('"$PROD_ENV"', content)
        self.assertIn('.env.prod.upload', content)
        self.assertIn('mv -f .env.prod.upload .env.prod', content)
        self.assertIn("chmod +x scripts/deploy/*.sh", content)
        self.assertNotIn('/var/jenkins_home/prod-secrets/.env.prod', content)

    def test_jenkinsfile_uses_pipeline_params_for_remote_flags(self):
        content = JENKINSFILE.read_text(encoding="utf-8")

        self.assertIn('params.ROLLBACK', content)
        self.assertIn('params.BUILD_GRAPHHOPPER.toString()', content)
        self.assertIn('params.DEPLOY_GRAPHHOPPER.toString()', content)

    def test_prod_deploy_runs_smoke_via_bash(self):
        content = PROD_DEPLOY.read_text(encoding="utf-8")

        self.assertIn("build backend ai admin", content)
        self.assertIn("up -d backend ai admin", content)
        self.assertIn('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"', content)
        self.assertIn('require_env_value JWT_SECRET', content)
        self.assertIn('require_env_value VITE_BACKEND_API_URL', content)
        self.assertLess(content.index("build backend ai admin"), content.index('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"'))
        self.assertLess(content.index("up -d backend ai admin"), content.index('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"'))

    def test_prod_rollback_runs_smoke_via_bash(self):
        content = PROD_ROLLBACK.read_text(encoding="utf-8")

        self.assertIn('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"', content)
        self.assertIn('require_env_value JWT_SECRET', content)
        self.assertIn('docker image inspect "$admin_image"', content)
        self.assertIn('SMOKE_ADMIN="$smoke_admin"', content)
        self.assertLess(
            content.index('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"'),
            content.index('cp "$DEPLOY_STATE_DIR/previous-app-image" "$DEPLOY_STATE_DIR/current-app-image"'),
        )

    def test_prod_smoke_retries_backend_and_ai_checks(self):
        content = PROD_SMOKE.read_text(encoding="utf-8")

        self.assertIn('SMOKE_RETRIES="${SMOKE_RETRIES:-24}"', content)
        self.assertIn('SMOKE_DELAY_SECONDS="${SMOKE_DELAY_SECONDS:-5}"', content)
        self.assertIn('"providers"[[:space:]]*:', content)
        self.assertIn('"POST /voice/analyze"', content)
        self.assertIn("/voice/analyze", content)
        self.assertIn("400", content)
        self.assertIn('"success"[[:space:]]*:[[:space:]]*false', content)
        self.assertIn('"intent"[[:space:]]*:[[:space:]]*"unknown"', content)
        self.assertIn('wait_for_url "http://127.0.0.1:${SERVER_PORT}/v3/api-docs" "Backend"', content)
        self.assertIn('SMOKE_ADMIN="${SMOKE_ADMIN:-true}"', content)
        self.assertIn('if [ "$SMOKE_ADMIN" = "true" ]; then', content)
        self.assertIn('http://127.0.0.1:${ADMIN_PORT}/health', content)
        self.assertIn('http://127.0.0.1:${ADMIN_PORT}/', content)

    def test_prod_up_runs_prod_smoke_after_start(self):
        content = PROD_UP.read_text(encoding="utf-8")

        self.assertIn('up -d backend ai admin', content)
        self.assertIn('bash "$ROOT_DIR/scripts/deploy/prod-smoke.sh"', content)

    def test_prod_up_graphhopper_keeps_admin_runtime(self):
        content = PROD_UP_GRAPHHOPPER.read_text(encoding="utf-8")

        self.assertIn('up -d --force-recreate backend ai admin', content)
        self.assertIn('wait_for_admin_health "$(admin_port)"', content)

    def test_prod_graphhopper_bootstrap_applies_accessibility_features_before_graph_build(self):
        content = PROD_GRAPHHOPPER_BOOTSTRAP.read_text(encoding="utf-8")

        self.assertIn('"$ROOT_DIR/scripts/db/load_road_network_prod.sh"', content)
        self.assertIn('"$ROOT_DIR/scripts/db/load_accessibility_features_prod.sh" --apply', content)
        self.assertIn('"$ROOT_DIR/scripts/make/docker/graphhopper-prod-build.sh"', content)
        self.assertLess(
            content.index('"$ROOT_DIR/scripts/db/load_road_network_prod.sh"'),
            content.index('"$ROOT_DIR/scripts/db/load_accessibility_features_prod.sh" --apply'),
        )
        self.assertLess(
            content.index('"$ROOT_DIR/scripts/db/load_accessibility_features_prod.sh" --apply'),
            content.index('"$ROOT_DIR/scripts/make/docker/graphhopper-prod-build.sh"'),
        )

    def test_prod_jenkinsfile_collects_remote_logs_on_failure(self):
        content = JENKINSFILE.read_text(encoding="utf-8")

        self.assertIn('docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=160 backend ai admin || true', content)

    def test_prod_admin_ingress_configures_admin_domain(self):
        content = PROD_ADMIN_INGRESS.read_text(encoding="utf-8")

        self.assertIn("ADMIN_DOMAIN=\"${ADMIN_DOMAIN:-admin.busaneumgil.com}\"", content)
        self.assertIn("ADMIN_PORT=\"${ADMIN_PORT:-3001}\"", content)
        self.assertIn("certbot_args=(--nginx -d \"$ADMIN_DOMAIN\"", content)

    def test_dev_jenkinsfile_notifies_success_and_failure(self):
        content = DEV_JENKINSFILE.read_text(encoding="utf-8")

        self.assertIn('##### ✅ DEV 배포가 완료되었습니다.', content)
        self.assertIn('##### ❌ DEV 배포가 실패했습니다.', content)

    def test_dev_jenkinsfile_rebuilds_ai_and_smokes_voice_analyze_endpoint(self):
        content = DEV_JENKINSFILE.read_text(encoding="utf-8")

        self.assertIn("up -d --build", content)
        self.assertIn("file(credentialsId: 'e102-dev-env-file'", content)
        self.assertIn('cp "$E102_DEV_ENV" .env.dev', content)
        self.assertNotIn("/opt/e102-server/.env.dev", content)
        self.assertNotIn("-o /tmp/e102-ai-health.json", content)
        self.assertNotIn("-o /tmp/e102-ai-voice-analyze.json", content)
        self.assertIn("AI_HEALTH_BODY=", content)
        self.assertIn("AI_RESPONSE_BODY=", content)
        self.assertIn("/health", content)
        self.assertIn('"providers"[[:space:]]*:', content)
        self.assertIn("/voice/analyze", content)
        self.assertIn("400", content)
        self.assertIn('"success"[[:space:]]*:[[:space:]]*false', content)
        self.assertIn('"intent"[[:space:]]*:[[:space:]]*"unknown"', content)
        self.assertLess(content.index("up -d --build postgres redis minio minio-init ai"), content.index('AI_HEALTH_STATUS='))

    def test_dev_job_bootstrap_loads_pipeline_from_scm_instead_of_inline_script(self):
        content = DEV_JOB_BOOTSTRAP.read_text(encoding="utf-8")

        self.assertIn("CpsScmFlowDefinition", content)
        self.assertIn("GitSCM", content)
        self.assertIn("INF/jenkins/pipelines/e102-dev-deploy.Jenkinsfile", content)
        self.assertIn("*/develop", content)
        self.assertIn("gitlab-pat", content)
        self.assertNotIn("CpsFlowDefinition", content)

    def test_prod_job_bootstrap_loads_pipeline_from_scm_instead_of_inline_script(self):
        content = PROD_JOB_BOOTSTRAP.read_text(encoding="utf-8")

        self.assertIn("CpsScmFlowDefinition", content)
        self.assertIn("GitSCM", content)
        self.assertIn("INF/jenkins/pipelines/e102-prod-deploy.Jenkinsfile", content)
        self.assertIn("*/master", content)
        self.assertIn("gitlab-pat", content)
        self.assertNotIn("CpsFlowDefinition", content)

    def test_prod_credential_bootstrap_keeps_only_non_env_credentials(self):
        content = PROD_CREDENTIAL_BOOTSTRAP.read_text(encoding="utf-8")

        self.assertNotIn("e102-dev-env-file", content)
        self.assertNotIn("e102-prod-env-file", content)
        self.assertNotIn("upsertFileCredential", content)
        self.assertNotIn("FileCredentialsImpl", content)
        self.assertIn("e102-s2-host", content)
        self.assertIn("e102-s2-ssh-key", content)
        self.assertIn("e102-mattermost-webhook-url", content)

    def test_jenkins_readme_documents_env_credentials_as_source_of_truth(self):
        content = JENKINS_README.read_text(encoding="utf-8")

        self.assertIn("e102-dev-env-file", content)
        self.assertIn("e102-prod-env-file", content)
        self.assertIn("source of truth", content)
        self.assertNotIn("host mounted env file", content)
        self.assertNotIn("02-e102-env-credentials.groovy", content)

    def test_jenkins_container_does_not_mount_deploy_env_files(self):
        content = JENKINS_COMPOSE.read_text(encoding="utf-8")

        self.assertNotIn('/home/ubuntu/e102/.env.dev:/opt/e102-server/.env.dev', content)
        self.assertNotIn('/home/ubuntu/e102/prod-secrets/.env.prod:/opt/e102-server/.env.prod', content)

    def test_init_groovy_does_not_overwrite_env_file_credentials(self):
        content = PROD_CREDENTIAL_BOOTSTRAP.read_text(encoding="utf-8")

        self.assertNotIn('e102-dev-env-file', content)
        self.assertNotIn('e102-prod-env-file', content)
        self.assertNotIn('FileCredentialsImpl', content)

    def test_compose_passes_cors_allowed_origins_to_backend(self):
        for compose_file in (DEV_COMPOSE, LOCAL_COMPOSE, PROD_COMPOSE):
            with self.subTest(compose_file=compose_file.name):
                content = compose_file.read_text(encoding="utf-8")
                self.assertIn('CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-', content)


if __name__ == "__main__":
    unittest.main()
