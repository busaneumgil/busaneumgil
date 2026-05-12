# S2 prod 로그 수집 템플릿

`INF/monitoring/s2`는 S2 운영 서버에서 `prod` Docker 로그를 `S1 Loki`로 보내기 위한 최소 템플릿이다.

## 목표

- `prod` 서비스 로그를 `Grafana Explore`와 `E102 운영 관측 개요` 대시보드에서 바로 찾을 수 있게 한다.
- `prod metric`은 아직 `CloudWatch`를 1차 기준으로 유지하고, 로그만 먼저 `Loki`에 붙인다.
- backend actuator는 여전히 `127.0.0.1:18080`에만 바인딩한다.

## 배치 위치

- S2 예시 경로: `/home/ubuntu/e102/monitoring`
- 파일:
  - `docker-compose.yml`
  - `promtail/config.yml`
  - `.env`

## 필수 env

```text
LOKI_PUSH_URL=http://<s1-private-ip>:3100/loki/api/v1/push
```

`LOKI_PUSH_URL`은 외부 공개 주소보다 `S1 private IP` 또는 내부 전용 도메인을 우선 사용한다.

현재 운영 반영 기준 fallback은 아래 ingress다.

```text
LOKI_PUSH_URL=https://plg.busaneumgil.com/loki/api/v1/push
```

이 경로는 `S2` IP만 허용하며, 직접 private 경로를 열기 전까지 운영 로그 수집용으로 사용할 수 있다.

## 실행

```bash
docker compose --env-file ./.env up -d
```

`docker-compose.yml`은 `.env`를 `env_file`로 읽으므로 같은 경로에 `LOKI_PUSH_URL`을 둔다.

## Grafana 조회 기준

- `환경(environment)`: `prod`
- `런타임(runtime_stack)`: `s2-prod`
- `서비스(service)`: `backend`, `ai`, `graphhopper`, `admin`

## 후속 확장

- `prod metric`을 Grafana에서 직접 보려면 아래 둘 중 하나를 추가로 선택한다.
  - `S2 -> S1` private scrape 경로 개방
  - agent 기반 metric forwarding 구성
- 지금 단계에서는 로그 2차 조회만 먼저 붙이고, 1차 운영 알람은 CloudWatch/SNS를 유지한다.
