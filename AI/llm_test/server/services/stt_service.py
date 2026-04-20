from faster_whisper import WhisperModel
import time
from utils.logger import get_logger, log_performance
from config import Config

logger = get_logger(__name__)


class STTService:
    def __init__(self):
        logger.info(f"Loading Whisper model: {Config.WHISPER_MODEL}")
        start_time = time.time()

        # faster-whisper로 변경
        self.model = WhisperModel(
            Config.WHISPER_MODEL,       # "large-v3"
            device="cuda",              # GPU 사용
            compute_type="int8_float16" # 양자화로 VRAM 절약
        )

        elapsed = time.time() - start_time
        logger.info(f"Whisper model loaded in {elapsed:.2f}s")

    @log_performance
    def transcribe(self, audio_path: str) -> str:
        """음성 파일을 텍스트로 변환"""
        logger.debug(f"Transcribing: {audio_path}")

        # faster-whisper는 제너레이터로 반환
        segments, info = self.model.transcribe(
            audio_path,
            language="ko",
            beam_size=5,                # 정확도 향상
        )

        # 세그먼트를 하나의 텍스트로 결합
        transcribed_text = " ".join([segment.text for segment in segments]).strip()
        logger.info(f"Transcribed: '{transcribed_text}'")

        return transcribed_text