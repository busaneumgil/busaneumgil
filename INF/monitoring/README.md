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
  - `dev backend`는 내부 관리 포트 `18080`에서 `/actuator/health`, `/actuator/prometheus`를 노출하고, S1 Prometheus가 `host.docker.internal:18080` 경로로 scrape 한다.
- `S2`
  - `prod backend`도 동일한 actuator endpoint를 내부 관리 포트 `18080`에서 운영 관측용으로 노출한다.
  - `prod metric`은 현재 `CloudWatch`를 1차 운영 알람 기준으로 두고, `S1`의 PLG stack에서는 2차 조회 관점으로만 다룬다.
  - `prod log`는 `INF/monitoring/s2` 템플릿을 사용해 `S2 promtail -> S1 Loki` 경로로 수집한다.
  - `S2 prod` metric을 `S1`에서 직접 보려면 별도 private scrape 경로 또는 agent 기반 forwarding 설계가 필요하다.

## 인증 기준

- `Grafana`: GitLab OAuth를 compose 환경변수로 활성화한다.
- `Grafana CloudWatch datasource`: `.env.ops`의 AWS key 또는 IAM role 기준으로 활성화한다.
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
- 현재 단계에서 Grafana는 `dev backend`, `dev redis`, `S1 host/container`, `S2 prod log`를 2차 조회 용도로 사용한다.
- Grafana datasource:
  - `Prometheus`: `dev metric`
  - `Loki`: `dev/prod log`
  - `CloudWatch`: `prod infra metric`
- Grafana provisioning은 `INF/monitoring/s1/grafana/provisioning/dashboards` 기준으로 자동 반영한다.
- Promtail은 `environment`, `runtime_stack`, `compose_service`, `service_name`, `container` 라벨을 붙여 Loki 조회 기준을 통일한다.
- 가능한 경우 `level=...` 패턴을 추출해 Grafana 로그 색상과 오류 필터링에 활용한다.
- `prod`는 bootstrap/운영 전환 과정이 끝날 때까지 `CloudWatch`를 1차 기준으로 유지한다.

## Grafana 대시보드 기준

- `dev` 기본 대시보드 이름은 `E102 운영 관측 개요 - Dev`다.
- `prod` 전용 대시보드는 `E102 운영 관측 개요 - Prod`다.
- 패널명과 안내 문구는 한국어 중심으로 유지한다.
- 상단 순서는 아래 흐름을 따른다.
  - `스크레이프 상태`
  - `HTTP 요청량 / 5xx / p95`
  - `JVM / Hikari / Redis`
  - `컨테이너 CPU / 메모리 / 재시작`
  - `호스트 디스크 / 네트워크`
  - `경고/오류 로그`
- `dev` 대시보드 변수:
  - `메트릭 서비스`: `backend`, `ai`, `graphhopper`, `postgres`, `redis`, `minio`, `admin`
  - `로그 서비스`: `backend`, `ai`, `graphhopper`, `admin`
- `prod` 대시보드 변수:
  - `로그 서비스`: `backend`, `ai`, `graphhopper`, `admin`
  - `AWS 리전`: 현재 `ap-northeast-2`
- 현재 제약:
  - `dev` overview는 `dev` 전용으로 고정하고, `prod`는 별도 dashboard로 분리한다.
  - 대시보드 로그 패널은 기본적으로 `warning 이상`과 `exception/timeout/failed` 같은 장애 단서를 우선 보여준다.
  - `prod log`는 S2 promtail 배치 후 같은 Grafana에서 즉시 조회 가능하다.
  - `prod infra metric`은 CloudWatch datasource가 살아 있어야 보인다.

## S2 prod 로그 수집 기준

- `INF/monitoring/s2/docker-compose.yml`을 S2 `/home/ubuntu/e102/monitoring` 기준으로 배치한다.
- `LOKI_PUSH_URL`은 S1 Loki가 닿는 비공개 주소를 사용한다.
  - 예시: `http://<s1-private-ip>:3100/loki/api/v1/push`
- promtail은 Docker stdout/stderr를 읽어 `environment=prod`, `runtime_stack=s2-prod` 라벨로 보낸다.
- 이 경로는 `로그 2차 조회` 목적이며, 1차 장애 알람은 계속 CloudWatch/SNS가 책임진다.

## CloudWatch datasource 기준

- Grafana는 `CloudWatch` datasource를 provisioning으로 생성한다.
- 기본 auth는 `keys`를 사용하고, 실제 값은 루트 `.env.ops` -> S1 `/home/ubuntu/e102/ops/.env`로 반영한다.
- 필요 env:
  - `GRAFANA_CLOUDWATCH_AUTH_TYPE`
  - `GRAFANA_CLOUDWATCH_DEFAULT_REGION`
  - `GRAFANA_CLOUDWATCH_ACCESS_KEY_ID`
  - `GRAFANA_CLOUDWATCH_SECRET_ACCESS_KEY`
  - `GRAFANA_CLOUDWATCH_ASSUME_ROLE_ARN`
  - `GRAFANA_CLOUDWATCH_EXTERNAL_ID`
- S1이 AWS 밖 서버라면 `default`보다 `keys` 또는 `credentials`가 현실적이다.

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
