from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import time
from dataclasses import asdict
from dotenv import load_dotenv

load_dotenv()

if not os.getenv("GMS_KEY"):
    raise EnvironmentError(".env 파일에 GMS_KEY가 설정되어 있지 않습니다.")

from config import Config
from utils.logger import get_logger
from utils.result_logger import save_result
from providers.gemini_provider import GeminiProvider
from providers.claude_provider import ClaudeProvider
from providers.gpt_mini_provider import GPTMiniProvider
from providers.utils import (
    SYSTEM_PROMPT_MOBILITY,
    SYSTEM_PROMPT_VISUALLY,
    SYSTEM_PROMPT_CONFIRM,
    parse_json_response,
)

logger = get_logger(__name__)

app = Flask(__name__)
app.config.from_object(Config)
CORS(app)

PROVIDERS = {
    "gemini":   GeminiProvider(),
    "claude":   ClaudeProvider(),
    "gpt_mini": GPTMiniProvider(),
}

MODE_PROMPTS = {
    "mobility": SYSTEM_PROMPT_MOBILITY,
    "visually": SYSTEM_PROMPT_VISUALLY,
}

logger.info("Providers ready: " + ", ".join(PROVIDERS.keys()))


@app.route('/api/chat/llm', methods=['POST'])
def chat_llm():
    """단일 모델 호출 (PoC용)"""
    body = request.get_json()
    text = body.get("text", "")
    model_key = body.get("model", Config.DEFAULT_MODEL)
    stt_start_ms = body.get("stt_start_ms", int(time.time() * 1000))

    provider = PROVIDERS.get(model_key)
    if not provider:
        return jsonify({"error": f"Unknown model: {model_key}"}), 400

    result = provider.call(text)
    result.total_latency_ms = int(time.time() * 1000) - stt_start_ms
    save_result(text, stt_start_ms, [result])

    return jsonify(asdict(result))


@app.route('/api/voice/analyze', methods=['POST'])
def voice_analyze():
    """백엔드 연동용 단일 엔드포인트 - STT 텍스트 의미 추론"""
    body = request.get_json()

    text = body.get("text", "").strip()
    if not text:
        return jsonify({
            "success": False,
            "intent": "unknown",
            "confirmation_message": "다시 말씀해 주세요",
            "error": "text 필드가 비어 있습니다",
            "model": body.get("model", Config.DEFAULT_MODEL),
            "latency_ms": 0
        }), 400

    model_key = body.get("model", Config.DEFAULT_MODEL)
    provider = PROVIDERS.get(model_key)
    if not provider:
        return jsonify({
            "success": False,
            "intent": "unknown",
            "confirmation_message": "다시 말씀해 주세요",
            "error": f"지원하지 않는 모델입니다: {model_key}",
            "model": model_key,
            "latency_ms": 0
        }), 400

    mode = body.get("mode", "mobility")
    if mode not in MODE_PROMPTS:
        return jsonify({
            "success": False,
            "intent": "unknown",
            "confirmation_message": "다시 말씀해 주세요",
            "error": f"지원하지 않는 mode입니다: {mode}. 사용 가능: mobility, visually",
            "model": model_key,
            "latency_ms": 0
        }), 400

    system_prompt = MODE_PROMPTS[mode]

    start_ms = int(time.time() * 1000)
    try:
        result = provider.call(text, system_prompt=system_prompt)
        latency_ms = int(time.time() * 1000) - start_ms
        data = asdict(result)
        return jsonify({
            "success": result.success,
            "intent": data.get("intent"),
            "departure": data.get("departure"),
            "destination": data.get("destination"),
            "place_name": data.get("place_name"),
            "facility_type": data.get("facility_type"),
            "confirmation_message": data.get("confirmation_message"),
            "model": model_key,
            "latency_ms": latency_ms
        })
    except Exception as e:
        latency_ms = int(time.time() * 1000) - start_ms
        logger.error(f"voice_analyze 오류: {e}")
        return jsonify({
            "success": False,
            "intent": "unknown",
            "confirmation_message": "다시 말씀해 주세요",
            "error": str(e),
            "model": model_key,
            "latency_ms": latency_ms
        }), 500


@app.route('/api/voice/confirm', methods=['POST'])
def voice_confirm():
    """시각장애인 모드 전용 - 확인 응답 판단 엔드포인트"""
    body = request.get_json()

    text = body.get("text", "").strip()
    if not text:
        return jsonify({
            "success": False,
            "confirmed": False,
            "message": "다시 말씀해 주세요",
            "error": "text 필드가 비어 있습니다",
            "model": body.get("model", Config.DEFAULT_MODEL),
            "latency_ms": 0
        }), 400

    model_key = body.get("model", Config.DEFAULT_MODEL)
    provider = PROVIDERS.get(model_key)
    if not provider:
        return jsonify({
            "success": False,
            "confirmed": False,
            "message": "다시 말씀해 주세요",
            "error": f"지원하지 않는 모델입니다: {model_key}",
            "model": model_key,
            "latency_ms": 0
        }), 400

    start_ms = int(time.time() * 1000)
    try:
        result = provider.call(text, system_prompt=SYSTEM_PROMPT_CONFIRM)
        latency_ms = int(time.time() * 1000) - start_ms
        parsed = parse_json_response(result.raw_text)
        confirmed = parsed.get("confirmed", False)
        message = parsed.get("message", "다시 말씀해 주세요")
        return jsonify({
            "success": True,
            "confirmed": confirmed,
            "message": message,
            "model": model_key,
            "latency_ms": latency_ms
        })
    except Exception as e:
        latency_ms = int(time.time() * 1000) - start_ms
        logger.error(f"voice_confirm 오류: {e}")
        return jsonify({
            "success": False,
            "confirmed": False,
            "message": "다시 말씀해 주세요",
            "error": str(e),
            "model": model_key,
            "latency_ms": latency_ms
        }), 500


@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        "status": "healthy",
        "providers": list(PROVIDERS.keys()),
        "modes": ["mobility", "visually"],
    })


if __name__ == '__main__':
    logger.info("=" * 60)
    logger.info("LLM Voice Server Starting...")
    logger.info("Providers: " + ", ".join(PROVIDERS.keys()))
    logger.info("=" * 60)
    app.run(host=Config.HOST, port=Config.PORT, debug=Config.DEBUG)
