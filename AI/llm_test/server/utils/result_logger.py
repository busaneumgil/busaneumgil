import json
import os
from datetime import datetime
from dataclasses import asdict
from typing import List
from providers.base_provider import LLMResponse


_RESULTS_DIR = os.path.join(os.path.dirname(__file__), "..", "tests", "results")


def save_result(input_text: str, stt_start_ms: int, results: List[LLMResponse]):
    os.makedirs(_RESULTS_DIR, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = os.path.join(_RESULTS_DIR, f"compare_{timestamp}.json")

    data = {
        "timestamp": datetime.now().isoformat(),
        "stt_start_ms": stt_start_ms,
        "input": input_text,
        "results": [asdict(r) for r in results],
        "summary": {
            "fastest_total": min(results, key=lambda r: r.total_latency_ms).provider,
            "cheapest": min(results, key=lambda r: r.cost_credit).provider,
            "success_count": sum(1 for r in results if r.success)
        }
    }
    with open(filename, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
