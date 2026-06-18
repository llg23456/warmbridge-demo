package com.warmbridge.demo.util

import com.warmbridge.demo.R
import java.util.Calendar

/**
 * 家长首页问候语资源，按本地时间返回「早上/中午/下午/晚上好」。
 *
 * 5–10 点早上；11–12 点中午；13–17 点下午；其余晚上。
 */
fun parentHomeGreetingResId(
    hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
): Int = when (hourOfDay) {
    in 5..10 -> R.string.home_greeting_parent_morning
    in 11..12 -> R.string.home_greeting_parent_noon
    in 13..17 -> R.string.home_greeting_parent_afternoon
    else -> R.string.home_greeting_parent_evening
}
