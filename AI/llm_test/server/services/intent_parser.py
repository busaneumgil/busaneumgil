import re
from utils.logger import get_logger

logger = get_logger(__name__)


class IntentParser:
    def __init__(self):
        logger.info("IntentParser initialized")

    def parse_route_query(self, text: str) -> dict:
        """길찾기 쿼리에서 출발지/도착지 추출"""
        logger.debug(f"Parsing route query: '{text}'")

        departure = None
        destination = None

        # 패턴 1: "A에서 B까지"
        pattern1 = r'(.+?)(에서|부터)\s*(.+?)(까지|로|가지|으로)'
        match = re.search(pattern1, text)
        if match:
            departure = match.group(1).strip()
            destination = match.group(3).strip()
            logger.debug(f"Pattern 1 matched - Departure: {departure}, Destination: {destination}")

        # 패턴 2: "A B 가는 법"
        if not departure:
            pattern2 = r'(.+?)\s+(.+?)\s*(가는|가려면|가고|가기)'
            match = re.search(pattern2, text)
            if match:
                departure = match.group(1).strip()
                destination = match.group(2).strip()
                logger.debug(f"Pattern 2 matched - Departure: {departure}, Destination: {destination}")

        result = {
            "departure": departure,
            "destination": destination,
            "is_route_query": departure is not None and destination is not None
        }

        logger.info(f"Parse result: {result}")
        return result

    def parse_dark_mode_intent(self, text: str) -> str:
        """다크모드 의도 파싱"""
        text_lower = text.lower()

        enable_keywords = ["다크모드", "다크 모드", "어두운", "검은", "켜", "실행", "on"]
        disable_keywords = ["꺼", "끄", "해제", "off", "취소", "라이트", "밝은"]

        if any(keyword in text_lower for keyword in enable_keywords):
            if any(keyword in text_lower for keyword in disable_keywords):
                logger.debug("Dark mode: disable")
                return "disable_dark_mode"
            logger.debug("Dark mode: enable")
            return "enable_dark_mode"

        logger.debug("Dark mode: none")
        return "none"

    def create_prompt(self, user_text: str, mode: str = "nlu") -> str:
        """프롬프트 생성 (mode: 'nlu' | 'confirm')"""
        if mode == "nlu":
            prompt = f"""당신은 시각장애인을 위한 길찾기 음성 도우미입니다.
사용자의 말에서 출발지와 도착지를 찾아, 아래 형식의 짧은 확인 문장만 생성하세요.

규칙:
1. 반드시 한 문장으로만 답변하세요.
2. 형식: "[출발지]에서 [도착지]까지 길을 찾을까요?"
3. 출발지나 도착지를 찾을 수 없으면: "출발지와 도착지를 다시 말씀해주세요."
4. 추가 설명이나 부가 정보는 절대 넣지 마세요.

예시:
- 입력: "부산역에서 서면역까지 얼마나 걸려"
  출력: 부산역에서 서면역까지 길을 찾을까요?
- 입력: "집에 가고 싶어"
  출력: 출발지와 도착지를 다시 말씀해주세요.

사용자 입력: {user_text}"""
        else:  # confirm
            prompt = f"""사용자가 방금 길찾기 확인 질문에 답했습니다.
이 답변이 "긍정"인지 "부정"인지 한 단어로만 답하세요.

규칙:
- 긍정이면: 예
- 부정이면: 아니요
- 모호하면: 아니요

예시:
- "응" → 예
- "맞아요" → 예
- "그래" → 예
- "아니야" → 아니요
- "틀렸어" → 아니요
- "다시" → 아니요

사용자 입력: {user_text}
답변:"""

        return prompt
