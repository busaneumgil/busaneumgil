import os


class Config:
    # 서버 설정
    HOST = '0.0.0.0'
    PORT = 5000
    DEBUG = True

    # Ollama 설정
    OLLAMA_URL = "http://localhost:11434/api/generate"
    OLLAMA_MODELS = {
        "qwen2.5:7b": "qwen2.5:7b",
        "solar": "solar",
        "mistral": "mistral"
    }

    # HuggingFace 모델 설정
    HF_MODELS = {
        "gemma-ko-2b": "beomi/gemma-ko-2b",
        # "gemma-ko-9b": "beomi/gemma-ko-9b",
        "kullm-5.8b": "nlpai-lab/kullm-polyglot-5.8b-v2"
    }

    # Whisper 설정
    WHISPER_MODEL = "large-v3"  # tiny, base, small, medium, large

    # 파일 업로드 설정
    UPLOAD_FOLDER = 'uploads'
    MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 16MB

    # 로그 설정
    LOG_LEVEL = "DEBUG"
