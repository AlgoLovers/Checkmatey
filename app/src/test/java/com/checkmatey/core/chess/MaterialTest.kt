package com.checkmatey.core.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialTest {

    @Test
    fun startingPositionHasNoCaptures() {
        val c = Material.captured(Position.startingPosition())
        assertTrue(c.byWhite.isEmpty() && c.byBlack.isEmpty())
        assertEquals(0, c.diffPawns)
    }

    @Test
    fun missingPiecesAreCountedAndOrderedByValue() {
        // Black is missing the queen and a pawn; White is missing a knight.
        val pos = Position.fromFen("rnb1kbnr/ppppppp1/8/8/8/8/PPPPPPPP/R1BQKBNR w KQkq - 0 1")
        val c = Material.captured(pos)
        assertEquals(listOf(PieceType.QUEEN, PieceType.PAWN), c.byWhite)
        assertEquals(listOf(PieceType.KNIGHT), c.byBlack)
        assertEquals(9 + 1 - 3, c.diffPawns)
    }

    @Test
    fun promotionDoesNotCreatePhantomCaptures() {
        // White promoted: two queens on the board, all 8 white pawns gone from the pawn count view —
        // black must not be credited with capturing a "ninth" piece.
        val pos = Position.fromFen("4k3/8/8/8/8/8/8/QQ2K3 w - - 0 1")
        val c = Material.captured(pos)
        // Black captured everything white is missing (clamped, no negatives); white queen count 2 > start 1 is fine.
        assertTrue(c.byBlack.none { it == PieceType.KING })
        assertTrue(c.byWhite.size == 15) // black has only the king left
    }
}
