from __future__ import annotations

from contextlib import asynccontextmanager
from zoneinfo import ZoneInfo

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import explain, feed, health, image, share, tts, video_quick
from app.services import feed_digest

_scheduler = BackgroundScheduler()


@asynccontextmanager
async def lifespan(app: FastAPI):
    feed_digest.refresh_daily_digest()
    _scheduler.add_job(
        feed_digest.refresh_daily_digest,
        CronTrigger(hour=7, minute=0, timezone=ZoneInfo("Asia/Shanghai")),
        id="daily_digest",
        replace_existing=True,
    )
    _scheduler.start()
    yield
    _scheduler.shutdown(wait=False)


app = FastAPI(title="WarmBridge BFF", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(feed.router)
app.include_router(explain.router)
app.include_router(share.router)
app.include_router(image.router)
app.include_router(video_quick.router)
app.include_router(tts.router)
