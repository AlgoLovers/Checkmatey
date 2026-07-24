package com.checkmatey.core.engine

import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.Position
import kotlin.random.Random

/**
 * A chess move-picking engine. The app depends only on this interface, so the underlying
 * engine (pure-Kotlin now, optionally Stockfish/Maia later) can be swapped without UI changes.
 */
interface Engine {
    /** Best move found by searching [depth] plies ahead. Deterministic. Null if game over. */
    fun bestMove(position: Position, depth: Int): Move?

    /**
     * [bestMove] plus its search score (side-to-move perspective). Implementations that already
     * know the root score should override this so callers don't pay for a second search.
     */
    fun bestMoveWithScore(position: Position, depth: Int): Pair<Move, Int>? =
        bestMove(position, depth)?.let { it to evaluateMove(position, it, depth) }

    /**
     * The expected line (principal variation) from [position], best move first — the "plan" a
     * human tutor would sketch: "여기 두면, 상대는 이렇게, 그러면 이렇게". Engines that keep a
     * transposition table should override this to walk it; the default is just the best move.
     */
    fun principalVariation(position: Position, depth: Int, maxLen: Int = 3): List<Move> =
        bestMove(position, depth)?.let { listOf(it) } ?: emptyList()

    /**
     * Search value of playing [move] from [before], in centipawns from the moving side's
     * perspective (higher = better). Used to score/compare moves for coaching feedback.
     */
    fun evaluateMove(before: Position, move: Move, depth: Int): Int

    /**
     * A move for the given [level]. May deliberately play a weaker move (see
     * [BotLevel.blunderChance]); [random] is injectable so games can be reproduced/tested.
     */
    fun chooseMove(position: Position, level: BotLevel, random: Random = Random.Default): Move?

    /** Static evaluation in centipawns, positive = good for White. */
    fun evaluate(position: Position): Int
}
