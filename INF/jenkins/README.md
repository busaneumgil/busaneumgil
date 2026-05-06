# Jenkins 운영 기준

`S1` Jenkins는 dev 배포 자동화를 담당한다. Jenkins 자체 설정 파일과 secret은 서버 로컬에만 두고 저장소에는 올리지 않는다.

## 현재 구성

- 외부 URL: `https://jenkins.busaneumgil.com/`
- dev API URL: `https://api.dev.busaneumgil.com/`
- dev AI URL: `https://ai.dev.busaneumgil.com/`
- 인증: GitLab OAuth
- 권한: Matrix Authorization
- dev 배포 잡: `e102-dev-deploy`
- 대상 브랜치: `develop`
- 배포 대상: S1 dev Docker Compose stack

## 설정 자산

S1 Jenkins의 기준 설정은 `INF/jenkins/s1`에서 관리한다.

- `docker-compose.yml`: Jenkins와 S1 nginx proxy compose
- `Dockerfile`: Jenkins Docker CLI/Compose plugin 포함 이미지
- `plugins.txt`: Jenkins 필수 plugin 목록
- `nginx.conf`: Jenkins, dev API, Grafana/PLG, SonarQube, Portainer host-based routing
GitLab OAuth Application에는 아래 Redirect URI가 등록되어 있어야 한다.

```text
https://jenkins.busaneumgil.com/securityRealm/finishLogin
```

Jenkins OAuth secret은 노션의 운영 env 기준을 확인한 뒤 S1 `/home/ubuntu/e102/jenkins/.env.jenkins`에 반영한다. GitLab Application 생성 기준은 `Docs/인프라/2026-04-29_운영도구_secret_관리_기준.md`를 따른다.

## 2026-04-29 반영 상태

- Jenkins는 `https://jenkins.busaneumgil.com/` 루트 경로로 접근한다.
- 과거 `/jenkins/` 경로는 루트로 redirect한다.
- dev backend는 `https://api.dev.busaneumgil.com/`로 접근한다.
- dev AI는 `https://ai.dev.busaneumgil.com/`로 접근한다.
- `api.dev.busaneumgil.com`, `ai.dev.busaneumgil.com` 인증서는 S1 host certbot standalone 방식으로 발급했고, S1 Docker nginx proxy에서 `/etc/letsencrypt`를 read-only mount해 사용한다.
- Docker nginx config는 bind mount이므로 `nginx.conf` 교체 후 `jenkins-proxy` 컨테이너를 recreate해야 한다.

```bash
cd /home/ubuntu/e102/jenkins
sudo docker compose up -d --force-recreate --no-deps jenkins-proxy
sudo docker exec e102-jenkins-proxy nginx -t
```

## Dev API Proxy

S1의 443 nginx proxy는 Jenkins와 dev backend, dev AI를 함께 라우팅한다.

- `https://jenkins.busaneumgil.com/`: Jenkins UI
- `https://api.dev.busaneumgil.com/`: S1 dev backend
- `https://ai.dev.busaneumgil.com/`: S1 dev AI

backend 원 포트는 dev Docker network 내부에서만 직접 접근하고, 외부에는 nginx 443 host-based routing으로만 공개한다. API 문서는 아래 주소로 확인한다.

```text
https://api.dev.busaneumgil.com/v3/api-docs
```

PostgreSQL은 HTTP reverse proxy 대상이 아니므로 `/db`로 열지 않는다. DB 접근이 필요하면 SSH tunnel 또는 SSM port forwarding을 사용한다.

## `e102-dev-deploy`

처리 순서:

1. GitLab `develop` checkout
2. Jenkins credential의 `.env.dev`, S1 override compose 복사
3. `docker compose config --quiet`
4. `PostGIS`, `Redis`, `MinIO`, `AI` 기동
5. GraphHopper graph-cache volume 확인
6. cache가 비어 있으면 `graphhopper-build` profile로 PostgreSQL LineString 기반 cache 생성
7. GraphHopper runtime 기동
8. backend image build 및 컨테이너 재생성
9. backend `/v3/api-docs`, GraphHopper `/healthcheck` smoke test
10. compose 상태 출력

GraphHopper는 S1 dev stack에 포함한다. runtime은 graph-cache serve only 구조이며, Jenkins dev pipeline은 cache가 비어 있을 때만 build job을 실행한다.

## Credentials

Jenkins job에서 사용하는 secret은 Jenkins Credentials로 관리한다.

| Credential ID | 종류 | 용도 | 상태 |
|---|---|---|---|
| `gitlab-pat` | Username/Password 또는 Secret text | GitLab repository checkout | 적용 완료 |
| `e102-dev-env-file` | Secret file | S1 dev 배포용 `.env.dev` | 적용 완료 |
| `e102-prod-env-file` | Secret file | prod 배포용 `.env.prod` | 적용 완료 |
| `e102-s2-host` | Secret text | S2 SSH host 또는 IP | 적용 완료 |
| `e102-s2-ssh-key` | SSH Username with private key | S2 배포 SSH 접속 | 적용 완료 |

`e102-dev-env-file`은 서버의 `/home/ubuntu/e102/.env.dev`를 기준으로 생성한다. Jenkins 컨테이너 재시작 시 `/var/jenkins_home/init.groovy.d/02-e102-env-credentials.groovy`가 credential을 다시 동기화한다.

prod 배포용 secret 원본은 S1 `/home/ubuntu/e102/prod-secrets` 하위에서 관리한다. Jenkins 컨테이너 재시작 시 `prod-deploy-credentials.groovy`가 `e102-prod-env-file`, `e102-s2-host`, `e102-s2-ssh-key`를 동기화한다.

## `e102-prod-deploy`

prod 배포 pipeline 기준 파일은 `INF/jenkins/pipelines/e102-prod-deploy.Jenkinsfile`이다.

처리 순서:

1. 지정 브랜치 checkout
2. workspace를 archive로 패키징
3. S2 `/home/ubuntu/e102/prod`로 코드와 `.env.prod` 업로드
4. `scripts/deploy/prod-deploy.sh` 또는 `scripts/deploy/prod-rollback.sh` 실행
5. `scripts/deploy/prod-smoke.sh`로 backend/AI/GraphHopper 상태 확인

파라미터:

| 파라미터 | 기본값 | 설명 |
|---|---:|---|
| `DEPLOY_BRANCH` | `master` | S2 prod에 배포할 브랜치 |
| `BUILD_GRAPHHOPPER` | `false` | PostgreSQL LineString에서 graph-cache를 새로 생성 |
| `DEPLOY_GRAPHHOPPER` | `false` | GraphHopper runtime까지 기동 |
| `ROLLBACK` | `false` | 이전 app image tag와 이전 graph-cache로 rollback |

초기 운영에서는 `BUILD_GRAPHHOPPER=false`, `DEPLOY_GRAPHHOPPER=false`로 backend/AI 배포만 먼저 안정화한다. `road_nodes`, `road_segments` 데이터 적재가 준비되면 GraphHopper 파라미터를 켠다.

## Webhook

Jenkins job에는 GitLab Push Hook 수신 트리거가 설정되어 있다.

- Jenkins endpoint: `https://jenkins.busaneumgil.com/project/e102-dev-deploy`
- 이벤트: Push events
- 브랜치 필터: `develop`
- Secret token: S1 서버 로컬 `/home/ubuntu/e102/.jenkins-webhook-secret`

GitLab 프로젝트 webhook 등록은 Maintainer 또는 Owner 권한이 필요하다. 현재 Jenkins 수신 endpoint는 실제 GitLab Push Hook 형태의 payload로 빌드 생성 및 `SUCCESS`까지 검증했다.

## Poll SCM

초기 운영에서는 GitLab webhook 권한 의존도를 줄이기 위해 Poll SCM을 함께 사용한다.

- 스케줄: `H/30 * * * *`
- 의미: 30분마다 `develop` 변경 여부 확인
- 동작: 변경이 있을 때만 `e102-dev-deploy` 빌드 실행
- 용도: GitLab webhook 등록 전까지의 기본 dev 자동 배포 경로

마감에 가까워져 GitLab Maintainer 권한과 webhook 설정이 정리되면 `develop` Push Hook 중심으로 전환한다.

## Backup

Jenkins home volume은 S1 로컬에서 매일 백업한다.

- 스크립트: `/home/ubuntu/e102/jenkins-backups/bin/jenkins-backup.sh`
- 백업 위치: `/home/ubuntu/e102/jenkins-backups/archives`
- 스케줄: `/etc/cron.d/e102-jenkins-backup`
- 실행 시간: UTC 18:30, KST 03:30
- 보관 기간: 14일

백업 archive에는 Jenkins credential과 secret material이 포함될 수 있으므로 root 전용 권한으로 관리한다.
