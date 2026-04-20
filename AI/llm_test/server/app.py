from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import re
import time
from werkzeug.utils import secure_filename

from config import Config
from models.model_manager import ModelManager
from services.stt_service import STTService
from services.intent_parser import IntentParser
from utils.logger import get_logger

logger = get_logger(__name__)

app = Flask(__name__)
app.config.from_object(Config)
CORS(app)

# 서비스 초기화
logger.info("Initializing services...")
model_manager = ModelManager()
stt_service = STTService()
intent_parser = IntentParser()

# 업로드 폴더 생성
os.makedirs(Config.UPLOAD_FOLDER, exist_ok=True)


def _build_result(request_id, model_name, message, conversation_state, inference_time, transcribed_text=None):
    """conversation_state에 따라 LLM 호출 및 결과 딕셔너리 반환"""
    dark_mode_action = intent_parser.parse_dark_mode_intent(message)

    if conversation_state == "waiting_confirmation":
        prompt = intent_parser.create_prompt(message, mode="confirm")
        response_text, llm_time = model_manager.generate(model_name, prompt)
        confirmed = response_text.strip().startswith("예")
        next_state = "confirmed" if confirmed else "initial"
        logger.info(f"[{request_id}] Confirm mode - response: '{response_text}', confirmed: {confirmed}")
        result = {
            "response": response_text,
            "action": dark_mode_action,
            "departure": None,
            "destination": None,
            "next_state": next_state,
            "confirmed": confirmed,
            "inference_time": round(inference_time + llm_time, 2),
            "model_name": model_name,
            "transcribed_text": transcribed_text
        }
    else:  # initial
        prompt = intent_parser.create_prompt(message, mode="nlu")
        response_text, llm_time = model_manager.generate(model_name, prompt)

        departure = None
        destination = None
        match = re.search(r'(.+?)에서\s*(.+?)까지\s*길을\s*찾을까요', response_text)
        if match:
            departure = match.group(1).strip()
            destination = match.group(2).strip()

        next_state = "waiting_confirmation" if (departure and destination) else "initial"
        logger.info(f"[{request_id}] NLU mode - departure: {departure}, destination: {destination}, next_state: {next_state}")
        result = {
            "response": response_text,
            "action": dark_mode_action,
            "departure": departure,
            "destination": destination,
            "next_state": next_state,
            "confirmed": False,
            "inference_time": round(inference_time + llm_time, 2),
            "model_name": model_name,
            "transcribed_text": transcribed_text
        }

    return result


@app.route('/api/chat/android_stt', methods=['POST'])
def chat_with_android_stt():
    """Android STT 방식"""
    request_id = f"android_stt_{int(time.time() * 1000)}"
    start_time = time.time()

    try:
        data = request.json
        model_name = data.get('model_name')
        message = data.get('message')
        session_id = data.get('session_id')
        conversation_state = data.get('conversation_state', 'initial')

        logger.info(f"[{request_id}] Android STT Request - state: {conversation_state}")
        logger.debug(f"[{request_id}] Model: {model_name}, Message: '{message}'")

        result = _build_result(request_id, model_name, message, conversation_state, 0.0)

        total_time = time.time() - start_time
        logger.info(f"[{request_id}] Completed in {total_time:.2f}s")

        return jsonify(result)

    except Exception as e:
        logger.error(f"[{request_id}] Error: {str(e)}", exc_info=True)
        return jsonify({"error": str(e)}), 500


@app.route('/api/chat/whisper_stt', methods=['POST'])
def chat_with_whisper_stt():
    """Whisper STT 방식"""
    request_id = f"whisper_stt_{int(time.time() * 1000)}"
    start_time = time.time()
    filepath = None

    try:
        # 파일 받기
        if 'audio' not in request.files:
            logger.error(f"[{request_id}] No audio file in request")
            return jsonify({"error": "No audio file"}), 400

        audio_file = request.files['audio']
        model_name = request.form.get('model_name')
        conversation_state = request.form.get('conversation_state', 'initial')

        logger.info(f"[{request_id}] Whisper STT Request - state: {conversation_state}")
        logger.debug(f"[{request_id}] Model: {model_name}, File: {audio_file.filename}")

        # 파일 저장
        save_start = time.time()
        filename = secure_filename(audio_file.filename)
        filepath = os.path.join(Config.UPLOAD_FOLDER, filename)
        audio_file.save(filepath)
        file_size = os.path.getsize(filepath)
        save_time = time.time() - save_start
        logger.debug(f"[{request_id}] File saved: {file_size} bytes in {save_time:.3f}s")

        # STT 변환
        stt_start = time.time()
        transcribed_text = stt_service.transcribe(filepath)
        stt_time = time.time() - stt_start
        logger.info(f"[{request_id}] STT: '{transcribed_text}' in {stt_time:.2f}s")

        # 파일 삭제
        os.remove(filepath)

        result = _build_result(request_id, model_name, transcribed_text, conversation_state, stt_time, transcribed_text=transcribed_text)

        total_time = time.time() - start_time
        logger.info(f"[{request_id}] Completed in {total_time:.2f}s")

        return jsonify(result)

    except Exception as e:
        logger.error(f"[{request_id}] Error: {str(e)}", exc_info=True)
        if filepath and os.path.exists(filepath):
            os.remove(filepath)
        return jsonify({"error": str(e)}), 500


@app.route('/health', methods=['GET'])
def health_check():
    """서버 상태 확인"""
    return jsonify({
        "status": "healthy",
        "loaded_models": list(model_manager.loaded_models)
    })


if __name__ == '__main__':
    logger.info("=" * 60)
    logger.info("LLM Voice Test Server Starting...")
    logger.info(f"Ollama Models: {list(Config.OLLAMA_MODELS.keys())}")
    logger.info(f"HuggingFace Models: {list(Config.HF_MODELS.keys())}")
    logger.info(f"Whisper Model: {Config.WHISPER_MODEL}")
    logger.info("=" * 60)

    app.run(
        host=Config.HOST,
        port=Config.PORT,
        debug=Config.DEBUG
    )
