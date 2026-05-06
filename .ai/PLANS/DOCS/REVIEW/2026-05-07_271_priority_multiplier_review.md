# 2026-05-07 271 보행 priority multiplier 구현 리뷰

## 커밋

`✨ Feat[S14P31E102-271]: 보행 priority multiplier 반영`

## 구현 범위

- 8개 GraphHopper 접근성 profile custom model에 `priority.multiply_by` 조건을 추가했다.
- `walk_access == NO`는 모든 profile에서 차단(`0`)하도록 반영했다.
- 휠체어 profile은 `stairs_state == YES`를 차단하고, 수동 휠체어는 `avg_slope_percent >= 15.0`을 차단하도록 반영했다.
- `UNKNOWN` 상태는 차단하지 않고 profile 성격에 맞는 penalty만 적용했다.
- `segment_type == CROSS_WALK`와 `signal_state` 조합을 횡단보도 선호/회피 조건으로 반영했다.
- 시각장애 profile에는 `braille_block_state`, `audio_signal_state` 조건을 추가했다.
- GraphHopper 11 import 요구사항에 맞춰 `custom_models.directory`를 지정하고 `custom_model_files`는 directory 기준 상대 경로로 보정했다.

## 결정 사항

- 현재 구현된 encoded value enum은 `slope_state = FLAT/MODERATE/STEEP/UNKNOWN`, `width_state = ADEQUATE_150/NARROW_120/NARROW_90/UNKNOWN`이다.
- 계획 문서의 구버전 표현인 `RISK`, `ADEQUATE_120`, `NARROW`는 실제 parser enum에 존재하지 않으므로 custom model에는 넣지 않았다.
- 구현 매핑은 `ADEQUATE_120 -> NARROW_120`, `NARROW -> NARROW_90` 기준으로 반영했다.

## 검증

- `python3 -m json.tool INF/graphhopper/custom_models/wheelchair_manual_safe.json`
- `rg -n 'RISK|ADEQUATE_120|NARROW\"' INF/graphhopper/custom_models`
- `docker compose --env-file .env.dev -f docker-compose.dev.yml build graphhopper-build`
- `docker run --rm --entrypoint java s14p31e102-dev-graphhopper-build -cp /opt/graphhopper/plugin/ieum-graphhopper-plugin.jar:/opt/graphhopper/graphhopper-web.jar com.ssafy.e102.graphhopper.IeumGraphHopperApplication check /opt/graphhopper/config-build.yml`
- `make graphhopper-dev-build`

## 결과

- GraphHopper 설정 검사를 통과했다.
- dev DB export, OSM XML -> PBF 변환, graph-cache import가 완료됐다.
- import 로그에서 8개 profile의 subnetwork 작업이 모두 생성되는 것을 확인했다.

## 후속 확인

- 실제 `/route` 호출 성공과 profile별 경로 차이는 다음 `접근성 profile route 검증 추가` 커밋의 smoke target에서 검증한다.
