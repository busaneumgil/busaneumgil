# S14P31E102-271 GraphHopper 접근성 encoded value 리뷰

## Implementation Harness Preflight

| 파일 | 이유 | 예상 영향 |
| --- | --- | --- |
| `INF/graphhopper/plugin/src/main/java/com/ssafy/e102/graphhopper/**` | GraphHopper 11 `ImportRegistry`에 이음길 EV 등록 | stock web jar 대신 이음길 Application으로 실행 |
| `INF/graphhopper/config-*.yml` | `graph.encoded_values`에 접근성 EV 추가 | graph-cache 재생성 필요 |
| `INF/graphhopper/Dockerfile` | plugin Java source compile 및 jar 포함 | build image가 JDK 기반으로 변경 |
| `scripts/docker/entrypoints/graphhopper*.sh` | 이음길 Application main class로 실행 | runtime/build 모두 custom registry 사용 |

## 구현 내용

- GraphHopper 11의 `ImportRegistry` 확장 방식에 맞춰 `IeumImportRegistry`를 추가했다.
- 아래 encoded value를 등록했다.
  - `walk_access`
  - `avg_slope_percent`
  - `width_meter`
  - `braille_block_state`
  - `audio_signal_state`
  - `slope_state`
  - `width_state`
  - `surface_state`
  - `stairs_state`
  - `signal_state`
  - `segment_type`
- `IeumGraphHopperApplication` / `IeumGraphHopperManaged`를 추가해 build/runtime 모두 custom registry를 사용한다.
- 이번 커밋의 parser는 fallback 값을 저장하는 최소 구현이다. 실제 `ieum:*` tag 값 파싱은 다음 커밋에서 반영한다.

## 검증

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml build graphhopper-build
docker run --rm --entrypoint java s14p31e102-dev-graphhopper-build \
  -cp /opt/graphhopper/plugin/ieum-graphhopper-plugin.jar:/opt/graphhopper/graphhopper-web.jar \
  com.ssafy.e102.graphhopper.IeumGraphHopperApplication check /opt/graphhopper/config-build.yml
```

결과:

- plugin Java source compile 성공
- `ieum-graphhopper-plugin.jar` 생성 성공
- GraphHopper config check 성공

## 주의사항

- 이 커밋 이후 생성되는 graph-cache는 custom EV layout을 포함한다.
- 기존 custom EV 없는 graph-cache와 호환되지 않으므로 build 후 runtime cache를 함께 갱신해야 한다.
