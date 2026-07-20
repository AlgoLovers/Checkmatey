package com.checkmatey.core.puzzle

import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Validates the bundled Lichess CC0 puzzle asset directly (JVM test — no Android Context), so bad
 * data can never ship. Crucially, it replays each sampled solution line through OUR engine's move
 * generator: a puzzle only passes if every UCI move is legal here and the sides alternate correctly.
 * That cross-checks the Python build pipeline against the Kotlin rules (castling, en passant,
 * promotion, checks) — if either disagrees, the test fails.
 */
class PuzzlesTest {

    private val puzzles: List<Puzzle> = loadAsset()

    @Test
    fun assetIsLargeWithUniqueIdsAndSaneRatings() {
        assertTrue("expected a big pool, got ${puzzles.size}", puzzles.size >= 10_000)
        assertEquals("ids must be unique", puzzles.size, puzzles.map { it.id }.toSet().size)
        assertTrue("ratings must sit in the beginner band", puzzles.all { it.rating in 400..1500 })
        assertTrue("every puzzle needs a solution", puzzles.all { it.solution.isNotEmpty() })
        // Solution lines end on a solver move, so they're odd length.
        assertTrue("lines should end on a solver move", puzzles.all { it.solution.size % 2 == 1 })
    }

    @Test
    fun everySampledSolutionLineIsFullyLegal() {
        val rng = Random(20260718)
        val sample = puzzles.shuffled(rng).take(500)
        for (p in sample) {
            val start = Position.fromFen(p.fen)
            val solver = start.sideToMove
            var pos = start
            p.solution.forEachIndexed { i, uci ->
                val expectSolver = i % 2 == 0
                assertEquals("${p.id}: wrong side to move at ply $i", solver == pos.sideToMove, expectSolver)
                val move = pos.findMove(uci)
                assertNotNull("${p.id}: move $i '$uci' is illegal in ${p.fen}", move)
                pos = pos.applyMove(move!!)
            }
        }
    }

    @Test
    fun isSolverMoveMatchesOriginAndDestinationIgnoringPromotionChoice() {
        // Pure grading used by the puzzle + placement screens — locked here instead of in the UI.
        val p = Puzzle("x", "8/P7/8/8/8/8/8/k1K5 w - - 0 1", listOf("a7a8q"), "승격", 500)
        val pos = p.position
        val queening = pos.findMove("a7a8q")!!
        assertTrue("the solution move grades correct", p.isSolverMove(0, queening))
        val kingMove = pos.legalMoves().first { it.from == Square.fromName("c1") }
        assertFalse("a different move is not the solver move", p.isSolverMove(0, kingMove))
        assertFalse("an out-of-range step is never correct", p.isSolverMove(9, queening))
    }

    @Test
    fun themesAreLabelledInKorean() {
        val themes = puzzles.map { it.theme }.toSet()
        assertTrue("themes should be Korean labels", themes.any { it.contains("메이트") || it.contains("포크") })
        assertTrue("theme labels must be non-empty", themes.all { it.isNotBlank() })
    }

    private fun loadAsset(): List<Puzzle> {
        val file = listOf(
            File("src/main/assets/puzzles.csv"),
            File("app/src/main/assets/puzzles.csv"),
        ).firstOrNull { it.exists() } ?: error("puzzles.csv not found (run tools/puzzles/build_puzzles.py)")
        // Parse with the SAME parser the app ships, so the test can't drift from production.
        return file.useLines { lines -> lines.drop(1).mapNotNull { Puzzle.parse(it) }.toList() }
    }
}
