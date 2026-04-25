# Jenkins 운영 기준

`S1` Jenkins는 dev 배포 자동화를 담당한다. Jenkins 자체 설정 파일과 secret은 서버 로컬에만 두고 저장소에는 올리지 않는다.

## 현재 구성

- 외부 URL: `https://k14e102.p.ssafy.io/jenkins/`
- dev API URL: `https://k14e102.p.ssafy.io/api/`
- 인증: GitLab OAuth
- 권한: Matrix Authorization
- dev 배포 잡: `e102-dev-deploy`
- 대상 브랜치: `develop`
- 배포 대상: S1 dev Docker Compose stack

## Dev API Proxy

S1의 443 nginx proxy는 Jenkins와 dev backend를 함께 라우팅한다.

- `/jenkins/`: Jenkins UI
- `/api/`: S1 dev backend

backend 원 포트는 `127.0.0.1:8080`에만 바인딩하고, 외부에는 nginx 443 경로로만 공개한다. `/api/` proxy는 backend의 root path로 전달하므로 API 문서는 아래 주소로 확인한다.

```text
https://k14e102.p.ssafy.io/api/v3/api-docs
```

PostgreSQL은 HTTP reverse proxy 대상이 아니므로 `/db`로 열지 않는다. DB 접근이 필요하면 SSH tunnel 또는 SSM port forwarding을 사용한다.

## `e102-dev-deploy`

처리 순서:

1. GitLab `develop` checkout
2. Jenkins credential의 `.env.dev`, S1 override compose 복사
3. `docker compose config --quiet`
4. `PostGIS`, `Redis`, `MinIO` 기동
5. backend image build 및 컨테이너 재생성
6. `/v3/api-docs` smoke test
7. compose 상태 출력

GraphHopper는 PostgreSQL LineString 기반 런타임 전환 전까지 dev 배포 잡에서 제외한다.

## Credentials

Jenkins job에서 사용하는 secret은 Jenkins Credentials로 관리한다.

| Credential ID | 종류 | 용도 | 상태 |
|---|---|---|---|
| `gitlab-pat` | Username/Password 또는 Secret text | GitLab repository checkout | 적용 완료 |
| `e102-dev-env-file` | Secret file | S1 dev 배포용 `.env.dev` | 적용 완료 |
| `e102-prod-env-file` | Secret file | prod 배포용 `.env.prod` | `.env.prod` 생성 후 적용 |

`e102-dev-env-file`은 서버의 `/home/ubuntu/e102/.env.dev`를 기준으로 생성한다. Jenkins 컨테이너 재시작 시 `/var/jenkins_home/init.groovy.d/02-e102-env-credentials.groovy`가 credential을 다시 동기화한다.

prod 배포를 시작할 때는 서버에 `/home/ubuntu/e102/.env.prod`를 만들고 Jenkins 컨테이너를 재시작하면 `e102-prod-env-file`도 같은 방식으로 생성된다.

## Webhook

Jenkins job에는 GitLab Push Hook 수신 트리거가 설정되어 있다.

- Jenkins endpoint: `https://k14e102.p.ssafy.io/jenkins/project/e102-dev-deploy`
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
