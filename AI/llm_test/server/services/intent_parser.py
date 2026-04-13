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

    def create_prompt(self, user_message: str) -> str:
        """프롬프트 생성"""
        system_prompt = """당신은 길찾기 앱의 AI 어시스턴트입니다.

사용자의 질문을 분석하여 다음 형식으로 응답하세요:

1. 길찾기 요청인 경우:
   - 출발지와 도착지를 파악하세요
   - "출발지는 [출발지], 도착지는 [도착지]가 맞나요?" 형식으로 확인하세요

2. 다크모드 요청인 경우:
   - "다크모드를 활성화하겠습니다" 또는 "다크모드를 해제하겠습니다"라고 응답하세요

3. 기타 질문인 경우:
   - 간단히 답변하되, 길찾기 앱임을 상기시키세요

항상 한국어로 간결하게 답변하세요."""

        prompt = f"""{system_prompt}

사용자: {user_message}
어시스턴트:"""

        return prompt
