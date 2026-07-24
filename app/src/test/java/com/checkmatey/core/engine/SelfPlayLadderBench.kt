package com.checkmatey.core.engine

import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.Position
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.log10
import kotlin.random.Random

/**
 * Measures the bot ladder EMPIRICALLY: adjacent levels play matches against each other (colours
 * alternating, seeds fixed, unfinished games adjudicated by evaluation), and the winrates are
 * converted to implied Elo gaps (Elo = -400·log10(1/score − 1)). This replaces gut-feel level
 * labels (~400/~700/~1000/~1300) with data — the ladder must at least be monotonic, and the
 * printed gaps say whether the labels' ~300-point spacing is honest.
 *
 * Opt-in bench (~1–2 min): RUN_BENCH=true ./gradlew testDebugUnitTest --tests '*SelfPlayLadderBench*'
 */
class SelfPlayLadderBench {

    @Test
    fun adjacentLevelsWinrateAndImpliedEloGap() {
        assumeTrue("self-play bench — set RUN_BENCH=true to run", System.getenv("RUN_BENCH") == "true")

        val pairs = listOf(
            BotLevel.BEGINNER to BotLevel.SEEDLING,
            BotLevel.INTERMEDIATE to BotLevel.BEGINNER,
            BotLevel.CHALLENGER to BotLevel.INTERMEDIATE,
        )
        println("=== Self-play ladder (n=$GAMES_PER_PAIR games/pair, colours alternate) ===")
        for ((strong, weak) in pairs) {
            var score = 0.0 // strong's points
            for (g in 0 until GAMES_PER_PAIR) {
                val strongIsWhite = g % 2 == 0
                val outcome = play(strong, weak, strongIsWhite, seed = 1000 + g)
                score += outcome
            }
            val rate = score / GAMES_PER_PAIR
            val elo = impliedElo(rate)
            val labelGap = strong.approxElo - weak.approxElo
            println(
                "  ${strong.displayName}(~${strong.approxElo}) vs ${weak.displayName}(~${weak.approxElo}): " +
                    "score ${"%.1f".format(score)}/$GAMES_PER_PAIR (${(rate * 100).toInt()}%) → implied gap ${elo}Elo (label gap $labelGap)",
            )
            assertTrue("${strong.displayName} must outscore ${weak.displayName} (rate=$rate)", rate > 0.5)
        }
    }

    /** One game; returns strong's points (1 win, 0.5 draw, 0 loss). */
    private fun play(strong: BotLevel, weak: BotLevel, strongIsWhite: Boolean, seed: Int): Double {
        val engineW = KotlinMinimaxEngine()
        val engineB = KotlinMinimaxEngine()
        val rng = Random(seed)
        var pos = Position.startingPosition()
        val seen = HashMap<String, Int>() // FEN-4 -> count, for threefold
        var plies = 0
        while (!pos.isGameOver() && plies < MAX_PLIES) {
            val key = pos.toFen().split(" ").take(4).joinToString(" ")
            if ((seen.merge(key, 1, Int::plus) ?: 0) >= 3) return 0.5 // threefold draw
            val whiteToMove = pos.sideToMove == PieceColor.WHITE
            val level = if (whiteToMove == strongIsWhite) strong else weak
            val engine = if (whiteToMove) engineW else engineB
            val move = engine.chooseMove(pos, level, rng) ?: break
            pos = pos.applyMove(move)
            plies++
        }
        val strongColor = if (strongIsWhite) PieceColor.WHITE else PieceColor.BLACK
        return when {
            pos.isCheckmate() -> if (pos.sideToMove != strongColor) 1.0 else 0.0
            pos.isGameOver() -> 0.5 // stalemate / insufficient material / fifty-move
            else -> { // adjudicate a long game by evaluation from strong's perspective
                val evalWhite = KotlinMinimaxEngine().evaluate(pos)
                val evalStrong = if (strongColor == PieceColor.WHITE) evalWhite else -evalWhite
                when {
                    evalStrong > ADJUDICATE_CP -> 1.0
                    evalStrong < -ADJUDICATE_CP -> 0.0
                    else -> 0.5
                }
            }
        }
    }

    private fun impliedElo(rate: Double): Int {
        val r = rate.coerceIn(0.02, 0.98) // avoid infinities on sweeps
        return (-400.0 * log10(1.0 / r - 1.0)).toInt()
    }

    private companion object {
        const val GAMES_PER_PAIR = 12
        const val MAX_PLIES = 160
        const val ADJUDICATE_CP = 150
    }
}
