import os
import time
import requests
from providers.base_provider import BaseProvider, LLMResponse
from providers.utils import SYSTEM_PROMPT, parse_json_response
from cost_calculator import calculate_cost


class ClaudeProvider(BaseProvider):
    BASE_URL = "https://gms.ssafy.io/gmsapi/api.anthropic.com/v1/messages"

    def __init__(self):
        self.gms_key = os.getenv("GMS_KEY")

    @property
    def provider_name(self):
        return "claude"

    def call(self, user_input: str) -> LLMResponse:
        headers = {
            "Content-Type": "application/json",
            "x-api-key": self.gms_key,
            "anthropic-version": "2023-06-01"
        }
        body = {
            "model": "claude-3-5-haiku-latest",
            "max_tokens": 1024,
            "system": SYSTEM_PROMPT,
            "messages": [
                {"role": "user", "content": user_input}
            ]
        }
        start = time.time()
        try:
            resp = requests.post(self.BASE_URL, headers=headers,
                                 json=body, timeout=15)
            resp.raise_for_status()
            data = resp.json()
            latency_ms = (time.time() - start) * 1000

            raw_text = data["content"][0]["text"]
            input_tokens = data["usage"]["input_tokens"]
            output_tokens = data["usage"]["output_tokens"]
            cost = calculate_cost("claude", input_tokens, output_tokens)
            parsed = parse_json_response(raw_text)

            return LLMResponse(
                provider="claude", raw_text=raw_text,
                departure=parsed.get("departure"),
                destination=parsed.get("destination"),
                intent=parsed.get("intent", "unknown"),
                llm_latency_ms=latency_ms, total_latency_ms=0,
                input_tokens=input_tokens, output_tokens=output_tokens,
                cost_credit=cost,
                success=bool(parsed.get("departure") or parsed.get("destination")),
                error=None,
                confirmation_message=parsed.get("confirmation_message")
            )
        except Exception as e:
            return LLMResponse(
                provider="claude", raw_text="", departure=None, destination=None,
                intent="unknown", llm_latency_ms=0, total_latency_ms=0,
                input_tokens=0, output_tokens=0, cost_credit=0,
                success=False, error=str(e), confirmation_message=None
            )
