# Transit API 응답시간 개선 구현 계획

## 목표

- 대상 API: `POST /routes/search/transit`
- 측정 기준: 클라이언트 총 소요 시간. Postman `Time`과 같이 네트워크, TLS, 응답 body 다운로드 시간을 포함한다.
- 1차 목표: 외부 API fan-out을 줄여 timeout 빈도와 5초대 응답을 낮춘다.
- 이번 단계에서는 cache/DB 저장 구조를 먼저 도입하지 않는다. 호출 순서와 후보 축소 구조를 먼저 바꾼다.

## 현재 구현 기준

근거 코드:

- `BE/src/main/java/com/ssafy/e102/domain/route/service/TransitRouteSearchService.java`
- `BE/src/main/java/com/ssafy/e102/global/external/odsay/OdsayClient.java`
- `BE/src/main/java/com/ssafy/e102/global/external/bims/BusanBimsClient.java`
- `Docs/API/길안내_도메인/2026-05-06_경로_API_명세.md`
- `Docs/ERD/ERD_v4.md`

현재 흐름:

```text
1. ODsay searchPubTransPathT 1회
2. ODsay path 최대 10개 파싱
3. 모든 path에 대해 ODsay loadLane 호출
4. 모든 path의 WALK leg에 대해 GraphHopper 호출
5. baseCandidate 생성
6. baseCandidate 중 BIMS shortlist 최대 5개 선택
7. BIMS 호출
8. 최종 RECOMMENDED / MIN_TRANSFER / MIN_WALK 3개 선택
```

현재 개선된 점:

- BIMS는 이미 ODsay 전체 후보 10개가 아니라 shortlist 최대 5개 대상으로 제한되어 있다.
- 같은 요청 안에서 같은 `(stopId, lineId, routeNo)`는 dedupe된다.
- BIMS 호출은 `bimsTaskExecutor`로 병렬 처리된다.

남은 문제:

- `loadLane`과 GraphHopper는 아직 ODsay 후보 최대 10개 전체에 대해 호출된다.
- BIMS는 여전히 가장 느린 외부 호출이며, shortlist 5개 안의 BUS leg/lane 수에 따라 여러 번 호출된다.
- 현재 search 단계 BIMS timeout은 전체 응답 실패로 이어질 수 있다.

## 용어 정리

### ODsay search

`searchPubTransPathT`는 경로 후보 목록을 만드는 API다. path 후보는 여기서 나온다.

포함 정보:

- 총 소요 시간
- 총 거리
- 총 도보 거리
- 버스/지하철 탑승 횟수
- WALK / BUS / SUBWAY subPath
- 버스 번호, 지하철 노선명
- 정류장/역 식별자
- `mapObj`

### ODsay loadLane

`loadLane`은 path 후보를 만드는 API가 아니다. `search`가 준 `mapObj`로 특정 후보의 버스/지하철 선형 geometry를 가져오는 보강 API다.

### GraphHopper WALK leg

ODsay path 안의 WALK 구간을 접근성 보행 경로로 다시 계산하는 내부 경로 호출이다.

예:

- 출발지에서 첫 버스 정류장까지
- 지하철 엘리베이터에서 환승 정류장까지
- 하차 정류장에서 목적지까지

GraphHopper는 계단, 경사, 보도폭, 노면, 횡단보도 등 보행 profile을 반영한다. 이 값은 실시간 정보가 아니므로 다음 단계에서 캐시 후보가 될 수 있다.

## 변경 방향

목표 흐름:

```text
1. ODsay search
2. ODsay 원본 totalTime / transferCount / totalWalk 기준으로 1차 shortlist 선택
3. shortlist에만 ODsay loadLane + GraphHopper 수행
4. 완성된 후보 중 최종 3개를 먼저 선택
5. 최종 3개에만 BIMS 수행
6. 최종 3개 반환
```

## 5개 shortlist 후 3개로 줄이는 기준

1차 shortlist는 ODsay 원본 값만 사용한다.

- `RECOMMENDED` 후보군: `totalTime`, `transferCount`, `totalWalk` 순
- `MIN_TRANSFER` 후보군: `transferCount`, `totalTime`, `totalWalk` 순
- `MIN_WALK` 후보군: `totalWalk`, `totalTime`, `transferCount` 순
- 각 기준 상위 후보를 합치고 중복 route key를 제거한다.
- 1차 shortlist 상한은 5개로 둔다.

`loadLane + GraphHopper` 이후 최종 3개는 실제 생성된 `baseCandidate` 기준으로 다시 고른다.

- `RECOMMENDED`: 실제 `durationSecond`, `transferCount`, `totalWalkMeter` 순
- `MIN_TRANSFER`: `transferCount`, 실제 `durationSecond`, `totalWalkMeter` 순
- `MIN_WALK`: `totalWalkMeter`, 실제 `durationSecond`, `transferCount` 순
- 중복 route key는 합쳐서 `routeOptions`에 복수 라벨을 담는다.
- 최종 응답 개수는 최대 3개다.

이 기준은 거리와 시간만 보는 단순 정렬이 아니라, 현재 API가 노출하는 3개 선택지의 의미를 유지하는 방식이다.

## BIMS를 최종 3개에만 호출하는 것에 대한 판단

찬성한다. 현재 측정에서 BIMS가 가장 느리고 timeout 위험이 가장 크기 때문에, BIMS 호출 대상을 5개에서 최종 3개로 줄이는 것은 타당하다.

다만 trade-off가 있다.

- 장점: BIMS unique request 수가 줄고, 가장 느린 BIMS 호출에 걸릴 확률도 낮아진다.
- 장점: 최종 사용자에게 내려가지 않을 후보에는 실시간 도착정보를 조회하지 않는다.
- 단점: BIMS의 저상버스 여부가 4~5순위 후보를 RECOMMENDED로 끌어올리는 효과는 줄어든다.

이번 단계의 결정:

- 성능을 우선해 BIMS는 최종 3개 후보의 BUS leg/lane에만 호출한다.
- BIMS는 최종 3개 안에서 `laneOptions`의 `remainingMinute`, `isLowFloor`, warning 보강에 사용한다.
- 저상버스 여부로 최종 후보 진입 자체를 바꾸는 정책은 cache/DB 구조 도입 단계에서 노선 단위 저상버스 정적 정보와 함께 다시 설계한다.

## 구현 계획

### Implementation Harness Preflight

예상 변경 파일:

- `BE/src/main/java/com/ssafy/e102/domain/route/service/TransitRouteSearchService.java`
  - ODsay raw path 기준 1차 shortlist를 추가한다.
  - `loadLane + GraphHopper` 수행 대상을 전체 path에서 shortlist로 줄인다.
  - BIMS enrichment 대상을 BIMS shortlist 5개에서 BIMS 없는 정적 최종 후보 최대 3개로 줄인다.
- `BE/src/test/java/com/ssafy/e102/domain/route/service/TransitRouteSearchServiceTest.java`
  - `loadLane`이 ODsay 전체 후보가 아니라 1차 shortlist에만 호출되는지 검증한다.
  - BIMS 호출이 최종 3개 후보 기준으로 제한되는지 검증한다.
- `api_test_2.md`
  - 최신 before/dev 측정 결과와 after 측정 기준을 유지한다.

예상 부작용:

- BIMS 저상버스 여부가 4~5순위 후보를 최종 3개 안으로 끌어올리는 정책은 이번 단계에서 사라진다.
- 최종 후보가 BIMS 전 정적 기준으로 먼저 결정되므로, BIMS는 최종 후보의 도착정보와 warning 보강 역할에 집중한다.

검증:

- `TransitRouteSearchServiceTest`
- 로컬 Spring Boot `8081` 기동 후 동일 좌표 `POST /routes/search/transit` 1회 호출
- 응답 `200/S2000`, route count, response size, BIMS unique request 수 로그 확인

롤백 기준:

- route option 병합이 깨지거나 최종 후보가 비어 `RT4040`이 증가한다.
- BUS/SUBWAY/WALK leg geometry가 누락된다.
- 기존 transit search 단위 테스트가 실패한다.

### 1. ODsay raw path 기반 shortlist 추가

- `TransitRouteSearchService`에 `selectOdsayShortlist(List<OdsayTransitPath>)`를 추가한다.
- `OdsayTransitPath`의 원본 값으로 비교한다.
  - `totalTimeMinute`
  - `busTransitCount + subwayTransitCount - 1`
  - `totalWalkMeter`
- route 중복 제거 key는 가능하면 `mapObj`와 transit leg 조합을 사용한다.
- shortlist limit은 5로 시작한다.

### 2. loadLane + GraphHopper 적용 범위 축소

기존:

```text
for searchResult.paths 최대 10개:
  loadLane
  GraphHopper WALK leg
```

변경:

```text
for selectOdsayShortlist(searchResult.paths) 최대 5개:
  loadLane
  GraphHopper WALK leg
```

기대 효과:

- ODsay `loadLane`: 최대 10회에서 최대 5회로 감소
- GraphHopper: 전체 후보의 WALK leg 수 기준에서 shortlist의 WALK leg 수 기준으로 감소

### 3. BIMS 전 최종 3개 선택

- `baseCandidates` 생성 후 `selectStaticFinalCandidates(baseCandidates)`를 추가한다.
- 이 함수는 BIMS 없는 `TransitRouteCandidate`를 기준으로 `RECOMMENDED`, `MIN_TRANSFER`, `MIN_WALK`를 먼저 고른다.
- 중복 route는 합치고 최대 3개만 남긴다.
- 이 3개만 `enrichBimsCandidates`로 넘긴다.

### 4. BIMS enrichment 범위 축소

기존:

```text
enrichBimsCandidates(selectBimsShortlist(baseCandidates))
```

변경:

```text
staticSelected = selectStaticFinalCandidates(baseCandidates)
enriched = enrichBimsCandidates(staticSelected)
selected = selectCandidates(enriched)
```

`selectCandidates(enriched)`는 최종 3개 안에서 route option label을 다시 정리하는 용도로 유지한다.

### 5. 로그 보강

Scouter 없이도 병목을 확인할 수 있도록 search 1회당 요약 로그를 남긴다.

필수 로그 필드:

- `searchId`
- `odsayPathCount`
- `odsayShortlistCount`
- `loadLaneRequestCount`
- `graphHopperRequestCount`
- `staticFinalCount`
- `bimsUniqueRequestCount`
- `elapsedMs`
- `phaseElapsedMs.search`
- `phaseElapsedMs.loadLaneGraphHopper`
- `phaseElapsedMs.bims`

로그 예:

```text
transit_search_perf searchId=rs_transit_x odsayPathCount=10 odsayShortlistCount=5 loadLaneRequestCount=5 graphHopperRequestCount=13 staticFinalCount=3 bimsUniqueRequestCount=3 elapsedMs=2340 searchMs=180 buildMs=730 bimsMs=980
```

### 6. 테스트 계획

단위 테스트:

- ODsay path 10개가 들어와도 `loadLane`은 shortlist 개수만 호출된다.
- shortlist limit이 5를 넘지 않는다.
- BIMS는 최종 3개 후보에 대해서만 호출된다.
- 같은 `(stopId, lineId, routeNo)`는 계속 1회만 호출된다.
- `RECOMMENDED`, `MIN_TRANSFER`, `MIN_WALK` route option이 유지된다.
- 중복 route는 하나로 합쳐지고 `routeOptions`에 복수 라벨이 들어간다.

회귀 테스트:

- subway-only 후보는 BIMS 0회여야 한다.
- BUS 후보가 없는 최종 3개에서는 BIMS 호출이 없어야 한다.
- GraphHopper `ROUTE_NOT_FOUND` 후보는 기존처럼 제외되어야 한다.
- 모든 후보가 제외되면 `RT4040` 또는 기존 external failure 정책을 유지한다.

## 응답시간 측정 도구 선택

### 선택지 1. `curl -w` + shell 집계

장점:

- 설치 부담이 거의 없다.
- Postman `Time`과 유사한 `time_total`을 바로 볼 수 있다.
- 단일 요청 반복 측정에는 충분하다.

단점:

- p95, max, 실패율 집계를 직접 작성해야 한다.
- 인증 토큰, 요청 payload, 결과 파일 관리가 번거롭다.

### 선택지 2. `k6`

장점:

- 클라이언트 총 소요 시간 기준의 `http_req_duration` p50/p90/p95/max를 바로 제공한다.
- 시나리오를 코드로 고정할 수 있어 변경 전/후 비교가 쉽다.
- `--summary-export`로 JSON 결과를 저장할 수 있다.
- Scouter처럼 서버 APM을 붙이지 않아도 응답시간 회귀 확인에 충분하다.

단점:

- 로컬 또는 CI에 k6 설치가 필요하다.
- 서버 내부 phase별 병목은 별도 애플리케이션 로그가 필요하다.

### 선택지 3. `hey`

장점:

- 설치와 실행이 단순하다.
- 평균, p95, max를 빠르게 확인할 수 있다.

단점:

- 복잡한 인증/요청 payload/시나리오 관리가 k6보다 약하다.
- 결과를 장기 비교용 artifact로 남기기에는 k6보다 불편하다.

### 추천

이번 작업에는 `k6 + 애플리케이션 요약 로그`를 추천한다.

- `k6`: 클라이언트가 느끼는 총 응답시간, p95, max, timeout 여부 측정
- 애플리케이션 로그: search 1회 안에서 ODsay, loadLane/GraphHopper, BIMS가 각각 얼마나 걸렸는지 확인

측정 예:

```bash
k6 run scripts/perf/transit-search.k6.js --summary-export .tmp/transit-search-summary.json
```

비교 기준:

- 변경 전/후 같은 route request를 10회 반복한다.
- 단일 호출은 외부 ODsay/BIMS 지연에 크게 흔들리므로 평균만 보지 않는다.
- 주요 지표는 `median/p50`, `mean`, `p90`, `max`, `failed requests`로 본다.
- 로컬 backend 측정에서는 서버 로그의 `bimsUniqueRequestCount`, `loadLaneRequestCount`, `graphHopperRequestCount`가 의도대로 줄었는지 함께 확인한다.

## 완료 기준

- ODsay `loadLane` 호출 수가 ODsay 전체 후보 수가 아니라 1차 shortlist 수 이하로 제한된다.
- GraphHopper 호출 수가 1차 shortlist의 WALK leg 수 기준으로 제한된다.
- BIMS 호출 수가 최종 3개 후보의 unique BUS lane key 수 기준으로 제한된다.
- `/routes/search/transit` 응답 shape는 기존 API 계약과 호환된다.
- 단위 테스트가 호출 수 축소와 route option 유지 정책을 검증한다.
- k6 측정 결과와 애플리케이션 요약 로그로 변경 전/후 응답시간 비교가 가능하다.

## Before/After 테스트 시나리오

### 공통 조건

- 목적: 개선 전과 개선 후의 `POST /routes/search/transit` 클라이언트 총 소요 시간을 같은 조건으로 비교한다.
- endpoint: `POST /routes/search/transit`
- 도보 API는 호출하지 않는다. `POST /routes/search/walk`는 테스트 대상에서 제외한다.
- 사용자 유형: 전동휠체어 사용자
- 인증: 아래에 고정한 전동휠체어 사용자 access token을 사용한다.
- 요청 좌표는 before/after 모두 동일하게 고정한다.
- before/after 모두 같은 네트워크 환경, 같은 API host, 같은 시간대에 최대한 가깝게 수행한다.
- smoke FE dev server(`localhost:3000`)는 이번 성능 테스트 범위에서 제외한다.

### 실행 환경

이 문서의 before/after 기본 실행 환경은 API 프로세스만 로컬에서 실행하고 무거운 의존성은 dev 서버 자원을 사용하는 방식이다.

로컬에서 실행:

- Spring Boot backend: `http://localhost:8081`
- k6: `.ai/LOCAL/bin/k6`

dev 서버 또는 외부 자원 사용:

- dev PostGIS/PostgreSQL: SSH tunnel `localhost:15432`
- dev Redis: SSH tunnel `localhost:16379`
- dev GraphHopper: SSH tunnel `localhost:18989`
- ODsay: 외부 API 직접 호출
- BIMS: 외부 API 직접 호출

이번 before/after 비교의 k6 `BASE_URL`은 `https://api.dev.busaneumgil.com`가 아니라 `http://localhost:8081`이다. dev URL을 쓰면 로컬 변경 코드가 아니라 dev 서버에 배포된 backend를 측정하게 된다.

단, 팀원이 머지한 뒤 배포된 dev 서버 자체를 확인할 때는 동일 시나리오를 `https://api.dev.busaneumgil.com`에 직접 호출한다. 이 결과는 로컬 before/after와 분리해서 "dev 배포 서버 참고 측정"으로 기록한다.

### Access Token

테스트용 전동휠체어 사용자 access token:

```bash
export ACCESS_TOKEN='eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJlMTAyLWRldiIsInN1YiI6ImQ2OWIzZDhhLWM3Y2QtNGIxYS1iMzVmLTRmY2QxMzNkN2ZmYyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTc3ODU5OTY1OCwiZXhwIjoxNzc4NjAwNTU4fQ.SDuYVubk1xcFH2kPGOhTjEwv_XfpPlk5Vrp9Cob_yIU'
```

토큰 payload 기준:

- issuer: `e102-dev`
- user id: `d69b3d8a-c7cd-4b1a-b35f-4fcd133d7ffc`
- issued at: `2026-05-13 00:27:38 KST`
- expires at: `2026-05-13 00:42:38 KST`

만료되면 같은 사용자 유형의 dev access token을 다시 발급해 위 `ACCESS_TOKEN` 값을 교체한다.

토큰 만료 확인:

```bash
node -e "const t=process.env.ACCESS_TOKEN; const p=JSON.parse(Buffer.from(t.split('.')[1],'base64url')); console.log(new Date(p.exp*1000).toISOString(), Date.now()<p.exp*1000)"
```

### Dev 의존성 tunnel

Spring Boot보다 tunnel을 먼저 연다. 이미 열려 있더라도 오래된 연결이면 DB/Redis/GraphHopper connection pool이 끊어진 tunnel을 물 수 있으므로, 의심되면 tunnel과 Spring Boot를 모두 재시작한다.

권장 포트:

| local port | 대상 |
| --- | --- |
| `15432` | dev PostGIS/PostgreSQL |
| `16379` | dev Redis |
| `18989` | dev GraphHopper |

수동 tunnel 예시:

```bash
chmod 600 K14E102T.pem
ssh -i K14E102T.pem -N \
  -L 127.0.0.1:15432:127.0.0.1:5432 \
  -L 127.0.0.1:16379:127.0.0.1:6379 \
  -L 127.0.0.1:18989:127.0.0.1:8998 \
  ubuntu@k14e102.p.ssafy.io
```

tunnel 확인:

```bash
lsof -nP -iTCP:15432 -sTCP:LISTEN
lsof -nP -iTCP:16379 -sTCP:LISTEN
lsof -nP -iTCP:18989 -sTCP:LISTEN
nc -vz localhost 15432
nc -vz localhost 16379
nc -vz localhost 18989
```

### 로컬 Spring Boot 실행

`.env.dev`를 process environment로 주입한 뒤, dev 서버 내부 host 값을 tunnel local port로 override한다.

```bash
set -a
source .env.dev
set +a

export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8081
export DB_URL="jdbc:postgresql://localhost:15432/${POSTGRES_DB:-e102}"
export REDIS_HOST=localhost
export REDIS_PORT=16379
export GRAPHHOPPER_BASE_URL=http://localhost:18989

cd BE
./gradlew bootRun
```

주의:

- Spring Boot를 tunnel보다 먼저 켰거나 tunnel을 재시작했다면 Spring Boot도 재시작한다.
- `make be-dev-up`은 Docker backend container 방식이므로 이번 before/after 기본 경로로 쓰지 않는다.
- smoke FE dev server는 `localhost:3000`을 사용하므로 API 테스트 backend 포트 `8081`과 충돌하지 않는다.
- 로컬 변경 코드의 before/after를 보려면 항상 `BASE_URL=http://localhost:8081`로 k6를 실행한다.
- 응답시간 비교를 시작한 뒤에는 Spring Boot 서버를 종료하지 않는다. 첫 요청에는 JVM, DispatcherServlet, HTTP client, DB pool warm-up 영향이 섞일 수 있으므로, 같은 PID를 유지한 상태에서 10회 표본을 비교한다.

### 테스트 좌표

출발지: 신호동 행정복지센터

- 위도: `35.0854552`
- 경도: `128.87913`

도착지: 명지1동 행정복지센터

- 위도: `35.0968107`
- 경도: `128.913377`

요청 body:

```json
{
  "startPoint": {
    "lat": 35.0854552,
    "lng": 128.87913
  },
  "endPoint": {
    "lat": 35.0968107,
    "lng": 128.913377
  }
}
```

### Before 측정

대상:

- 현재 구현 또는 개선 전 배포 버전
- ODsay 후보 전체에 대해 `loadLane + GraphHopper`를 수행하고, BIMS는 현재 shortlist 정책을 따르는 버전

실행:

```bash
export ACCESS_TOKEN='eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJlMTAyLWRldiIsInN1YiI6ImQ2OWIzZDhhLWM3Y2QtNGIxYS1iMzVmLTRmY2QxMzNkN2ZmYyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTc3ODU5OTY1OCwiZXhwIjoxNzc4NjAwNTU4fQ.SDuYVubk1xcFH2kPGOhTjEwv_XfpPlk5Vrp9Cob_yIU'
BASE_URL=http://localhost:8081 \
K6_SCENARIO=fixed \
K6_VUS=1 \
K6_DURATION=1m \
.ai/LOCAL/perf/transit/run_k6_transit_search.sh
```

확인 지표:

- `http_req_duration` p50, p90, p95, max
- 실패율
- timeout 발생 수
- 서버 로그의 `loadLaneRequestCount`
- 서버 로그의 `graphHopperRequestCount`
- 서버 로그의 `bimsUniqueRequestCount`
- 서버 로그의 phase별 elapsed ms

### After 측정

대상:

- 이번 개선이 반영된 버전
- ODsay 원본 기준 1차 shortlist 후, shortlist에만 `loadLane + GraphHopper`를 수행하는 버전
- BIMS는 최종 3개 후보에만 호출하는 버전

실행:

```bash
export ACCESS_TOKEN='eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJlMTAyLWRldiIsInN1YiI6ImQ2OWIzZDhhLWM3Y2QtNGIxYS1iMzVmLTRmY2QxMzNkN2ZmYyIsInRva2VuVHlwZSI6IkFDQ0VTUyIsImlhdCI6MTc3ODU5OTY1OCwiZXhwIjoxNzc4NjAwNTU4fQ.SDuYVubk1xcFH2kPGOhTjEwv_XfpPlk5Vrp9Cob_yIU'
BASE_URL=http://localhost:8081 \
K6_SCENARIO=fixed \
K6_VUS=1 \
K6_DURATION=1m \
.ai/LOCAL/perf/transit/run_k6_transit_search.sh
```

확인 지표:

- `http_req_duration` p50, p90, p95, max
- 실패율
- timeout 발생 수
- 서버 로그의 `loadLaneRequestCount`
- 서버 로그의 `graphHopperRequestCount`
- 서버 로그의 `bimsUniqueRequestCount`
- 서버 로그의 phase별 elapsed ms

### After 단일 확인 결과 - local 1회 - 2026-05-13 01:03 KST

실행 환경:

- backend: local Spring Boot `http://127.0.0.1:8081`
- Spring Boot PID: `14882`
- Spring profile: `dev`
- 사용자 유형: 전동휠체어
- 좌표: 신호동 행정복지센터 -> 명지1동 행정복지센터
- 산출물 디렉터리: `.ai/LOCAL/EVALS/transit-api-perf/20260513-010259-after-single-local-8081`

결과:

| 항목 | 값 |
| --- | ---: |
| HTTP status | `200` |
| API status | `S2000` |
| API message | `정상 처리되었습니다.` |
| client `time_total` | `24.552312s` |
| client `time_starttransfer` | `24.312417s` |
| response size | `17,171 bytes` |
| route count | `2` |
| route options | `RECOMMENDED`, `MIN_WALK` |
| server `latency_ms` | `10,622ms` |
| ODsay path count | `8` |
| ODsay shortlist count | `5` |
| static final count | `2` |
| BIMS `uniqueArrivalRequestCount` | `2` |

주의:

- 이 호출은 새 Spring Boot 프로세스의 첫 transit 요청이므로 DispatcherServlet 초기화와 첫 외부 API 호출 지연이 섞였다.
- 기능 확인 목적의 smoke로만 사용하고, 실제 after 성능 비교는 warmed 상태 10회 표본으로 다시 측정한다.
- 호출 수 관점에서는 ODsay path `8`개 중 shortlist `5`개만 보강했고, BIMS unique request는 before `5`에서 after `2`로 줄었다.

### 비교 기준

before/after 비교는 같은 시나리오와 같은 좌표로만 수행한다.

1차 성공 기준:

- `time_total` 또는 `http_req_duration`의 median/p50 감소
- `time_total` 또는 `http_req_duration`의 p90/max 감소
- timeout 또는 failed request 감소
- `loadLaneRequestCount`가 ODsay 전체 후보 수가 아니라 shortlist 수 이하로 감소
- `graphHopperRequestCount`가 shortlist의 WALK leg 수 기준으로 감소
- `bimsUniqueRequestCount`가 최종 3개 후보의 unique BUS lane key 수 기준으로 감소

주의:

- BIMS는 외부 실시간 API라 시간대별 흔들림이 크다.
- 따라서 단일 요청 1회가 아니라 동일 조건 10회 표본으로 비교한다.
- 부하 테스트가 아니라 응답시간 회귀 확인 목적이므로 `K6_VUS=1`부터 시작한다.
- 필요하면 같은 조건으로 `K6_DURATION=1m` 또는 `3m`까지 늘려 p95/max 안정성을 확인한다.

### Before 측정 결과 - 2026-05-12 23:31 KST

실행 환경:

- backend: local Spring Boot `http://127.0.0.1:8081`
- Spring profile: `dev`
- dev dependency tunnel:
  - DB `127.0.0.1:15432`
  - Redis `127.0.0.1:16379`
  - GraphHopper `127.0.0.1:18989`
- scenario: `fixed`
- 사용자 유형: 전동휠체어
- 좌표: 신호동 행정복지센터 -> 명지1동 행정복지센터
- 산출물 디렉터리: `.ai/LOCAL/EVALS/transit-api-perf/20260512-233059-before-local-8081`

주의:

- 문서에 고정했던 token은 `2026-05-12 23:18:41 KST`에 만료되어 그대로 사용할 수 없었다.
- 같은 전동휠체어 smoke user로 새 access token을 발급해 측정했다.
- 이번 측정은 부하 테스트가 아니라 단일 사용자 응답시간 샘플링이다.

단일 API 호출 결과:

| 항목 | 값 |
| --- | --- |
| HTTP status | `200` |
| API status | `S2000` |
| `curl time_total` | `8.030838s` |
| `curl time_starttransfer` | `8.015634s` |
| response size | `18,525 bytes` |
| route count | `2` |
| route options | `["RECOMMENDED", "MIN_TRANSFER"]`, `["MIN_WALK"]` |
| route estimated minutes | `27`, `28` |
| leg types | `WALK -> BUS -> WALK`, `WALK -> BUS -> WALK` |
| BUS lane option count in response | `2` |

서버 로그 관측:

| 항목 | 값 |
| --- | --- |
| ODsay candidate count | `8` |
| BIMS shortlist count | `5` |
| shortlisted BUS leg count | `5` |
| shortlisted BUS lane option count | `5` |
| BIMS unique arrival request count | `5` |
| first request server latency | `5,306ms` |

k6 1 VU / 1분 샘플링 결과:

| 항목 | 값 |
| --- | --- |
| command | `K6_SCENARIO=fixed K6_VUS=1 K6_DURATION=1m` |
| iterations / HTTP requests | `22` |
| failed requests | `0 / 22` |
| checks | `88 / 88 passed` |
| `http_req_duration avg` | `1.767s` |
| `http_req_duration min` | `893.626ms` |
| `http_req_duration median` | `1.786s` |
| `http_req_duration p90` | `2.242s` |
| `http_req_duration p95` | `2.588s` |
| `http_req_duration max` | `2.648s` |
| `client_total_duration p95` | `2.588s` |

해석:

- warm-up 전 단일 호출은 `8.03s`로 느렸다. 이 호출에는 dispatcher 초기화 이후 첫 transit 요청 비용과 외부 호출 지연이 함께 섞였다.
- k6 반복 샘플링에서는 p95 `2.588s`, max `2.648s`로 안정화됐다.
- 현재 before 코드에서는 이 좌표에서 ODsay 후보 `8`개를 받은 뒤 BIMS shortlist `5`개, BIMS unique request `5`개가 발생했다.
- after에서는 같은 조건에서 `loadLane/GraphHopper`가 1차 shortlist 이하로 줄고, BIMS unique request가 최종 3개 후보 기준으로 줄었는지 비교한다.

### Before 측정 결과 - local 10회 warmed sample - 2026-05-13 00:20 KST

실행 환경:

- backend: local Spring Boot `http://127.0.0.1:8081`
- Spring Boot PID: `70539`
- 서버 상태: 재시작하지 않은 warmed 프로세스 유지
- Spring profile: `dev`
- dev dependency tunnel:
  - DB `127.0.0.1:15432`
  - Redis `127.0.0.1:16379`
  - GraphHopper `127.0.0.1:18989`
- 사용자 유형: 전동휠체어
- 좌표: 신호동 행정복지센터 -> 명지1동 행정복지센터
- 요청 간격: 2초
- 산출물 디렉터리: `.ai/LOCAL/EVALS/transit-api-perf/20260513-002020-before-10x-warmed-local-8081`

요약:

| 항목 | 값 |
| --- | ---: |
| 성공률 | `10/10` |
| client `time_total` min | `0.820781s` |
| client `time_total` median | `1.768989s` |
| client `time_total` mean | `1.799666s` |
| client `time_total` p90 | `2.304498s` |
| client `time_total` max | `3.944978s` |
| server `latency_ms` min | `775ms` |
| server `latency_ms` median | `1721.5ms` |
| server `latency_ms` mean | `1738.7ms` |
| server `latency_ms` p90 | `2246.9ms` |
| server `latency_ms` max | `3767ms` |
| BIMS `uniqueArrivalRequestCount` | 매 요청 `5` |

요청별 결과:

| # | client `time_total` | server `latency_ms` | BIMS unique |
| ---: | ---: | ---: | ---: |
| 1 | `3.944978s` | `3767ms` | `5` |
| 2 | `0.820781s` | `775ms` | `5` |
| 3 | `2.084519s` | `2041ms` | `5` |
| 4 | `1.361590s` | `1317ms` | `5` |
| 5 | `1.069482s` | `1025ms` | `5` |
| 6 | `1.800379s` | `1752ms` | `5` |
| 7 | `1.737600s` | `1691ms` | `5` |
| 8 | `2.122222s` | `2078ms` | `5` |
| 9 | `1.198173s` | `1155ms` | `5` |
| 10 | `1.856937s` | `1786ms` | `5` |

해석:

- warmed 상태에서도 client `time_total`은 `0.82s`부터 `3.94s`까지 흔들렸다.
- 단일 호출 결과로 개선 여부를 판단하면 외부 API 변동성에 쉽게 왜곡된다.
- after 비교는 이 local 10회 표본과 같은 조건으로 수행한다.
- 핵심 비교값은 client median/mean/p90/max와 server `latency_ms`, BIMS unique request 수다.

### After 측정 결과 - local 10회 sample - 2026-05-13 01:55 KST

실행 환경:

- backend: local Spring Boot `http://127.0.0.1:8081`
- Spring Boot PID: `40940`
- 서버 상태: 개선 코드 반영을 위해 Spring Boot 재시작 후 측정
- Spring profile: `dev`
- dev dependency tunnel:
  - DB `127.0.0.1:15432`
  - Redis `127.0.0.1:16379`
  - GraphHopper `127.0.0.1:18989`
- 사용자 유형: 전동휠체어
- 좌표: 신호동 행정복지센터 -> 명지1동 행정복지센터
- 요청 간격: 2초
- 산출물 디렉터리: `.ai/LOCAL/EVALS/transit-api-perf/20260513-014836-after-10x-warmed-local-8081`

요약:

| 항목 | 값 |
| --- | ---: |
| 성공률 | `10/10` |
| HTTP status | 모두 `200` |
| API status | 모두 `S2000` |
| route count | 매 요청 `2` |
| response size | 매 요청 `17,171 bytes` |
| route options | 매 요청 `RECOMMENDED + MIN_TRANSFER`, `MIN_WALK` |
| client `time_total` min | `0.871840s` |
| client `time_total` median | `1.367307s` |
| client `time_total` mean | `2.106353s` |
| client `time_total` p90 | `3.150750s` |
| client `time_total` max | `8.348405s` |
| server `latency_ms` min | `756ms` |
| server `latency_ms` median | `1271.5ms` |
| server `latency_ms` mean | `1601.4ms` |
| server `latency_ms` p90 | `2662.4ms` |
| server `latency_ms` max | `4061ms` |
| ODsay path count | 매 요청 `8` |
| ODsay shortlist count | 매 요청 `5` |
| static final count | 매 요청 `2` |
| BIMS `uniqueArrivalRequestCount` | 매 요청 `2` |

요청별 결과:

| # | client `time_total` | server `latency_ms` | BIMS unique |
| ---: | ---: | ---: | ---: |
| 1 | `8.348405s` | `4061ms` | `2` |
| 2 | `1.643569s` | `1549ms` | `2` |
| 3 | `2.573233s` | `2507ms` | `2` |
| 4 | `1.424410s` | `1350ms` | `2` |
| 5 | `1.673720s` | `1612ms` | `2` |
| 6 | `1.033240s` | `976ms` | `2` |
| 7 | `1.310203s` | `1163ms` | `2` |
| 8 | `1.266478s` | `1193ms` | `2` |
| 9 | `0.918436s` | `847ms` | `2` |
| 10 | `0.871840s` | `756ms` | `2` |

재시작 직후 첫 요청 제외 참고값:

| 항목 | 값 |
| --- | ---: |
| client `time_total` median | `1.310203s` |
| client `time_total` mean | `1.412792s` |
| client `time_total` p90 | `1.853623s` |
| client `time_total` max | `2.573233s` |
| server `latency_ms` median | `1193ms` |
| server `latency_ms` mean | `1328.1ms` |
| server `latency_ms` p90 | `1791.0ms` |
| server `latency_ms` max | `2507ms` |

Before local 10회 warmed sample 대비:

| 항목 | before | after | 변화 |
| --- | ---: | ---: | ---: |
| client `time_total` median | `1.768989s` | `1.367307s` | `-22.7%` |
| client `time_total` mean | `1.799666s` | `2.106353s` | `+17.0%` |
| client `time_total` p90 | `2.304498s` | `3.150750s` | `+36.7%` |
| client `time_total` max | `3.944978s` | `8.348405s` | `+111.6%` |
| server `latency_ms` median | `1721.5ms` | `1271.5ms` | `-26.1%` |
| server `latency_ms` mean | `1738.7ms` | `1601.4ms` | `-7.9%` |
| server `latency_ms` p90 | `2246.9ms` | `2662.4ms` | `+18.5%` |
| server `latency_ms` max | `3767ms` | `4061ms` | `+7.8%` |
| BIMS `uniqueArrivalRequestCount` | 매 요청 `5` | 매 요청 `2` | `-60.0%` |

해석:

- 호출 수 관점에서는 BIMS unique request가 before `5`에서 after `2`로 줄어 의도한 최종 후보 기준 enrichment가 확인됐다.
- client median과 server median은 개선됐지만, 재시작 직후 첫 요청의 client `8.348405s` 때문에 전체 mean/p90/max는 악화됐다.
- 첫 요청을 제외한 2~10회 기준 client median은 `1.310203s`, p90은 `1.853623s`로 before warmed sample보다 낮다.
- 외부 ODsay/BIMS 지연 변동성이 있으므로 release 판단에는 같은 PID를 유지한 추가 반복 표본을 한 번 더 보는 것이 안전하다.

### Dev 배포 서버 참고 측정 결과 - 10회 sample - 2026-05-13 00:39 KST

목적:

- 현재 배포된 `https://api.dev.busaneumgil.com` 서버를 같은 시나리오로 직접 호출했을 때의 client 총 소요 시간을 확인한다.
- 이 값은 로컬 변경 코드의 before/after가 아니라, dev 배포 서버 현재 상태에 대한 참고 baseline이다.

실행 환경:

- endpoint: `POST https://api.dev.busaneumgil.com/routes/search/transit`
- 사용자 유형: 전동휠체어
- access token: 이 문서의 재발급 token 사용
- 좌표: 신호동 행정복지센터 -> 명지1동 행정복지센터
- 요청 간격: 2초
- 산출물 디렉터리: `.ai/LOCAL/EVALS/transit-api-perf/20260513-003956-dev-10x-final-chat-only`

요약:

| 항목 | 값 |
| --- | ---: |
| 성공률 | `10/10` |
| HTTP status | 모두 `200` |
| API status | 모두 `S2000` |
| route count | 매 요청 `2` |
| response size | 매 요청 `19,412 bytes` |
| route options | 매 요청 `RECOMMENDED`, `MIN_WALK` |
| client `time_total` min | `1.525594s` |
| client `time_total` median | `2.967898s` |
| client `time_total` mean | `2.772463s` |
| client `time_total` p90 | `3.341644s` |
| client `time_total` max | `3.874174s` |
| client `time_starttransfer` median | `2.950570s` |

요청별 결과:

| # | client `time_total` | response size | route count | route options |
| ---: | ---: | ---: | ---: | --- |
| 1 | `1.525594s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |
| 2 | `2.640979s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |
| 3 | `3.107409s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |
| 4 | `3.117428s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |
| 5 | `3.282474s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |
| 6 | `2.995244s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |
| 7 | `2.121677s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |
| 8 | `2.119095s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |
| 9 | `3.874174s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |
| 10 | `2.940552s` | `19,412 bytes` | `2` | `RECOMMENDED`, `MIN_WALK` |

주의:

- dev 배포 서버 직접 호출은 서버 로그를 함께 보지 않았으므로 `BIMS uniqueArrivalRequestCount`, `loadLaneRequestCount`, `graphHopperRequestCount`는 확인하지 못했다.
- 이번 재측정에서는 10회 모두 route count와 response size가 같았다. after 비교 시에도 route count와 response size를 함께 확인한다.
- 로컬 before와 dev 배포 서버는 실행 위치, TLS, 네트워크 경로, 서버 리소스가 다르므로 숫자를 직접 비교하지 않는다.

## 후속 단계

이번 리팩토링 이후 다음 단계에서 cache/DB 저장 구조를 설계한다.

- `loadLane(mapObj)` 결과 cache 또는 DB 저장
- GraphHopper WALK leg cache
- `stopId + routeNo -> lineId` 매핑 저장
- BIMS `stopId + lineId` Redis TTL cache를 search에도 적용
- BIMS timeout 시 search 전체 실패 대신 `remainingMinute=null`, `isLowFloor=null`로 degrade
