import os


class Config:
    # 서버 설정
    HOST = '0.0.0.0'
    PORT = 5000
    DEBUG = True

    # 기본 모델 설정
    DEFAULT_MODEL = os.getenv("DEFAULT_MODEL", "gemini")

    # 로그 설정
    LOG_LEVEL = "DEBUG"
