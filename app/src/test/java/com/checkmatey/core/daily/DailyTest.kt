package com.checkmatey.core.daily

import com.checkmatey.core.puzzle.Puzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTest {

    private val puzzles = (0 until 200).map { Puzzle("p$it", "8/8/8/8/8/8/8/K1k5 w - - 0 1", listOf("a1a2"), "t", 800 + it) }

    @Test
    fun dailyPuzzleIsStableWithinADayAndInRange() {
        val a = DailyPuzzle.forDay(20_000, puzzles)
        val b = DailyPuzzle.forDay(20_000, puzzles)
        assertEquals("same day = same puzzle", a, b)
        assertTrue("comes from the set", a in puzzles)
    }

    @Test
    fun consecutiveDaysDoNotJustWalkInOrder() {
        // The hash should scatter days, not serve p0, p1, p2… — check a run yields several distinct ids.
        val ids = (20_000L..20_009L).map { DailyPuzzle.forDay(it, puzzles).id }.toSet()
        assertTrue("expected variety across days, got $ids", ids.size >= 8)
        assertNotEquals(DailyPuzzle.forDay(20_000, puzzles), DailyPuzzle.forDay(20_001, puzzles))
    }

    @Test
    fun reminderDelayIsTodayWhenAheadTomorrowWhenPast() {
        val tenAm = 10 * 3_600_000L
        assertEquals(9 * 3_600_000L, ReminderTime.millisUntilHour(tenAm, 19)) // 10:00 -> 19:00 today = 9h
        val eightPm = 20 * 3_600_000L
        assertEquals(23 * 3_600_000L, ReminderTime.millisUntilHour(eightPm, 19)) // 20:00 -> 19:00 next day = 23h
        // Exactly on the hour rolls to tomorrow (no immediate fire).
        assertEquals(ReminderTime.MS_PER_DAY, ReminderTime.millisUntilHour(19 * 3_600_000L, 19))
    }
}
