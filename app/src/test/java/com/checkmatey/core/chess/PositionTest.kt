package com.checkmatey.core.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rules-engine gate tests. The backbone is `perft` (counting the leaf nodes of the move
 * tree to a fixed depth) against published reference numbers — the standard way to prove a
 * move generator handles captures, castling, en passant, promotion, checks and pins correctly.
 */
class PositionTest {

    /** Counts legal move-tree leaves to [depth]. */
    private fun perft(pos: Position, depth: Int): Long {
        if (depth == 0) return 1L
        var nodes = 0L
        for (m in pos.legalMoves()) {
            nodes += perft(pos.applyMove(m), depth - 1)
        }
        return nodes
    }

    // ---- perft reference positions ---------------------------------------------------

    @Test
    fun startingPositionHasTwentyLegalMoves() {
        assertEquals(20, Position.startingPosition().legalMoves().size)
    }

    @Test
    fun perftStartingPosition() {
        val start = Position.startingPosition()
        assertEquals(20L, perft(start, 1))
        assertEquals(400L, perft(start, 2))
        assertEquals(8902L, perft(start, 3))
    }

    @Test
    fun perftKiwipete() {
        // "Kiwipete": rich in castling, en passant, promotions and pins.
        val pos = Position.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        assertEquals(48L, perft(pos, 1))
        assertEquals(2039L, perft(pos, 2))
    }

    // ---- FEN round trips -------------------------------------------------------------

    @Test
    fun fenRoundTrips() {
        assertEquals(Position.STARTING_FEN, Position.startingPosition().toFen())
        val kiwipete = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
        assertEquals(kiwipete, Position.fromFen(kiwipete).toFen())
    }

    @Test
    fun doublePawnPushSetsEnPassantTargetAndFlipsSide() {
        val start = Position.startingPosition()
        val e2e4 = start.legalMoves().first { it.from == Square.fromName("e2") && it.to == Square.fromName("e4") }
        val after = start.applyMove(e2e4)
        assertEquals(Square.fromName("e3"), after.enPassantTarget)
        assertEquals(PieceColor.BLACK, after.sideToMove)
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", after.toFen())
    }

    // ---- Special moves ---------------------------------------------------------------

    @Test
    fun enPassantCaptureIsGenerated() {
        // White pawn e5, black just played ...d5 -> en passant target d6.
        val pos = Position.fromFen("rnbqkbnr/pp2pppp/8/2ppP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3")
        val ep = pos.legalMoves().firstOrNull { it.isEnPassant }
        assertTrue("expected an en passant capture", ep != null)
        assertEquals(Square.fromName("d6"), ep!!.to)
        // Applying it removes the black d5 pawn.
        val after = pos.applyMove(ep)
        assertEquals(null, after.pieceAt(Square.fromName("d5")))
        assertEquals(Piece(PieceColor.WHITE, PieceType.PAWN), after.pieceAt(Square.fromName("d6")))
    }

    @Test
    fun promotionGeneratesFourMoves() {
        val pos = Position.fromFen("8/P7/8/8/8/8/8/4k2K w - - 0 1")
        val promos = pos.legalMoves().filter { it.from == Square.fromName("a7") }
        assertEquals(4, promos.size)
        assertEquals(
            setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT),
            promos.mapNotNull { it.promotion }.toSet(),
        )
        assertTrue(promos.all { it.to == Square.fromName("a8") })
    }

    @Test
    fun bothCastlesAreGeneratedWhenLegal() {
        val pos = Position.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val moves = pos.legalMoves()
        assertTrue("kingside castle missing", moves.any { it.isCastleKingSide })
        assertTrue("queenside castle missing", moves.any { it.isCastleQueenSide })
    }

    @Test
    fun cannotCastleThroughAnAttackedSquare() {
        // Black rook on f8 attacks f1, so the king cannot pass f1 (no kingside), queenside is fine.
        val pos = Position.fromFen("5r2/4k3/8/8/8/8/8/R3K2R w KQ - 0 1")
        val moves = pos.legalMoves()
        assertFalse("kingside castle should be illegal", moves.any { it.isCastleKingSide })
        assertTrue("queenside castle should be legal", moves.any { it.isCastleQueenSide })
    }

    // ---- Game end --------------------------------------------------------------------

    @Test
    fun detectsCheckmate() {
        // Fool's mate: white is mated after 1.f3 e5 2.g4 Qh4#.
        val pos = Position.fromFen("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3")
        assertTrue(pos.isInCheck())
        assertTrue(pos.isCheckmate())
        assertTrue(pos.legalMoves().isEmpty())
    }

    @Test
    fun detectsStalemate() {
        // Black to move, not in check, but has no legal move.
        val pos = Position.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
        assertFalse(pos.isInCheck())
        assertTrue(pos.isStalemate())
        assertTrue(pos.legalMoves().isEmpty())
    }
}
