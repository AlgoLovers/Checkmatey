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

    @Test
    fun callsOutAFreeCapture() {
        // The rook grabs an undefended queen — the coach should say it's free.
        val pos = Position.fromFen("4k3/8/8/8/3q4/8/8/3RK3 w - - 0 1")
        val rxd4 = pos.legalMoves().first { it.from == Square.fromName("d1") && it.to == Square.fromName("d4") }
        val annotation = annotator.annotate(pos, rxd4)
        assertTrue("reason should say the capture is free: ${annotation.reason}", annotation.reason.contains("공짜"))
    }

    @Test
    fun explainsAPin() {
        // Bishop f1xb5 grabs a free pawn AND pins the c6 knight to the king on e8 (b5-c6-d7-e8).
        val pos = Position.fromFen("4k3/8/2n5/1p6/8/8/8/4KB2 w - - 0 1")
        val bxb5 = pos.legalMoves().first { it.from == Square.fromName("f1") && it.to == Square.fromName("b5") }
        val annotation = annotator.annotate(pos, bxb5)
        assertEquals(MoveQuality.BEST, annotation.quality)
        assertTrue("reason should mention a pin: ${annotation.reason}", annotation.reason.contains("핀"))
    }

    @Test
    fun explainsADiscoveredCheck() {
        // Knight d4xf5 wins the queen and the rook on d1 uncovers a check on the king at d8
        // (the knight itself doesn't attack d8, so it's a *discovered* check).
        val pos = Position.fromFen("3k4/8/8/5q2/3N4/8/8/3RK3 w - - 0 1")
        val nxf5 = pos.legalMoves().first { it.from == Square.fromName("d4") && it.to == Square.fromName("f5") }
        val annotation = annotator.annotate(pos, nxf5)
        assertTrue("reason should mention a discovered check: ${annotation.reason}", annotation.reason.contains("디스커버드"))
    }

    @Test
    fun theHintedMoveIsNeverGradedAnythingButBest() {
        // Invariant behind the "you told me to play this, then called it bad" bug: playing the coach's
        // own recommended move must always grade BEST. (The live bug was a concurrency race sharing one
        // engine between the bot and the coach; this locks the single-threaded logic so hint/annotate
        // can never drift apart in code.)
        val fens = listOf(
            "4k3/8/8/8/3q4/8/8/3RK3 w - - 0 1",                              // free queen
            "6k1/5ppp/8/8/8/8/8/R6K w - - 0 1",                              // back-rank mate
            "4k3/8/2n5/1p6/8/8/8/4KB2 w - - 0 1",                            // pin + free pawn
            "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4", // quiet opening
        )
        for (fen in fens) {
            val pos = Position.fromFen(fen)
            val hint = annotator.hint(pos)
            assertNotNull("hint should exist for $fen", hint)
            val graded = annotator.annotate(pos, hint!!.bestMove!!)
            assertEquals("the hinted move must grade BEST for $fen", MoveQuality.BEST, graded.quality)
        }
    }
}
