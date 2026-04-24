# Jenkins 운영 기준

`S1` Jenkins는 dev 배포 자동화를 담당한다. Jenkins 자체 설정 파일과 secret은 서버 로컬에만 두고 저장소에는 올리지 않는다.

## 현재 구성

- 외부 URL: `https://k14e102.p.ssafy.io/jenkins/`
- 인증: GitLab OAuth
- 권한: Matrix Authorization
- dev 배포 잡: `e102-dev-deploy`
- 대상 브랜치: `develop`
- 배포 대상: S1 dev Docker Compose stack

## `e102-dev-deploy`

처리 순서:

1. GitLab `develop` checkout
2. S1 서버의 `.env.dev`, S1 override compose 복사
3. `docker compose config --quiet`
4. `PostGIS`, `Redis`, `MinIO` 기동
5. backend image build 및 컨테이너 재생성
6. `/v3/api-docs` smoke test
7. compose 상태 출력

GraphHopper는 PostgreSQL LineString 기반 런타임 전환 전까지 dev 배포 잡에서 제외한다.

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
