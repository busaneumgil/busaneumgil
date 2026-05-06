# S14P31E102-271 구현 리뷰

## 커밋

- 메시지: `Chore[S14P31E102-271]: dev road network CSV 적재 절차 추가`
- 브랜치: `be/feat/graphhopper-setting-271`

## 구현 범위

- dev DB에 `.ai/LOCAL/nodes.csv`, `.ai/LOCAL/segments.csv`를 적재하는 스크립트를 추가했다.
- Makefile에 `road-network-dev-load` 타깃을 추가했다.
- `make help`에 dev road network 적재 타깃을 노출했다.

## 주요 흐름

1. 로컬 CSV 경로를 확인한다.
2. DB 접속 전에 Python 표준 라이브러리로 CSV header, 필수값, 중복 ID, node reference, enum 후보를 검증한다.
3. 기본 모드에서는 `be-dev` 공통 스크립트의 `.env.dev` 읽기와 SSH tunnel 준비 함수를 사용한다.
4. `psql`로 staging table에 CSV를 먼저 적재한다.
5. staging 검증을 통과하면 `road_segments`, `road_nodes`를 전체 교체 적재한다.
6. 적재 후 row count, orphan reference, PostGIS geometry validity를 검증한다.

## 변경 파일

- `scripts/db/load_road_network_dev.sh`
- `Makefile`
- `scripts/make/help.sh`

## 검증

- `bash -n scripts/db/load_road_network_dev.sh` 성공
- `scripts/db/load_road_network_dev.sh --validate-only` 성공
  - nodes: 187,975
  - segments: 204,582
  - `segmentType`: `CROSS_WALK=3,184`, `SIDE_LINE=201,398`
- `make road-network-dev-load` 성공
  - `road_nodes`: 187,975 rows
  - `road_segments`: 204,582 rows
  - `segmentType`: `CROSS_WALK=3,184`, `SIDE_LINE=201,398`
- `make help`에서 `road-network-dev-load` 노출 확인
- `./.ai/scripts/verify.sh` 성공

## 실행 중 발견한 보정

- `psql \copy`는 `:nodes_csv` 변수를 그대로 파일명으로 해석해 실패했으므로, shell이 quote 처리된 절대경로를 직접 주입하도록 수정했다.
- staging table의 orphan 검증 join이 index 없이 오래 걸렸으므로, staging index와 `ANALYZE`를 추가했다.
- dev DB에 JPA가 아직 `road_nodes`, `road_segments`를 만들지 않은 상태에서도 적재 가능하도록 Entity와 동일한 최소 `CREATE TABLE IF NOT EXISTS` DDL을 추가했다.
