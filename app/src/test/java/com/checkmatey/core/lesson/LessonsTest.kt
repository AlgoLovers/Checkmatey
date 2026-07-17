package com.checkmatey.core.lesson

import com.checkmatey.core.chess.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonsTest {

    @Test
    fun idsAreUniqueAndLessonsNonEmpty() {
        assertEquals(Lessons.ALL.size, Lessons.ALL.map { it.id }.toSet().size)
        assertTrue(Lessons.ALL.all { it.steps.isNotEmpty() })
    }

    @Test
    fun everyAcceptedMoveIsLegalInItsPosition() {
        for (lesson in Lessons.ALL) {
            for ((i, step) in lesson.steps.withIndex()) {
                val pos = Position.fromFen(step.fen) // throws on a bad FEN
                val legal = pos.legalMoves().map { it.uci() }.toSet()
                for (uci in step.acceptUci) {
                    assertTrue("${lesson.id} step$i: $uci not legal (legal=$legal)", uci in legal)
                }
            }
        }
    }

    @Test
    fun mateStepsReallyMate() {
        for (lesson in Lessons.ALL) {
            for ((i, step) in lesson.steps.withIndex()) {
                if (!step.expectMate) continue
                val pos = Position.fromFen(step.fen)
                for (uci in step.acceptUci) {
                    val move = pos.legalMoves().first { it.uci() == uci }
                    assertTrue("${lesson.id} step$i: $uci should be mate", pos.applyMove(move).isCheckmate())
                }
            }
        }
    }
}
