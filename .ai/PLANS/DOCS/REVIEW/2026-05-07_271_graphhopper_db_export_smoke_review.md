# S14P31E102-271 GraphHopper DB export smoke check 리뷰

## Implementation Harness Preflight

| 파일 | 이유 | 예상 영향 |
| --- | --- | --- |
| `scripts/graphhopper/smoke_postgis_export.py` | dev DB export query와 validation 샘플을 빠르게 검증 | full graph-cache build 전에 DB/export 문제를 조기 발견 |
| `scripts/make/docker/graphhopper-dev-export-smoke.sh` | dev 터널과 graphhopper-build 컨테이너를 연결하는 make 실행 스크립트 | 로컬 `psycopg2` 설치 없이 smoke 실행 가능 |
| `Makefile`, `scripts/make/help.sh` | `make graphhopper-dev-export-smoke` target 노출 | 개발자가 동일 명령으로 재현 가능 |
| `INF/graphhopper/Dockerfile` | smoke 스크립트를 graphhopper build image에 포함 | image rebuild 시 smoke 도구 포함 |

## 구현 내용

- `make graphhopper-dev-export-smoke`를 추가했다.
- `be-dev` 터널 helper를 재사용해 `host.docker.internal:{BE_DEV_DB_LOCAL_PORT}`로 dev DB에 접근한다.
- compose dependency를 올리지 않도록 `--no-deps`를 사용한다.
- build 컨테이너 기본 entrypoint를 우회하도록 `--entrypoint python3`를 명시했다.
- smoke script는 아래를 확인한다.
  - `road_nodes`, `road_segments` 전체 row count
  - segment sample의 from/to node reference 로딩
  - exporter validation blocker 여부
  - JSON report 생성

## 검증

```bash
python3 -m py_compile scripts/graphhopper/smoke_postgis_export.py
bash -n scripts/make/docker/graphhopper-dev-export-smoke.sh
make graphhopper-dev-export-smoke
```

결과:

- DB 전체 row count: `road_nodes=187975`, `road_segments=204582`
- sample: `segments=100`, referenced/loaded nodes `146`
- smoke report: `PASS`, blocker `0`, warning `9`

## 주의사항

- smoke는 full export/build가 아니라 빠른 샘플 검증이다.
- 전체 OSM/PBF/cache 검증은 `make graphhopper-dev-build`가 담당한다.
