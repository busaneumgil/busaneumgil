import os
import re
import sys
import time
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from datetime import datetime

from config import Config
from models.model_manager import ModelManager
from services.stt_service import STTService
from services.intent_parser import IntentParser

# ── 경로 설정 ──────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
AUDIO_DIR = os.path.join(SCRIPT_DIR, "test_audio")
RESULTS_DIR = os.path.join(SCRIPT_DIR, "test_results")
SUPPORTED_EXT = {".m4a", ".wav", ".mp3"}
LLM_TIMEOUT = 120  # 초


def extract_route(llm_response: str):
    match = re.search(r'(.+?)에서\s*(.+?)까지\s*길을\s*찾을까요', llm_response)
    if match:
        return match.group(1).strip(), match.group(2).strip()
    return None, None


def run_with_timeout(fn, *args, timeout=LLM_TIMEOUT):
    with ThreadPoolExecutor(max_workers=1) as executor:
        future = executor.submit(fn, *args)
        try:
            return future.result(timeout=timeout)
        except FuturesTimeoutError:
            return None, None, "TIMEOUT"


def generate_with_error_handling(model_manager, model_name, prompt):
    try:
        response_text, inference_time = model_manager.generate(model_name, prompt)
        return response_text, inference_time, None
    except Exception as e:
        return None, None, str(e)


def main():
    os.makedirs(AUDIO_DIR, exist_ok=True)
    os.makedirs(RESULTS_DIR, exist_ok=True)

    # 음성 파일 목록
    audio_files = sorted([
        f for f in os.listdir(AUDIO_DIR)
        if os.path.splitext(f)[1].lower() in SUPPORTED_EXT
    ])

    if not audio_files:
        print("테스트할 음성 파일이 없습니다.")
        print(f"  → {AUDIO_DIR} 폴더에 .m4a / .wav / .mp3 파일을 넣어주세요.")
        sys.exit(0)

    all_models = list(Config.OLLAMA_MODELS.keys()) + list(Config.HF_MODELS.keys())
    run_time = datetime.now()

    print("=" * 40)
    print("배치 테스트 시작")
    print("=" * 40)
    print(f"테스트할 음성 파일: {len(audio_files)}개")
    print(f"테스트할 LLM 모델: {len(all_models)}개")
    print()

    # 서비스 초기화
    stt_service = STTService()
    model_manager = ModelManager()
    intent_parser = IntentParser()

    # ── 결과 수집 ──────────────────────────────────────────
    # results[filename] = {
    #   "stt_text": str, "stt_time": float,
    #   "models": { model_name: {"response": str, "departure": str|None,
    #                             "destination": str|None, "inference_time": float|None,
    #                             "error": str|None} }
    # }
    results = {}

    for idx, filename in enumerate(audio_files, start=1):
        filepath = os.path.join(AUDIO_DIR, filename)
        print(f"[{idx}/{len(audio_files)}] Processing: {filename}")

        # STT
        stt_start = time.time()
        try:
            stt_text = stt_service.transcribe(filepath)
            stt_time = time.time() - stt_start
        except Exception as e:
            stt_text = ""
            stt_time = time.time() - stt_start
            print(f"  - Whisper STT 오류: {e}")
            results[filename] = {"stt_text": f"ERROR: {e}", "stt_time": stt_time, "models": {}}
            continue

        print(f'  - Whisper STT: "{stt_text}" ({stt_time:.2f}s)')

        file_result = {"stt_text": stt_text, "stt_time": stt_time, "models": {}}

        # LLM
        prompt = intent_parser.create_prompt(stt_text, mode="nlu")

        for model_name in all_models:
            response_text, inference_time, error = run_with_timeout(
                generate_with_error_handling,
                model_manager, model_name, prompt,
                timeout=LLM_TIMEOUT
            )

            # run_with_timeout이 TIMEOUT 문자열을 error로 반환하는 경우
            if response_text is None and inference_time is None:
                error = error or "TIMEOUT"

            if error:
                departure, destination = None, None
                status = "❌"
                label = error
            else:
                departure, destination = extract_route(response_text)
                status = "✅" if (departure and destination) else "❌"
                if departure and destination:
                    label = f"{departure} → {destination} ({inference_time:.2f}s)"
                else:
                    label = f"출발지/도착지 추출 실패 ({inference_time:.2f}s)"

            print(f"  - {model_name}: {label} {status}")

            file_result["models"][model_name] = {
                "response": response_text or error,
                "departure": departure,
                "destination": destination,
                "inference_time": inference_time,
                "error": error,
            }

        results[filename] = file_result
        print()

    # ── Markdown 생성 ──────────────────────────────────────
    output_filename = f"batch_test_{run_time.strftime('%Y-%m-%d_%H-%M-%S')}.md"
    output_path = os.path.join(RESULTS_DIR, output_filename)

    lines = []
    lines.append("# 배치 테스트 결과\n")
    lines.append(f"- **실행 시각**: {run_time.strftime('%Y-%m-%d %H:%M:%S')}")
    lines.append(f"- **테스트한 음성 파일 수**: {len(audio_files)}개")
    lines.append(f"- **테스트한 LLM 모델 수**: {len(all_models)}개 ({', '.join(all_models)})")
    lines.append(f"- **Whisper 모델**: {Config.WHISPER_MODEL}")
    lines.append("")

    for file_idx, (filename, data) in enumerate(results.items(), start=1):
        lines.append("---\n")
        lines.append(f"## 📁 파일 {file_idx}: {filename}\n")
        lines.append("### 🎤 Whisper STT 결과")
        lines.append(f"- **변환된 텍스트**: `{data['stt_text']}`")
        lines.append(f"- **STT 처리 시간**: {data['stt_time']:.2f}초")
        lines.append("")

        if data["models"]:
            lines.append("### 🤖 LLM별 결과\n")
            for m_idx, (model_name, m_data) in enumerate(data["models"].items(), start=1):
                lines.append(f"#### {m_idx}. {model_name}")
                if m_data["error"]:
                    lines.append(f"- **결과**: `{m_data['error']}`")
                else:
                    lines.append(f"- **LLM 응답 원문**:")
                    lines.append(f"  ```")
                    lines.append(f"  {m_data['response']}")
                    lines.append(f"  ```")
                    lines.append(f"- **추출된 출발지**: `{m_data['departure'] or '추출 실패'}`")
                    lines.append(f"- **추출된 도착지**: `{m_data['destination'] or '추출 실패'}`")
                    lines.append(f"- **추론 시간**: {m_data['inference_time']:.2f}초")
                    lines.append(f"- **총 처리 시간 (STT + LLM)**: {data['stt_time'] + m_data['inference_time']:.2f}초")
                lines.append("")

    lines.append("---\n")

    # 요약 테이블
    lines.append("## 📊 요약 테이블\n")
    lines.append("| 파일명 | STT 결과 | 모델 | 출발지 | 도착지 | 추론 시간 |")
    lines.append("|--------|---------|------|--------|--------|-----------|")
    for filename, data in results.items():
        for model_name, m_data in data.get("models", {}).items():
            dep = m_data["departure"] or "-"
            dst = m_data["destination"] or "-"
            t = f"{m_data['inference_time']:.2f}s" if m_data["inference_time"] else m_data["error"]
            stt_short = data["stt_text"][:30] + ("..." if len(data["stt_text"]) > 30 else "")
            lines.append(f"| {filename} | {stt_short} | {model_name} | {dep} | {dst} | {t} |")
    lines.append("")

    # 모델별 평균 추론 시간
    lines.append("## 📈 모델별 평균 추론 시간\n")
    lines.append("| 모델 | 평균 추론 시간 | 출발지/도착지 추출 성공률 |")
    lines.append("|------|---------------|---------------------------|")
    total_files = len(results)
    for model_name in all_models:
        times = []
        success = 0
        for data in results.values():
            m = data.get("models", {}).get(model_name)
            if m and not m["error"] and m["inference_time"] is not None:
                times.append(m["inference_time"])
                if m["departure"] and m["destination"]:
                    success += 1
        avg = f"{sum(times)/len(times):.2f}s" if times else "N/A"
        rate = f"{success}/{total_files} ({int(success/total_files*100)}%)" if total_files else "0/0"
        lines.append(f"| {model_name} | {avg} | {rate} |")

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print("=" * 40)
    print(f"완료! 결과 파일: test_results/{output_filename}")
    print("=" * 40)


if __name__ == "__main__":
    main()
