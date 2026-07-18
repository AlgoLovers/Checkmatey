package com.checkmatey.core.habit

/** Daily-practice state: the consecutive-day streak, its record, and today's solved count. */
data class DailyState(
    val dayStreak: Int,
    val longestStreak: Int,
    val solvedToday: Int,
    val lastActiveDay: Long, // epoch day of the last solve; 0 = never practised
)

/**
 * The daily habit loop — a streak that only survives if you practise every day, plus a small daily
 * puzzle goal. Habit is what turns a useful tool into a routine (and a routine into a subscriber).
 * Pure and epoch-day based: the UI passes in `today`, so this never touches the clock or Android and
 * stays fully unit-tested.
 */
object DailyGoal {
    /** Puzzles per day that count as "goal met". */
    const val GOAL = 5

    /** Register one solved puzzle on [today], returning the advanced state. */
    fun onSolved(prev: DailyState, today: Long): DailyState {
        val sameDay = today == prev.lastActiveDay
        val streak = when {
            sameDay -> prev.dayStreak.coerceAtLeast(1)             // already practised today
            today == prev.lastActiveDay + 1 -> prev.dayStreak + 1  // next day — extend the streak
            else -> 1                                              // first ever, or a gap — restart
        }
        return DailyState(
            dayStreak = streak,
            longestStreak = maxOf(prev.longestStreak, streak),
            solvedToday = if (sameDay) prev.solvedToday + 1 else 1,
            lastActiveDay = today,
        )
    }

    /**
     * How the stored state should read *today* without new activity: today's count resets on a new
     * day, and the streak lapses to 0 once a full day has been missed. Keeps the raw record intact so
     * the next solve still computes correctly.
     */
    fun viewOn(prev: DailyState, today: Long): DailyState = when {
        today <= prev.lastActiveDay -> prev                            // same day (guards clock skew)
        today == prev.lastActiveDay + 1 -> prev.copy(solvedToday = 0)  // new day, streak still alive
        else -> prev.copy(dayStreak = 0, solvedToday = 0)             // missed a day — streak broken
    }

    fun metToday(state: DailyState): Boolean = state.solvedToday >= GOAL
}
