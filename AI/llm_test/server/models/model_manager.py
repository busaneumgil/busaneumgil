import time
import torch
from typing import Tuple
from .ollama_models import OllamaModel
from .hf_models import HuggingFaceModel
from config import Config
from utils.logger import get_logger, log_performance

logger = get_logger(__name__)


class ModelManager:
    def __init__(self):
        self.ollama_models = {}
        self.hf_models = {}
        self.loaded_models = set()
        logger.info("ModelManager initialized")

    @log_performance
    def load_model(self, model_name: str):
        """모델 로드"""
        if model_name in self.loaded_models:
            logger.debug(f"Model {model_name} already loaded")
            return

        logger.info(f"Loading model: {model_name}")

        if model_name in Config.OLLAMA_MODELS:
            self.ollama_models[model_name] = OllamaModel(model_name)
        elif model_name in Config.HF_MODELS:
            self.hf_models[model_name] = HuggingFaceModel(
                Config.HF_MODELS[model_name]
            )
        else:
            raise ValueError(f"Unknown model: {model_name}")

        self.loaded_models.add(model_name)
        logger.info(f"Model {model_name} loaded successfully")

    @log_performance
    def generate(self, model_name: str, prompt: str) -> Tuple[str, float]:
        """텍스트 생성"""
        self.load_model(model_name)

        logger.debug(f"Generating with {model_name}")
        logger.debug(f"Prompt length: {len(prompt)} chars")

        start_time = time.time()

        if model_name in self.ollama_models:
            response = self.ollama_models[model_name].generate(prompt)
        elif model_name in self.hf_models:
            response = self.hf_models[model_name].generate(prompt)
        else:
            raise ValueError(f"Model not loaded: {model_name}")

        inference_time = time.time() - start_time

        logger.info(f"Generated {len(response)} chars in {inference_time:.2f}s")

        return response, inference_time
