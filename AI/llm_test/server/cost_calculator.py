# GMS 크레딧 단가 (per 1K tokens)
# 출처: GMS 각 모델 API 문서
PRICING = {
    "gemini":      {"input": 0.001,  "output": 0.004},
    "claude":      {"input": 0.008,  "output": 0.04},
    "gpt":         {"input": 0.0005, "output": 0.004},
    "gpt_mini":    {"input": 0.0025, "output": 0.02},
}


def calculate_cost(provider_key: str, input_tokens: int, output_tokens: int) -> float:
    """차감될 GMS 크레딧 반환 (단가 × 토큰 수 / 1000)"""
    p = PRICING.get(provider_key, {"input": 0.0, "output": 0.0})
    return (input_tokens * p["input"] + output_tokens * p["output"]) / 1000
