package com.checkmatey.core.engine

import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotatorTest {

    private val annotator = Annotator(KotlinMinimaxEngine(), depth = 3)

    @Test
    fun hintPointsToTheFreeCapture() {
        // Black queen on d4 hangs to the rook on d1.
        val pos = Position.fromFen("4k3/8/8/8/3q4/8/8/3RK3 w - - 0 1")
        val hint = annotator.hint(pos)
        assertNotNull(hint)
        assertEquals(Square.fromName("d4"), hint!!.bestMove?.to)
        assertTrue("reason should mention the capture: ${hint.reason}", hint.reason.contains("잡"))
    }

    @Test
    fun hintAnnouncesMate() {
        val pos = Position.fromFen("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1")
        val hint = annotator.hint(pos)
        assertNotNull(hint)
        assertTrue("reason should mention mate: ${hint!!.reason}", hint.reason.contains("체크메이트"))
    }

    @Test
    fun grabbingTheFreeQueenIsRatedBest() {
        val pos = Position.fromFen("4k3/8/8/8/3q4/8/8/3RK3 w - - 0 1")
        val rxd4 = pos.legalMoves().first { it.from == Square.fromName("d1") && it.to == Square.fromName("d4") }
        val annotation = annotator.annotate(pos, rxd4)
        assertEquals(MoveQuality.BEST, annotation.quality)
    }

    @Test
    fun ignoringTheFreeQueenIsABlunder() {
        val pos = Position.fromFen("4k3/8/8/8/3q4/8/8/3RK3 w - - 0 1")
        val kingMove = pos.legalMoves().first { it.from == Square.fromName("e1") && it.to == Square.fromName("e2") }
        val annotation = annotator.annotate(pos, kingMove)
        assertEquals(MoveQuality.BLUNDER, annotation.quality)
        assertTrue("should suggest the better move: ${annotation.reason}", annotation.reason.contains("Rxd4"))
    }

    @Test
    fun detectsAKnightFork() {
        // Knight from e6 to c7 checks the king on a8 and hits the queen on e8.
        // (White king on g1 so the knight isn't pinned on the e-file.)
        val pos = Position.fromFen("k3q3/8/4N3/8/8/8/8/6K1 w - - 0 1")
        val nc7 = pos.legalMoves().first { it.from == Square.fromName("e6") && it.to == Square.fromName("c7") }
        val annotation = annotator.annotate(pos, nc7)
        assertTrue("reason should mention a fork: ${annotation.reason}", annotation.reason.contains("포크"))
    }
}
