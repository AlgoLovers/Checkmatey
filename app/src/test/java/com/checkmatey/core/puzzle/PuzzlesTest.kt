package com.checkmatey.core.puzzle

import com.checkmatey.core.engine.KotlinMinimaxEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class PuzzlesTest {

    private val engine = KotlinMinimaxEngine()

    @Test
    fun everyPuzzleHasADecisiveBestMove() {
        for (p in Puzzles.ALL) {
            val pos = p.position
            val best = engine.bestMove(pos, 3)
            assertNotNull("${p.id}: no legal move", best)
            if (pos.applyMove(best!!).isCheckmate()) continue // mate tactics are decisive
            val values = pos.legalMoves().map { engine.evaluateMove(pos, it, 3) }.sortedDescending()
            val gap = values[0] - (values.getOrNull(1) ?: values[0])
            assertTrue("${p.id}: best move not decisive (gap=$gap)", gap >= 150)
        }
    }

    @Test
    fun idsAreUniqueAndRatingsSane() {
        assertEquals(Puzzles.ALL.size, Puzzles.ALL.map { it.id }.toSet().size)
        assertTrue(Puzzles.ALL.all { it.rating in 400..2000 })
    }

    @Test
    fun nextPicksSomethingNearRating() {
        val p = Puzzles.next(650, emptySet(), Random(1))
        assertTrue(abs(p.rating - 650) <= 350)
    }
}
