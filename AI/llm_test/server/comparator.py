import concurrent.futures
import time
from typing import Iterator
from providers.base_provider import LLMResponse


class LLMComparator:
    def __init__(self):
        from providers.ollama_provider import OllamaProvider
        from providers.gemini_provider import GeminiProvider
        from providers.claude_provider import ClaudeProvider
        from providers.gpt_provider import GPTProvider
        from providers.gpt_mini_provider import GPTMiniProvider

        self.providers = {
            "qwen_ollama": OllamaProvider(),
            "gemini":      GeminiProvider(),
            "claude":      ClaudeProvider(),
            "gpt":         GPTProvider(),
            "gpt_mini":    GPTMiniProvider(),
        }

    def compare_stream(self, user_input: str, stt_start_ms: int) -> Iterator[LLMResponse]:
        """
        4개 provider 병렬 호출. 완료 순서대로 yield.
        stt_start_ms: Android STT 시작 시각 (Unix ms)
        """
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
            futures = {
                executor.submit(p.call, user_input): name
                for name, p in self.providers.items()
            }
            for future in concurrent.futures.as_completed(futures):
                name = futures[future]
                try:
                    result = future.result()
                    result.total_latency_ms = int(time.time() * 1000) - stt_start_ms
                    yield result
                except Exception as e:
                    yield LLMResponse(
                        provider=name, raw_text="", departure=None,
                        destination=None, intent="unknown",
                        llm_latency_ms=0, total_latency_ms=0,
                        input_tokens=0, output_tokens=0, cost_credit=0,
                        success=False, error=str(e), confirmation_message=None
                    )
