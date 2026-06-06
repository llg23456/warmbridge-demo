from pathlib import Path

from pydantic import AliasChoices, Field
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
    # AI 解读 / 追问：优先 pro，限流时 fallback lite
    vivo_explain_model: str = "Doubao-Seed-2.0-pro"
    vivo_explain_model_fallback: str = "Doubao-Seed-2.0-lite"
    vivo_explain_use_tools: bool = True
    vivo_chat_url: str = "https://api-ai.vivo.com.cn/v1/chat/completions"
    # §8 等：部分接口（如通用 OCR 的 businessid）可能需 AppId，当前 explain 流程未使用
    vivo_app_id: str = ""
    # 分享解读：DuckDuckGo Instant Answer 轻量检索（无需单独 API Key；内网/限流时可能为空）
    web_search_enabled: bool = True
    # 通俗视频封面：RAWG 游戏图（https://rawg.io/apidocs）；未配置时静默降级 Bing
    rawg_api_key: str = ""
    # 通俗视频 D2：vivo 文生图/轮播（false 时 D1 仍可用 Ken Burns + 封面）
    popular_video_use_vivo_media: bool = True
    # §5 文生图
    vivo_image_url: str = "https://api-ai.vivo.com.cn/api/v1/image_generation"
    vivo_image_model: str = "Doubao-Seedream-4.5"
    # §5：Seedream 最小约 1920×1920（3686400 px）；勿用 1280x720，合成层再裁 16:9
    vivo_image_size: str = "2K"
    vivo_image_timeout_sec: float = 90.0
    # §6 图生视频（D3 片头）
    popular_video_use_vivo_intro: bool = True
    vivo_video_submit_url: str = "https://api-ai.vivo.com.cn/api/v1/submit_task"
    vivo_video_query_url: str = "https://api-ai.vivo.com.cn/api/v1/query_task"
    # 可选：Doubao-Seedance-1.0-pro（默认）、Doubao-Seedance-2.0、Doubao-Seedance-2.0-fast
    vivo_video_model: str = "Doubao-Seedance-1.0-pro"
    vivo_video_duration_sec: int = 5
    vivo_video_submit_timeout_sec: float = 60.0
    vivo_video_query_timeout_sec: float = 30.0
    vivo_video_poll_interval_sec: float = 5.0
    vivo_video_poll_max_attempts: int = 60
    # 通俗视频成片：未保存离开页面后超过此秒数删 mp4（中间 work 任务结束即删）
    popular_video_mp4_idle_sec: int = 3600
    popular_video_mp4_max_keep: int = 5
    # §8 OCR：文档示例为 http://（若 HTTPS 可用可在 .env 覆盖）
    vivo_ocr_url: str = "http://api-ai.vivo.com.cn/ocr/general_recognition"
    vivo_ocr_businessid: str = Field(
        default="",
        validation_alias=AliasChoices("VIVO_OCR_BUSINESSID", "VIVO_OCR_BUSINESS_ID"),
    )
    # pos：0 仅文字；2 文字+相对坐标（文档常推荐 2；若 0 取不到字可改 2 试）
    vivo_ocr_pos: str = "0"
    # §14 TTS：未配置 vaid 时服务端使用官方示例 123456789；勿把 APP_ID 当 vaid
    vivo_tts_engineid: str = "long_audio_synthesis_screen"
    vivo_tts_vcn: str = "x2_yige_news"
    vivo_tts_vaid: str = Field(
        default="",
        validation_alias=AliasChoices("VIVO_TTS_VAID", "VIVO_VAID"),
    )


settings = Settings()
