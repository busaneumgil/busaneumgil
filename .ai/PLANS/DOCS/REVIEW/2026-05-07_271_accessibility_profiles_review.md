# S14P31E102-271 접근성 profile 8종 추가 리뷰

## Implementation Harness Preflight

| 파일 | 이유 | 예상 영향 |
| --- | --- | --- |
| `INF/graphhopper/config-build.yml` | build job이 8개 운영 profile 기준으로 graph-cache 생성 | 기존 `foot` fallback 제거 |
| `INF/graphhopper/config-runtime.yml` | runtime이 8개 운영 profile만 제공 | backend 요청 profile과 일치 |
| `INF/graphhopper/config-local.yml` | local config도 동일 profile contract 유지 | local/dev/prod 설정 차이 감소 |
| `INF/graphhopper/custom_models/*.json` | profile별 speed와 distance influence 선언 | priority multiplier는 후속 커밋에서 보강 |
| `INF/graphhopper/Dockerfile` | custom model 파일을 이미지에 포함 | graph-cache build/runtime 모두 동일 모델 사용 |

## 구현 내용

- 운영 profile 8종을 추가했다.
  - `pedestrian_safe`
  - `pedestrian_fast`
  - `visual_safe`
  - `visual_fast`
  - `wheelchair_manual_safe`
  - `wheelchair_manual_fast`
  - `wheelchair_auto_safe`
  - `wheelchair_auto_fast`
- `foot` fallback profile은 제거했다.
- profile별 기본 속도 `speed.limit_to`와 `distance_influence`를 문서 기준으로 반영했다.
- custom model JSON은 `/opt/graphhopper/custom_models`에 복사되며 config에서 절대 경로로 참조한다.

## 검증

```bash
python3 -m json.tool INF/graphhopper/custom_models/pedestrian_safe.json
docker compose --env-file .env.dev -f docker-compose.dev.yml build graphhopper-build
docker run --rm --entrypoint java s14p31e102-dev-graphhopper-build \
  -cp /opt/graphhopper/plugin/ieum-graphhopper-plugin.jar:/opt/graphhopper/graphhopper-web.jar \
  com.ssafy.e102.graphhopper.IeumGraphHopperApplication check /opt/graphhopper/config-build.yml
```

결과:

- custom model JSON 파싱 성공
- graphhopper-build image build 성공
- GraphHopper config check 성공

## 주의사항

- 이번 커밋은 profile과 기본 speed/distance influence만 추가했다.
- 접근성 조건별 priority multiplier는 다음 커밋에서 반영한다.
