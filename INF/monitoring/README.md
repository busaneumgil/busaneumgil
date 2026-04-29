# S1 운영도구 설정

`INF/monitoring/s1`은 S1 서버에서 운영하는 보조 운영도구 stack의 기준 설정이다.

## 구성

- `Portainer`: 외부 공개 없이 SSH 터널 전용으로 접근한다.
- `Grafana/Prometheus/Loki/Promtail`: PLG 기반 보조 모니터링이다.
- `SonarQube`: 코드 품질 점검용 운영도구다.

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

운영도구 env 값은 노션의 운영 env 기준을 확인한 뒤 S1 `/home/ubuntu/e102/ops/.env`로 배포한다.

Secret 위치와 GitLab Application 생성 기준은 `Docs/인프라/2026-04-29_운영도구_secret_관리_기준.md`를 따른다.

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
http://localhost:9000
```
