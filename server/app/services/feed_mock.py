"""Mock 热点：白名单演示数据，可按标签与频道筛选。"""

from __future__ import annotations

from app.schemas import FeedItem

_MOCK: list[FeedItem] = [
    FeedItem(
        id="m1",
        title="人工智能在医疗领域的应用引讨论",
        summary="近期多篇文章讨论 AI 辅助诊断的进展与风险，监管与伦理成为焦点。",
        source="新华网",
        url="https://www.news.cn/tech/20241219/fa8d539d4b164cc190738d2943ca080c/c.html",
        tag="科技",
        channel="tag",
        updated_at="2026-04-15",
    ),
    FeedItem(
        id="m2",
        title="载人航天新计划公布",
        summary="官方发布下一阶段空间站应用规划，公众关注科普与青少年教育配套。",
        source="新华网",
        url="https://www.news.cn/tech/20241016/93c4e0197d1841efa5a85ec020bef711/c.html",
        tag="军事",
        channel="tag",
        updated_at="2026-04-14",
    ),
    FeedItem(
        id="m3",
        title="春季文旅消费回暖",
        summary="多地推出非遗与乡村游线路，短视频平台相关话题播放量上升。",
        source="新华网",
        url="https://www.news.cn/travel/20240611/10311c05f01e420abcfb651d75cb87c4/c.html",
        tag="人文",
        channel="tag",
        updated_at="2026-04-13",
    ),
    FeedItem(
        id="m4",
        title="春季过敏与用药安全提醒",
        summary="医生科普花粉季防护，提醒长辈勿自行加量服药，不适及时就诊。",
        source="新华网",
        url="https://www.news.cn/health/",
        tag="健康",
        channel="tag",
        updated_at="2026-04-12",
    ),
    FeedItem(
        id="m5",
        title="新款手机续航与系统更新引关注",
        summary="数码媒体讨论大电池与 AI 功能对耗电的影响，建议关闭不必要后台刷新。",
        source="新华网",
        url="https://www.news.cn/tech/",
        tag="数码",
        channel="tag",
        updated_at="2026-04-11",
    ),
    FeedItem(
        id="m6",
        title="网友热议一档综艺名场面",
        summary="节目片段在社交平台二次传播，评论区多围绕「松弛感」与代际审美差异。",
        source="澎湃新闻",
        url="https://www.thepaper.cn/",
        tag="吃瓜",
        channel="tag",
        updated_at="2026-04-10",
    ),
    FeedItem(
        id="m7",
        title="大模型辅助写文案：边界在哪？",
        summary="讨论 AI 生成内容的标注与版权，普通用户宜辨别来源、勿轻信「权威口吻」。",
        source="新华网",
        url="https://www.news.cn/tech/",
        tag="AI",
        channel="tag",
        updated_at="2026-04-09",
    ),
    FeedItem(
        id="m8",
        title="社区便民服务线上预约试点",
        summary="多地推水电维修、助老餐预约小程序，长辈可请子女协助首次绑定。",
        source="新华网",
        url="https://www.news.cn/society/",
        tag="生活",
        channel="tag",
        updated_at="2026-04-08",
    ),
    FeedItem(
        id="m9",
        title="志愿服务与邻里互助报道",
        summary="媒体报道社区互助驿站，鼓励年轻人与长辈结对使用智能手机办事。",
        source="新华网",
        url="https://www.news.cn/society/",
        tag="社会",
        channel="tag",
        updated_at="2026-04-07",
    ),
    FeedItem(
        id="t1",
        title="「梗」科普：什么是「显眼包」",
        summary="年轻人常用词，多指爱出风头又有点可爱的人或行为，常带调侃语气。",
        source="澎湃新闻",
        url="https://www.thepaper.cn/newsDetail_forward_23506591",
        tag="社会",
        channel="trend",
        updated_at="2026-04-15",
    ),
]

_DAILY_DIGEST: list[FeedItem] = []


def replace_daily_digest(items: list[FeedItem]) -> None:
    global _DAILY_DIGEST
    _DAILY_DIGEST = list(items)


def all_mock_items() -> list[FeedItem]:
    return list(_MOCK) + list(_DAILY_DIGEST)


def filter_by_tag(tag: str | None) -> list[FeedItem]:
    """tag 为空表示全部；多个标签用 | 分隔（与 App 导航约定一致），取并集。"""
    base = [i for i in _MOCK + _DAILY_DIGEST if i.channel == "tag"]
    if not tag or not tag.strip():
        return base
    tags = {t.strip() for t in tag.split("|") if t.strip()}
    if not tags:
        return base
    return [i for i in base if i.tag in tags]


def trend_items() -> list[FeedItem]:
    return [i for i in _MOCK if i.channel == "trend"]
