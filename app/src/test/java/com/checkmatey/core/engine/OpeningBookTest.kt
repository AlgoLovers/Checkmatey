package com.checkmatey.core.engine

import com.checkmatey.core.chess.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OpeningBookTest {

    @Test
    fun everyBookLineIsFullyLegalFromTheStart() {
        for ((name, line) in OpeningBook.allLines()) {
            var pos = Position.startingPosition()
            for (uci in line.split(" ")) {
                val move = pos.findMove(uci)
                assertNotNull("$name: '$uci' is illegal at ${pos.toFen()}", move)
                pos = pos.applyMove(move!!)
            }
        }
        assertTrue("book should carry a real repertoire", OpeningBook.lineCount >= 12)
    }

    @Test
    fun startingPositionOffersVariedFirstMoves() {
        val first = OpeningBook.moves(Position.startingPosition()).map { it.uci }.toSet()
        assertTrue("first-move variety expected (e4/d4/c4)", first.containsAll(setOf("e2e4", "d2d4", "c2c4")))
    }

    @Test
    fun botPlaysABookMoveOutOfTheOpening() {
        val engine = KotlinMinimaxEngine()
        val start = Position.startingPosition()
        val bookUcis = OpeningBook.moves(start).map { it.uci }.toSet()
        for (level in listOf(BotLevel.SEEDLING, BotLevel.MASTER)) {
            val move = engine.chooseMove(start, level, Random(3))
            assertTrue("$level should open from the book, played ${move?.uci()}", move!!.uci() in bookUcis)
        }
    }

    @Test
    fun coachNamesABookMoveAndNeverGradesItBelowGood() {
        val annotator = Annotator(KotlinMinimaxEngine(), depth = 2)
        // 1.e4 c5 — the Sicilian: a fine move the engine itself might not prefer.
        val afterE4 = Position.startingPosition().applyMove(Position.startingPosition().findMove("e2e4")!!)
        val sicilian = afterE4.findMove("c7c5")!!
        val a = annotator.annotate(afterE4, sicilian)
        assertTrue("book move must carry its opening name: ${a.reason}", "정석" in a.reason && "시실리안" in a.reason)
        assertTrue("book move never grades below GOOD, got ${a.quality}", a.quality.ordinal <= MoveQuality.GOOD.ordinal)
    }

    @Test
    fun openingOfReturnsNullOffBook() {
        val start = Position.startingPosition()
        val offBook = start.findMove("a2a3")!!
        assertEquals(null, OpeningBook.openingOf(start, offBook))
    }
}
