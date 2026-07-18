package com.checkmatey.core.engine

import com.checkmatey.core.chess.Position
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Measures engine quality against ground truth: a fixed, seeded sample of Lichess puzzles (whose
 * solutions are independently verified) is solved with [Engine.bestMove], and the solve rate is
 * printed per rating band. This is the number the engine work is judged by — any search/eval change
 * must move it up (or hold it) to land. The assertion floor only guards against catastrophic
 * regressions; read the printed table for the real signal.
 */
class EngineStrengthBench {

    @Test
    fun tacticalSolveRateByRatingBand() {
        val puzzles = loadPuzzles()
        val rng = Random(20260718) // fixed seed — same sample every run
        val sample = puzzles.shuffled(rng).take(SAMPLE)

        val engine = KotlinMinimaxEngine()
        var solved = 0
        val byBand = sortedMapOf<Int, IntArray>() // band -> [solved, total]
        val t0 = System.nanoTime()
        for (p in sample) {
            val pos = Position.fromFen(p.fen)
            val best = engine.bestMove(pos, depth = DEPTH)
            val ok = best?.uci() == p.solution
            if (ok) solved++
            val cell = byBand.getOrPut((p.rating / 300) * 300) { IntArray(2) }
            if (ok) cell[0]++
            cell[1]++
        }
        val ms = (System.nanoTime() - t0) / 1_000_000
        val rate = solved * 100 / sample.size

        println("=== Engine tactical bench (depth=$DEPTH, n=${sample.size}, ${ms}ms, ${ms / sample.size}ms/puzzle) ===")
        for ((band, cell) in byBand) {
            println("  $band-${band + 299}: ${cell[0]}/${cell[1]}  (${cell[0] * 100 / maxOf(1, cell[1])}%)")
        }
        println("  TOTAL: $solved/${sample.size}  ($rate%)")

        assertTrue("catastrophic regression: solve rate $rate% < $FLOOR%", rate >= FLOOR)
    }

    private data class Bench(val fen: String, val solution: String, val rating: Int)

    private fun loadPuzzles(): List<Bench> {
        val file = listOf(
            File("src/main/assets/puzzles.csv"),
            File("app/src/main/assets/puzzles.csv"),
        ).firstOrNull { it.exists() } ?: error("puzzles.csv not found")
        return file.useLines { lines ->
            lines.drop(1).mapNotNull { line ->
                val c = line.split(",")
                if (c.size < 5) return@mapNotNull null
                val rating = c[3].toIntOrNull() ?: return@mapNotNull null
                val first = c[2].split(" ").firstOrNull { it.isNotEmpty() } ?: return@mapNotNull null
                Bench(c[1], first, rating)
            }.toList()
        }
    }

    private companion object {
        const val SAMPLE = 150
        const val DEPTH = 3
        const val FLOOR = 40 // % — far below expectation; only catches disasters
    }
}
