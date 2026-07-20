package com.checkmatey.core.daily

/**
 * When the next daily reminder should fire. Pure so the scheduling maths is tested without the clock
 * or Android — the caller supplies "milliseconds since local midnight", this returns the delay until
 * the next occurrence of [hour]:00 (today if it's still ahead, otherwise tomorrow).
 */
object ReminderTime {
    const val MS_PER_DAY = 86_400_000L
    private const val MS_PER_HOUR = 3_600_000L

    fun millisUntilHour(nowMillisSinceMidnight: Long, hour: Int): Long {
        val target = hour.coerceIn(0, 23) * MS_PER_HOUR
        val diff = target - nowMillisSinceMidnight
        // At or past the target today → schedule for tomorrow (never fire immediately).
        return if (diff > 0) diff else diff + MS_PER_DAY
    }
}
