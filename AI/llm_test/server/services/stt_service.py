import whisper
import time
from utils.logger import get_logger, log_performance
from config import Config

logger = get_logger(__name__)


class STTService:
    def __init__(self):
        logger.info(f"Loading Whisper model: {Config.WHISPER_MODEL}")
        start_time = time.time()

        self.model = whisper.load_model(Config.WHISPER_MODEL)

        elapsed = time.time() - start_time
        logger.info(f"Whisper model loaded in {elapsed:.2f}s")

    @log_performance
    def transcribe(self, audio_path: str) -> str:
        """음성 파일을 텍스트로 변환"""
        logger.debug(f"Transcribing: {audio_path}")

        result = self.model.transcribe(
            audio_path,
            language="ko",
            fp16=False
        )

        transcribed_text = result["text"].strip()
        logger.info(f"Transcribed: '{transcribed_text}'")

        return transcribed_text
