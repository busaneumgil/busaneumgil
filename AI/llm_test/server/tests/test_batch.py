import os
import sys
import time
from datetime import datetime
from dataclasses import asdict

# server/ 루트를 경로에 추가
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from providers.gemini_provider import GeminiProvider
from providers.claude_provider import ClaudeProvider
from providers.gpt_mini_provider import GPTMiniProvider

PROVIDERS = {
    "gemini":   GeminiProvider(),
    "claude":   ClaudeProvider(),
    "gpt_mini": GPTMiniProvider(),
}

RESULTS_DIR = os.path.join(os.path.dirname(__file__), "test_results")

TEST_INPUTS = [
    "해운대까지 가줘",
    "부산역에서 서면까지 길 알려줘",
    "근처 편의점 어디야",
    "병원 찾아줘",
    "안녕하세요",
]


def main():
    os.makedirs(RESULTS_DIR, exist_ok=True)

    run_time = datetime.now()

    print("=" * 40)
    print("배치 테스트 시작")
    print("=" * 40)
    print(f"테스트 입력 수: {len(TEST_INPUTS)}개")
    print(f"테스트 모델: {list(PROVIDERS.keys())}")
    print()

    results = {}

    for idx, text in enumerate(TEST_INPUTS, start=1):
        print(f"[{idx}/{len(TEST_INPUTS)}] 입력: {text}")
        model_results = []

        for model_key, provider in PROVIDERS.items():
            start_ms = int(time.time() * 1000)
            result = provider.call(text)
            result.total_latency_ms = int(time.time() * 1000) - start_ms
            model_results.append((model_key, result))

            status = "✅" if result.success else "❌"
            print(f"  - {model_key}: intent={result.intent} | {result.total_latency_ms}ms {status}")

        results[text] = model_results
        print()

    # Markdown 결과 저장
    output_filename = f"batch_test_{run_time.strftime('%Y-%m-%d_%H-%M-%S')}.md"
    output_path = os.path.join(RESULTS_DIR, output_filename)

    lines = ["# 배치 테스트 결과\n"]
    lines.append(f"- **실행 시각**: {run_time.strftime('%Y-%m-%d %H:%M:%S')}")
    lines.append(f"- **테스트 입력 수**: {len(TEST_INPUTS)}개")
    lines.append(f"- **테스트 모델**: {', '.join(PROVIDERS.keys())}")
    lines.append("")

    for text, model_results in results.items():
        lines.append("---\n")
        lines.append(f"## 입력: `{text}`\n")
        lines.append("| 모델 | intent | departure | destination | latency(ms) | 성공 |")
        lines.append("|------|--------|-----------|-------------|-------------|------|")
        for model_key, r in sorted(model_results, key=lambda x: x[1].total_latency_ms):
            d = asdict(r)
            lines.append(
                f"| {model_key} | {d.get('intent', '-')} | {d.get('departure') or '-'} "
                f"| {d.get('destination') or '-'} | {r.total_latency_ms} | {'✅' if r.success else '❌'} |"
            )
        lines.append("")

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print("=" * 40)
    print(f"완료! 결과 파일: test_results/{output_filename}")
    print("=" * 40)


if __name__ == "__main__":
    main()
