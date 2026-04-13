import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from utils.logger import get_logger

logger = get_logger(__name__)


class HuggingFaceModel:
    def __init__(self, model_path: str):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        logger.info(f"Loading {model_path} on {self.device}...")

        self.tokenizer = AutoTokenizer.from_pretrained(model_path)
        self.model = AutoModelForCausalLM.from_pretrained(
            model_path,
            torch_dtype=torch.float16 if self.device == "cuda" else torch.float32,
            device_map="auto" if self.device == "cuda" else None
        )

        if self.device == "cpu":
            self.model = self.model.to(self.device)

        logger.info(f"Model loaded successfully on {self.device}")

    def generate(self, prompt: str, max_length: int = 512) -> str:
        """HuggingFace 모델로 텍스트 생성"""
        logger.debug(f"Generating on {self.device}")

        inputs = self.tokenizer(prompt, return_tensors="pt").to(self.device)

        with torch.no_grad():
            outputs = self.model.generate(
                **inputs,
                max_new_tokens=max_length,
                temperature=0.7,
                top_p=0.9,
                do_sample=True,
                pad_token_id=self.tokenizer.eos_token_id
            )

        generated_text = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        response = generated_text[len(prompt):].strip()

        logger.debug(f"Generated response length: {len(response)} chars")

        return response
