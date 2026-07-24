package com.checkmatey.core.engine

import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.chess.PieceType
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.Square
import com.checkmatey.core.chess.koreanName

/** A danger the opponent is threatening right now, worth warning the student about. */
data class Threat(val square: Square, val piece: PieceType, val text: String, val severity: Int)

/**
 * The "human tutor" behaviours — the part a review screen can't give you: it watches the board
 * WITH you (warning about threats before you move), teaches by questions instead of answers
 * (a Socratic hint ladder), and connects mistakes to your history (theme extraction for the
 * "this keeps happening" callback). Pure Kotlin over the existing detectors, fully unit-tested.
 */
object Tutor {

    // ---- ① Proactive threat radar -------------------------------------------------------

    /**
     * What the opponent is threatening against [myColor] right now: checkmate next move, or
     * winning one of my valuable pieces. At most the two worst threats, worst first — a tutor
     * points at the wolf nearest the sled, not every dog on the horizon.
     */
    fun threats(position: Position, myColor: PieceColor): List<Threat> {
        val enemy = myColor.opposite()
        val found = ArrayList<Threat>()

        // Mate-in-1 threat: if it were the opponent's move, could they mate? (side-flipped FEN)
        matingMove(position, enemy)?.let { mv ->
            found += Threat(mv.to, PieceType.KING, "🚨 조심! 상대가 다음 수로 체크메이트를 노려요 (${mv.to.name} 방향)", 1000)
        }

        // Hanging / underdefended pieces (knight and up — pawn nags would drown the signal).
        for (i in 0..63) {
            val p = position.squares[i] ?: continue
            if (p.color != myColor || p.type == PieceType.KING || p.type == PieceType.PAWN) continue
            val sq = Square(i % 8, i / 8)
            if (!position.isSquareAttacked(sq, enemy)) continue
            val defended = position.isSquareAttacked(sq, myColor)
            val cheapest = cheapestAttackerValue(position, sq, enemy) ?: continue
            val value = Evaluation.value(p.type)
            if (!defended || cheapest < value) {
                val how = if (!defended) "무방비예요" else "더 싼 기물로 노리고 있어요"
                found += Threat(sq, p.type, "⚠️ ${sq.name}의 ${p.type.koreanName()}이(가) 위험해요 — $how", value)
            }
        }
        return found.sortedByDescending { it.severity }.take(2)
    }

    /** A move that would checkmate if [attacker] were to move now, or null. */
    private fun matingMove(position: Position, attacker: PieceColor) =
        flipSideToMove(position)?.takeIf { it.sideToMove == attacker }?.let { flipped ->
            flipped.legalMoves().firstOrNull { flipped.applyMove(it).isCheckmate() }
        }

    /** The position with the other side to move (en passant cleared). Null while in check. */
    private fun flipSideToMove(position: Position): Position? {
        if (position.isInCheck()) return null // passing while in check isn't a meaningful "threat" probe
        val f = position.toFen().split(" ").toMutableList()
        if (f.size < 6) return null
        f[1] = if (f[1] == "w") "b" else "w"
        f[3] = "-"
        return runCatching { Position.fromFen(f.joinToString(" ")) }.getOrNull()
    }

    private fun cheapestAttackerValue(position: Position, target: Square, by: PieceColor): Int? {
        var cheapest: Int? = null
        for (i in 0..63) {
            val p = position.squares[i] ?: continue
            if (p.color != by) continue
            val from = Square(i % 8, i / 8)
            if (!position.attacksFrom(from, target)) continue
            val v = Evaluation.value(p.type).let { if (p.type == PieceType.KING) 10_000 else it }
            if (cheapest == null || v < cheapest!!) cheapest = v
        }
        return cheapest
    }

    // ---- ② Socratic hint ladder ---------------------------------------------------------

    /**
     * Three disclosures from one engine hint: a guiding QUESTION, then the piece, then the move.
     * Answering the question yourself is where the learning happens — the tutor only opens the
     * next door when you knock again.
     */
    fun hintLadder(position: Position, hint: MoveAnnotation): List<String> {
        val reason = hint.reason
        val question = when {
            "체크메이트" in reason -> "메이트 냄새가 나요! 상대 킹이 도망갈 칸을 세어 보세요 — 막을 수 있는 수가 보이나요?"
            "포크" in reason -> "한 수로 두 기물을 동시에 노릴 수 있어요. 어떤 기물이 그런 점프를 할 수 있을까요?"
            "핀" in reason -> "상대 기물 하나를 킹에 묶어버릴 수 있어요. 일직선을 찾아보세요."
            "공짜" in reason || "잡" in reason -> "지금 잡을 수 있는 상대 기물 중, 잡아도 되받아치지 못하는 게 있어요. 어느 걸까요?"
            "디스커버드" in reason -> "기물 하나를 비키면 뒤에 숨은 공격이 드러나요. 어떤 기물이 길을 막고 있죠?"
            "위협" in reason || "노립니다" in reason -> "상대의 약한 기물(지켜지지 않은 기물)을 찾아보세요. 그걸 노리는 수가 있어요."
            "캐슬링" in reason -> "킹이 가운데 너무 오래 있었어요. 안전하게 숨기는 수는?"
            "승격" in reason -> "폰이 끝줄에 거의 다 왔어요!"
            else -> "가장 일을 안 하고 있는 기물을 찾아 보세요 — 그 기물에게 더 좋은 자리가 있어요."
        }
        val piece = position.pieceAt(hint.move.from)
        val pieceLine = "움직일 기물: ${piece?.type?.koreanName() ?: "?"} (${hint.move.from.name})"
        val plan = if (hint.bestLine.size >= 2) "\n🗺 계획: " + hint.bestLine.joinToString(" → ") else ""
        val answer = "${hint.san} — ${hint.reason}$plan"
        return listOf(question, pieceLine, answer)
    }

    // ---- ③ Memory: connect a mistake to the player's history -----------------------------

    /** The trainable puzzle theme a coach explanation points at, or null. Mirrors GameReviewer. */
    fun themeOf(reason: String): String? = when {
        "포크" in reason -> "포크"
        "핀" in reason -> "핀"
        "체크메이트" in reason -> "메이트 1수"
        "잡" in reason || "이득" in reason -> "매달린 기물"
        else -> null
    }

    /**
     * The "this keeps happening" line, when a graded mistake matches a theme the player has
     * struggled with ([attempts] tries at [successRate]% so far). Null when there's no pattern —
     * a tutor who nags about everything teaches nothing.
     */
    fun recallLine(theme: String, attempts: Int, successRate: Int): String? {
        if (attempts < 3 || successRate > 50) return null
        return "📌 \"$theme\" 유형이 자꾸 발목을 잡네요 (성공률 $successRate%) — 퍼즐 탭에서 이 테마를 집중 훈련하면 바로 좋아져요."
    }
}
