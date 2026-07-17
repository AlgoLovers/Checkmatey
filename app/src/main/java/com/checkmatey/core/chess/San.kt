package com.checkmatey.core.chess

/**
 * Standard Algebraic Notation (SAN) for moves, e.g. "Nf3", "exd5", "O-O", "e8=Q+", "Rae1", "Qxb2#".
 *
 * [toSan] renders a legal move in this position (with the disambiguation and check/mate suffix a
 * reader expects). [parseSan] finds the legal move a SAN token refers to by matching against the
 * SAN of every legal move — so the two stay consistent by construction.
 */
fun Position.toSan(move: Move): String {
    if (move.isCastleKingSide) return move.withCheckSuffix("O-O", this)
    if (move.isCastleQueenSide) return move.withCheckSuffix("O-O-O", this)

    val piece = pieceAt(move.from) ?: return move.uci()
    val isCapture = pieceAt(move.to) != null || move.isEnPassant
    val sb = StringBuilder()

    if (piece.type == PieceType.PAWN) {
        if (isCapture) sb.append('a' + move.from.file).append('x')
        sb.append(move.to.name)
        if (move.promotion != null) sb.append('=').append(move.promotion.letter.uppercaseChar())
    } else {
        sb.append(piece.type.letter.uppercaseChar())
        sb.append(disambiguation(move, piece.type))
        if (isCapture) sb.append('x')
        sb.append(move.to.name)
    }
    return move.withCheckSuffix(sb.toString(), this)
}

/** Extra file/rank needed when another same-type piece could also make this move. */
private fun Position.disambiguation(move: Move, type: PieceType): String {
    val rivals = legalMoves().filter {
        it.to == move.to && it.from != move.from && pieceAt(it.from)?.type == type
    }
    if (rivals.isEmpty()) return ""
    val sameFile = rivals.any { it.from.file == move.from.file }
    val sameRank = rivals.any { it.from.rank == move.from.rank }
    return when {
        !sameFile -> ('a' + move.from.file).toString()
        !sameRank -> (move.from.rank + 1).toString()
        else -> move.from.name
    }
}

private fun Move.withCheckSuffix(base: String, before: Position): String {
    val after = before.applyMove(this)
    return base + when {
        after.isCheckmate() -> "#"
        after.isInCheck() -> "+"
        else -> ""
    }
}

/** Finds the legal move that [san] denotes, or null if none matches. Tolerant of +, #, !, ?, 0-0. */
fun Position.parseSan(san: String): Move? {
    val target = normalizeSan(san)
    return legalMoves().firstOrNull { normalizeSan(toSan(it)) == target }
}

private fun normalizeSan(san: String): String =
    san.trim()
        .replace("0-0-0", "O-O-O")
        .replace("0-0", "O-O")
        .replace(Regex("[+#!?]"), "")
        .removeSuffix("e.p.")
        .trim()
