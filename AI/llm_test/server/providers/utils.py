import re
import json

# 채택 근거:
# SYSTEM_PROMPT_MOBILITY  → claude_B    (place 테스트 전 모델 100%)
# SYSTEM_PROMPT_VISUALLY  → claude_visually_B (visually 테스트 전 모델 100%)
# SYSTEM_PROMPT_CONFIRM   → gpt_confirm_A    (confirm 테스트 전 모델 100%)
# 테스트 일시: 2026-04-28

SYSTEM_PROMPT_MOBILITY = """부산 지역 교통약자 길찾기 앱의 음성 명령을 분석해 JSON으로만 응답하세요.
다른 텍스트는 절대 포함하지 마세요. 한국어만 사용하세요. 해당 없는 필드는 null.
입력 텍스트에서 로마자나 숫자가 한국어 음절로 발음 표기된 경우 원래 로마자·숫자 표기로 복원 후 장소명을 추출하세요.

출력 형식:
{"intent": "place_search" | "unknown", "place_name": "장소명 또는 null"}

---

입력: "부산역 어디야"
출력: {"intent":"place_search","place_name":"부산역"}

입력: "롯데마트 찾아줘"
출력: {"intent":"place_search","place_name":"롯데마트"}

입력: "근처 스타벅스 알려줘"
출력: {"intent":"place_search","place_name":"스타벅스"}

입력: "주변 CGV 있어?"
출력: {"intent":"place_search","place_name":"CGV"}

입력: "해운대 어디노"
출력: {"intent":"place_search","place_name":"해운대"}

입력: "부산항 어디 있노"
출력: {"intent":"place_search","place_name":"부산항"}

입력: "서면 어디 인노"
출력: {"intent":"place_search","place_name":"서면"}

입력: "맥도날드 알려도"
출력: {"intent":"place_search","place_name":"맥도날드"}

입력: "근처 롯데마트 알려도"
출력: {"intent":"place_search","place_name":"롯데마트"}

입력: "사상역 어디 있나예"
출력: {"intent":"place_search","place_name":"사상역"}

입력: "부산대학교 차자줘"
출력: {"intent":"place_search","place_name":"부산대학교"}

입력: "어디야"
출력: {"intent":"unknown","place_name":null}

입력: "어디 있노"
출력: {"intent":"unknown","place_name":null}

입력: "어디 인노"
출력: {"intent":"unknown","place_name":null}

입력: "어대 있노"
출력: {"intent":"unknown","place_name":null}

입력: "찾아줘"
출력: {"intent":"unknown","place_name":null}

입력: "찾아 주이소"
출력: {"intent":"unknown","place_name":null}

입력: "차자 주이소"
출력: {"intent":"unknown","place_name":null}

입력: "빨리"
출력: {"intent":"unknown","place_name":null}

입력: "알려줘"
출력: {"intent":"unknown","place_name":null}

입력: "근처"
출력: {"intent":"unknown","place_name":null}

입력: "지에스이십오 찾아줘" 출력: {"intent":"place_search","place_name":"GS25"}
입력: "씨유 어디야" 출력: {"intent":"place_search","place_name":"CU"}
입력: "케이에프씨 알려줘" 출력: {"intent":"place_search","place_name":"KFC"}
입력: "에이치앤엠 어디고" 출력: {"intent":"place_search","place_name":"H&M"}
입력: "이케아 어디 있노" 출력: {"intent":"place_search","place_name":"IKEA"}
"""

SYSTEM_PROMPT_VISUALLY = """부산 지역 시각장애인 길찾기 앱의 음성 명령을 분석해 JSON으로만 응답하세요.
다른 텍스트는 절대 포함하지 마세요. 한국어만 사용하세요. 해당 없는 필드는 null.
입력 텍스트에서 로마자나 숫자가 한국어 음절로 발음 표기된 경우 원래 로마자·숫자 표기로 복원 후 장소명을 추출하세요.
대화 히스토리가 있으면 이전 맥락을 참고해서 판단하세요.

출력 형식:
{"intent": "place_search" | "unknown", "place_name": "장소명 또는 null", "confirmed": true | false | null, "confirmation_message": "TTS 메시지 또는 null"}

필드 규칙:
- 새 장소 추출 시: intent=place_search, place_name=장소명, confirmed=null, confirmation_message="[장소명]을 찾으시나요?"
- 긍정 확인 시: intent=place_search, place_name=이전 장소명, confirmed=true, confirmation_message=null
- 부정 + 새 장소: intent=place_search, place_name=새장소명, confirmed=null, confirmation_message="[새장소명]을 찾으시나요?"
- 부정만 or unknown: intent=unknown, place_name=null, confirmed=false, confirmation_message="찾으시는 장소를 다시 말씀해 주세요"

---

[히스토리 없음]
입력: "부산역 어디야"
출력: {"intent":"place_search","place_name":"부산역","confirmed":null,"confirmation_message":"부산역을 찾으시나요?"}

입력: "롯데마트 찾아줘"
출력: {"intent":"place_search","place_name":"롯데마트","confirmed":null,"confirmation_message":"롯데마트 맞으시나요?"}

입력: "근처 스타벅스 알려줘"
출력: {"intent":"place_search","place_name":"스타벅스","confirmed":null,"confirmation_message":"스타벅스를 찾으시나요?"}

입력: "해운대 어디노"
출력: {"intent":"place_search","place_name":"해운대","confirmed":null,"confirmation_message":"해운대를 찾으시나요?"}

입력: "서면 어디 인노"
출력: {"intent":"place_search","place_name":"서면","confirmed":null,"confirmation_message":"서면을 찾으시나요?"}

입력: "맥도날드 알려도"
출력: {"intent":"place_search","place_name":"맥도날드","confirmed":null,"confirmation_message":"맥도날드 맞으시나요?"}

입력: "어디야"
출력: {"intent":"unknown","place_name":null,"confirmed":false,"confirmation_message":"찾으시는 장소를 다시 말씀해 주세요"}

입력: "찾아줘"
출력: {"intent":"unknown","place_name":null,"confirmed":false,"confirmation_message":"찾으시는 장소를 다시 말씀해 주세요"}

입력: "빨리"
출력: {"intent":"unknown","place_name":null,"confirmed":false,"confirmation_message":"찾으시는 장소를 다시 말씀해 주세요"}

입력: "지에스이십오 찾아줘" 출력: {"intent":"place_search","place_name":"GS25","confirmed":null,"confirmation_message":"GS25를 찾으시나요?"}
입력: "씨유 어디야" 출력: {"intent":"place_search","place_name":"CU","confirmed":null,"confirmation_message":"CU를 찾으시나요?"}
입력: "케이에프씨 알려줘" 출력: {"intent":"place_search","place_name":"KFC","confirmed":null,"confirmation_message":"KFC를 찾으시나요?"}
입력: "에이치앤엠 어디고" 출력: {"intent":"place_search","place_name":"H&M","confirmed":null,"confirmation_message":"H&M을 찾으시나요?"}
입력: "이케아 어디 있노" 출력: {"intent":"place_search","place_name":"IKEA","confirmed":null,"confirmation_message":"IKEA를 찾으시나요?"}

[히스토리 있음 - 긍정]
이전 assistant: {"intent":"PLACE_SEARCH","placeName":"부산역","confirmed":null,"confirmationMessage":"부산역을 찾으시나요?"}
입력: "응"
출력: {"intent":"place_search","place_name":"부산역","confirmed":true,"confirmation_message":null}

[히스토리 있음 - 부정 + 새 장소]
이전 assistant: {"intent":"PLACE_SEARCH","placeName":"부산역","confirmed":null,"confirmationMessage":"부산역을 찾으시나요?"}
입력: "아니 부산대학교"
출력: {"intent":"place_search","place_name":"부산대학교","confirmed":null,"confirmation_message":"부산대학교를 찾으시나요?"}

[히스토리 있음 - 새 장소 단독 발화]
이전 assistant: {"intent":"PLACE_SEARCH","placeName":"부산역","confirmed":null,"confirmationMessage":"부산역을 찾으시나요?"}
입력: "부산대학교"
출력: {"intent":"place_search","place_name":"부산대학교","confirmed":null,"confirmation_message":"부산대학교를 찾으시나요?"}

[히스토리 있음 - 부정만]
이전 assistant: {"intent":"PLACE_SEARCH","placeName":"부산역","confirmed":null,"confirmationMessage":"부산역을 찾으시나요?"}
입력: "아니"
출력: {"intent":"unknown","place_name":null,"confirmed":false,"confirmation_message":"찾으시는 장소를 다시 말씀해 주세요"}
"""

# SYSTEM_PROMPT_CONFIRM은 /api/voice/confirm 엔드포인트 폐기로 미사용
# 대화형 흐름은 SYSTEM_PROMPT_VISUALLY의 히스토리 기반으로 처리
# SYSTEM_PROMPT_CONFIRM = """
# 당신은 시각장애인용 길찾기 앱의 확인 응답 판단 AI입니다.
# ...
# """


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


def is_success(intent: str, parsed: dict, prompt_type: str = "mobility") -> bool:
    if prompt_type == "mobility":
        if intent == "place_search":
            return bool(parsed.get("place_name"))
        if intent == "unknown":
            return True

    if prompt_type == "visually":
        if intent == "place_search":
            return bool(parsed.get("place_name"))
        if intent == "unknown":
            return True
        # confirmed: true이면 검색 가능 상태
        if parsed.get("confirmed") is True:
            return True

    # 미사용 (주석 처리 유지)
    # if intent == "route_search":
    #     return bool(parsed.get("departure") or parsed.get("destination"))
    # if intent == "nearby_search":
    #     return bool(parsed.get("facility_type"))

    return False


def get_system_prompt(mode: str) -> str:
    if mode == "visually":
        return SYSTEM_PROMPT_VISUALLY
    return SYSTEM_PROMPT_MOBILITY  # 기본값: mobility
