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
