# LLM Test — AI 음성 명령 추론 서버

Android STT 텍스트를 받아 LLM으로 의미를 추론하고 intent와 파라미터를 JSON으로 반환한다.
백엔드 서버가 내부적으로 호출하며, 프론트는 직접 호출하지 않는다.

```
프론트 (Android)
    ↓  STT 텍스트
백엔드 서버
    ↓  LLM 추론 요청
AI 서버 (여기)
    ↓  intent + 파라미터 반환
백엔드 서버
    ↓  장소명 → 좌표 변환, 경로/장소 API 호출
프론트 (Android)
```

---

## 디렉토리 구조

```
llm_test/
├── server/                     # Flask AI 서버
│   ├── app.py                  # 진입점 — 엔드포인트 정의
│   ├── config.py               # 서버 설정 (HOST, PORT, DEFAULT_MODEL)
│   ├── environment.yaml        # conda 환경 정의
│   ├── requirements.txt        # pip 의존 패키지
│   ├── .env.example            # 환경변수 예시
│   │
│   ├── providers/              # LLM 모델별 구현
│   │   ├── base_provider.py    # LLMResponse 데이터클래스 + BaseProvider 인터페이스
│   │   ├── utils.py            # SYSTEM_PROMPT, parse_json_response, is_success
│   │   ├── gemini_provider.py  # Gemini 2.5 Flash
│   │   ├── claude_provider.py  # Claude Haiku 4.5
│   │   └── gpt_mini_provider.py # GPT-5 mini
│   │
│   ├── utils/                  # 공통 유틸리티
│   │   ├── logger.py           # 로깅 설정
│   │   ├── cost_calculator.py  # GMS 크레딧 비용 계산
│   │   └── result_logger.py    # API 호출 결과 JSON 저장
│   │
│   ├── services/               # 부가 서비스 (현재 미사용)
│   │   ├── intent_parser.py
│   │   └── stt_service.py
│   │
│   └── tests/
│       ├── test_batch.py       # 3개 모델 배치 테스트
│       ├── results/            # /api/chat/llm 호출 결과 JSON
│       ├── test_audio/         # 테스트용 음성 파일
│       └── test_results/       # 배치 테스트 Markdown 결과
│
└── FE/                         # Android 테스트 앱 (PoC용)
```

---

## 지원 모델 (GMS 프록시)

| 모델 키 | 실제 모델 | Input 단가 | Output 단가 |
|---------|-----------|-----------|------------|
| `gemini` | gemini-2.5-flash | 0.003 / 1K tokens | 0.025 / 1K tokens |
| `claude` | claude-haiku-4-5-20251001 | 0.01 / 1K tokens | 0.05 / 1K tokens |
| `gpt_mini` | gpt-5-mini | 0.0025 / 1K tokens | 0.02 / 1K tokens |

---

## 설치 및 실행

### 1. 환경변수 설정

```bash
cd server/
cp .env.example .env
```

`.env` 파일에 GMS_KEY 입력:

```
GMS_KEY=your_gms_key_here
```

### 2. conda 가상환경 생성 및 패키지 설치

```bash
conda env create -f server/environment.yaml
conda activate llmtest
```

또는 pip로 직접 설치:

```bash
pip install -r server/requirements.txt
```

### 3. 서버 실행

```bash
cd server/
python app.py
```

기본 포트: `5000`

---

## 엔드포인트

### `GET /health`

서버 상태 및 로드된 provider 목록 확인

**Response**
```json
{
  "status": "healthy",
  "providers": ["gemini", "claude", "gpt_mini"]
}
```

---

### `POST /api/voice/analyze`

백엔드 연동용 메인 엔드포인트. STT 텍스트를 받아 LLM으로 의미 추론.

**Request**
```json
{
  "text": "해운대까지 가줘",
  "model": "gemini"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `text` | string | STT 변환 텍스트 (필수) |
| `model` | string | 사용할 모델 키 (선택, 기본값: `gemini`) |

**Response — 성공**
```json
{
  "success": true,
  "intent": "route_search",
  "place_name": null,
  "departure": null,
  "destination": "해운대",
  "facility_type": null,
  "confirmation_message": "현재 위치에서 해운대까지 경로를 탐색할게요",
  "model": "gemini",
  "latency_ms": 1120
}
```

**Response — 실패**
```json
{
  "success": false,
  "intent": "unknown",
  "confirmation_message": "다시 말씀해 주세요",
  "error": "에러 메시지",
  "model": "gemini",
  "latency_ms": 0
}
```

---

### `POST /api/chat/llm`

단일 모델 직접 호출 (PoC·디버깅용). `LLMResponse` 전체 필드(토큰 수, 비용 포함) 반환.

**Request**
```json
{
  "text": "부산역에서 서면까지",
  "model": "claude",
  "stt_start_ms": 1714000000000
}
```

---

## Intent 분류

| intent | 설명 | 추출 필드 |
|--------|------|-----------|
| `place_search` | 특정 장소 이름 검색 (예: "롯데마트 찾아줘") | `place_name` |
| `route_search` | 출발지→도착지 경로 탐색 (예: "해운대까지 가줘") | `departure`, `destination` |
| `nearby_search` | 현재 위치 주변 시설 유형 검색 (예: "근처 병원") | `facility_type` |
| `unknown` | 의도 파악 불가 — 재입력 유도 | — |

**성공 조건** (`is_success`):
- `place_search`: `place_name`이 존재할 때
- `route_search`: `departure` 또는 `destination` 중 하나 이상 존재할 때
- `nearby_search`: `facility_type`이 존재할 때
- `unknown`: 항상 `false`

---

## 테스트 실행

### 배치 테스트 (3개 모델 비교)

```bash
cd server/
python tests/test_batch.py
```

결과 파일: `server/tests/test_results/batch_test_YYYY-MM-DD_HH-MM-SS.md`

---

## 환경변수 목록

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `GMS_KEY` | GMS API 인증 키 (필수) | — |
| `HOST` | 서버 바인딩 주소 | `0.0.0.0` |
| `PORT` | 서버 포트 | `5000` |
| `DEBUG` | Flask 디버그 모드 | `True` |
| `DEFAULT_MODEL` | 모델 미지정 시 기본값 | `gemini` |
