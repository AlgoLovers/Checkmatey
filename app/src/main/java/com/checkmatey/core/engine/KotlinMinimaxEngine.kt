package com.checkmatey.core.engine

import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import kotlin.math.exp
import kotlin.random.Random

/**
 * The pure-Kotlin engine, tuned for speed and strength while staying dependency-free:
 *
 * - **Negamax + alpha-beta** with **check extensions** (a move that gives check searches one ply
 *   deeper, so mating nets are found and dodged) and **path-repetition detection** (a repeated
 *   position inside the search line scores 0 — no more "winning" lines that are actually draws).
 * - **Quiescence search** that is check-aware: in check it searches all evasions (and recognises
 *   checkmate at the horizon); otherwise it resolves captures with **delta pruning** to keep the
 *   node budget for the main search.
 * - **Pseudo-legal search** with legality checked when applying (halves per-node work).
 * - **Move ordering**: TT move, then captures by MVV-LVA, then killer moves, then the **history
 *   heuristic** (quiet moves that caused cutoffs elsewhere get tried early).
 * - **Transposition table** keyed by Zobrist hash. The table and all heuristics are cleared per
 *   search, so identical calls always return identical results (grading stays deterministic).
 * - **Iterative deepening** with an honest node budget: when the budget trips mid-iteration the
 *   search stops, nothing more is written to the TT, and the last *completed* depth's answer stands.
 * - **Human-feeling weak levels**: instead of "perfect move or uniformly random blunder", levels
 *   with an error chance sample from the root moves' scores (softmax with a per-level temperature),
 *   so a weak bot prefers plausible second-best moves like a real beginner.
 *
 * A single instance runs one search at a time (callers hop to a background dispatcher).
 */
class KotlinMinimaxEngine : Engine {

    private var nodes = 0
    private var stopped = false
    private val tt = HashMap<Long, TtEntry>()
    private val killers = Array(MAX_PLY) { arrayOfNulls<Move>(2) }
    private val history = Array(2) { IntArray(64 * 64) }
    private val pathKeys = LongArray(MAX_PLY)

    override fun evaluate(position: Position): Int = Evaluation.evaluate(position)

    override fun chooseMove(position: Position, level: BotLevel, random: Random): Move? {
        val moves = position.legalMoves()
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves.first()
        // Perfect play for the strong levels.
        if (level.blunderChance <= 0.0) return bestMove(position, level.searchDepth)
        val scored = scoredRootMoves(position, level.searchDepth)
        if (scored.isEmpty()) return bestMove(position, level.searchDepth)
        // Fallible levels play SOLIDLY most of the time (the best move) and only slip occasionally —
        // a human-like softmax pick with probability [blunderChance]. Sampling on *every* move made
        // the mid levels feel weak because they never played the best move; this keeps them sharp
        // while staying beatable and human at the low levels.
        val best = scored.maxByOrNull { it.second }?.first ?: return bestMove(position, level.searchDepth)
        if (random.nextDouble() >= level.blunderChance) return best
        return sampleSoftmax(scored, temperatureFor(level), cutoffFor(level), random)
    }

    override fun evaluateMove(before: Position, move: Move, depth: Int): Int {
        startSearch()
        pathKeys[0] = ZobristKeys.hash(before)
        return -negamax(before.applyMove(move), depth - 1, NEG_INF, -NEG_INF, ply = 1)
    }

    override fun bestMove(position: Position, depth: Int): Move? = bestMoveWithScore(position, depth)?.first

    override fun bestMoveWithScore(position: Position, depth: Int): Pair<Move, Int>? {
        val rootMoves = position.legalMoves()
        if (rootMoves.isEmpty()) return null
        startSearch()
        pathKeys[0] = ZobristKeys.hash(position)

        var best = rootMoves.first()
        var bestScore = evaluateForSideToMove(position)
        var ordered = rootMoves
        for (d in 1..depth) {
            var alpha = NEG_INF
            var iterBest = ordered.first()
            var iterBestScore = NEG_INF
            val scored = ArrayList<Pair<Move, Int>>(ordered.size)
            for (move in ordered) {
                val score = -negamax(position.applyMove(move), d - 1, NEG_INF, -alpha, ply = 1)
                if (stopped) break // scores from a truncated search are not comparable — discard
                scored.add(move to score)
                if (score > alpha) {
                    alpha = score
                    iterBest = move
                    iterBestScore = score
                }
            }
            if (stopped) break // keep the last COMPLETED depth's answer
            best = iterBest
            bestScore = iterBestScore
            // Next iteration searches the best candidates first — cheap, big pruning win.
            ordered = scored.sortedByDescending { it.second }.map { it.first }
        }
        return best to bestScore
    }

    /**
     * The expected line from [position]: run the normal search, then walk the transposition
     * table's best-move chain. Each step is validated as legal and repetition-guarded, so the
     * line shown to the student is always playable.
     */
    override fun principalVariation(position: Position, depth: Int, maxLen: Int): List<Move> {
        val first = bestMove(position, depth) ?: return emptyList() // populates the TT
        val line = mutableListOf(first)
        var pos = position.applyMove(first)
        val seen = hashSetOf(ZobristKeys.hash(position))
        while (line.size < maxLen) {
            val key = ZobristKeys.hash(pos)
            if (!seen.add(key)) break
            val next = tt[key]?.move ?: break
            val legal = pos.findMove(next.uci()) ?: break
            line.add(legal)
            pos = pos.applyMove(legal)
        }
        return line
    }

    /**
     * Every root move scored with a full window at the final depth (exact values, not bounds) —
     * the raw material for human-feeling move sampling. Earlier iterations only order the moves.
     */
    fun scoredRootMoves(position: Position, depth: Int): List<Pair<Move, Int>> {
        val rootMoves = position.legalMoves()
        if (rootMoves.isEmpty()) return emptyList()
        startSearch()
        pathKeys[0] = ZobristKeys.hash(position)

        var ordered = rootMoves
        var lastFull: List<Pair<Move, Int>> = emptyList()
        for (d in 1..depth) {
            val scored = ArrayList<Pair<Move, Int>>(ordered.size)
            for (move in ordered) {
                // Full window so every move's score is exact (needed for fair sampling).
                val score = -negamax(position.applyMove(move), d - 1, NEG_INF, -NEG_INF, ply = 1)
                if (stopped) break
                scored.add(move to score)
            }
            if (stopped) break
            lastFull = scored
            ordered = scored.sortedByDescending { it.second }.map { it.first }
        }
        return lastFull
    }

    // ---- Core search --------------------------------------------------------------------

    private fun negamax(position: Position, depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        nodes++
        if (nodes > NODE_BUDGET) stopped = true
        if (stopped) return evaluateForSideToMove(position)
        if (depth <= 0) return quiescence(position, alphaIn, beta, ply, QUIESCENCE_PLIES)

        val key = ZobristKeys.hash(position)
        // A position repeated along the current line is a draw by repetition — score it as one.
        for (i in 0 until ply) if (pathKeys[i] == key) return 0
        if (ply < MAX_PLY) pathKeys[ply] = key

        var alpha = alphaIn
        val entry = tt[key]
        if (entry != null && entry.depth >= depth && !isMateScore(entry.score)) {
            when (entry.flag) {
                TtFlag.EXACT -> return entry.score
                TtFlag.LOWER -> if (entry.score >= beta) return entry.score
                TtFlag.UPPER -> if (entry.score <= alpha) return entry.score
            }
        }

        val moves = ordered(position, position.pseudoLegalMoves(), entry?.move, ply)
        val mover = position.sideToMove
        var legalFound = false
        var best = NEG_INF
        var bestMove: Move? = null
        for (move in moves) {
            val child = position.applyMove(move)
            if (child.isKingAttacked(mover)) continue // pseudo-legal move left our king hanging
            legalFound = true
            // Check extension: a checking move searches one ply deeper so mating nets resolve.
            val ext = if (ply + 1 < MAX_PLY - 1 && child.isInCheck()) 1 else 0
            val score = -negamax(child, depth - 1 + ext, -beta, -alpha, ply + 1)
            if (score > best) {
                best = score
                bestMove = move
            }
            if (best > alpha) alpha = best
            if (alpha >= beta) {
                rememberQuietCutoff(position, move, ply, depth)
                break
            }
        }
        if (!legalFound) {
            return if (position.isInCheck()) -(MATE - ply) else 0 // mate (prefer faster) / stalemate
        }

        // Never store from a truncated search — those scores are garbage and would poison later probes.
        if (!stopped && !isMateScore(best) && tt.size < TT_MAX) {
            val flag = when {
                best <= alphaIn -> TtFlag.UPPER
                best >= beta -> TtFlag.LOWER
                else -> TtFlag.EXACT
            }
            tt[key] = TtEntry(depth, best, flag, bestMove)
        }
        return best
    }

    /**
     * Resolve pending forcing sequences so material is judged after the dust settles. Check-aware:
     * in check it searches every evasion (finding horizon checkmates); otherwise it stands pat or
     * tries captures, skipping ones that can't raise alpha (delta pruning).
     */
    private fun quiescence(position: Position, alphaIn: Int, beta: Int, ply: Int, qPlies: Int): Int {
        nodes++
        if (nodes > NODE_BUDGET) stopped = true
        if (stopped) return evaluateForSideToMove(position)

        val inCheck = position.isInCheck()
        val mover = position.sideToMove
        var alpha = alphaIn

        if (!inCheck) {
            val standPat = evaluateForSideToMove(position)
            if (standPat >= beta) return beta
            if (qPlies <= 0) return standPat
            if (standPat > alpha) alpha = standPat

            val captures = position.pseudoLegalMoves()
                .filter { position.pieceAt(it.to) != null || it.isEnPassant }
                .sortedByDescending { mvvLva(position, it) }
            for (move in captures) {
                // Delta pruning: if even winning this piece can't lift us to alpha, skip the branch.
                // (Promotions and queen captures are always worth a look.)
                if (move.promotion == null) {
                    val victim = position.pieceAt(move.to)?.type ?: PieceType.PAWN
                    if (victim != PieceType.QUEEN && standPat + Evaluation.value(victim) + DELTA_MARGIN <= alpha) continue
                }
                val child = position.applyMove(move)
                if (child.isKingAttacked(mover)) continue
                val score = -quiescence(child, -beta, -alpha, ply + 1, qPlies - 1)
                if (score >= beta) return beta
                if (score > alpha) alpha = score
            }
            return alpha
        }

        // In check there is no "stand pat" — the check must be answered. Search every evasion;
        // none legal means checkmate right here at the horizon.
        if (qPlies <= 0) return evaluateForSideToMove(position)
        var legalFound = false
        val moves = position.pseudoLegalMoves().sortedByDescending { mvvLva(position, it) }
        for (move in moves) {
            val child = position.applyMove(move)
            if (child.isKingAttacked(mover)) continue
            legalFound = true
            val score = -quiescence(child, -beta, -alpha, ply + 1, qPlies - 1)
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return if (!legalFound) -(MATE - ply) else alpha
    }

    // ---- Move ordering ------------------------------------------------------------------

    private fun ordered(position: Position, moves: List<Move>, ttMove: Move?, ply: Int): List<Move> {
        val k = killers.getOrNull(ply)
        val hist = history[if (position.sideToMove == PieceColor.WHITE) 0 else 1]
        return moves.sortedByDescending { move ->
            when {
                ttMove != null && move == ttMove -> 1_000_000
                position.pieceAt(move.to) != null || move.isEnPassant -> 10_000 + mvvLva(position, move)
                k != null && (move == k[0] || move == k[1]) -> 5_000
                else -> hist[move.from.index * 64 + move.to.index].coerceAtMost(4_999)
            }
        }
    }

    /** Most valuable victim first, least valuable attacker as tiebreak. */
    private fun mvvLva(position: Position, move: Move): Int {
        val victim = position.pieceAt(move.to)?.type ?: PieceType.PAWN // en passant captures a pawn
        val attacker = position.pieceAt(move.from)?.type ?: PieceType.PAWN
        return Evaluation.value(victim) * 10 - Evaluation.value(attacker)
    }

    /** A quiet move that caused a beta cutoff: remember it as a killer and boost its history. */
    private fun rememberQuietCutoff(position: Position, move: Move, ply: Int, depth: Int) {
        if (position.pieceAt(move.to) != null || move.isEnPassant) return // only quiet moves
        if (ply < MAX_PLY) {
            val slot = killers[ply]
            if (slot[0] != move) {
                slot[1] = slot[0]
                slot[0] = move
            }
        }
        val hist = history[if (position.sideToMove == PieceColor.WHITE) 0 else 1]
        val idx = move.from.index * 64 + move.to.index
        hist[idx] = (hist[idx] + depth * depth).coerceAtMost(1_000_000)
    }

    // ---- Human-feeling weak play --------------------------------------------------------

    /** Sample a root move with probability ∝ exp(score/T), ignoring hopeless moves. */
    private fun sampleSoftmax(
        scored: List<Pair<Move, Int>>,
        temperature: Double,
        cutoff: Int,
        random: Random,
    ): Move {
        val best = scored.maxOf { it.second }
        val pool = scored.filter { it.second >= best - cutoff }
        val weights = pool.map { exp((it.second - best) / temperature) }
        var r = random.nextDouble() * weights.sum()
        for (i in pool.indices) {
            r -= weights[i]
            if (r <= 0) return pool[i].first
        }
        return pool.last().first
    }

    /** How wide the weak levels' choices spread, in centipawns. */
    private fun temperatureFor(level: BotLevel): Double = when (level) {
        BotLevel.SEEDLING -> 300.0
        BotLevel.BEGINNER -> 180.0
        BotLevel.INTERMEDIATE -> 100.0
        else -> 50.0
    }

    /** Moves this far below the best are never sampled — even a weak bot has limits. */
    private fun cutoffFor(level: BotLevel): Int = when (level) {
        BotLevel.SEEDLING -> 1_600
        BotLevel.BEGINNER -> 1_200
        BotLevel.INTERMEDIATE -> 800
        else -> 500
    }

    // ---- Bookkeeping --------------------------------------------------------------------

    /** Reset all per-search state so identical calls give identical answers (deterministic grading). */
    private fun startSearch() {
        nodes = 0
        stopped = false
        tt.clear()
        for (slot in killers) { slot[0] = null; slot[1] = null }
        for (side in history) side.fill(0)
    }

    private fun evaluateForSideToMove(position: Position): Int {
        val white = Evaluation.evaluate(position)
        val side = if (position.sideToMove == PieceColor.WHITE) white else -white
        return side + TEMPO // the move is worth something — stabilises odd/even comparisons
    }

    private fun isMateScore(score: Int): Boolean = score > MATE - 1000 || score < -(MATE - 1000)

    private enum class TtFlag { EXACT, LOWER, UPPER }
    private data class TtEntry(val depth: Int, val score: Int, val flag: TtFlag, val move: Move?)

    companion object {
        const val MATE = 1_000_000

        private const val NEG_INF = -2_000_000
        private const val QUIESCENCE_PLIES = 12
        private const val NODE_BUDGET = 400_000
        private const val MAX_PLY = 64
        private const val TT_MAX = 200_000
        private const val DELTA_MARGIN = 200
        private const val TEMPO = 12
    }
}

/** Zobrist hashing: a stable 64-bit key per position (placement, side, castling, en passant). */
private object ZobristKeys {
    private val rng = Random(20260718)
    private val pieces = LongArray(12 * 64) { rng.nextLong() }
    private val side = rng.nextLong()
    private val castling = LongArray(16) { rng.nextLong() }
    private val epFile = LongArray(8) { rng.nextLong() }

    fun hash(p: Position): Long {
        var h = 0L
        for (i in 0..63) {
            val piece = p.squares[i] ?: continue
            val idx = (piece.type.ordinal * 2 + if (piece.color == PieceColor.WHITE) 0 else 1) * 64 + i
            h = h xor pieces[idx]
        }
        if (p.sideToMove == PieceColor.BLACK) h = h xor side
        var c = 0
        if (p.castling.whiteKing) c = c or 1
        if (p.castling.whiteQueen) c = c or 2
        if (p.castling.blackKing) c = c or 4
        if (p.castling.blackQueen) c = c or 8
        h = h xor castling[c]
        p.enPassantTarget?.let { h = h xor epFile[it.file] }
        return h
    }
}
