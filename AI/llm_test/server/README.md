# AI 서버 — LLM 의미 추론 서버

## 역할
Android STT 텍스트를 받아 LLM으로 의미를 추론하고
intent와 파라미터를 JSON으로 반환한다.
백엔드 서버가 내부적으로 호출하며, 프론트는 직접 호출하지 않는다.

## 시스템 구조
프론트 → 백엔드 → AI 서버(여기) → 백엔드 → 프론트

## 지원 모델
- Qwen 2.5 7B (로컬 Ollama)
- Gemini 2.5 Flash Lite (GMS 프록시)
- claude-haiku-4-5-20251001 (GMS 프록시)
- GPT-5 nano (GMS 프록시)
- GPT-5 mini (GMS 프록시)

## 설치 및 실행

1. 환경변수 설정
   ```
   cp .env.example .env
   ```
   `.env` 파일에 `GMS_KEY` 입력

2. 의존 패키지 설치
   ```
   pip install -r requirements.txt
   ```

3. 서버 실행
   ```
   python app.py
   ```

## 주요 엔드포인트

| 엔드포인트 | 메서드 | 용도 |
|---|---|---|
| /health | GET | 서버 상태 확인 |
| /api/voice/analyze | POST | 백엔드 연동용 (실서비스) |
| /api/chat/llm | POST | 단일 모델 호출 (PoC용) |
| /api/compare/stream | GET | 4모델 SSE 비교 (PoC용) |

## /api/voice/analyze 요청/응답 예시

Request
```json
{
  "text": "해운대까지 가줘",
  "model": "gemini"
}
```

Response 성공
```json
{
  "success": true,
  "intent": "route_search",
  "departure": null,
  "destination": "해운대",
  "place_name": null,
  "facility_type": null,
  "confirmation_message": "현재 위치에서 해운대까지 경로를 탐색할게요",
  "model": "gemini",
  "latency_ms": 1120
}
```

Response 실패
```json
{
  "success": false,
  "intent": "unknown",
  "confirmation_message": "다시 말씀해 주세요",
  "error": "에러 메시지",
  "model": "gemini",
  "latency_ms": 980
}
```

## 테스트 실행

```
cd tests/
python test_batch.py
```

## intent 종류

| intent | 설명 | 백엔드 연결 API |
|---|---|---|
| place_search | 장소 검색 | POST /places/search/voice |
| route_search | 경로 탐색 | POST /routes/search |
| nearby_search | 주변 시설 조회 | GET /places |
| unknown | 재입력 유도 | 호출 없음 |
