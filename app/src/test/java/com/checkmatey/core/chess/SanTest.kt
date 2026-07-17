package com.checkmatey.core.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SanTest {

    @Test
    fun rendersBasicMovesFromStart() {
        val p = Position.startingPosition()
        val e4 = p.legalMoves().first { it.from == Square.fromName("e2") && it.to == Square.fromName("e4") }
        assertEquals("e4", p.toSan(e4))
        val nf3 = p.legalMoves().first { it.from == Square.fromName("g1") && it.to == Square.fromName("f3") }
        assertEquals("Nf3", p.toSan(nf3))
    }

    @Test
    fun parseInvertsToSanForEveryLegalMove() {
        // If toSan is unique per move (correct disambiguation, captures, checks), parseSan recovers it.
        val fens = listOf(
            Position.STARTING_FEN,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", // Kiwipete
            "8/P7/8/8/8/8/8/4k2K w - - 0 1", // promotion
        )
        for (fen in fens) {
            val p = Position.fromFen(fen)
            for (m in p.legalMoves()) {
                assertEquals("SAN round-trip failed for ${m.uci()} (${p.toSan(m)})", m, p.parseSan(p.toSan(m)))
            }
        }
    }

    @Test
    fun handlesCastlingAndPromotion() {
        val castle = Position.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertTrue(castle.parseSan("O-O")!!.isCastleKingSide)
        assertTrue(castle.parseSan("O-O-O")!!.isCastleQueenSide)

        val promo = Position.fromFen("8/P7/8/8/8/8/8/4k2K w - - 0 1")
        assertNotNull(promo.parseSan("a8=Q"))
        assertEquals(PieceType.QUEEN, promo.parseSan("a8=Q")!!.promotion)
    }

    @Test
    fun rendersCheckAndMateSuffixes() {
        // Back-rank mate: Ra8#.
        val p = Position.fromFen("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1")
        val mate = p.legalMoves().first { it.from == Square.fromName("a1") && it.to == Square.fromName("a8") }
        assertEquals("Ra8#", p.toSan(mate))
    }
}
