package com.checkmatey.core.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Fast, deterministic gate tests for the pure-Kotlin chess model. */
class BoardTest {

    private val start = Board.startingPosition()

    @Test
    fun startingPositionHasThirtyTwoPieces() {
        assertEquals(32, start.pieceCount)
    }

    @Test
    fun whiteKingStartsOnE1() {
        assertEquals(Piece(PieceColor.WHITE, PieceType.KING), start.pieceAt(Square.fromName("e1")))
    }

    @Test
    fun blackQueenStartsOnD8() {
        assertEquals(Piece(PieceColor.BLACK, PieceType.QUEEN), start.pieceAt(Square.fromName("d8")))
    }

    @Test
    fun centerSquaresStartEmpty() {
        assertNull(start.pieceAt(Square.fromName("e4")))
        assertNull(start.pieceAt(Square.fromName("d5")))
    }

    @Test
    fun squareNameRoundTrips() {
        assertEquals("a1", Square(0, 0).name)
        assertEquals("h8", Square(7, 7).name)
        assertEquals(Square(4, 3), Square.fromName("e4"))
    }

    @Test
    fun fenCharsAreCasedByColor() {
        assertEquals('N', Piece(PieceColor.WHITE, PieceType.KNIGHT).fenChar)
        assertEquals('q', Piece(PieceColor.BLACK, PieceType.QUEEN).fenChar)
    }
}
