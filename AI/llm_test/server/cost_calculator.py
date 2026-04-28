# GMS 크레딧 단가 (per 1K tokens)
# 출처: GMS 각 모델 API 문서
PRICING = {
    "gemini":   {"input": 0.003,  "output": 0.025},   # gemini-2.5-flash
    "claude":   {"input": 0.01,   "output": 0.05},    # claude-haiku-4-5-20251001
    "gpt_mini": {"input": 0.0025, "output": 0.02},    # gpt-5-mini
}


def calculate_cost(provider_key: str, input_tokens: int, output_tokens: int) -> float:
    """차감될 GMS 크레딧 반환 (단가 × 토큰 수 / 1000)"""
    p = PRICING.get(provider_key, {"input": 0.0, "output": 0.0})
    return (input_tokens * p["input"] + output_tokens * p["output"]) / 1000
