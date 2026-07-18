package com.checkmatey.core.engine

import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorTest {

    @Test
    fun warnsAboutAHangingPiece() {
        // White to move; White's knight on d4 is attacked by the rook on d8 and undefended.
        val pos = Position.fromFen("3r2k1/8/8/8/3N4/8/8/6K1 w - - 0 1")
        val threats = Tutor.threats(pos, PieceColor.WHITE)
        assertTrue("should warn about the knight", threats.any { it.square == Square.fromName("d4") })
    }

    @Test
    fun staysQuietWhenNothingIsThreatened() {
        val pos = Position.startingPosition()
        assertTrue(Tutor.threats(pos, PieceColor.WHITE).isEmpty())
    }

    @Test
    fun defendedPieceAttackedByBiggerPieceIsNotAThreat() {
        // White knight d4 defended by the c3 pawn, attacked only by the enemy ROOK (worth more):
        // taking would lose the exchange, so the tutor shouldn't cry wolf.
        val pos = Position.fromFen("3r2k1/8/8/8/3N4/2P5/8/6K1 w - - 0 1")
        val threats = Tutor.threats(pos, PieceColor.WHITE)
        assertTrue(threats.none { it.square == Square.fromName("d4") })
    }

    @Test
    fun warnsAboutAMateInOneThreat() {
        // If it were Black's move, Qh4 would deliver the Fool's-mate pattern: White must be warned.
        val pos = Position.fromFen("rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR w KQkq - 0 3")
        val threats = Tutor.threats(pos, PieceColor.WHITE)
        assertTrue("mate threat must be the top warning", threats.isNotEmpty() && "체크메이트" in threats.first().text)
    }

    @Test
    fun hintLadderDisclosesInThreeSteps() {
        // Back-rank mate in one: hint reason mentions checkmate.
        val pos = Position.fromFen("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1")
        val annotator = Annotator(KotlinMinimaxEngine(), depth = 2)
        val hint = annotator.hint(pos)
        assertNotNull(hint)
        val ladder = Tutor.hintLadder(pos, hint!!)
        assertEquals(3, ladder.size)
        assertTrue("stage 1 is a question, not the answer", "?" in ladder[0] && hint.san !in ladder[0])
        assertTrue("stage 2 names the piece square", hint.move.from.name in ladder[1])
        assertTrue("stage 3 is the full answer", hint.san in ladder[2])
    }

    @Test
    fun recallLineOnlyFiresOnARealPattern() {
        assertNull("too few attempts", Tutor.recallLine("포크", attempts = 2, successRate = 0))
        assertNull("doing fine", Tutor.recallLine("포크", attempts = 5, successRate = 80))
        assertNotNull("repeated weakness", Tutor.recallLine("포크", attempts = 4, successRate = 25))
    }

    @Test
    fun themeOfMapsReasonsToTrainableThemes() {
        assertEquals("포크", Tutor.themeOf("한 수로 두 기물 이상을 동시에 노립니다 (포크)"))
        assertEquals("핀", Tutor.themeOf("상대 기물을 킹에 묶습니다 (핀) — 그 기물은 움직일 수 없습니다"))
        assertNull(Tutor.themeOf("안정적인 전개 수입니다"))
    }
}
