package com.checkmatey.core.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the SM-2 scheduling math down as deterministic pure logic (rule #2: chess/learning rules
 * are code + tests, never recomputed per run). Every interval and due date here is hand-derived
 * from the SuperMemo 2 formula, so a regression in the moat's core is caught before it ships.
 */
class SrsTest {

    private val today = 20_000L // arbitrary epoch day

    @Test
    fun newCardIsDueTodayAndUnstudied() {
        val c = Srs.new("p1", today)
        assertEquals(today, c.dueDay)
        assertEquals(0, c.reps)
        assertEquals(0, c.intervalDays)
        assertEquals(2.5, c.ease, 1e-9)
    }

    @Test
    fun successProgressionFollowsOneThenSixThenEaseMultiple() {
        var c = Srs.new("p", today)
        c = Srs.review(c, Grade.GOOD, today)
        assertEquals(1, c.intervalDays) // first success: 1 day
        assertEquals(today + 1, c.dueDay)
        assertEquals(1, c.reps)

        c = Srs.review(c, Grade.GOOD, today + 1)
        assertEquals(6, c.intervalDays) // second success: 6 days
        assertEquals(today + 1 + 6, c.dueDay)

        val easeBefore = c.ease
        c = Srs.review(c, Grade.GOOD, today + 7)
        assertEquals((6 * easeBefore).toInt(), c.intervalDays) // then interval * ease
        assertEquals(3, c.reps)
    }

    @Test
    fun missResetsRepsAndBringsCardBackTomorrow() {
        var c = Srs.new("p", today)
        repeat(3) { c = Srs.review(c, Grade.GOOD, c.dueDay) } // graduate it out to a long interval
        assertTrue(c.intervalDays > 6)
        val afterMiss = Srs.review(c, Grade.AGAIN, c.dueDay)
        assertEquals(0, afterMiss.reps)
        assertEquals(1, afterMiss.intervalDays)
        assertEquals(c.dueDay + 1, afterMiss.dueDay)
    }

    @Test
    fun easeRisesOnEasyFallsOnMissAndIsFloored() {
        val start = Srs.new("p", today)
        assertTrue("EASY raises ease", Srs.review(start, Grade.EASY, today).ease > start.ease)
        assertTrue("miss lowers ease", Srs.review(start, Grade.AGAIN, today).ease < start.ease)

        // Repeated misses must never drop ease below the SM-2 floor.
        var c = start
        repeat(20) { c = Srs.review(c, Grade.AGAIN, c.dueDay) }
        assertEquals(Srs.MIN_EASE, c.ease, 1e-9)
    }

    @Test
    fun dueFiltersByDateAndSortsMostOverdueFirst() {
        val cards = listOf(
            SrsCard("late", 2.5, 3, 2, today - 5),
            SrsCard("today", 2.5, 1, 1, today),
            SrsCard("future", 2.5, 6, 2, today + 4),
            SrsCard("yesterday", 2.5, 1, 1, today - 1),
        )
        val due = Srs.due(cards, today)
        assertEquals(listOf("late", "yesterday", "today"), due.map { it.id })
        assertEquals(3, Srs.dueCount(cards, today))
    }

    @Test
    fun encodeDecodeRoundTrips() {
        val c = SrsCard("00sHx", 2.375, 12, 4, 20_123L)
        val back = SrsCard.decode(c.encode())
        assertEquals(c, back)
    }

    @Test
    fun decodeRejectsMalformedRows() {
        assertNull(SrsCard.decode(""))
        assertNull(SrsCard.decode("id:2500:3")) // too few fields
        assertNull(SrsCard.decode(":2500:3:2:100")) // blank id
        assertNull(SrsCard.decode("id:x:3:2:100")) // non-numeric ease
    }
}
