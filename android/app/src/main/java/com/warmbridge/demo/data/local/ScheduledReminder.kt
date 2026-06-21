package com.warmbridge.demo.data.local

data class ScheduledReminder(
    val id: String,
    val message: String,
    val triggerAtMillis: Long,
    val createdAtMillis: Long,
    val status: String = STATUS_PENDING,
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_FIRED = "fired"
    }
}
