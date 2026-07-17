package com.checkmatey.core.engine

import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EngineTest {

    private val engine = KotlinMinimaxEngine()

    @Test
    fun findsMateInOne() {
        // Rook to a8 is checkmate; the king is boxed in by its own f7/g7/h7 pawns.
        val pos = Position.fromFen("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1")
        val move = engine.bestMove(pos, depth = 2)
        assertNotNull(move)
        assertTrue("engine should find the mate", pos.applyMove(move!!).isCheckmate())
    }

    @Test
    fun capturesFreeMaterial() {
        // Black queen on d4 is hanging to the rook on d1.
        val pos = Position.fromFen("4k3/8/8/8/3q4/8/8/3RK3 w - - 0 1")
        val move = engine.bestMove(pos, depth = 2)
        assertNotNull(move)
        assertEquals("should grab the free queen", Square.fromName("d4"), move!!.to)
    }

    @Test
    fun returnsNullWhenGameOver() {
        // Fool's mate: White is checkmated, no legal move.
        val pos = Position.fromFen("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3")
        assertNull(engine.bestMove(pos, depth = 3))
        assertNull(engine.chooseMove(pos, BotLevel.CHALLENGER, Random(1)))
    }

    @Test
    fun startingPositionEvaluatesToZero() {
        assertEquals(0, engine.evaluate(Position.startingPosition()))
    }

    @Test
    fun evaluationReflectsMaterialAdvantage() {
        // White has an extra queen.
        val pos = Position.fromFen("4k3/8/8/8/8/8/8/3QK3 w - - 0 1")
        assertTrue("a queen up should score clearly positive", engine.evaluate(pos) > 500)
    }

    @Test
    fun chooseMoveReturnsALegalMoveForEveryLevel() {
        // Sparse endgame so even the deepest level searches quickly.
        val pos = Position.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1")
        val legal = pos.legalMoves().toSet()
        for (level in BotLevel.entries) {
            val move = engine.chooseMove(pos, level, Random(7))
            assertNotNull("level $level returned null", move)
            assertTrue("level $level returned an illegal move", move in legal)
        }
    }

    @Test
    fun adaptiveLevelTracksRating() {
        assertEquals(BotLevel.SEEDLING, BotLevel.forRating(400))
        assertEquals(BotLevel.BEGINNER, BotLevel.forRating(700))
        assertEquals(BotLevel.INTERMEDIATE, BotLevel.forRating(1000))
        assertEquals(BotLevel.CHALLENGER, BotLevel.forRating(1300))
    }

    @Test
    fun playsAShortGameWithoutError() {
        var pos = Position.startingPosition()
        val random = Random(42)
        var plies = 0
        while (!pos.isGameOver() && plies < 12) {
            val move = engine.chooseMove(pos, BotLevel.BEGINNER, random) ?: break
            assertTrue(move in pos.legalMoves())
            pos = pos.applyMove(move)
            plies++
        }
        assertTrue("game should have progressed", plies > 0)
    }
}
