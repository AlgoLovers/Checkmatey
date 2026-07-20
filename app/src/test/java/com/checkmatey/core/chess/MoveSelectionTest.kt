package com.checkmatey.core.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the tap-to-move FSM that seven board screens now share. Every transition the screens used to
 * re-implement by hand is pinned here, so a change can't quietly break selection on one screen.
 */
class MoveSelectionTest {

    private val start = Position.startingPosition()
    private fun sq(name: String) = Square.fromName(name)

    @Test
    fun firstTapSelectsOwnPieceAndIgnoresEmptyOrEnemy() {
        assertEquals(TapResult.Select(sq("e2")), MoveSelection.onTap(start, null, sq("e2"), PieceColor.WHITE))
        assertEquals(TapResult.Ignore, MoveSelection.onTap(start, null, sq("e4"), PieceColor.WHITE)) // empty
        assertEquals(TapResult.Ignore, MoveSelection.onTap(start, null, sq("e7"), PieceColor.WHITE)) // enemy
    }

    @Test
    fun tappingTheSelectedSquareClears() {
        assertEquals(TapResult.Clear, MoveSelection.onTap(start, sq("e2"), sq("e2"), PieceColor.WHITE))
    }

    @Test
    fun tappingALegalDestinationYieldsThatMove() {
        val r = MoveSelection.onTap(start, sq("e2"), sq("e4"), PieceColor.WHITE)
        assertTrue(r is TapResult.Moves)
        val moves = (r as TapResult.Moves).candidates
        assertEquals(1, moves.size)
        assertEquals("e2e4", moves.first().uci())
    }

    @Test
    fun tappingAnotherOwnPieceReselectsElseClears() {
        // e2 selected, tap d2 (own pawn, no legal e2->d2) -> reselect d2.
        assertEquals(TapResult.Select(sq("d2")), MoveSelection.onTap(start, sq("e2"), sq("d2"), PieceColor.WHITE))
        // e2 selected, tap e5 (empty, not reachable) -> clear.
        assertEquals(TapResult.Clear, MoveSelection.onTap(start, sq("e2"), sq("e5"), PieceColor.WHITE))
    }

    @Test
    fun aPromotionOffersAllFourPieces() {
        val pos = Position.fromFen("8/P7/8/8/8/8/8/k1K5 w - - 0 1")
        val r = MoveSelection.onTap(pos, sq("a7"), sq("a8"), PieceColor.WHITE)
        assertTrue(r is TapResult.Moves)
        val moves = (r as TapResult.Moves).candidates
        assertEquals(4, moves.size)
        assertTrue("all four are promotions", moves.all { it.promotion != null })
    }
}
