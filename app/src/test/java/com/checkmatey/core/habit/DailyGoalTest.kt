package com.checkmatey.core.habit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalTest {

    private val zero = DailyState(0, 0, 0, 0)

    @Test
    fun firstSolveStartsStreakAtOne() {
        val s = DailyGoal.onSolved(zero, today = 100)
        assertEquals(1, s.dayStreak)
        assertEquals(1, s.solvedToday)
        assertEquals(1, s.longestStreak)
        assertEquals(100L, s.lastActiveDay)
    }

    @Test
    fun sameDaySolvesRaiseCountButNotStreak() {
        var s = DailyGoal.onSolved(zero, 100)
        s = DailyGoal.onSolved(s, 100)
        s = DailyGoal.onSolved(s, 100)
        assertEquals(1, s.dayStreak)
        assertEquals(3, s.solvedToday)
    }

    @Test
    fun consecutiveDaysExtendStreakAndResetDailyCount() {
        var s = DailyGoal.onSolved(zero, 100)
        s = DailyGoal.onSolved(s, 101)
        assertEquals(2, s.dayStreak)
        assertEquals(1, s.solvedToday) // new day resets the count
        s = DailyGoal.onSolved(s, 102)
        assertEquals(3, s.dayStreak)
        assertEquals(3, s.longestStreak)
    }

    @Test
    fun aGapResetsTheStreakButKeepsTheRecord() {
        var s = DailyGoal.onSolved(zero, 100)
        s = DailyGoal.onSolved(s, 101)
        s = DailyGoal.onSolved(s, 102) // streak 3, longest 3
        s = DailyGoal.onSolved(s, 105) // missed 103, 104
        assertEquals(1, s.dayStreak)
        assertEquals(3, s.longestStreak)
    }

    @Test
    fun viewOnLapsesStreakAfterAMissedDayWithoutLosingTheRecord() {
        val active = DailyState(dayStreak = 4, longestStreak = 4, solvedToday = 5, lastActiveDay = 100)
        assertEquals(active, DailyGoal.viewOn(active, 100))                 // same day: unchanged
        assertEquals(0, DailyGoal.viewOn(active, 101).solvedToday)         // next day: count resets
        assertEquals(4, DailyGoal.viewOn(active, 101).dayStreak)          // streak still alive
        val lapsed = DailyGoal.viewOn(active, 103)                         // missed day 102
        assertEquals(0, lapsed.dayStreak)
        assertEquals(4, lapsed.longestStreak)                             // record preserved
        // The next solve after a lapse restarts at 1.
        assertEquals(1, DailyGoal.onSolved(active, 103).dayStreak)
    }

    @Test
    fun goalMetAtFive() {
        val four = DailyState(1, 1, 4, 100)
        assertFalse(DailyGoal.metToday(four))
        assertTrue(DailyGoal.metToday(four.copy(solvedToday = 5)))
    }
}
