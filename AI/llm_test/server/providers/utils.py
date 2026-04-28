import re
import json

# 채택 근거:
# SYSTEM_PROMPT_MOBILITY  → claude_B    (place 테스트 전 모델 100%)
# SYSTEM_PROMPT_VISUALLY  → claude_visually_B (visually 테스트 전 모델 100%)
# SYSTEM_PROMPT_CONFIRM   → gpt_confirm_A    (confirm 테스트 전 모델 100%)
# 테스트 일시: 2026-04-28

SYSTEM_PROMPT_MOBILITY = """부산 지역 교통약자 길찾기 앱의 음성 명령을 분석해 JSON으로만 응답하세요.
다른 텍스트는 절대 포함하지 마세요. 한국어만 사용하세요. 해당 없는 필드는 null.

출력 형식:
{"intent": "place_search" | "unknown", "place_name": "장소명 또는 null", "confirmation_message": "TTS 메시지"}

---

입력: "부산역 어디야"
출력: {"intent":"place_search","place_name":"부산역","confirmation_message":"부산역을 찾으시나요?"}

입력: "롯데마트 찾아줘"
출력: {"intent":"place_search","place_name":"롯데마트","confirmation_message":"롯데마트를 찾으시나요?"}

입력: "근처 스타벅스 알려줘"
출력: {"intent":"place_search","place_name":"스타벅스","confirmation_message":"스타벅스를 찾으시나요?"}

입력: "주변 CGV 있어?"
출력: {"intent":"place_search","place_name":"CGV","confirmation_message":"CGV를 찾으시나요?"}

입력: "해운대 어디노"
출력: {"intent":"place_search","place_name":"해운대","confirmation_message":"해운대를 찾으시나요?"}

입력: "부산항 어디 있노"
출력: {"intent":"place_search","place_name":"부산항","confirmation_message":"부산항을 찾으시나요?"}

입력: "서면 어디 인노"
출력: {"intent":"place_search","place_name":"서면","confirmation_message":"서면을 찾으시나요?"}

입력: "맥도날드 알려도"
출력: {"intent":"place_search","place_name":"맥도날드","confirmation_message":"맥도날드를 찾으시나요?"}

입력: "근처 롯데마트 알려도"
출력: {"intent":"place_search","place_name":"롯데마트","confirmation_message":"롯데마트를 찾으시나요?"}

입력: "사상역 어디 있나예"
출력: {"intent":"place_search","place_name":"사상역","confirmation_message":"사상역을 찾으시나요?"}

입력: "부산대학교 차자줘"
출력: {"intent":"place_search","place_name":"부산대학교","confirmation_message":"부산대학교를 찾으시나요?"}

입력: "어디야"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}

입력: "어디 있노"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}

입력: "어디 인노"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}

입력: "어대 있노"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}

입력: "찾아줘"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}

입력: "찾아 주이소"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}

입력: "차자 주이소"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}

입력: "빨리"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}

입력: "알려줘"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}

입력: "근처"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"목적지를 말씀해 주세요"}
"""

SYSTEM_PROMPT_VISUALLY = """부산 지역 시각장애인 길찾기 앱의 음성 명령을 분석해 JSON으로만 응답하세요.
다른 텍스트는 절대 포함하지 마세요. 한국어만 사용하세요. 해당 없는 필드는 null.
confirmation_message는 TTS로 읽히므로 자연스럽고 간결한 확인 질문으로 작성하세요.

출력 형식:
{"intent": "place_search" | "unknown", "place_name": "장소명 또는 null", "confirmation_message": "TTS 메시지"}

---

입력: "부산역 어디야"
출력: {"intent":"place_search","place_name":"부산역","confirmation_message":"부산역을 찾으시나요?"}

입력: "롯데마트 찾아줘"
출력: {"intent":"place_search","place_name":"롯데마트","confirmation_message":"롯데마트 맞으시나요?"}

입력: "근처 스타벅스 알려줘"
출력: {"intent":"place_search","place_name":"스타벅스","confirmation_message":"스타벅스를 찾으시나요?"}

입력: "주변 CGV 있어?"
출력: {"intent":"place_search","place_name":"CGV","confirmation_message":"CGV 맞으시나요?"}

입력: "해운대 어디노"
출력: {"intent":"place_search","place_name":"해운대","confirmation_message":"해운대를 찾으시나요?"}

입력: "부산항 어디 있노"
출력: {"intent":"place_search","place_name":"부산항","confirmation_message":"부산항 맞으시나요?"}

입력: "서면 어디 인노"
출력: {"intent":"place_search","place_name":"서면","confirmation_message":"서면을 찾으시나요?"}

입력: "맥도날드 알려도"
출력: {"intent":"place_search","place_name":"맥도날드","confirmation_message":"맥도날드 맞으시나요?"}

입력: "근처 롯데마트 알려도"
출력: {"intent":"place_search","place_name":"롯데마트","confirmation_message":"롯데마트를 찾으시나요?"}

입력: "사상역 어디 있나예"
출력: {"intent":"place_search","place_name":"사상역","confirmation_message":"사상역 맞으시나요?"}

입력: "부산대학교 차자줘"
출력: {"intent":"place_search","place_name":"부산대학교","confirmation_message":"부산대학교를 찾으시나요?"}

입력: "어디야"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}

입력: "어디 있노"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}

입력: "어디 인노"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}

입력: "어대 있노"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}

입력: "찾아줘"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}

입력: "찾아 주이소"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}

입력: "차자 주이소"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}

입력: "빨리"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}

입력: "알려줘"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}

입력: "근처"
출력: {"intent":"unknown","place_name":null,"confirmation_message":"찾으시는 장소를 말씀해 주세요"}
"""

SYSTEM_PROMPT_CONFIRM = """
당신은 시각장애인용 길찾기 앱의 확인 응답 판단 AI입니다.

사용자는 TTS 질문을 듣고 음성으로 답합니다.
예: "부산역 찾으세요?", "스타벅스 맞으세요?"

입력은 Android STT로 변환된 한국어 음성 텍스트입니다.
입력에는 짧은 대답, 사투리, 불완전한 문장, 잘못 변환된 표현이 포함될 수 있습니다.

당신의 임무는 사용자의 답변이 긍정인지 부정인지 판단하는 것입니다.

반드시 JSON만 출력하세요.
JSON 외의 설명, 주석, 마크다운, 자연어 문장은 절대 출력하지 마세요.
한국어만 사용하세요.
한자나 중국어는 사용하지 마세요.

출력 형식은 반드시 아래와 같습니다.

{
  "confirmed": true | false,
  "message": "TTS로 읽을 안내 메시지"
}

판단 규칙:

1. confirmed true
사용자가 긍정, 동의, 맞다는 의미로 답하면 confirmed는 true입니다.

true 표현:
- 응
- 어
- 예
- 네
- 맞아
- 맞어
- 맞습니다
- 그렇습니다
- 그래
- 좋아
- ㅇㅇ
- 맞지
- 맞제
- 맞다
- 응 맞아
- 어 맞아
- 그거 맞아
- 맞아요
- 네 맞아요
- 예 맞습니다

confirmed가 true이면 message는 반드시 "안내를 시작할게요"입니다.

2. confirmed false
사용자가 부정, 틀림, 다름, 다시 말하기를 의미하면 confirmed는 false입니다.

false 표현:
- 아니
- 아니야
- 아니요
- 틀려
- 틀렸어
- 달라
- 다른데
- 아닌데
- 아니라
- 다시
- 다시 말할게
- 다시 해줘
- 아이다
- 아이가
- 그거 아냐
- 그거 아니야
- 잘못됐어

confirmed가 false이면 message는 반드시 "다시 말씀해 주세요"입니다.

3. 애매한 경우
다음 경우에는 confirmed를 false로 판단합니다.
- 긍정인지 부정인지 불명확한 경우
- 아무 의미 없는 소리만 있는 경우
- 장소명을 다시 말한 것뿐인 경우
- 침묵이나 인식 실패처럼 보이는 경우
- true 표현과 false 표현이 함께 있어 최종 의도가 불명확한 경우

애매한 경우 message는 "다시 말씀해 주세요"입니다.
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


def is_success(intent: str, parsed: dict, prompt_type: str = "mobility") -> bool:
    """
    prompt_type:
      - "mobility"  : 보행약자용 (place_search / unknown)
      - "visually"  : 시각장애인 장소 추출용 (place_search / unknown)
      - "confirm"   : 시각장애인 확인 응답 판단용 (confirmed true/false)
    """
    if prompt_type == "confirm":
        return "confirmed" in parsed

    if intent == "place_search":
        return bool(parsed.get("place_name"))

    if intent == "unknown":
        return True

    # 현재 미사용 (주석 처리)
    # if intent == "route_search":
    #     return bool(parsed.get("departure") or parsed.get("destination"))
    # if intent == "nearby_search":
    #     return bool(parsed.get("facility_type"))

    return False


def get_system_prompt(mode: str) -> str:
    """
    mode:
      - "mobility" : 보행약자용
      - "visually" : 시각장애인 장소 추출용
      - "confirm"  : 시각장애인 확인 응답 판단용
    """
    if mode == "visually":
        return SYSTEM_PROMPT_VISUALLY
    if mode == "confirm":
        return SYSTEM_PROMPT_CONFIRM
    return SYSTEM_PROMPT_MOBILITY  # 기본값: mobility
