import os
import time
import requests
from providers.base_provider import BaseProvider, LLMResponse
from providers.utils import SYSTEM_PROMPT, parse_json_response, is_success
from utils.cost_calculator import calculate_cost


class GeminiProvider(BaseProvider):
    BASE_URL = (
        "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com"
        "/v1beta/models/gemini-2.5-flash:generateContent"
    )

    def __init__(self):
        self.gms_key = os.getenv("GMS_KEY")

    @property
    def provider_name(self):
        return "gemini"

    def call(self, user_input: str) -> LLMResponse:
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": self.gms_key
        }
        body = {
            "system_instruction": {
                "parts": [{"text": SYSTEM_PROMPT}]
            },
            "contents": [
                {"parts": [{"text": user_input}]}
            ]
        }
        start = time.time()
        try:
            resp = requests.post(self.BASE_URL, headers=headers,
                                 json=body, timeout=15)
            resp.raise_for_status()
            data = resp.json()
            latency_ms = (time.time() - start) * 1000

            raw_text = data["candidates"][0]["content"]["parts"][0]["text"]
            input_tokens = data["usageMetadata"]["promptTokenCount"]
            output_tokens = data["usageMetadata"]["candidatesTokenCount"]
            cost = calculate_cost("gemini", input_tokens, output_tokens)
            parsed = parse_json_response(raw_text)

            intent = parsed.get("intent", "unknown")
            return LLMResponse(
                provider="gemini", raw_text=raw_text,
                intent=intent,
                place_name=parsed.get("place_name"),
                departure=parsed.get("departure"),
                destination=parsed.get("destination"),
                facility_type=parsed.get("facility_type"),
                confirmation_message=parsed.get("confirmation_message"),
                llm_latency_ms=latency_ms, total_latency_ms=0,
                input_tokens=input_tokens, output_tokens=output_tokens,
                cost_credit=cost,
                success=is_success(intent, parsed),
                error=None,
            )
        except Exception as e:
            return LLMResponse(
                provider="gemini", raw_text="",
                intent="unknown", place_name=None, departure=None,
                destination=None, facility_type=None, confirmation_message=None,
                llm_latency_ms=0, total_latency_ms=0,
                input_tokens=0, output_tokens=0, cost_credit=0,
                success=False, error=str(e),
            )
