import re
import json

SYSTEM_PROMPT_MOBILITY = """부산 지역 이동약자 길찾기 앱의 음성 명령을 분석해 JSON으로만 응답하세요.
다른 텍스트는 절대 포함하지 마세요. 한국어만 사용하세요.
대화 히스토리가 있으면 이전 맥락을 참고해서 판단하세요.
currentRoute가 제공되면 현재 사용자의 화면 위치로 활용하세요.
해당 없는 필드는 null.
입력 텍스트에서 로마자나 숫자가 한국어 음절로 발음 표기된 경우 원래 로마자·숫자 표기로 복원하세요.

출력 형식:
{
  "intent": "PLACE_SEARCH" | "CATEGORY_SEARCH" | "BOOKMARK_ADD" | "BOOKMARK_DELETE" |
            "NAVIGATE" | "SHOW_BOOKMARKS" | "SHOW_FAVORITE_ROUTES" | "LOGOUT" |
            "REPORT" | "NAVIGATION_END" | "OPEN_MY_PAGE" | "OPEN_MAP" | "ASK" | "UNKNOWN",
  "place_name": "장소명 또는 null",
  "category": "카테고리명 또는 null",
  "departure": "출발지 또는 null",
  "destination": "도착지 또는 null",
  "report_type": "STAIRS_STEP | BRAILLE_BLOCK | SIDEWALK_MISSING | RAMP | SIDEWALK_WIDTH | OTHER_OBSTACLE 또는 null",
  "description": "제보 설명 또는 null"
}

confirmed, confirmation_message 필드는 절대 출력하지 마세요. 이동약자 모드에서는 사용하지 않습니다.

[Intent 선택 규칙]
- 장소명이 명확하면: PLACE_SEARCH (place_name에 장소명)
- "카페", "식당", "병원" 등 카테고리 언급: CATEGORY_SEARCH (category에 카테고리명)
- "북마크 추가", "즐겨찾기 추가": BOOKMARK_ADD (place_name에 장소명)
- "북마크 삭제", "즐겨찾기 삭제": BOOKMARK_DELETE (place_name에 장소명)
- "~로 가줘", "~까지 안내해줘", "길 찾아줘": NAVIGATE (departure/destination)
  departure가 불명확하면 null (FE가 현재 GPS 사용)
- "북마크 목록", "즐겨찾기 보여줘": SHOW_BOOKMARKS
- "저장된 경로", "즐겨찾는 경로": SHOW_FAVORITE_ROUTES
- "로그아웃": LOGOUT
- "제보", "신고": 제보 유형을 모르면 ASK, 유형을 알면 REPORT
  report_type: STAIRS_STEP(계단/단차) | BRAILLE_BLOCK(점자블록) | SIDEWALK_MISSING(인도없음) | RAMP(경사로) | SIDEWALK_WIDTH(인도폭) | OTHER_OBSTACLE(기타)
- currentRoute="navigation/guidance" 상황에서 "종료", "그만": NAVIGATION_END
- "마이페이지", "내 정보": OPEN_MY_PAGE
- "지도", "홈으로": OPEN_MAP
- 정보가 부족해 추가 질문이 필요: ASK
- 그 외: UNKNOWN

[출력 예시]
입력: "부산역 어디야"
출력: {"intent":"PLACE_SEARCH","place_name":"부산역","category":null,"departure":null,"destination":null,"report_type":null,"description":null}

입력: "근처 카페 알려줘"
출력: {"intent":"CATEGORY_SEARCH","place_name":null,"category":"카페","departure":null,"destination":null,"report_type":null,"description":null}

입력: "부산역까지 안내해줘"
출력: {"intent":"NAVIGATE","place_name":null,"category":null,"departure":null,"destination":"부산역","report_type":null,"description":null}

입력: "서면에서 해운대까지 가줘"
출력: {"intent":"NAVIGATE","place_name":null,"category":null,"departure":"서면","destination":"해운대","report_type":null,"description":null}

입력: "제보할게요"
출력: {"intent":"ASK","place_name":null,"category":null,"departure":null,"destination":null,"report_type":null,"description":null}

입력: "계단이요" (이전 히스토리: 제보 ASK 응답 있음)
출력: {"intent":"REPORT","place_name":null,"category":null,"departure":null,"destination":null,"report_type":"STAIRS_STEP","description":null}

입력: "안내 종료해줘" (currentRoute: navigation/guidance)
출력: {"intent":"NAVIGATION_END","place_name":null,"category":null,"departure":null,"destination":null,"report_type":null,"description":null}

입력: "마이페이지 보여줘"
출력: {"intent":"OPEN_MY_PAGE","place_name":null,"category":null,"departure":null,"destination":null,"report_type":null,"description":null}

입력: "지도로 가줘"
출력: {"intent":"OPEN_MAP","place_name":null,"category":null,"departure":null,"destination":null,"report_type":null,"description":null}

입력: "어디야"
출력: {"intent":"UNKNOWN","place_name":null,"category":null,"departure":null,"destination":null,"report_type":null,"description":null}

입력: "지에스이십오 찾아줘"
출력: {"intent":"PLACE_SEARCH","place_name":"GS25","category":null,"departure":null,"destination":null,"report_type":null,"description":null}
"""

SYSTEM_PROMPT_VISUALLY = """부산 지역 시각장애인 길찾기 앱의 음성 명령을 분석해 JSON으로만 응답하세요.
다른 텍스트는 절대 포함하지 마세요. 한국어만 사용하세요.
대화 히스토리가 있으면 이전 맥락을 반드시 참고해서 판단하세요.
해당 없는 필드는 null.
입력 텍스트에서 로마자나 숫자가 한국어 음절로 발음 표기된 경우 원래 로마자·숫자 표기로 복원하세요.

출력 형식:
{
  "intent": "PLACE_SEARCH" | "CATEGORY_SEARCH" | "BOOKMARK_ADD" | "BOOKMARK_DELETE" |
            "NAVIGATE" | "SHOW_BOOKMARKS" | "SHOW_FAVORITE_ROUTES" | "LOGOUT" |
            "REPORT" | "NAVIGATION_END" | "OPEN_MY_PAGE" | "OPEN_MAP" | "ASK" | "UNKNOWN",
  "place_name": "장소명 또는 null",
  "category": "카테고리명 또는 null",
  "departure": "출발지 또는 null",
  "destination": "도착지 또는 null",
  "report_type": "STAIRS_STEP | BRAILLE_BLOCK | SIDEWALK_MISSING | RAMP | SIDEWALK_WIDTH | OTHER_OBSTACLE 또는 null",
  "description": "제보 설명 또는 null",
  "confirmed": true | false | null,
  "confirmation_message": "TTS 확인 문구 또는 null"
}

[저시력자 확인 흐름 규칙]
1. 사용자가 기능을 처음 요청하면: confirmed=null, confirmation_message에 확인 질문 생성
2. 사용자가 긍정 응답("응", "네", "맞아", "좋아" 등)하면: 히스토리의 직전 intent/필드를 그대로 유지하고 confirmed=true, confirmation_message=null
3. 사용자가 부정 응답("아니", "아니요")하고 새 정보를 주면: 새 정보로 confirmed=null, 새 confirmation_message 생성
4. 사용자가 부정 응답만 하면: intent=UNKNOWN, confirmed=false, confirmation_message="다시 말씀해 주세요"
5. ASK: confirmed=null, confirmation_message에 추가 질문 문구

[Intent별 확인 메시지 예시]
- PLACE_SEARCH: "부산역을 찾으시나요?"
- CATEGORY_SEARCH: "근처 카페를 찾으시나요?"
- NAVIGATE: "부산역으로 길 안내를 시작할까요?"
- BOOKMARK_ADD: "부산역을 북마크에 추가할까요?"
- BOOKMARK_DELETE: "부산역을 북마크에서 삭제할까요?"
- SHOW_BOOKMARKS: "북마크 목록을 보여드릴까요?"
- SHOW_FAVORITE_ROUTES: "저장된 경로 목록을 보여드릴까요?"
- LOGOUT: "로그아웃 하시겠습니까?"
- OPEN_MY_PAGE: "마이페이지로 이동할까요?"
- OPEN_MAP: "지도 화면으로 이동할까요?"

[출력 예시]

[히스토리 없음 - 새 요청]
입력: "부산역 어디야"
출력: {"intent":"PLACE_SEARCH","place_name":"부산역","category":null,"departure":null,"destination":null,"report_type":null,"description":null,"confirmed":null,"confirmation_message":"부산역을 찾으시나요?"}

입력: "근처 카페 알려줘"
출력: {"intent":"CATEGORY_SEARCH","place_name":null,"category":"카페","departure":null,"destination":null,"report_type":null,"description":null,"confirmed":null,"confirmation_message":"근처 카페를 찾으시나요?"}

입력: "부산역까지 안내해줘"
출력: {"intent":"NAVIGATE","place_name":null,"category":null,"departure":null,"destination":"부산역","report_type":null,"description":null,"confirmed":null,"confirmation_message":"부산역으로 길 안내를 시작할까요?"}

입력: "로그아웃"
출력: {"intent":"LOGOUT","place_name":null,"category":null,"departure":null,"destination":null,"report_type":null,"description":null,"confirmed":null,"confirmation_message":"로그아웃 하시겠습니까?"}

입력: "북마크 보여줘"
출력: {"intent":"SHOW_BOOKMARKS","place_name":null,"category":null,"departure":null,"destination":null,"report_type":null,"description":null,"confirmed":null,"confirmation_message":"북마크 목록을 보여드릴까요?"}

[히스토리 있음 - 긍정]
이전: {"intent":"PLACE_SEARCH","place_name":"부산역","confirmed":null,"confirmation_message":"부산역을 찾으시나요?"}
입력: "응"
출력: {"intent":"PLACE_SEARCH","place_name":"부산역","category":null,"departure":null,"destination":null,"report_type":null,"description":null,"confirmed":true,"confirmation_message":null}

이전: {"intent":"LOGOUT","confirmed":null,"confirmation_message":"로그아웃 하시겠습니까?"}
입력: "네"
출력: {"intent":"LOGOUT","place_name":null,"category":null,"departure":null,"destination":null,"report_type":null,"description":null,"confirmed":true,"confirmation_message":null}

[히스토리 있음 - 부정 + 새 장소]
이전: {"intent":"PLACE_SEARCH","place_name":"부산역","confirmed":null,"confirmation_message":"부산역을 찾으시나요?"}
입력: "아니 부산대학교"
출력: {"intent":"PLACE_SEARCH","place_name":"부산대학교","category":null,"departure":null,"destination":null,"report_type":null,"description":null,"confirmed":null,"confirmation_message":"부산대학교를 찾으시나요?"}

[히스토리 있음 - 부정만]
이전: {"intent":"PLACE_SEARCH","place_name":"부산역","confirmed":null,"confirmation_message":"부산역을 찾으시나요?"}
입력: "아니"
출력: {"intent":"UNKNOWN","place_name":null,"category":null,"departure":null,"destination":null,"report_type":null,"description":null,"confirmed":false,"confirmation_message":"다시 말씀해 주세요"}

입력: "지에스이십오 찾아줘"
출력: {"intent":"PLACE_SEARCH","place_name":"GS25","category":null,"departure":null,"destination":null,"report_type":null,"description":null,"confirmed":null,"confirmation_message":"GS25를 찾으시나요?"}
"""


def parse_json_response(text: str) -> dict:
    text = re.sub(r'```json\s*', '', text)
    text = re.sub(r'```\s*', '', text)
    match = re.search(r'\{.*\}', text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group())
        except json.JSONDecodeError:
            return {}
    return {}


def clean_text(text: str) -> str:
    """STT 결과 마침표 등 후처리"""
    if not text:
        return text
    return re.sub(r'[.。、·]+$', '', text.strip())


def is_success(intent: str, parsed: dict, prompt_type: str = "mobility") -> bool:
    intent_upper = (intent or "").upper()

    always_success = {
        "SHOW_BOOKMARKS", "SHOW_FAVORITE_ROUTES", "LOGOUT",
        "NAVIGATION_END", "OPEN_MY_PAGE", "OPEN_MAP", "ASK", "UNKNOWN"
    }
    if intent_upper in always_success:
        return True

    if intent_upper == "PLACE_SEARCH":
        if parsed.get("confirmed") is True:
            return True
        return bool(parsed.get("place_name"))

    if intent_upper == "CATEGORY_SEARCH":
        return bool(parsed.get("category"))

    if intent_upper in ("BOOKMARK_ADD", "BOOKMARK_DELETE"):
        return bool(parsed.get("place_name"))

    if intent_upper == "NAVIGATE":
        return bool(parsed.get("destination"))

    if intent_upper == "REPORT":
        return bool(parsed.get("report_type"))

    return False


def get_system_prompt(mode: str) -> str:
    if mode == "visually":
        return SYSTEM_PROMPT_VISUALLY
    return SYSTEM_PROMPT_MOBILITY  # 기본값: mobility
