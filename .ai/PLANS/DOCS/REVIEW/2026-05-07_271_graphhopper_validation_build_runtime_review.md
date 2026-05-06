# S14P31E102-271 GraphHopper validation/build/runtime 파이프라인 구현 리뷰

## Implementation Harness Preflight

### 변경 예상 파일

| 파일 | 이유 | 예상 영향 |
| --- | --- | --- |
| `scripts/graphhopper/export_postgis_to_osm.py` | DB export 결과를 GraphHopper import 전에 검증하고 JSON report를 생성 | blocker가 있으면 OSM/PBF/graph-cache 생성 중단 |
| `scripts/test/test_graphhopper_export.py` | validation report와 OSM export tag 회귀 테스트 추가 | exporter 변경의 최소 안전망 |
| `scripts/docker/entrypoints/graphhopper-build.sh` | validation report 생성, OSM XML -> PBF 변환, graph-cache build/publish 순서 연결 | build job이 DB export와 cache publish를 단일 파이프라인으로 수행 |
| `INF/graphhopper/Dockerfile` | PBF 변환 도구 `osmium` 설치 | GraphHopper build image 크기 소폭 증가 |

### 테스트 계획

- `python3 -m py_compile scripts/graphhopper/export_postgis_to_osm.py scripts/test/test_graphhopper_export.py`
- `python3 scripts/test/test_graphhopper_export.py`
- `sh -n scripts/docker/entrypoints/graphhopper-build.sh`
- `.ai/scripts/verify.sh`

### 롤백 기준

- validation report가 정상 데이터에도 blocker를 생성하는 경우
- PBF 변환 실패로 기존 OSM XML 기반 build까지 사용할 수 없는 경우
- build entrypoint가 graph-cache publish 전에 실패 상태를 숨기는 경우

## 구현 결과

### 1. GraphHopper validation report

- `scripts/graphhopper/export_postgis_to_osm.py`에 `--report-json` 옵션을 추가했다.
- DB export 결과 기준으로 아래 항목을 blocker/warning으로 분류해 JSON report를 남긴다.
  - 필수 필드 누락
  - node lon/lat 좌표 범위
  - segment LINESTRING 형식, 점 개수, zero-length
  - from/to node reference
  - segment endpoint와 from/to node 좌표 불일치
  - duplicate `vertex_id`, duplicate `edge_id`
  - self loop
  - enum violation
  - `walk_access != NO` routeable edge 존재 여부
  - routeable component 단절 warning
  - 접근성 상태값 `UNKNOWN` 과다 warning
- blocker가 있으면 exporter가 non-zero로 종료하므로 OSM/PBF/graph-cache 단계로 진행하지 않는다.

### 2. PBF 및 graph-cache build pipeline

- `scripts/docker/entrypoints/graphhopper-build.sh`의 build 순서를 아래처럼 확정했다.
  1. PostGIS `road_nodes`, `road_segments` export
  2. validation report 생성
  3. OSM XML 산출물 non-empty 확인
  4. `osmium cat`으로 OSM XML -> PBF 변환
  5. PBF build report 생성
  6. GraphHopper가 PBF를 import해 임시 graph-cache build
  7. build 결과가 비어 있지 않으면 runtime graph-cache volume으로 publish
- `INF/graphhopper/Dockerfile`에 `osmium-tool`을 추가해 build image 안에서 PBF 변환이 가능하게 했다.
- `docker-compose.local.yml`, `docker-compose.dev.yml`에 OSM/PBF/report 경로와 `OSMIUM_BIN` override 환경변수를 노출했다.
- runtime entrypoint는 기존처럼 DB에 접근하지 않고 graph-cache volume만 읽는다.

## 커밋

- `afabca1` `✨ Feat[S14P31E102-271]: GraphHopper validation report 생성`
- `7fd1489` `✨ Feat[S14P31E102-271]: PBF 및 graph-cache 빌드 파이프라인 구현`

## 검증

```bash
python3 -m py_compile scripts/graphhopper/export_postgis_to_osm.py scripts/test/test_graphhopper_export.py
python3 scripts/test/test_graphhopper_export.py
sh -n scripts/docker/entrypoints/graphhopper-build.sh scripts/docker/entrypoints/graphhopper.sh
JWT_SECRET=test-secret docker compose -f docker-compose.local.yml config --quiet
JWT_SECRET=test-secret docker compose -f docker-compose.dev.yml config --quiet
```

결과:

- exporter unit test 4개 통과
- GraphHopper build/runtime entrypoint shell syntax 통과
- local/dev compose config 파싱 통과

## 미실행 항목

- 실제 `make graphhopper-local-build` 또는 `make graphhopper-dev-build`는 수행하지 않았다.
- 이유: 해당 단계는 Docker image build, DB export, PBF 변환, GraphHopper import까지 포함하는 장시간/환경 의존 작업이다. 현재 구현 검증은 스크립트/compose/단위 테스트까지 완료했다.
