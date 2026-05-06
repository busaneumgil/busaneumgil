# S14P31E102-271 ieum OSM tag parser 리뷰

## Implementation Harness Preflight

| 파일 | 이유 | 예상 영향 |
| --- | --- | --- |
| `INF/graphhopper/plugin/src/main/java/com/ssafy/e102/graphhopper/ieum/Ieum*TagParser.java` | `ieum:*` OSM way tag를 GraphHopper EV로 저장 | route custom model에서 접근성 값을 사용할 기반 마련 |
| `INF/graphhopper/plugin/src/main/java/com/ssafy/e102/graphhopper/ieum/IeumEncodedValues.java` | EV 이름과 OSM tag 이름 매핑 | exporter tag contract와 parser contract 연결 |

## 구현 내용

- `IeumEnumTagParser`가 `way.getTag("ieum:{ev}")` 값을 읽어 enum EV에 저장하도록 변경했다.
- `IeumDecimalTagParser`가 `ieum:avg_slope_percent`, `ieum:width_meter` 값을 `double`로 파싱해 decimal EV에 저장하도록 변경했다.
- 값이 없거나 잘못된 경우 fallback을 유지한다.
  - enum 기본값: 대부분 `UNKNOWN`
  - `segment_type`: `SIDE_LINE`
  - numeric: `0.0`
- `segment_type` enum은 `CROSS_WALK`, `SIDE_LINE`만 허용한다.

## 검증

```bash
make graphhopper-dev-build
```

결과:

- OSM export: `187975 nodes`, `204582 segments`
- validation report: `PASS`
- PBF 변환 성공
- `IeumGraphHopperApplication`으로 GraphHopper import 성공
- import log에서 custom EV 포함 확인
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
- graph-cache publish 완료

## 주의사항

- 현재는 custom EV 저장까지 완료된 상태다.
- EV를 실제 routing priority로 사용하는 profile/custom model은 후속 커밋에서 반영한다.
