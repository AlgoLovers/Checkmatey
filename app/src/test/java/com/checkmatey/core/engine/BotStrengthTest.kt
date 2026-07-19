package com.checkmatey.core.engine

import com.checkmatey.core.chess.Position
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Quantifies how strongly each bot level actually plays via chooseMove (not just bestMove): over a
 * fixed sample of positions, the average centipawn loss of the level's chosen move versus the best
 * move. Lower = plays closer to best = stronger.
 *
 * This guards the fix for "the ~1000 bot plays weak": the mid/high levels must play the best move
 * *most* of the time (small average loss), while the ladder stays monotonic (weaker levels lose
 * more). A regression that makes a level sample away from best on every move (the M33 bug) would
 * spike its average loss and fail here.
 */
class BotStrengthTest {

    private val engine = KotlinMinimaxEngine()

    @Test
    fun levelsFormAMonotonicLadderAndMidLevelsPlayMostlyBest() {
        val positions = loadPositions().shuffled(Random(7)).take(40)
        val rng = Random(20260719)

        val meanLoss = LinkedHashMap<BotLevel, Int>()
        for (level in listOf(BotLevel.SEEDLING, BotLevel.BEGINNER, BotLevel.INTERMEDIATE, BotLevel.CHALLENGER)) {
            var total = 0L
            var n = 0
            for (fen in positions) {
                val pos = Position.fromFen(fen)
                if (pos.legalMoves().size < 2) continue
                val bestScore = engine.bestMoveWithScore(pos, level.searchDepth)?.second ?: continue
                val chosen = engine.chooseMove(pos, level, rng) ?: continue
                val chosenScore = engine.evaluateMove(pos, chosen, level.searchDepth)
                total += (bestScore - chosenScore).coerceIn(0, 2000)
                n++
            }
            meanLoss[level] = if (n == 0) 0 else (total / n).toInt()
        }

        println("=== Bot strength — mean centipawn loss vs best (lower = stronger) ===")
        meanLoss.forEach { (lvl, loss) -> println("  ${lvl.displayName} (~${lvl.approxElo}): ${loss}cp") }

        // Monotonic ladder: each stronger level plays at least as close to best as the weaker one.
        assertTrue("SEEDLING should be loosest", meanLoss[BotLevel.SEEDLING]!! >= meanLoss[BotLevel.BEGINNER]!!)
        assertTrue("BEGINNER weaker than INTERMEDIATE", meanLoss[BotLevel.BEGINNER]!! >= meanLoss[BotLevel.INTERMEDIATE]!!)
        assertTrue("INTERMEDIATE weaker than CHALLENGER", meanLoss[BotLevel.INTERMEDIATE]!! >= meanLoss[BotLevel.CHALLENGER]!!)
        // The mid level must NOT bleed material every move — it plays the best move most of the time.
        assertTrue("INTERMEDIATE mean loss too high (feels weak): ${meanLoss[BotLevel.INTERMEDIATE]}cp", meanLoss[BotLevel.INTERMEDIATE]!! <= 60)
    }

    private fun loadPositions(): List<String> {
        val file = listOf(File("src/main/assets/puzzles.csv"), File("app/src/main/assets/puzzles.csv"))
            .firstOrNull { it.exists() } ?: error("puzzles.csv not found")
        return file.useLines { lines -> lines.drop(1).mapNotNull { it.split(",").getOrNull(1) }.toList() }
    }
}
