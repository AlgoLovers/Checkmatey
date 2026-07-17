package com.checkmatey.core.engine

import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.Position
import kotlin.random.Random

/**
 * A pure-Kotlin opponent: negamax + alpha-beta with capture-first move ordering and a
 * **quiescence search** at the leaves (it keeps resolving captures so it doesn't misjudge a
 * position mid-trade — the "horizon effect"). That makes the higher levels play a genuinely
 * strong tactical game. A node budget bounds the deepest levels' thinking time.
 *
 * Uses the immutable [Position.applyMove]; a single engine instance is used from one screen at a
 * time (searches are sequential), so the mutable node counter is safe here.
 */
class KotlinMinimaxEngine : Engine {

    private var nodes = 0

    override fun evaluate(position: Position): Int = Evaluation.evaluate(position)

    override fun chooseMove(position: Position, level: BotLevel, random: Random): Move? {
        val moves = position.legalMoves()
        if (moves.isEmpty()) return null
        if (random.nextDouble() < level.blunderChance) {
            return moves[random.nextInt(moves.size)]
        }
        return bestMove(position, level.searchDepth)
    }

    override fun evaluateMove(before: Position, move: Move, depth: Int): Int {
        nodes = 0
        return -negamax(before.applyMove(move), depth - 1, NEG_INF, -NEG_INF, ply = 1)
    }

    override fun bestMove(position: Position, depth: Int): Move? {
        nodes = 0
        val moves = order(position, position.legalMoves())
        if (moves.isEmpty()) return null

        var best = moves.first()
        var bestScore = NEG_INF
        var alpha = NEG_INF
        for (move in moves) {
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
        nodes++
        val moves = position.legalMoves()
        if (moves.isEmpty()) {
            return if (position.isInCheck()) -(MATE - ply) else 0 // checkmate (prefer faster) / stalemate
        }
        if (depth <= 0) return quiescence(position, alpha, beta, QUIESCENCE_PLIES)
        if (nodes > NODE_BUDGET) return evaluateForSideToMove(position)

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

    /** Resolve pending captures so material is judged after the dust settles. */
    private fun quiescence(position: Position, alpha: Int, beta: Int, qPlies: Int): Int {
        nodes++
        val standPat = evaluateForSideToMove(position)
        if (standPat >= beta) return beta
        if (qPlies <= 0 || nodes > NODE_BUDGET) return standPat

        var a = if (standPat > alpha) standPat else alpha
        val captures = position.legalMoves().filter { position.pieceAt(it.to) != null || it.isEnPassant }
        for (move in order(position, captures)) {
            val score = -quiescence(position.applyMove(move), -beta, -a, qPlies - 1)
            if (score >= beta) return beta
            if (score > a) a = score
        }
        return a
    }

    private fun evaluateForSideToMove(position: Position): Int {
        val white = Evaluation.evaluate(position)
        return if (position.sideToMove == PieceColor.WHITE) white else -white
    }

    /** Captures first, most valuable victim first — helps alpha-beta prune. */
    private fun order(position: Position, moves: List<Move>): List<Move> =
        moves.sortedByDescending { move ->
            position.pieceAt(move.to)?.let { Evaluation.value(it.type) } ?: 0
        }

    companion object {
        const val MATE = 1_000_000

        private const val NEG_INF = -2_000_000
        private const val QUIESCENCE_PLIES = 12
        private const val NODE_BUDGET = 400_000
    }
}
