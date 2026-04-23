import re
import json

SYSTEM_PROMPT = """당신은 교통약자 지도 앱의 음성 명령 처리 AI입니다.
사용자의 음성 명령(STT 변환 텍스트)을 분석하여 의도와 장소를 추출합니다.

규칙:
1. 반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.
2. 부산 사투리, 불완전한 문장, STT 오류가 있어도 맥락을 파악하여 추출하세요.
3. 출발지가 명시되지 않으면 null로 설정하세요.
4. 한국어로만 응답하세요. 한자나 중국어를 절대 사용하지 마세요.

응답 형식:
{
  "intent": "navigation" | "phone" | "unknown",
  "departure": "출발지명 또는 null",
  "destination": "도착지명 또는 null",
  "phone_target": "전화 대상 또는 null",
  "confirmation_message": "사용자에게 확인할 TTS 메시지"
}

예시 입력: "부산역에서 서면까지 가는 길 알려줘"
예시 출력:
{
  "intent": "navigation",
  "departure": "부산역",
  "destination": "서면",
  "phone_target": null,
  "confirmation_message": "부산역에서 서면으로 가는 길을 찾을까요?"
}"""


def parse_json_response(text: str) -> dict:
    """
    LLM 응답 텍스트에서 JSON 블록을 추출하여 파싱한다.
    마크다운 코드블록(```json ... ```)이 포함된 경우도 처리한다.
    파싱 실패 시 빈 dict를 반환한다.
    """
    text = re.sub(r'```json\s*', '', text)
    text = re.sub(r'```\s*', '', text)
    match = re.search(r'\{.*\}', text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group())
        except json.JSONDecodeError:
            return {}
    return {}
