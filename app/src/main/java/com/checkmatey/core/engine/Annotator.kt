package com.checkmatey.core.engine

import com.checkmatey.core.chess.Material
import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.core.chess.koreanName
import com.checkmatey.core.chess.toSan

private val ROOK_DIRS = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
private val BISHOP_DIRS = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))

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
 * move to the engine's best (centipawn loss -> quality) and describes the "why" from tactical motifs
 * and positional plans — free captures, forks, pins, discovered checks, threats on undefended
 * pieces, checkmate, castling, promotion, and (failing all that) the developing/centre/rook idea.
 */
class Annotator(private val engine: Engine, private val depth: Int = 4) {

    /** The best move for [position], with an explanation — i.e. a hint. */
    fun hint(position: Position): MoveAnnotation? {
        val best = engine.bestMove(position, depth) ?: return null
        val san = position.toSan(best)
        return MoveAnnotation(best, san, MoveQuality.BEST, 0, reasonFor(position, best), best, san)
    }

    /** Judge a move actually played from [before] against the engine's best. */
    fun annotate(before: Position, move: Move): MoveAnnotation {
        val bestScored = engine.bestMoveWithScore(before, depth)
        val best = bestScored?.first
        val isBest = best != null && move.from == best.from && move.to == best.to && move.promotion == best.promotion
        // The root search already scored the best move — only the played move needs a search.
        val bestValue = bestScored?.second ?: 0
        val moveValue = if (isBest) bestValue else engine.evaluateMove(before, move, depth)
        val loss = (bestValue - moveValue).coerceIn(0, 1500)

        // Grade on WIN-PROBABILITY drop, not raw centipawns: mate scores saturate instead of
        // exploding (winning slower than mate-in-N is no longer a "blunder"), and the same cp loss
        // matters less in a decided position than in a level one — kinder, truer coaching.
        val drop = winPct(bestValue) - winPct(moveValue)
        val quality = when {
            isBest || drop < 3 -> MoveQuality.BEST
            drop < 8 -> MoveQuality.GOOD
            drop < 15 -> MoveQuality.INACCURACY
            drop < 25 -> MoveQuality.MISTAKE
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

    /**
     * Rule/template explanation of why [move] is a good idea in [before]. Beyond single-move motifs
     * (capture/fork/check/castle/promotion) it now reads *plans and threats*: whether material is won
     * for free, whether the move pins a piece to the king, delivers a discovered check, or simply
     * threatens an undefended piece — and, when nothing tactical happens, names the positional idea
     * (develop, seize the centre, activate a rook). Still fully deterministic and on-device.
     */
    private fun reasonFor(before: Position, move: Move): String {
        val after = before.applyMove(move)
        if (after.isCheckmate()) return "체크메이트! 게임을 끝냅니다"
        val mover = before.sideToMove
        val enemy = mover.opposite()

        val parts = mutableListOf<String>()
        if (move.isCastleKingSide || move.isCastleQueenSide) parts += "캐슬링으로 킹을 안전하게 하고 룩을 전개합니다"
        if (move.promotion != null) parts += "${move.promotion.koreanName()}(으)로 승격합니다"

        val captured = before.pieceAt(move.to)?.type ?: if (move.isEnPassant) PieceType.PAWN else null
        if (captured != null) {
            // Free if the enemy can't recapture on the landing square.
            val free = !after.isSquareAttacked(move.to, enemy)
            parts += if (free) "${move.to.name}의 ${captured.koreanName()}을(를) 공짜로 잡습니다"
            else "${move.to.name}의 ${captured.koreanName()}을(를) 잡습니다"
        }

        if (forkCount(after, move.to) >= 2) parts += "한 수로 두 기물 이상을 동시에 노립니다 (포크)"
        if (pinsToKing(after, move.to)) parts += "상대 기물을 킹에 묶습니다 (핀) — 그 기물은 움직일 수 없습니다"

        if (after.isInCheck()) {
            val king = findKing(after, enemy)
            val direct = king != null && after.attacksFrom(move.to, king)
            parts += if (direct) "체크를 겁니다" else "디스커버드 체크! 숨어 있던 기물이 체크를 겁니다"
        }

        if (parts.isEmpty()) threatFrom(after, move.to)?.let { parts += it }
        if (parts.isEmpty()) parts += positionalReason(before, move)
        return parts.joinToString(", ")
    }

    /** Number of non-pawn enemy pieces the piece now on [from] attacks. */
    /** Win probability (0–100) for the side to move, from a centipawn score — Lichess's mapping. */
    private fun winPct(cp: Int): Double {
        val c = cp.coerceIn(-1500, 1500)
        return 100.0 / (1.0 + kotlin.math.exp(-0.00368208 * c))
    }

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

    /** True if the slider now on [from] pins an enemy piece against its king (absolute pin). */
    private fun pinsToKing(pos: Position, from: Square): Boolean {
        val piece = pos.pieceAt(from) ?: return false
        val dirs = when (piece.type) {
            PieceType.BISHOP -> BISHOP_DIRS
            PieceType.ROOK -> ROOK_DIRS
            PieceType.QUEEN -> ROOK_DIRS + BISHOP_DIRS
            else -> return false
        }
        val enemy = piece.color.opposite()
        for ((df, dr) in dirs) {
            var f = from.file + df
            var r = from.rank + dr
            var pinned = false // whether we've passed the candidate pinned piece
            while (f in 0..7 && r in 0..7) {
                val p = pos.pieceAt(f, r)
                if (p != null) {
                    if (!pinned) {
                        if (p.color != enemy || p.type == PieceType.KING) break
                        pinned = true // first enemy piece — could be pinned
                    } else {
                        if (p.color == enemy && p.type == PieceType.KING) return true
                        break // some other piece shields the king — no absolute pin
                    }
                }
                f += df; r += dr
            }
        }
        return false
    }

    /** If the piece now on [from] threatens a valuable, undefended enemy piece, describe it. */
    private fun threatFrom(pos: Position, from: Square): String? {
        val piece = pos.pieceAt(from) ?: return null
        val enemy = piece.color.opposite()
        // A threat from a piece that is itself hanging isn't a threat — it just loses the attacker.
        if (pos.isSquareAttacked(from, enemy)) return null
        var best: Pair<Square, PieceType>? = null
        for (i in 0..63) {
            val target = pos.squares[i] ?: continue
            if (target.color != enemy || target.type == PieceType.KING) continue
            val sq = Square(i % 8, i / 8)
            if (!pos.attacksFrom(from, sq)) continue
            val defended = pos.isSquareAttacked(sq, enemy)
            val worthIt = pieceValue(target.type) > pieceValue(piece.type) || !defended
            if (worthIt && (best == null || pieceValue(target.type) > pieceValue(best!!.second))) {
                best = sq to target.type
            }
        }
        return best?.let { "다음 수로 ${it.first.name}의 ${it.second.koreanName()}을(를) 노립니다" }
    }

    private fun findKing(pos: Position, color: PieceColor): Square? {
        for (i in 0..63) {
            val p = pos.squares[i] ?: continue
            if (p.type == PieceType.KING && p.color == color) return Square(i % 8, i / 8)
        }
        return null
    }

    // Trade value for threat comparisons: the shared pawn scale, but the king is an infinite
    // sentinel (you never "win a trade" by attacking with, or capturing, the king).
    private fun pieceValue(type: PieceType): Int =
        if (type == PieceType.KING) 100 else Material.pawnValue(type)

    /** Positional idea when a move makes no immediate tactic — a small, level-appropriate plan. */
    private fun positionalReason(before: Position, move: Move): String {
        val piece = before.pieceAt(move.from)
        val backRank = if (before.sideToMove == PieceColor.WHITE) 0 else 7
        return when {
            piece?.type in setOf(PieceType.KNIGHT, PieceType.BISHOP) && move.from.rank == backRank ->
                "미개발 기물을 전개해 실전을 준비합니다"
            piece?.type == PieceType.PAWN && move.to.file in 3..4 && move.to.rank in 3..4 ->
                "중앙을 장악해 공간을 넓힙니다"
            piece?.type == PieceType.ROOK && isOpenFile(before, move.to.file) ->
                "룩을 열린 파일에 놓아 압박합니다"
            move.to.file in 2..5 && move.to.rank in 2..5 -> "기물을 더 활동적인 자리로 옮깁니다"
            else -> "안정적인 전개 수입니다"
        }
    }

    /** No pawns of either colour on [file] — a rook there works at full range. */
    private fun isOpenFile(pos: Position, file: Int): Boolean =
        (0..7).none { rank -> pos.pieceAt(file, rank)?.type == PieceType.PAWN }
}
