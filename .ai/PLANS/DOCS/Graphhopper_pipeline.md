# DB to GraphHopper 파이프라인 구현 가이드

> 작성일: 2026-05-06  
> 목적: `road_nodes`, `road_segments` DB 기준 보행 네트워크를 GraphHopper가 import 가능한 OSM/PBF와 graph-cache로 만들고, 확정된 보행 기준의 profile/가중치를 GraphHopper 설정에 반영한다.  
> 기준 문서: `ERD_v4.md`, `경로_API.md`, `보행기준.md`  
> 기준 레포 경로: `graphhopper/`, `docker-compose.yml`

---

## 1. 큰그림

이 문서의 목표는 다른 문서를 보지 않아도 GraphHopper 세팅과 구현을 진행할 수 있게 하는 것이다.

전체 플로우는 아래 순서로 고정한다.

```text
PostgreSQL/PostGIS
  road_nodes
  road_segments
    ↓
DB export 또는 CSV snapshot
  etl/raw/gangseo_road_nodes_v8.csv
  etl/raw/gangseo_road_segments_mapping_v2.csv
    ↓
validation
  graphhopper/scripts/validate_csv_graph.py
    ↓
OSM XML export
  graphhopper/scripts/csv_to_osm.py
  graphhopper/data/gangseo.osm
    ↓
PBF build
  graphhopper/scripts/build_pbf.sh
  graphhopper/data/gangseo.osm.pbf
    ↓
GraphHopper import
  graphhopper/config.yml
  graphhopper/plugin/**
  graphhopper/custom_models/*.json
    ↓
graph-cache
  graphhopper/graph-cache
    ↓
Backend route API
  GRAPHHOPPER_BASE_URL=http://graphhopper:8989
```

운영 원칙:

| 항목 | 결정 |
| --- | --- |
| GraphHopper source of truth | DB의 `road_nodes`, `road_segments` |
| 중간 산출물 | CSV snapshot은 허용하지만 원천 데이터가 아니다 |
| GraphHopper 입력 | `graphhopper/data/gangseo.osm.pbf` |
| GraphHopper profile | 8개 접근성 profile |
| 접근성 tag namespace | `ieum:*` |
| 경로 비용 방식 | `blocked` 먼저 처리 후 `priority.multiply_by` 적용 |
| graph-cache 재사용 | config, plugin, custom model, PBF 중 하나라도 바뀌면 재import |

---

## 2. 현재 레포 기준 파일 역할

| 경로 | 역할 | 구현 시 확인할 것 |
| --- | --- | --- |
| `docker-compose.yml` | `postgres`, `redis`, `graphhopper`, `backend` 실행 | backend의 `GRAPHHOPPER_BASE_URL`은 compose 내부 URL `http://graphhopper:8989` |
| `graphhopper/Dockerfile` | custom plugin jar 빌드 후 GraphHopper image 생성 | `graphhopper/plugin` 변경 시 image 재빌드 필요 |
| `graphhopper/config.yml` | GraphHopper server, encoded value, profile 설정 | profile 8종과 EV 목록이 모두 있어야 함 |
| `graphhopper/plugin/**` | `ieum:*` OSM tag를 GraphHopper encoded value로 import | ERD enum과 Java enum이 일치해야 함 |
| `graphhopper/custom_models/*.json` | profile별 priority/speed/cost 모델 | 아래 8개 profile별 multiplier를 반영해야 함 |
| `graphhopper/scripts/validate_csv_graph.py` | CSV topology/enum validation | ERD enum 값과 동일해야 함 |
| `graphhopper/scripts/csv_to_osm.py` | CSV를 OSM XML로 변환 | `road_segments` 접근성 컬럼을 `ieum:*` tag로 내보내야 함 |
| `graphhopper/scripts/build_pbf.sh` | OSM XML을 PBF로 변환 | `osmium` 필요 |
| `graphhopper/data/gangseo.osm.pbf` | GraphHopper import 입력 | 변경 시 graph-cache 삭제 후 재import |
| `graphhopper/graph-cache` | GraphHopper import cache | config/plugin/model/PBF 변경 시 재생성 |

---

## 3. DB 스키마 계약

### 3.1 `road_nodes`

GraphHopper node export에 필요한 최소 컬럼:

| 컬럼 | 타입 | 조건 |
| --- | --- | --- |
| `vertexId` | BIGINT 또는 숫자 문자열 | OSM node id의 추적 기준 |
| `point` | `GEOMETRY(POINT, 4326)` | lon/lat 추출 가능해야 함 |

CSV snapshot 컬럼:

```text
vertexId,point
```

`point`는 `POINT(lon lat)` 또는 `SRID=4326;POINT(lon lat)` WKT로 내보낸다.

### 3.2 `road_segments`

GraphHopper way export에 필요한 최소 컬럼:

| 컬럼 | 타입 | NULL/default | GraphHopper 사용 |
| --- | --- | --- | --- |
| `edgeId` | BIGINT | NOT NULL | `ieum:edge_id` |
| `fromNodeId` | BIGINT | NOT NULL | way 시작 node |
| `toNodeId` | BIGINT | NOT NULL | way 종료 node |
| `geom` | `GEOMETRY(LINESTRING, 4326)` | NOT NULL | way geometry |
| `lengthMeter` | NUMERIC(10,2) | NOT NULL | 검증/리포트 |
| `walkAccess` | ENUM | default `UNKNOWN` | 통행 가능 여부 |
| `avgSlopePercent` | NUMERIC(6,2) | NULL | 경사 세부 threshold |
| `widthMeter` | NUMERIC(6,2) | NULL | 폭 세부 판단 |
| `brailleBlockState` | ENUM | default `UNKNOWN` | 시각장애 profile |
| `audioSignalState` | ENUM | default `UNKNOWN` | 시각장애 profile |
| `slopeState` | ENUM | default `UNKNOWN` | 경사 penalty |
| `widthState` | ENUM | default `UNKNOWN` | 보도폭 penalty |
| `surfaceState` | VARCHAR(30) | default `UNKNOWN` | 노면 penalty |
| `stairsState` | ENUM | default `UNKNOWN` | 계단 blocked/penalty |
| `signalState` | ENUM | default `UNKNOWN` | 횡단 신호 판단 |
| `segmentType` | VARCHAR(30) | default `SIDE_LINE` | 횡단보도 판단 |

CSV snapshot 컬럼:

```text
edgeId,fromNodeId,toNodeId,geom,lengthMeter,walkAccess,avgSlopePercent,widthMeter,brailleBlockState,audioSignalState,slopeState,widthState,surfaceState,stairsState,signalState,segmentType
```

### 3.3 enum 값

| 컬럼 | 허용값 |
| --- | --- |
| `walkAccess` | `YES`, `NO`, `UNKNOWN` |
| `brailleBlockState` | `YES`, `NO`, `UNKNOWN` |
| `audioSignalState` | `YES`, `NO`, `UNKNOWN` |
| `stairsState` | `YES`, `NO`, `UNKNOWN` |
| `signalState` | `YES`, `NO`, `UNKNOWN` |
| `slopeState` | `FLAT`, `MODERATE`, `STEEP`, `RISK`, `UNKNOWN` |
| `widthState` | `ADEQUATE_150`, `ADEQUATE_120`, `NARROW`, `UNKNOWN` |
| `surfaceState` | `PAVED`, `UNPAVED`, `UNKNOWN` |
| `segmentType` | `CROSS_WALK`, `SIDE_LINE` |

`segmentType`에는 `UNKNOWN`을 두지 않는다. Java plugin enum과 validation도 `CROSS_WALK`, `SIDE_LINE`만 허용하도록 맞춘다.

---

## 4. 파생 규칙

### 4.1 `slopeState`

| 조건 | 값 | 구현 의미 |
| --- | --- | --- |
| `0 <= avgSlopePercent < 5.56` | `FLAT` | 낮은 부담 |
| `5.56 <= avgSlopePercent < 8.33` | `MODERATE` | 중간 penalty |
| `8.33 <= avgSlopePercent < 12.00` | `STEEP` | 강한 penalty |
| `12.00 <= avgSlopePercent` | `RISK` | 강한 회피 또는 프로필별 차단 후보 |
| 경사값 없음/불신 | `UNKNOWN` | 차단하지 않고 penalty |

수동 휠체어는 `avgSlopePercent >= 15.00`이면 blocked로 처리한다. `10.00 <= avgSlopePercent < 15.00`은 강한 penalty다.

### 4.2 `widthState`

| 조건 | 값 | 구현 의미 |
| --- | --- | --- |
| `widthMeter >= 1.50` | `ADEQUATE_150` | `WIDE` |
| `1.20 <= widthMeter < 1.50` | `ADEQUATE_120` | `NORMAL` |
| `widthMeter < 1.20` | `NARROW` | 휠체어 강한 penalty |
| 폭값 없음/불신 | `UNKNOWN` | 차단하지 않고 penalty |

### 4.3 `crossingState`

`crossingState`는 DB 컬럼으로 저장하지 않는다. GraphHopper custom model에서는 `segment_type`과 `signal_state` 조건으로 직접 판단한다.

| 파생 조건 | 파생값 | ETA 추가시간 |
| --- | --- | ---: |
| `segment_type != CROSS_WALK` | `NONE` | `+0초` |
| `segment_type == CROSS_WALK && signal_state == YES` | `SIGNALIZED` | `+30초` |
| `segment_type == CROSS_WALK && signal_state == NO` | `UNSIGNALIZED` | `+8초` |
| `segment_type == CROSS_WALK && signal_state == UNKNOWN` | `UNKNOWN` | `+10초` |

GraphHopper는 주로 priority를 계산하고, 횡단 추가 대기시간은 backend가 path detail/step을 보고 ETA에 더한다.

---

## 5. OSM export 계약

모든 `road_segments`는 GraphHopper way가 된다. `walkAccess=NO`도 OSM에는 내보낸다. 차단은 custom model에서 `priority=0`으로 처리해야 path detail과 디버깅이 가능하다.

GraphHopper export 대상 `road_segments`는 feature 위치 기준 분할이 반영된 최종 segment다. 원천 segment 하나에 횡단보도, 계단, 폭 변화, 노면 변화, 경사 변화가 여러 위치에 있으면 ETL 단계에서 `segment_features.geom` 경계 기준으로 나눈 뒤 각 최종 segment의 접근성 상태값을 확정한다.

API step은 GraphHopper way 또는 원천 segment와 1:1이 아니다. route/search 응답의 `steps[]`는 최종 segment, feature 이벤트, 회전 이벤트를 조합해 만들며, 이벤트가 없는 인접 segment는 하나의 step으로 병합할 수 있다. 회전 안내 `TURN_LEFT`, `TURN_RIGHT`는 `segment_features`가 아니라 route geometry 또는 연속 segment 방향 변화에서 생성한다.

OSM way tag:

```xml
<tag k="highway" v="footway" />
<tag k="foot" v="yes" />
<tag k="oneway" v="no" />
<tag k="ieum:edge_id" v="{edgeId}" />
<tag k="ieum:walk_access" v="{walkAccess}" />
<tag k="ieum:avg_slope_percent" v="{avgSlopePercent}" />
<tag k="ieum:width_meter" v="{widthMeter}" />
<tag k="ieum:braille_block_state" v="{brailleBlockState}" />
<tag k="ieum:audio_signal_state" v="{audioSignalState}" />
<tag k="ieum:slope_state" v="{slopeState}" />
<tag k="ieum:width_state" v="{widthState}" />
<tag k="ieum:surface_state" v="{surfaceState}" />
<tag k="ieum:stairs_state" v="{stairsState}" />
<tag k="ieum:signal_state" v="{signalState}" />
<tag k="ieum:segment_type" v="{segmentType}" />
```

빈값 기본값:

| 컬럼 | 기본값 |
| --- | --- |
| `walkAccess` | `UNKNOWN` |
| `brailleBlockState` | `UNKNOWN` |
| `audioSignalState` | `UNKNOWN` |
| `slopeState` | `UNKNOWN` |
| `widthState` | `UNKNOWN` |
| `surfaceState` | `UNKNOWN` |
| `stairsState` | `UNKNOWN` |
| `signalState` | `UNKNOWN` |
| `segmentType` | `SIDE_LINE` |
| `avgSlopePercent` | 빈값이면 parser fallback `0.0` |
| `widthMeter` | 빈값이면 parser fallback `0.0` |

---

## 6. GraphHopper encoded values

`graphhopper/config.yml`의 `graph.encoded_values`에는 아래 값이 있어야 한다.

```yaml
graph.encoded_values: >-
  foot_access,foot_priority,foot_average_speed,
  walk_access,avg_slope_percent,width_meter,braille_block_state,
  audio_signal_state,slope_state,width_state,surface_state,
  stairs_state,signal_state,segment_type
```

plugin import mapping:

| EV | OSM tag | 타입 | fallback |
| --- | --- | --- | --- |
| `walk_access` | `ieum:walk_access` | enum `YES/NO/UNKNOWN` | `UNKNOWN` |
| `avg_slope_percent` | `ieum:avg_slope_percent` | decimal | `0.0` |
| `width_meter` | `ieum:width_meter` | decimal | `0.0` |
| `braille_block_state` | `ieum:braille_block_state` | enum `YES/NO/UNKNOWN` | `UNKNOWN` |
| `audio_signal_state` | `ieum:audio_signal_state` | enum `YES/NO/UNKNOWN` | `UNKNOWN` |
| `slope_state` | `ieum:slope_state` | enum `FLAT/MODERATE/STEEP/RISK/UNKNOWN` | `UNKNOWN` |
| `width_state` | `ieum:width_state` | enum `ADEQUATE_150/ADEQUATE_120/NARROW/UNKNOWN` | `UNKNOWN` |
| `surface_state` | `ieum:surface_state` | enum `PAVED/UNPAVED/UNKNOWN` | `UNKNOWN` |
| `stairs_state` | `ieum:stairs_state` | enum `YES/NO/UNKNOWN` | `UNKNOWN` |
| `signal_state` | `ieum:signal_state` | enum `YES/NO/UNKNOWN` | `UNKNOWN` |
| `segment_type` | `ieum:segment_type` | enum `CROSS_WALK/SIDE_LINE` | `SIDE_LINE` |

---

## 7. Profile 계약

GraphHopper profile 이름은 route API와 정확히 일치해야 한다.

| 사용자 조건 | SAFE 후보 profile | SHORTEST 후보 profile | 기본속도 |
| --- | --- | --- | ---: |
| 일반 보행 | `pedestrian_safe` | `pedestrian_fast` | `1.30 m/s` = `4.68 km/h` |
| 시각장애 사용자 | `visual_safe` | `visual_fast` | `0.80 m/s` = `2.88 km/h` |
| 수동 휠체어 사용자 | `wheelchair_manual_safe` | `wheelchair_manual_fast` | `0.50 m/s` = `1.80 km/h` |
| 전동 휠체어 사용자 | `wheelchair_auto_safe` | `wheelchair_auto_fast` | `1.20 m/s` = `4.32 km/h` |

`graphhopper/config.yml` profile:

```yaml
profiles:
  - name: pedestrian_safe
    custom_model_files: [pedestrian_safe.json]
  - name: pedestrian_fast
    custom_model_files: [pedestrian_fast.json]
  - name: visual_safe
    custom_model_files: [visual_safe.json]
  - name: visual_fast
    custom_model_files: [visual_fast.json]
  - name: wheelchair_manual_safe
    custom_model_files: [wheelchair_manual_safe.json]
  - name: wheelchair_manual_fast
    custom_model_files: [wheelchair_manual_fast.json]
  - name: wheelchair_auto_safe
    custom_model_files: [wheelchair_auto_safe.json]
  - name: wheelchair_auto_fast
    custom_model_files: [wheelchair_auto_fast.json]
profiles_ch: []
profiles_lm: []
```

운영 전에는 `foot` fallback을 두지 않는다. 요청 profile이 없으면 배포 실패 또는 서버 설정 오류로 본다.

---

## 8. Custom model 작성 기준

GraphHopper custom model은 `priority.multiply_by` 기준을 사용한다.

| 값 | 의미 |
| --- | --- |
| `0` | 통과 금지 |
| `< 1.0` | 회피 penalty |
| `1.0` | 영향 없음 |
| `> 1.0` | 선호 bonus |

backend cost로 환산할 때는 다음 의미와 같다.

```text
base_time = distance / profile_base_speed
priority_product = walkAccess * stairs * slope * width * surface * crossing * audio * braille
final_cost = base_time / max(priority_product, 0.01) + extra_wait_time
```

### 8.1 speed

각 custom model에는 profile 기본속도 상한을 넣는다.

```json
"speed": [
  { "if": "true", "limit_to": "4.68" }
]
```

| profile | `limit_to` |
| --- | ---: |
| `pedestrian_safe` | `4.68` |
| `pedestrian_fast` | `4.68` |
| `visual_safe` | `2.88` |
| `visual_fast` | `2.88` |
| `wheelchair_manual_safe` | `1.80` |
| `wheelchair_manual_fast` | `1.80` |
| `wheelchair_auto_safe` | `4.32` |
| `wheelchair_auto_fast` | `4.32` |

### 8.2 distance influence

초기값은 아래로 둔다. safe는 우회 허용성이 크고, fast는 거리/시간을 더 우선한다.

| profile | `distance_influence` |
| --- | ---: |
| `pedestrian_safe` | `80` |
| `pedestrian_fast` | `35` |
| `visual_safe` | `95` |
| `visual_fast` | `45` |
| `wheelchair_manual_safe` | `120` |
| `wheelchair_manual_fast` | `55` |
| `wheelchair_auto_safe` | `90` |
| `wheelchair_auto_fast` | `45` |

### 8.3 blocked 규칙

| 조건 | pedestrian | visual | wheelchair_manual | wheelchair_auto |
| --- | --- | --- | --- | --- |
| `walk_access == NO` | `0` | `0` | `0` | `0` |
| `stairs_state == YES` | penalty | strong penalty | `0` | `0` |
| `avg_slope_percent >= 15.0` | penalty | penalty | `0` | strong penalty |
| `UNKNOWN` | blocked 아님 | blocked 아님 | blocked 아님 | blocked 아님 |

### 8.4 priority multiplier

아래 값을 custom model에 반영한다. `LOW/MIDDLE/HIGH`는 GraphHopper 조건에서 `FLAT/MODERATE/STEEP`로 매핑한다.

| 속성 | 값 | `pedestrian_safe` | `pedestrian_fast` | `visual_safe` | `visual_fast` | `wheelchair_manual_safe` | `wheelchair_manual_fast` | `wheelchair_auto_safe` | `wheelchair_auto_fast` |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `walk_access` | `YES` | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 |
| `walk_access` | `NO` | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 |
| `walk_access` | `UNKNOWN` | 0.85 | 0.95 | 0.80 | 0.90 | 0.65 | 0.80 | 0.75 | 0.85 |
| `slope_state` | `FLAT` | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 |
| `slope_state` | `MODERATE` | 0.85 | 0.95 | 0.75 | 0.90 | 0.45 | 0.65 | 0.65 | 0.80 |
| `slope_state` | `STEEP` | 0.60 | 0.85 | 0.50 | 0.75 | 0.10 | 0.25 | 0.25 | 0.45 |
| `slope_state` | `RISK` | 0.30 | 0.70 | 0.20 | 0.60 | 0.05 | 0.15 | 0.10 | 0.30 |
| `slope_state` | `UNKNOWN` | 0.90 | 1.00 | 0.90 | 1.00 | 0.80 | 0.90 | 0.85 | 0.95 |
| `width_state` | `ADEQUATE_150` | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 |
| `width_state` | `ADEQUATE_120` | 1.00 | 1.00 | 0.95 | 1.00 | 0.65 | 0.80 | 0.75 | 0.90 |
| `width_state` | `NARROW` | 0.90 | 0.95 | 0.75 | 0.90 | 0.12 | 0.25 | 0.20 | 0.35 |
| `width_state` | `UNKNOWN` | 0.95 | 1.00 | 0.90 | 1.00 | 0.75 | 0.90 | 0.80 | 0.95 |
| `surface_state` | `PAVED` | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 |
| `surface_state` | `UNPAVED` | 0.75 | 0.85 | 0.70 | 0.85 | 0.25 | 0.45 | 0.35 | 0.55 |
| `surface_state` | `UNKNOWN` | 0.90 | 0.95 | 0.85 | 0.95 | 0.70 | 0.85 | 0.80 | 0.90 |
| `stairs_state` | `NO` | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 |
| `stairs_state` | `YES` | 0.55 | 0.75 | 0.45 | 0.75 | 0.00 | 0.00 | 0.00 | 0.00 |
| `stairs_state` | `UNKNOWN` | 0.90 | 0.95 | 0.85 | 0.90 | 0.60 | 0.75 | 0.70 | 0.80 |
| `crossingState` | `SIGNALIZED` | 1.10 | 1.00 | 1.10 | 1.00 | 1.05 | 1.00 | 1.05 | 1.00 |
| `crossingState` | `UNSIGNALIZED` | 0.85 | 1.00 | 0.60 | 0.90 | 0.75 | 0.90 | 0.80 | 0.90 |
| `crossingState` | `UNKNOWN` | 0.90 | 1.00 | 0.80 | 0.95 | 0.85 | 0.95 | 0.90 | 0.95 |
| `braille_block_state` | `YES` | 1.00 | 1.00 | 1.08 | 1.05 | 1.00 | 1.00 | 1.00 | 1.00 |
| `braille_block_state` | `NO` | 1.00 | 1.00 | 0.90 | 0.95 | 1.00 | 1.00 | 1.00 | 1.00 |
| `braille_block_state` | `UNKNOWN` | 1.00 | 1.00 | 0.95 | 0.98 | 1.00 | 1.00 | 1.00 | 1.00 |
| `audio_signal_state` | `YES` | 1.00 | 1.00 | 1.10 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 |
| `audio_signal_state` | `NO` | 1.00 | 1.00 | 0.85 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 |
| `audio_signal_state` | `UNKNOWN` | 1.00 | 1.00 | 0.95 | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 |

`crossingState` 조건은 custom model에서 아래처럼 쓴다.

| crossingState | custom model 조건 |
| --- | --- |
| `SIGNALIZED` | `segment_type == CROSS_WALK && signal_state == YES` |
| `UNSIGNALIZED` | `segment_type == CROSS_WALK && signal_state == NO` |
| `UNKNOWN` | `segment_type == CROSS_WALK && signal_state == UNKNOWN` |
| `NONE` | 별도 조건 없음 |

---

## 9. Custom model JSON 작성 패턴

각 JSON은 아래 순서로 작성한다.

1. blocked 조건
2. `UNKNOWN` penalty
3. 위험 속성 penalty
4. 좋은 속성 bonus
5. speed
6. distance influence

예시:

```json
{
  "priority": [
    { "if": "walk_access == NO", "multiply_by": "0" },
    { "if": "walk_access == UNKNOWN", "multiply_by": "0.85" },
    { "if": "stairs_state == YES", "multiply_by": "0.55" },
    { "if": "stairs_state == UNKNOWN", "multiply_by": "0.90" },
    { "if": "slope_state == MODERATE", "multiply_by": "0.85" },
    { "if": "slope_state == STEEP", "multiply_by": "0.60" },
    { "if": "slope_state == RISK", "multiply_by": "0.30" },
    { "if": "surface_state == UNPAVED", "multiply_by": "0.75" },
    { "if": "surface_state == UNKNOWN", "multiply_by": "0.90" },
    { "if": "segment_type == CROSS_WALK && signal_state == YES", "multiply_by": "1.10" },
    { "if": "segment_type == CROSS_WALK && signal_state == NO", "multiply_by": "0.85" },
    { "if": "segment_type == CROSS_WALK && signal_state == UNKNOWN", "multiply_by": "0.90" }
  ],
  "speed": [
    { "if": "true", "limit_to": "4.68" }
  ],
  "distance_influence": 80
}
```

`wheelchair_manual_*`에는 아래 조건을 추가한다.

```json
{ "if": "avg_slope_percent >= 15.0", "multiply_by": "0" }
```

`wheelchair_auto_*`에는 아래 조건을 추가한다.

```json
{ "if": "avg_slope_percent >= 10.0", "multiply_by": "0.10" }
```

---

## 10. Validation 기준

GraphHopper import 전에 validation이 실패하면 PBF/graph-cache를 만들지 않는다.

| 검증 | 실패 기준 | 등급 |
| --- | --- | --- |
| 필수 컬럼 | node/segment 필수 컬럼 누락 | blocker |
| node geometry | POINT 아님, 좌표 파싱 실패, lon/lat 범위 오류 | blocker |
| segment geometry | LINESTRING 아님, 점 2개 미만, 길이 0 | blocker |
| node reference | `fromNodeId`, `toNodeId`가 node에 없음 | blocker |
| endpoint mismatch | segment 시작/끝 좌표가 from/to node와 허용 오차 밖 | blocker |
| duplicate `edgeId` | 동일 `edgeId` 중복 | blocker |
| self loop | `fromNodeId == toNodeId` | blocker |
| enum violation | 이 문서의 enum 외 값 | blocker |
| no routeable edge | `walkAccess != NO`인 edge가 없음 | blocker |
| small component | 작은 단절 component 존재 | warning |
| high unknown ratio | `UNKNOWN` 비율 과다 | warning |

권장 report:

```text
runtime/graphhopper/validation/gangseo_validation_report.json
runtime/graphhopper/osm/gangseo_osm_report.json
runtime/graphhopper/pbf/gangseo_pbf_report.json
```

---

## 11. 구현 단계

### Step 1. DB에서 CSV snapshot 생성

현재 스크립트는 CSV를 입력으로 받는다. DB를 source of truth로 유지하려면 build 전에 DB에서 CSV snapshot을 생성한다.

node export 예시:

```sql
COPY (
  SELECT
    "vertexId" AS "vertexId",
    ST_AsText("point"::geometry) AS "point"
  FROM road_nodes
  ORDER BY "vertexId"
) TO STDOUT WITH CSV HEADER;
```

segment export 예시:

```sql
COPY (
  SELECT
    "edgeId",
    "fromNodeId",
    "toNodeId",
    ST_AsText("geom"::geometry) AS "geom",
    "lengthMeter",
    COALESCE("walkAccess"::text, 'UNKNOWN') AS "walkAccess",
    "avgSlopePercent",
    "widthMeter",
    COALESCE("brailleBlockState"::text, 'UNKNOWN') AS "brailleBlockState",
    COALESCE("audioSignalState"::text, 'UNKNOWN') AS "audioSignalState",
    COALESCE("slopeState"::text, 'UNKNOWN') AS "slopeState",
    COALESCE("widthState"::text, 'UNKNOWN') AS "widthState",
    COALESCE("surfaceState", 'UNKNOWN') AS "surfaceState",
    COALESCE("stairsState"::text, 'UNKNOWN') AS "stairsState",
    COALESCE("signalState"::text, 'UNKNOWN') AS "signalState",
    COALESCE("segmentType", 'SIDE_LINE') AS "segmentType"
  FROM road_segments
  ORDER BY "edgeId"
) TO STDOUT WITH CSV HEADER;
```

출력 위치:

```text
etl/raw/gangseo_road_nodes_v8.csv
etl/raw/gangseo_road_segments_mapping_v2.csv
```

### Step 2. CSV validation

```bash
python graphhopper/scripts/validate_csv_graph.py \
  --nodes etl/raw/gangseo_road_nodes_v8.csv \
  --segments etl/raw/gangseo_road_segments_mapping_v2.csv \
  --report-json runtime/graphhopper/validation/gangseo_validation_report.json
```

blocker가 있으면 이후 단계를 중단한다.

### Step 3. OSM XML 생성

```bash
python graphhopper/scripts/csv_to_osm.py \
  --nodes etl/raw/gangseo_road_nodes_v8.csv \
  --segments etl/raw/gangseo_road_segments_mapping_v2.csv \
  --output graphhopper/data/gangseo.osm \
  --report-json runtime/graphhopper/osm/gangseo_osm_report.json
```

생성된 OSM XML에는 5장에 정의한 `ieum:*` tag가 포함되어야 한다.

### Step 4. PBF 생성

```bash
graphhopper/scripts/build_pbf.sh \
  --input graphhopper/data/gangseo.osm \
  --output graphhopper/data/gangseo.osm.pbf \
  --report-json runtime/graphhopper/pbf/gangseo_pbf_report.json
```

`osmium`이 없으면 설치하거나 `OSMIUM_BIN`을 지정한다.

```bash
OSMIUM_BIN=/path/to/osmium graphhopper/scripts/build_pbf.sh
```

### Step 5. plugin/config/custom model 정합성 확인

확인 항목:

```bash
rg "walk_access|avg_slope_percent|width_meter|braille_block_state|audio_signal_state|slope_state|width_state|surface_state|stairs_state|signal_state|segment_type" graphhopper/config.yml graphhopper/plugin graphhopper/custom_models
rg "pedestrian_safe|pedestrian_fast|visual_safe|visual_fast|wheelchair_manual_safe|wheelchair_manual_fast|wheelchair_auto_safe|wheelchair_auto_fast" graphhopper/config.yml
```

정합성 체크:

| 항목 | 통과 기준 |
| --- | --- |
| EV 이름 | config, plugin, custom model에서 동일 |
| OSM tag | exporter와 plugin parser가 `ieum:*`로 동일 |
| enum | ERD, validation, Java enum이 동일 |
| profile | API 명세의 8개 profile이 모두 존재 |
| custom model | 8장의 multiplier와 speed가 반영됨 |

### Step 6. graph-cache 재생성

아래 파일 중 하나라도 바뀌면 기존 `graphhopper/graph-cache`를 삭제하고 다시 import한다.

```text
graphhopper/data/gangseo.osm.pbf
graphhopper/config.yml
graphhopper/plugin/**
graphhopper/custom_models/*.json
```

local에서는 image를 다시 빌드한다.

```bash
docker compose build graphhopper
docker compose up graphhopper
```

### Step 7. health check

compose 외부 포트 기본값은 `18989`다.

```bash
curl http://localhost:18989/health
```

compose 내부 backend는 아래 URL을 사용한다.

```text
http://graphhopper:8989
```

### Step 8. profile별 smoke test

```bash
curl "http://localhost:18989/route?point=35.1200,128.9360&point=35.1315,128.8823&profile=pedestrian_safe&points_encoded=false"
curl "http://localhost:18989/route?point=35.1200,128.9360&point=35.1315,128.8823&profile=pedestrian_fast&points_encoded=false"
curl "http://localhost:18989/route?point=35.1200,128.9360&point=35.1315,128.8823&profile=visual_safe&points_encoded=false"
curl "http://localhost:18989/route?point=35.1200,128.9360&point=35.1315,128.8823&profile=visual_fast&points_encoded=false"
curl "http://localhost:18989/route?point=35.1200,128.9360&point=35.1315,128.8823&profile=wheelchair_manual_safe&points_encoded=false"
curl "http://localhost:18989/route?point=35.1200,128.9360&point=35.1315,128.8823&profile=wheelchair_manual_fast&points_encoded=false"
curl "http://localhost:18989/route?point=35.1200,128.9360&point=35.1315,128.8823&profile=wheelchair_auto_safe&points_encoded=false"
curl "http://localhost:18989/route?point=35.1200,128.9360&point=35.1315,128.8823&profile=wheelchair_auto_fast&points_encoded=false"
```

---

## 12. Backend API 연결 계약

GraphHopper는 도보 네트워크 경로의 source of truth다.

| API 기능 | GraphHopper 사용 |
| --- | --- |
| `POST /routes/search/walk` | `SAFE`, `SHORTEST` 후보 계산 |
| `POST /routes/search/transit` | ODsay BUS/SUBWAY 후보의 모든 WALK leg 재계산 |
| `POST /routes/{routeId}/reroute` | 현재 위치 기준 WALK repair 또는 full reroute |
| 지하철 엘리베이터 보정 | SUBWAY 승하차 좌표를 elevator 좌표로 바꾼 뒤 WALK leg 재계산 |

BUS/SUBWAY leg의 실제 이동 선형은 GraphHopper가 아니라 ODsay `loadLane`이 source of truth다. `SearchPubTransPathT.info.mapObj`로 `loadLane`을 호출하고, `loadLane.result.lane[].class`의 `1=BUS`, `2=SUBWAY` 기준으로 `section[].graphPos[]`를 flatten해 leg별 단일 WKT `LINESTRING(lng lat, ...)`을 만든다. FE 응답에는 이 leg `geometry`만 노출하고 `mapObj`, `loadLane` 원본 구조, BIMS/ODsay 내부 식별자는 Redis 후보 snapshot과 select 이후 `route_sessions.routeSnapshotJson`에 backend-only metadata로 저장한다.

GraphHopper 응답 -> route DTO:

| GraphHopper/OSM 값 | API DTO 반영 |
| --- | --- |
| route distance | `distanceMeter` |
| route time | `estimatedTimeMinute` |
| points/path geometry | `geometry`, leg/step `geometry` |
| `ieum:edge_id` | backend debug/path detail |
| `avg_slope_percent` | `slopePercent`, slope badge/alert |
| `width_state`, `width_meter` | `widthState`, `NARROW_SIDEWALK` badge/alert |
| `surface_state` | `UNPAVED` badge/alert |
| `stairs_state` | `STAIR` badge/alert |
| `segment_type`, `signal_state` | `CROSSWALK` alert, crossing wait time |

오류 처리:

| 상황 | API 오류 |
| --- | --- |
| GraphHopper 실패 | `EX5020` |
| GraphHopper timeout 5초 초과 | `EX5040` |
| 후보 없음 | `RT4040` |
| snap 거리 10m 초과, snap edge `walkAccess=NO`, 또는 path 없음 | snap 실패로 처리 |

---

## 13. 운영 기준

| 기준 | 값 |
| --- | --- |
| GraphHopper timeout | `5초` |
| 출발지-도착지 근접 오류 | `20m 이하`면 `RT4004` |
| 도보 우선 노출 | GraphHopper `SAFE` 보행거리 `750m 이하` |
| 거리 산정 1순위 | GraphHopper `SAFE` route distance |
| 거리 산정 2순위 | GraphHopper `SHORTEST` route distance |
| 제외 거리 기준 | 직선거리, ODsay `pointDistance` |
| 경로 이탈 | route geometry에서 `10m` 이상 |
| 이탈 판단 GPS 정확도 | `accuracy <= 20m` |
| 이탈 지속 조건 | `3회 연속` 또는 `3초 이상` |
| 서버 측 WALK repair | 기존 route geometry에서 `100m 이하` |
| 서버 측 full reroute | `100m 초과 ~ 500m 이하` |
| 과도 이탈 | `500m 초과`면 `RT4091` |

`estimatedTimeMinute`는 backend에서 초 단위 계산값을 분 단위로 내림 처리한다.

```text
estimatedTimeMinute = max(1, floor(totalSeconds / 60))
```

---

## 14. Golden test

GraphHopper 변경 후 최소 아래 3개 케이스를 확인한다.

| 케이스 | 검증 내용 |
| --- | --- |
| 도보 only 짧은 거리 | `SAFE`, `SHORTEST` 후보가 나오고 750m 이하 도보 우선 정책에 사용할 distance가 안정적으로 계산됨 |
| 저상 버스 포함 대중교통 | ODsay WALK leg를 GraphHopper로 재계산할 수 있고, BIMS 저상버스 ranking과 함께 후보가 유지됨 |
| 지하철 + 엘리베이터 포함 대중교통 | SUBWAY 승하차 좌표를 엘리베이터 좌표로 보정한 뒤 GraphHopper WALK leg가 생성됨 |

profile별 smoke test에서는 최소 아래를 확인한다.

| 검증 | 기대 |
| --- | --- |
| `walk_access == NO` edge | 모든 profile에서 통과하지 않음 |
| `stairs_state == YES` edge | 휠체어 profile에서 통과하지 않음 |
| `width_state == NARROW` edge | 휠체어 safe profile이 강하게 회피 |
| `surface_state == UNPAVED` edge | 휠체어와 visual safe profile이 회피 |
| `segment_type == CROSS_WALK && signal_state == YES` | safe profile에서 무신호보다 선호 |
| `braille_block_state == YES` | visual profile에서 선호 |
| `audio_signal_state == YES` | visual safe profile에서 선호 |

---

## 15. 구현 완료 기준

아래 조건을 모두 만족해야 GraphHopper 연동 완료로 본다.

| 항목 | 완료 기준 |
| --- | --- |
| DB export | `road_nodes`, `road_segments`에서 필요한 컬럼이 누락 없이 snapshot됨 |
| validation | blocker 없이 report 생성 |
| OSM export | 모든 way에 `ieum:*` tag 포함 |
| PBF | `graphhopper/data/gangseo.osm.pbf` 생성 및 비어 있지 않음 |
| plugin | `ieum:*` tag가 EV로 import됨 |
| config | EV와 profile 8종이 설정됨 |
| custom model | 이 문서의 multiplier, speed, distance influence 반영 |
| graph-cache | 최신 PBF/config/plugin/model 기준으로 재생성됨 |
| smoke test | 8개 profile route 호출 성공 |
| backend | route API가 GraphHopper profile 이름을 그대로 사용 |
