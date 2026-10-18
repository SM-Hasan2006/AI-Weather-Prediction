from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path


@dataclass(frozen=True)
class Settings:
    models_dir: Path = Path(__file__).resolve().parent.parent / "models"
    cors_allowed_origins: list[str] = field(default_factory=lambda: ["http://localhost:5173", "http://localhost:8080"])
    api_key: str | None = None  # optional shared secret between Spring Boot and this service

    @staticmethod
    def from_env() -> "Settings":
        origins = [o.strip() for o in os.getenv("ML_CORS_ALLOWED_ORIGINS", "http://localhost:5173,http://localhost:8080").split(",") if o.strip()]
        return Settings(
            models_dir=Path(os.getenv("ML_MODELS_DIR", str(Path(__file__).resolve().parent.parent / "models"))),
            cors_allowed_origins=origins,
            api_key=os.getenv("ML_SERVICE_API_KEY") or None,
        )
