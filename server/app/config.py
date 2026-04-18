from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# 固定从「server/」目录读 .env，避免在仓库根目录或其它路径执行 uvicorn 时读不到密钥
_SERVER_DIR = Path(__file__).resolve().parent.parent
_ENV_FILE = _SERVER_DIR / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(_ENV_FILE),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # §2 鉴权：Bearer Token（文档中的 AppKey）
    vivo_app_key: str = ""
    # §3 Chat：模型与 URL（可用环境变量覆盖默认值）
    vivo_chat_model: str = "Doubao-Seed-2.0-mini"
    vivo_chat_url: str = "https://api-ai.vivo.com.cn/v1/chat/completions"
    # §8 等：部分接口（如通用 OCR 的 businessid）可能需 AppId，当前 explain 流程未使用
    vivo_app_id: str = ""


settings = Settings()
