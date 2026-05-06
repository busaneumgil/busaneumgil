# 2026-05-07 271 접근성 profile route 검증 리뷰

## 커밋

`🔧 Chore[S14P31E102-271]: 접근성 profile route 검증 추가`

## 구현 범위

- `make graphhopper-dev-profile-smoke` target을 추가했다.
- dev GraphHopper runtime을 `docker compose up -d --build graphhopper`로 띄운 뒤 `/healthcheck`를 기다리도록 했다.
- dev DB의 `road_segments`에서 실제 routeable 후보 segment를 조회하고, 해당 segment 양 끝 좌표로 8개 접근성 profile `/route` 호출을 수행하도록 했다.
- 검증 결과는 graphhopper import volume의 `/graphhopper/import/road-network-profile-smoke-report.json`에 JSON report로 남긴다.

## 변경 파일

- `Makefile`
- `scripts/make/help.sh`
- `scripts/make/docker/graphhopper-dev-profile-smoke.sh`
- `scripts/graphhopper/smoke_graphhopper_profiles.py`
- `INF/graphhopper/Dockerfile`

## 검증 방식

1. dev 터널과 Docker daemon을 확인한다.
2. GraphHopper runtime을 최신 이미지로 build/up 한다.
3. runtime admin port `/healthcheck`가 통과할 때까지 대기한다.
4. smoke 컨테이너에서 dev DB 후보 segment를 조회한다.
5. `pedestrian_safe`, `pedestrian_fast`, `visual_safe`, `visual_fast`, `wheelchair_manual_safe`, `wheelchair_manual_fast`, `wheelchair_auto_safe`, `wheelchair_auto_fast` 순서로 `/route`를 호출한다.

## 실행 결과

```text
GraphHopper profile smoke ok: profiles=8, edgeId=164955, lengthMeter=3843.46
```

## 남은 검증

- `walk_access == NO` edge가 실제 대체 경로에서 제외되는지 확인하려면 차단 edge와 우회 edge가 함께 있는 fixture 또는 검증용 seed data가 필요하다.
- 계단, 좁은 보도, 비포장, 급경사, 신호 횡단 선호도는 현재 smoke에서 custom model 문법과 route 성공까지만 검증하고, 경로 선택 품질은 별도 시나리오 테스트로 분리하는 것이 맞다.
