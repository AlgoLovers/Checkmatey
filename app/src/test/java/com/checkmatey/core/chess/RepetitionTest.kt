package com.checkmatey.core.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks threefold repetition as pure logic (it used to be computed inline in the Play screen).
 * Shuffling the knights out and back returns to the starting position, so we can count occurrences
 * exactly — a draw only on the third.
 */
class RepetitionTest {

    private val cycle = listOf("g1f3", "g8f6", "f3g1", "f6g8") // both sides out and back to start

    @Test
    fun thirdOccurrenceOfAPositionIsAThreefoldDraw() {
        val history = mutableListOf(Position.startingPosition())
        fun play(uci: String) {
            val pos = history.last()
            history.add(pos.applyMove(pos.findMove(uci)!!))
        }

        cycle.forEach(::play) // starting position now seen twice
        assertEquals(2, Repetition.count(history, history.last()))
        assertFalse("twice is not yet a draw", Repetition.isThreefold(history, history.last()))

        cycle.forEach(::play) // and now a third time
        assertEquals(3, Repetition.count(history, history.last()))
        assertTrue("third occurrence is a draw", Repetition.isThreefold(history, history.last()))
    }

    @Test
    fun distinctPositionsDoNotCountAsRepetition() {
        val start = Position.startingPosition()
        val afterE4 = start.applyMove(start.findMove("e2e4")!!)
        assertEquals(1, Repetition.count(listOf(start, afterE4), afterE4))
        // The move counters differ but the repetition key ignores them: same board = same key.
        assertEquals(Repetition.key(start), Repetition.key(Position.startingPosition()))
    }
}
