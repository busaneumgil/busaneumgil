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
    get_system_prompt,
    parse_json_response,
    is_success,
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


@app.route('/voice/analyze', methods=['POST'])
def voice_analyze():
    """백엔드 연동용 단일 엔드포인트 - STT 텍스트 의미 추론"""
    body = request.get_json()

    text = body.get("text", "").strip()
    if not text:
        return jsonify({
            "success": False,
            "intent": "unknown",
            "confirmationMessage": "다시 말씀해 주세요",
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
            "confirmationMessage": "다시 말씀해 주세요",
            "error": f"지원하지 않는 모델입니다: {model_key}",
            "model": model_key,
            "latency_ms": 0
        }), 400

    mode = body.get("mode", "MOBILITY_IMPAIRED")
    if mode not in ("MOBILITY_IMPAIRED", "LOW_VISION"):
        return jsonify({
            "success": False,
            "error": "mode는 'MOBILITY_IMPAIRED' 또는 'LOW_VISION'만 허용됩니다.",
            "model": model_key,
            "mode": mode,
        }), 400

    internal_mode = "visually" if mode == "LOW_VISION" else "mobility"
    history = body.get("history", [])
    system_prompt = get_system_prompt(internal_mode)

    if mode == "LOW_VISION" and history:
        messages = history + [{"role": "user", "content": text}]
    else:
        messages = [{"role": "user", "content": text}]

    start_ms = int(time.time() * 1000)
    try:
        result = provider.call(text, system_prompt=system_prompt, messages=messages)
        latency_ms = int(time.time() * 1000) - start_ms
        data = asdict(result)
        intent_raw = data.get("intent") or "unknown"
        intent_upper = intent_raw.upper()
        return jsonify({
            "success": result.success,
            "intent": intent_upper,
            "placeName": data.get("place_name"),
            "confirmed": data.get("confirmed") if mode == "LOW_VISION" else None,
            "confirmationMessage": data.get("confirmation_message") if mode == "LOW_VISION" else None,
            "model": model_key,
            "mode": mode,
            "latency_ms": latency_ms
        })
    except Exception as e:
        latency_ms = int(time.time() * 1000) - start_ms
        logger.error(f"voice_analyze 오류: {e}")
        return jsonify({
            "success": False,
            "intent": "UNKNOWN",
            "placeName": None,
            "confirmed": None,
            "confirmationMessage": None,
            "error": str(e),
            "model": model_key,
            "mode": mode,
            "latency_ms": latency_ms
        }), 500


# /api/voice/confirm 엔드포인트 폐기
# 확인 응답 판단은 /api/voice/analyze의 히스토리 기반으로 처리
# @app.route('/api/voice/confirm', methods=['POST'])
# def voice_confirm():
#     """시각장애인 모드 전용 - 확인 응답 판단 엔드포인트"""
#     body = request.get_json()
#
#     text = body.get("text", "").strip()
#     if not text:
#         return jsonify({
#             "success": False,
#             "confirmed": False,
#             "message": "다시 말씀해 주세요",
#             "error": "text 필드가 비어 있습니다",
#             "model": body.get("model", Config.DEFAULT_MODEL),
#             "latency_ms": 0
#         }), 400
#
#     model_key = body.get("model", Config.DEFAULT_MODEL)
#     provider = PROVIDERS.get(model_key)
#     if not provider:
#         return jsonify({
#             "success": False,
#             "confirmed": False,
#             "message": "다시 말씀해 주세요",
#             "error": f"지원하지 않는 모델입니다: {model_key}",
#             "model": model_key,
#             "latency_ms": 0
#         }), 400
#
#     start_ms = int(time.time() * 1000)
#     try:
#         result = provider.call(text, system_prompt=SYSTEM_PROMPT_CONFIRM)
#         latency_ms = int(time.time() * 1000) - start_ms
#         parsed = parse_json_response(result.raw_text)
#         confirmed = parsed.get("confirmed", False)
#         message = parsed.get("message", "다시 말씀해 주세요")
#         return jsonify({
#             "success": is_success("", parsed, prompt_type="confirm"),
#             "confirmed": confirmed,
#             "message": message,
#             "model": model_key,
#             "latency_ms": latency_ms
#         })
#     except Exception as e:
#         latency_ms = int(time.time() * 1000) - start_ms
#         logger.error(f"voice_confirm 오류: {e}")
#         return jsonify({
#             "success": False,
#             "confirmed": False,
#             "message": "다시 말씀해 주세요",
#             "error": str(e),
#             "model": model_key,
#             "latency_ms": latency_ms
#         }), 500


@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        "status": "healthy",
        "providers": list(PROVIDERS.keys()),
        "modes": ["MOBILITY_IMPAIRED", "LOW_VISION"],
        "endpoints": [
            "POST /voice/analyze",
        ],
    })


if __name__ == '__main__':
    logger.info("=" * 60)
    logger.info("LLM Voice Server Starting...")
    logger.info("Providers: " + ", ".join(PROVIDERS.keys()))
    logger.info("=" * 60)
    app.run(host=Config.HOST, port=Config.PORT, debug=Config.DEBUG)
