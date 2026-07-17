package com.checkmatey.core.engine

import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.Position
import kotlin.random.Random

/**
 * A pure-Kotlin opponent: negamax search with alpha-beta pruning and capture-first move
 * ordering, scored by [Evaluation]. Strong enough for the beginner range (<=1200) and fully
 * on-device with no model or network. Permissively licensed (our own code).
 *
 * It uses the immutable [Position.applyMove] rather than make/unmake — simpler and correct;
 * fast enough for the shallow depths these levels use.
 */
class KotlinMinimaxEngine : Engine {

    override fun evaluate(position: Position): Int = Evaluation.evaluate(position)

    override fun chooseMove(position: Position, level: BotLevel, random: Random): Move? {
        val moves = position.legalMoves()
        if (moves.isEmpty()) return null
        // Deliberate blunder: play a random legal move so weaker levels feel human.
        if (random.nextDouble() < level.blunderChance) {
            return moves[random.nextInt(moves.size)]
        }
        return bestMove(position, level.searchDepth)
    }

    override fun evaluateMove(before: Position, move: Move, depth: Int): Int =
        -negamax(before.applyMove(move), depth - 1, NEG_INF, -NEG_INF, ply = 1)

    override fun bestMove(position: Position, depth: Int): Move? {
        val moves = order(position, position.legalMoves())
        if (moves.isEmpty()) return null

        var best = moves.first()
        var bestScore = NEG_INF
        var alpha = NEG_INF
        for (move in moves) {
            // Child window: alpha = -beta_root (= NEG_INF), beta = -alpha_root.
            val score = -negamax(position.applyMove(move), depth - 1, NEG_INF, -alpha, ply = 1)
            if (score > bestScore) {
                bestScore = score
                best = move
            }
            if (score > alpha) alpha = score
        }
        return best
    }

    private fun negamax(position: Position, depth: Int, alpha: Int, beta: Int, ply: Int): Int {
        val moves = position.legalMoves()
        if (moves.isEmpty()) {
            // Checkmate (bad for side to move) or stalemate (draw). Prefer faster mates via ply.
            return if (position.isInCheck()) -(MATE - ply) else 0
        }
        if (depth == 0) return evaluateForSideToMove(position)

        var a = alpha
        var best = NEG_INF
        for (move in order(position, moves)) {
            val score = -negamax(position.applyMove(move), depth - 1, -beta, -a, ply + 1)
            if (score > best) best = score
            if (best > a) a = best
            if (a >= beta) break // beta cutoff
        }
        return best
    }

    private fun evaluateForSideToMove(position: Position): Int {
        val white = Evaluation.evaluate(position)
        return if (position.sideToMove == PieceColor.WHITE) white else -white
    }

    /** Order moves so captures come first (most valuable victim first) — helps alpha-beta prune. */
    private fun order(position: Position, moves: List<Move>): List<Move> =
        moves.sortedByDescending { move ->
            position.pieceAt(move.to)?.let { Evaluation.value(it.type) } ?: 0
        }

    companion object {
        const val MATE = 1_000_000

        // Symmetric finite bounds so negating alpha/beta never overflows.
        private const val NEG_INF = -2_000_000
    }
}
