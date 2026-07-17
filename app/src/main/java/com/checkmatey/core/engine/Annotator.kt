package com.checkmatey.core.engine

import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.core.chess.toSan

/** How good a move was, relative to the engine's best. */
enum class MoveQuality(val label: String, val symbol: String) {
    BEST("최선", "★"),
    GOOD("좋은 수", "✓"),
    INACCURACY("부정확", "?!"),
    MISTAKE("실수", "?"),
    BLUNDER("블런더", "??"),
}

/** A move plus a coach's verdict and plain-language reason. */
data class MoveAnnotation(
    val move: Move,
    val san: String,
    val quality: MoveQuality,
    val centipawnLoss: Int,
    val reason: String,
    val bestMove: Move?,
    val bestSan: String?,
)

/**
 * Turns engine numbers into coaching. Fully on-device and deterministic (no LLM): it compares a
 * move to the engine's best (centipawn loss -> quality) and describes the "why" from simple
 * tactical motifs — captures, forks, checks, checkmate, castling, promotion, central development.
 */
class Annotator(private val engine: Engine, private val depth: Int = 3) {

    /** The best move for [position], with an explanation — i.e. a hint. */
    fun hint(position: Position): MoveAnnotation? {
        val best = engine.bestMove(position, depth) ?: return null
        val san = position.toSan(best)
        return MoveAnnotation(best, san, MoveQuality.BEST, 0, reasonFor(position, best), best, san)
    }

    /** Judge a move actually played from [before] against the engine's best. */
    fun annotate(before: Position, move: Move): MoveAnnotation {
        val best = engine.bestMove(before, depth)
        val moveValue = engine.evaluateMove(before, move, depth)
        val bestValue = if (best != null) engine.evaluateMove(before, best, depth) else moveValue
        val loss = (bestValue - moveValue).coerceAtLeast(0)
        val isBest = best != null && move.from == best.from && move.to == best.to && move.promotion == best.promotion

        val quality = when {
            isBest || loss <= 15 -> MoveQuality.BEST
            loss <= 60 -> MoveQuality.GOOD
            loss <= 130 -> MoveQuality.INACCURACY
            loss <= 280 -> MoveQuality.MISTAKE
            else -> MoveQuality.BLUNDER
        }

        val reason = if (quality == MoveQuality.BEST || quality == MoveQuality.GOOD) {
            reasonFor(before, move)
        } else {
            val betterWhy = best?.let { reasonFor(before, it) }.orEmpty()
            val betterSan = best?.let { before.toSan(it) }
            if (betterSan != null) "더 좋은 수: $betterSan — $betterWhy" else "다른 수가 더 좋았습니다"
        }

        return MoveAnnotation(
            move = move,
            san = before.toSan(move),
            quality = quality,
            centipawnLoss = loss,
            reason = reason,
            bestMove = best,
            bestSan = best?.let { before.toSan(it) },
        )
    }

    /** Rule/template explanation of why [move] is a good idea in [before]. */
    private fun reasonFor(before: Position, move: Move): String {
        val after = before.applyMove(move)
        if (after.isCheckmate()) return "체크메이트! 게임을 끝냅니다"

        val parts = mutableListOf<String>()
        if (move.isCastleKingSide || move.isCastleQueenSide) parts += "캐슬링으로 킹을 안전하게 하고 룩을 전개합니다"
        if (move.promotion != null) parts += "${pieceName(move.promotion)}(으)로 승격합니다"

        val captured = before.pieceAt(move.to)?.type ?: if (move.isEnPassant) PieceType.PAWN else null
        if (captured != null) parts += "${move.to.name}의 ${pieceName(captured)}을(를) 잡아 이득을 봅니다"

        if (forkCount(after, move.to) >= 2) parts += "한 수로 두 기물 이상을 동시에 노립니다 (포크)"
        if (after.isInCheck()) parts += "체크를 겁니다"

        if (parts.isEmpty()) parts += positionalReason(move)
        return parts.joinToString(", ")
    }

    /** Number of non-pawn enemy pieces the piece now on [from] attacks. */
    private fun forkCount(pos: Position, from: Square): Int {
        val piece = pos.pieceAt(from) ?: return 0
        val enemy = piece.color.opposite()
        var count = 0
        for (i in 0..63) {
            val target = pos.squares[i] ?: continue
            if (target.color != enemy || target.type == PieceType.PAWN) continue
            if (pos.attacksFrom(from, Square(i % 8, i / 8))) count++
        }
        return count
    }

    private fun positionalReason(move: Move): String = when {
        move.to.file in 3..4 && move.to.rank in 3..4 -> "중앙을 장악합니다"
        move.to.file in 2..5 && move.to.rank in 2..5 -> "좋은 자리로 기물을 전개합니다"
        else -> "안정적인 전개 수입니다"
    }

    private fun pieceName(type: PieceType): String = when (type) {
        PieceType.PAWN -> "폰"
        PieceType.KNIGHT -> "나이트"
        PieceType.BISHOP -> "비숍"
        PieceType.ROOK -> "룩"
        PieceType.QUEEN -> "퀸"
        PieceType.KING -> "킹"
    }
}
