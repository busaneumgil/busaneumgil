from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Optional


@dataclass
class LLMResponse:
    provider: str               # "gemini" | "claude" | "gpt_mini"
    raw_text: str               # LLM 원본 응답 텍스트
    intent: str                 # "place_search" | "unknown"
    place_name: Optional[str]   # 장소명 (place_search)
    departure: Optional[str]    # 출발지 (route_search)
    destination: Optional[str]  # 도착지 (route_search)
    facility_type: Optional[str]  # 시설 유형 (nearby_search)
    confirmed: Optional[bool]   # 시각장애인 모드 확인 응답 (true/false/null)
    confirmation_message: Optional[str]  # TTS용 확인 메시지
    llm_latency_ms: float       # LLM 추론 시간만 (ms)
    total_latency_ms: float     # STT 시작 ~ 버튼 생성 전체 시간 (ms)
    input_tokens: int           # 입력 토큰 수
    output_tokens: int          # 출력 토큰 수
    cost_credit: float          # 차감 크레딧 (토큰 수 × 단가)
    success: bool               # 의도 추출 성공 여부
    error: Optional[str]        # 실패 시 에러 메시지


class BaseProvider(ABC):
    @abstractmethod
    def call(self, user_input: str, system_prompt: str = "", messages: list = None) -> LLMResponse:
        pass

    @property
    @abstractmethod
    def provider_name(self) -> str:
        pass
