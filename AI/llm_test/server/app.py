from flask import Flask, request, jsonify, Response, stream_with_context
from flask_cors import CORS
import os
import time
import json
from dataclasses import asdict
from dotenv import load_dotenv

load_dotenv()

if not os.getenv("GMS_KEY"):
    raise EnvironmentError(".env 파일에 GMS_KEY가 설정되어 있지 않습니다.")

from config import Config
from utils.logger import get_logger
from comparator import LLMComparator
from result_logger import save_result

logger = get_logger(__name__)

app = Flask(__name__)
app.config.from_object(Config)
CORS(app)

logger.info("Initializing services...")
comparator = LLMComparator()
logger.info("LLMComparator ready (qwen_ollama / gemini / claude / gpt)")


@app.route('/api/chat/llm', methods=['POST'])
def chat_llm():
    """단일 모델 호출 - Android 메인 사용"""
    body = request.get_json()
    text = body.get("text", "")
    model_key = body.get("model", "qwen_ollama")
    stt_start_ms = body.get("stt_start_ms", int(time.time() * 1000))

    provider = comparator.providers.get(model_key)
    if not provider:
        return jsonify({"error": f"Unknown model: {model_key}"}), 400

    result = provider.call(text)
    result.total_latency_ms = int(time.time() * 1000) - stt_start_ms
    save_result(text, stt_start_ms, [result])

    return jsonify(asdict(result))


@app.route('/api/compare/stream')
def compare_stream():
    """SSE 스트리밍 - 4개 모델 동시 비교"""
    text = request.args.get("text", "")
    stt_start_ms = int(request.args.get("stt_start_ms", int(time.time() * 1000)))
    stt_included = "stt_start_ms" in request.args

    def generate():
        results = []
        for result in comparator.compare_stream(text, stt_start_ms):
            results.append(result)
            data = asdict(result)
            if not stt_included:
                data["stt_included"] = False
            yield f"data: {json.dumps(data, ensure_ascii=False)}\n\n"
        save_result(text, stt_start_ms, results)
        yield 'data: {"event":"done"}\n\n'

    return Response(
        stream_with_context(generate()),
        mimetype="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"}
    )


@app.route('/api/compare', methods=['POST'])
def compare_all():
    """4개 모델 비교 - 디버깅용, 전체 완료 후 반환"""
    body = request.get_json()
    text = body.get("text", "")
    stt_start_ms = body.get("stt_start_ms", int(time.time() * 1000))

    results = list(comparator.compare_stream(text, stt_start_ms))
    results_sorted = sorted(results, key=lambda r: r.total_latency_ms)
    save_result(text, stt_start_ms, results)

    return jsonify({
        "input": text,
        "results": [asdict(r) for r in results_sorted],
        "summary": {
            "fastest_total": min(results, key=lambda r: r.total_latency_ms).provider,
            "cheapest": min(results, key=lambda r: r.cost_credit).provider,
            "success_count": sum(1 for r in results if r.success)
        }
    })


@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({"status": "healthy", "providers": list(comparator.providers.keys())})


if __name__ == '__main__':
    logger.info("=" * 60)
    logger.info("LLM Voice Test Server Starting...")
    logger.info("Providers: qwen_ollama / gemini / claude / gpt")
    logger.info("=" * 60)
    app.run(host=Config.HOST, port=Config.PORT, debug=Config.DEBUG)
