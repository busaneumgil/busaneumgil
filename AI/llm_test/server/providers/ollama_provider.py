import os
import time
import requests
from providers.base_provider import BaseProvider, LLMResponse
from providers.utils import SYSTEM_PROMPT, parse_json_response


class OllamaProvider(BaseProvider):
    def __init__(self):
        base_url = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        self.endpoint = f"{base_url}/api/generate"

    @property
    def provider_name(self):
        return "qwen_ollama"

    def call(self, user_input: str) -> LLMResponse:
        body = {
            "model": "qwen2.5:7b",
            "prompt": f"{SYSTEM_PROMPT}\n\n사용자 입력: {user_input}",
            "stream": False
        }
        start = time.time()
        try:
            resp = requests.post(self.endpoint, json=body, timeout=30)
            resp.raise_for_status()
            data = resp.json()
            latency_ms = (time.time() - start) * 1000

            raw_text = data["response"]
            input_tokens = data.get("prompt_eval_count", 0)
            output_tokens = data.get("eval_count", 0)
            parsed = parse_json_response(raw_text)

            return LLMResponse(
                provider="qwen_ollama", raw_text=raw_text,
                departure=parsed.get("departure"),
                destination=parsed.get("destination"),
                intent=parsed.get("intent", "unknown"),
                llm_latency_ms=latency_ms, total_latency_ms=0,
                input_tokens=input_tokens, output_tokens=output_tokens,
                cost_credit=0.0,
                success=bool(parsed.get("departure") or parsed.get("destination")),
                error=None,
                confirmation_message=parsed.get("confirmation_message")
            )
        except Exception as e:
            return LLMResponse(
                provider="qwen_ollama", raw_text="", departure=None, destination=None,
                intent="unknown", llm_latency_ms=0, total_latency_ms=0,
                input_tokens=0, output_tokens=0, cost_credit=0,
                success=False, error=str(e), confirmation_message=None
            )
