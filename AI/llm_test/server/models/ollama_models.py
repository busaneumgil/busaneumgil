import requests
from config import Config
from utils.logger import get_logger

logger = get_logger(__name__)


class OllamaModel:
    def __init__(self, model_name: str):
        self.model_name = Config.OLLAMA_MODELS[model_name]
        logger.info(f"Initialized Ollama model: {self.model_name}")

    def generate(self, prompt: str) -> str:
        """Ollama API로 텍스트 생성"""
        logger.debug(f"Calling Ollama API with model: {self.model_name}")

        response = requests.post(
            Config.OLLAMA_URL,
            json={
                "model": self.model_name,
                "prompt": prompt,
                "stream": False,
                "options": {
                    "temperature": 0.7,
                    "top_p": 0.9
                }
            },
            timeout=120
        )

        if response.status_code == 200:
            result = response.json()['response']
            logger.debug(f"Ollama response length: {len(result)} chars")
            return result
        else:
            logger.error(f"Ollama API error: {response.status_code} - {response.text}")
            raise Exception(f"Ollama API error: {response.text}")
