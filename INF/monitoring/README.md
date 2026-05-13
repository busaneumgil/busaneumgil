# S1 운영도구 설정

`INF/monitoring/s1`은 S1 서버에서 운영하는 보조 운영도구 stack의 기준 설정이다.

## 구성

- `Portainer`: 외부 공개 없이 SSH 터널 전용으로 접근한다.
- `Grafana/Prometheus/Loki/Promtail`: PLG 기반 보조 모니터링이다.
- `SonarQube`: 코드 품질 점검용 운영도구다.

## S1 / S2 역할

- `S1`
  - `Grafana/Prometheus/Loki/Promtail/node-exporter/cadvisor` 운영도구 stack을 유지한다.
  - `redis-exporter`를 함께 띄워 `dev redis` 상태와 메모리 사용량을 본다.
  - `blackbox-exporter`를 함께 띄워 `dev/prod` 공개 health endpoint를 점검한다.
  - `dev backend`는 내부 관리 포트 `18080`에서 `/actuator/health`, `/actuator/prometheus`를 노출하고, S1 Prometheus는 `s14p31e102-dev_default` 네트워크를 통해 `backend:18080`을 직접 scrape 한다.
- `S2`
  - `prod backend`도 동일한 actuator endpoint를 내부 관리 포트 `18080`에서 운영 관측용으로 노출한다.
  - `prod` 상태 카드는 `api/ai/admin`과 backend가 공개하는 `db/redis` dependency health endpoint를 S1 blackbox-exporter가 검사한다.
  - `prod log`는 `INF/monitoring/s2` 템플릿을 사용해 `S2 promtail -> S1 Loki` 경로로 수집한다.
  - 현재 운영 반영 기준으로는 `https://plg.busaneumgil.com/loki/api/v1/push` 경로를 사용하며, 이 ingress는 `S2` IP만 허용한다.
  - `S2 prod`의 상세 JVM/Hikari metric은 아직 S1에서 직접 scrape 하지 않는다.

## 인증 기준

- `Grafana`: GitLab OAuth를 compose 환경변수로 활성화한다.
- `SonarQube`: GitLab OAuth 설정을 SonarQube Settings API로 적용한다.
- `Portainer`: GitLab OAuth를 붙이지 않고 SSH 터널 접근으로 제한한다.

GitLab OAuth Application에는 아래 Redirect URI를 등록해야 한다.

```text
https://grafana.busaneumgil.com/login/gitlab
https://sonarqube.busaneumgil.com/oauth2/callback/gitlab
```

Jenkins OAuth까지 같은 Application으로 묶을 경우 아래 URI도 함께 등록한다.

```text
https://jenkins.busaneumgil.com/securityRealm/finishLogin
```

## 배포 기준

S1 서버의 `/home/ubuntu/e102/ops`에 이 폴더 내용을 배치한 뒤 실행한다.

```bash
docker network create e102-ops || true
docker compose --env-file ./.env up -d
./scripts/apply-sonarqube-oauth.sh
```

운영 기준으로는 Jenkins `e102-monitoring-deploy` 잡에서 `scripts/deploy/s1-monitoring-sync.sh`를 실행해 아래를 함께 맞춘다.

- `INF/monitoring/s1/**` -> `/home/ubuntu/e102/ops`
- `INF/jenkins/s1/nginx.conf` -> `/home/ubuntu/e102/jenkins/nginx.conf`
- monitoring stack 재적용
- 필요 시 `e102-jenkins-proxy` 재시작

`s14p31e102-dev_default` 네트워크가 아직 없다면, sync 스크립트가 bootstrap network를 먼저 만들고 monitoring stack을 올린다. 이 bootstrap network는 dev compose가 같은 이름으로 다시 합류할 수 있도록 compose label을 같이 붙인다.

운영도구 env 값은 노션의 운영 env 기준을 확인한 뒤 S1 `/home/ubuntu/e102/ops/.env`로 배포한다.

Secret 위치와 GitLab Application 생성 기준은 `Docs/인프라/2026-04-29_운영도구_secret_관리_기준.md`를 따른다.

## Backend metric 연동 기준

- Spring Boot backend는 `actuator + Prometheus registry`를 사용한다.
- actuator는 앱 공개 포트와 분리된 내부 관리 포트(`MANAGEMENT_SERVER_PORT`, 기본 `18080`)로 노출한다.
- 기본 metric endpoint:
  - `/actuator/health`
  - `/actuator/prometheus`
- 현재 Prometheus scrape 대상:
  - `prometheus`
  - `node-exporter`
  - `cadvisor`
  - `redis-exporter`
  - `s1-dev-backend`
  - `blackbox-dev-http`
  - `blackbox-prod-http`
- 현재 단계에서 Grafana는 `dev backend`, `dev redis`, `S1 host/container`, `prod 공개 health`, `S2 prod log`를 2차 조회 용도로 사용한다.
- Grafana datasource:
  - `Prometheus`: `dev metric`
  - `Loki`: `dev/prod log`
- Grafana provisioning은 `INF/monitoring/s1/grafana/provisioning/dashboards` 기준으로 자동 반영한다.
- Promtail은 `environment`, `runtime_stack`, `compose_service`, `service_name`, `container` 라벨을 붙여 Loki 조회 기준을 통일한다.
- 가능한 경우 `level=...` 패턴을 추출해 Grafana 로그 색상과 오류 필터링에 활용한다.

## Grafana 대시보드 기준

- `dev` 기본 대시보드 이름은 `E102 운영 관측 개요 - Dev`다.
- `prod` 전용 대시보드는 `E102 운영 관측 개요 - Prod`다.
- 패널명과 안내 문구는 한국어 중심으로 유지한다.
- 상단 순서는 아래 흐름을 따른다.
  - `서비스/의존성 상태`
  - `최근 로그 건수`
  - `경고/오류 로그`
- `dev` 대시보드 변수:
  - `메트릭 서비스`: `backend`, `ai`, `graphhopper`, `postgres`, `redis`, `minio`, `admin`
  - `로그 서비스`: `backend`, `ai`, `graphhopper`, `admin`
- `prod` 대시보드 변수:
  - `로그 서비스`: `backend`, `ai`, `graphhopper`, `admin`
- 현재 제약:
  - `dev` overview는 `dev` 전용으로 고정하고, `prod`는 별도 dashboard로 분리한다.
  - `prod`의 `DB 연결 상태`, `Redis 연결 상태`는 RDS/ElastiCache 자체 상태가 아니라 backend dependency health를 의미한다.
  - `prod`의 `GraphHopper 상태` 카드는 Redis active slot 기준의 `graphhopper` probe와 슬롯별 원시 `graphhopper-blue`, `graphhopper-green` probe 결과를 함께 본다.
  - 대시보드 로그 패널은 기본적으로 `warning 이상`과 `exception/timeout/failed` 같은 장애 단서를 우선 보여준다.
  - `prod log`는 S2 promtail 배치 후 같은 Grafana에서 즉시 조회 가능하다.
  - `prod`의 상세 JVM/Hikari 지표는 별도 private scrape를 열기 전까지 카드로 노출하지 않는다.

## S2 prod 로그 수집 기준

- `INF/monitoring/s2/docker-compose.yml`을 S2 `/home/ubuntu/e102/monitoring` 기준으로 배치한다.
- `LOKI_PUSH_URL`은 우선 `S1 private IP` 또는 내부 전용 주소를 사용한다.
  - 예시: `http://<s1-private-ip>:3100/loki/api/v1/push`
- private 경로를 바로 열 수 없는 경우 현재 운영 반영 기준으로는 아래 ingress를 사용한다.
  - `https://plg.busaneumgil.com/loki/api/v1/push`
  - 이 경로는 `S2` IP만 허용한다.
- promtail은 Docker stdout/stderr를 읽어 `environment=prod`, `runtime_stack=s2-prod` 라벨로 보낸다.
- 이 경로는 `로그 2차 조회` 목적이다.

## 2026-04-29 반영 상태

- Grafana: `https://grafana.busaneumgil.com/`에서 GitLab OAuth 로그인 적용
- SonarQube: `https://sonarqube.busaneumgil.com/`에서 GitLab OAuth 로그인 적용
- GitLab group 제한은 실제 OAuth group claim 기준으로 `ssafy_14th`를 사용한다.
- PLG 보조 조회는 `https://plg.busaneumgil.com/`로 Grafana에 연결한다.
- Portainer는 `https://portainer.busaneumgil.com/`에서 404를 반환하고, SSH 터널 전용으로만 접근한다.

```bash
make portainer-tunnel
```

접속 주소:

```text
http://localhost:19000
```
