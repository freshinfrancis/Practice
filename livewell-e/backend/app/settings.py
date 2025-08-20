from pydantic import BaseModel
import os

class Settings(BaseModel):
    openai_api_key: str = os.getenv("OPENAI_API_KEY", "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")
    openai_model: str = os.getenv("OPENAI_MODEL", "gpt-4o-mini")

settings = Settings()