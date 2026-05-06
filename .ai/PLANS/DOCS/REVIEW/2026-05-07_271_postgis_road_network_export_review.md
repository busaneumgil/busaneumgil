# S14P31E102-271 구현 리뷰

## 커밋

- 메시지: `Feat[S14P31E102-271]: PostGIS road network export 구현`
- 브랜치: `be/feat/graphhopper-setting-271`

## 구현 범위

- PostGIS `road_nodes`, `road_segments`를 GraphHopper 입력 OSM XML로 변환하는 exporter를 최신 road network 계약에 맞게 수정했다.
- exporter 단위 테스트를 추가해 `ieum:*` tag와 `walkAccess=NO` export 동작을 검증했다.

## 주요 변경

- `road_segments` export SQL에서 삭제된 `crossingState` 참조를 제거했다.
- `walkAccess=NO` segment도 OSM way로 export하도록 필터를 제거했다.
- OSM tag namespace를 `e102:*`에서 `ieum:*`로 변경했다.
- GraphHopper custom parser가 읽을 tag를 모두 출력한다.
  - `ieum:edge_id`
  - `ieum:walk_access`
  - `ieum:avg_slope_percent`
  - `ieum:width_meter`
  - `ieum:braille_block_state`
  - `ieum:audio_signal_state`
  - `ieum:slope_state`
  - `ieum:width_state`
  - `ieum:surface_state`
  - `ieum:stairs_state`
  - `ieum:signal_state`
  - `ieum:segment_type`
- `highway=footway`, `foot=yes`, `oneway=no` 기본 tag를 명시한다.
- 로컬에 `psycopg2`가 없어도 순수 OSM 생성 로직을 테스트할 수 있게 DB driver import를 `connect()` 내부로 늦췄다.

## 검증

- `python3 -m py_compile scripts/graphhopper/export_postgis_to_osm.py scripts/test/test_graphhopper_export.py` 성공
- `python3 scripts/test/test_graphhopper_export.py` 성공
- dev DB에서 export SQL 샘플 조회 성공
- dev DB 기준 `road_segments` row count 확인
  - exported candidate segments: 204,582
  - `walkAccess=NO`: 0
- `./.ai/scripts/verify.sh` 성공

## 주의사항

- 로컬 Python에는 `psycopg2`가 설치되어 있지 않아 전체 DB export 파일 생성은 로컬에서 직접 실행하지 않았다.
- 실제 전체 export는 `graphhopper-build` Docker 이미지의 `python3-psycopg2` 환경에서 `export-postgis-to-osm.py`를 실행하는 흐름으로 검증해야 한다.
