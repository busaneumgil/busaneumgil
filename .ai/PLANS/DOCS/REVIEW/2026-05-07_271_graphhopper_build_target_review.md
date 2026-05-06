# S14P31E102-271 GraphHopper build target 연결 리뷰

## 구현 범위

- `make graphhopper-dev-build`가 로컬 compose PostgreSQL을 띄우지 않고 dev DB 터널을 통해 클라우드 dev DB를 읽도록 수정했다.
- `graphhopper-build` 컨테이너에서 `host.docker.internal`을 안정적으로 해석할 수 있도록 dev compose에 `extra_hosts`를 추가했다.
- 기존 실패 원인인 로컬 `5432` 포트 충돌을 제거했다.

## 변경 파일

| 파일 | 변경 내용 |
| --- | --- |
| `scripts/make/docker/graphhopper-dev-build.sh` | `be-dev` tunnel helper를 재사용하고 `--no-deps`로 build job만 실행 |
| `docker-compose.dev.yml` | `graphhopper-build`에 `host.docker.internal:host-gateway` 추가 |

## 검증 결과

```bash
make graphhopper-dev-build
```

결과:

- dev tunnel 재사용: `127.0.0.1:15432 -> dev PostgreSQL`
- OSM export: `187975 nodes`, `204582 segments`
- validation report: `PASS`, blocker `0`
- PBF 생성: `road-network.osm.pbf`, `6012892 bytes`
- GraphHopper import: `187975 nodes`, `204582 edges`
- graph-cache publish 완료

```bash
curl http://localhost:8999/healthcheck
```

결과:

- `graphhopper.healthy=true`
- `deadlocks.healthy=true`

## 운영 메모

- 검증 후 `docker compose --env-file .env.dev -f docker-compose.dev.yml stop graphhopper`로 runtime 컨테이너는 중지했다.
- 생성된 graph-cache와 import 산출물은 `s14p31e102-dev_graphhopper-dev-data`, `s14p31e102-dev_graphhopper-dev-import` Docker volume에 남아 있다.
