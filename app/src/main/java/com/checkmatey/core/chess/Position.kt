package com.checkmatey.core.chess

import kotlin.math.abs
import kotlin.math.sign

/** Castling availability for both sides. */
data class CastlingRights(
    val whiteKing: Boolean = false,
    val whiteQueen: Boolean = false,
    val blackKing: Boolean = false,
    val blackQueen: Boolean = false,
) {
    /** The castling field of a FEN string, e.g. "KQkq" or "-". */
    fun fen(): String {
        val sb = StringBuilder()
        if (whiteKing) sb.append('K')
        if (whiteQueen) sb.append('Q')
        if (blackKing) sb.append('k')
        if (blackQueen) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }
}

// Ray/step tables (file delta, rank delta).
private val ROOK_DIRS = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
private val BISHOP_DIRS = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))
private val QUEEN_DIRS = ROOK_DIRS + BISHOP_DIRS
private val KING_OFFSETS = QUEEN_DIRS
private val KNIGHT_OFFSETS = arrayOf(
    intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
    intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2),
)

/**
 * A full, immutable chess position: piece placement plus side to move, castling rights,
 * en passant target and the move clocks. [applyMove] returns a new position, so positions
 * are safe to share (UI, search trees, undo history).
 *
 * Move generation uses the simplest correct strategy: generate pseudo-legal moves, then
 * keep only those that don't leave the mover's own king in check. Speed is fine for the
 * rules layer and for a beginner-level (<=1200) engine; the search engine can switch to
 * make/unmake later without changing this API.
 */
class Position(
    /** 64 squares, index = rank * 8 + file. Rank 0 is white's first rank. */
    val squares: List<Piece?>,
    val sideToMove: PieceColor,
    val castling: CastlingRights,
    val enPassantTarget: Square?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
) {
    init {
        require(squares.size == 64) { "squares must have 64 entries, got ${squares.size}" }
    }

    fun pieceAt(square: Square): Piece? = squares[square.index]
    fun pieceAt(file: Int, rank: Int): Piece? = squares[rank * 8 + file]

    // ---- Move generation ------------------------------------------------------------

    /** All fully legal moves for [sideToMove]. */
    fun legalMoves(): List<Move> {
        val mover = sideToMove
        return pseudoLegalMoves().filter { move ->
            // A move is legal iff it does not leave the mover's king attacked.
            !applyMove(move).isKingAttacked(mover)
        }
    }

    /**
     * Moves that follow piece movement rules but may leave the own king in check.
     * The engine searches these and validates legality when applying (cheaper than
     * filtering up front at every node); UI code should use [legalMoves].
     */
    fun pseudoLegalMoves(): List<Move> {
        val moves = ArrayList<Move>(48)
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = pieceAt(file, rank) ?: continue
                if (piece.color != sideToMove) continue
                when (piece.type) {
                    PieceType.PAWN -> genPawn(file, rank, moves)
                    PieceType.KNIGHT -> genStep(file, rank, moves, KNIGHT_OFFSETS)
                    PieceType.BISHOP -> genSliding(file, rank, moves, BISHOP_DIRS)
                    PieceType.ROOK -> genSliding(file, rank, moves, ROOK_DIRS)
                    PieceType.QUEEN -> genSliding(file, rank, moves, QUEEN_DIRS)
                    PieceType.KING -> {
                        genStep(file, rank, moves, KING_OFFSETS)
                        genCastles(moves)
                    }
                }
            }
        }
        return moves
    }

    private fun genSliding(file: Int, rank: Int, moves: MutableList<Move>, dirs: Array<IntArray>) {
        val from = Square(file, rank)
        for (d in dirs) {
            var f = file + d[0]
            var r = rank + d[1]
            while (f in 0..7 && r in 0..7) {
                val target = pieceAt(f, r)
                if (target == null) {
                    moves.add(Move(from, Square(f, r)))
                } else {
                    if (target.color != sideToMove) moves.add(Move(from, Square(f, r)))
                    break
                }
                f += d[0]
                r += d[1]
            }
        }
    }

    private fun genStep(file: Int, rank: Int, moves: MutableList<Move>, offsets: Array<IntArray>) {
        val from = Square(file, rank)
        for (o in offsets) {
            val f = file + o[0]
            val r = rank + o[1]
            if (f in 0..7 && r in 0..7) {
                val target = pieceAt(f, r)
                if (target == null || target.color != sideToMove) moves.add(Move(from, Square(f, r)))
            }
        }
    }

    private fun genPawn(file: Int, rank: Int, moves: MutableList<Move>) {
        val from = Square(file, rank)
        val dir = if (sideToMove == PieceColor.WHITE) 1 else -1
        val startRank = if (sideToMove == PieceColor.WHITE) 1 else 6
        val promoRank = if (sideToMove == PieceColor.WHITE) 7 else 0
        val oneR = rank + dir

        // Forward pushes.
        if (oneR in 0..7 && pieceAt(file, oneR) == null) {
            addPawnMove(from, Square(file, oneR), oneR == promoRank, moves)
            if (rank == startRank && pieceAt(file, rank + 2 * dir) == null) {
                moves.add(Move(from, Square(file, rank + 2 * dir)))
            }
        }

        // Captures (including en passant).
        for (df in intArrayOf(-1, 1)) {
            val cf = file + df
            val cr = rank + dir
            if (cf !in 0..7 || cr !in 0..7) continue
            val target = pieceAt(cf, cr)
            if (target != null && target.color != sideToMove) {
                addPawnMove(from, Square(cf, cr), cr == promoRank, moves)
            } else if (target == null && enPassantTarget != null &&
                enPassantTarget.file == cf && enPassantTarget.rank == cr
            ) {
                moves.add(Move(from, Square(cf, cr), isEnPassant = true))
            }
        }
    }

    private fun addPawnMove(from: Square, to: Square, isPromotion: Boolean, moves: MutableList<Move>) {
        if (isPromotion) {
            for (t in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                moves.add(Move(from, to, promotion = t))
            }
        } else {
            moves.add(Move(from, to))
        }
    }

    private fun genCastles(moves: MutableList<Move>) {
        val color = sideToMove
        val backRank = if (color == PieceColor.WHITE) 0 else 7
        val king = pieceAt(4, backRank)
        if (king == null || king.type != PieceType.KING || king.color != color) return
        // Cannot castle while in check.
        if (isKingAttacked(color)) return
        val them = color.opposite()
        val kingFrom = Square(4, backRank)

        val kingSide = if (color == PieceColor.WHITE) castling.whiteKing else castling.blackKing
        val queenSide = if (color == PieceColor.WHITE) castling.whiteQueen else castling.blackQueen

        if (kingSide && isRook(7, backRank, color) &&
            pieceAt(5, backRank) == null && pieceAt(6, backRank) == null &&
            !isSquareAttacked(Square(5, backRank), them) && !isSquareAttacked(Square(6, backRank), them)
        ) {
            moves.add(Move(kingFrom, Square(6, backRank), isCastleKingSide = true))
        }
        if (queenSide && isRook(0, backRank, color) &&
            pieceAt(1, backRank) == null && pieceAt(2, backRank) == null && pieceAt(3, backRank) == null &&
            !isSquareAttacked(Square(3, backRank), them) && !isSquareAttacked(Square(2, backRank), them)
        ) {
            moves.add(Move(kingFrom, Square(2, backRank), isCastleQueenSide = true))
        }
    }

    private fun isRook(file: Int, rank: Int, color: PieceColor): Boolean {
        val p = pieceAt(file, rank)
        return p != null && p.type == PieceType.ROOK && p.color == color
    }

    // ---- Attack / check detection ---------------------------------------------------

    /** True if [square] is attacked by any piece of [byColor]. */
    fun isSquareAttacked(square: Square, byColor: PieceColor): Boolean {
        val f = square.file
        val r = square.rank

        // Pawn attacks: a byColor pawn one rank "behind" (relative to its march) hits the square.
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        for (df in intArrayOf(-1, 1)) {
            val pf = f + df
            val pr = r - pawnDir
            if (pf in 0..7 && pr in 0..7) {
                val p = pieceAt(pf, pr)
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
        }

        // Knight.
        for (o in KNIGHT_OFFSETS) {
            val nf = f + o[0]
            val nr = r + o[1]
            if (nf in 0..7 && nr in 0..7) {
                val p = pieceAt(nf, nr)
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // King.
        for (o in KING_OFFSETS) {
            val kf = f + o[0]
            val kr = r + o[1]
            if (kf in 0..7 && kr in 0..7) {
                val p = pieceAt(kf, kr)
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }

        // Sliding: rook/queen orthogonally, bishop/queen diagonally.
        if (slidingHit(f, r, ROOK_DIRS, byColor, PieceType.ROOK)) return true
        if (slidingHit(f, r, BISHOP_DIRS, byColor, PieceType.BISHOP)) return true
        return false
    }

    private fun slidingHit(
        f: Int,
        r: Int,
        dirs: Array<IntArray>,
        byColor: PieceColor,
        straightType: PieceType,
    ): Boolean {
        for (d in dirs) {
            var sf = f + d[0]
            var sr = r + d[1]
            while (sf in 0..7 && sr in 0..7) {
                val p = pieceAt(sf, sr)
                if (p != null) {
                    if (p.color == byColor && (p.type == straightType || p.type == PieceType.QUEEN)) return true
                    break
                }
                sf += d[0]
                sr += d[1]
            }
        }
        return false
    }

    /** True if the piece on [from] attacks [to] (pseudo — ignores pins and whose turn it is). */
    fun attacksFrom(from: Square, to: Square): Boolean {
        val piece = pieceAt(from) ?: return false
        val df = to.file - from.file
        val dr = to.rank - from.rank
        if (df == 0 && dr == 0) return false
        return when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) 1 else -1
                dr == dir && abs(df) == 1
            }
            PieceType.KNIGHT -> (abs(df) == 1 && abs(dr) == 2) || (abs(df) == 2 && abs(dr) == 1)
            PieceType.KING -> abs(df) <= 1 && abs(dr) <= 1
            PieceType.BISHOP -> abs(df) == abs(dr) && clearPath(from, to)
            PieceType.ROOK -> (df == 0 || dr == 0) && clearPath(from, to)
            PieceType.QUEEN -> (abs(df) == abs(dr) || df == 0 || dr == 0) && clearPath(from, to)
        }
    }

    private fun clearPath(from: Square, to: Square): Boolean {
        val stepF = (to.file - from.file).sign
        val stepR = (to.rank - from.rank).sign
        var f = from.file + stepF
        var r = from.rank + stepR
        while (f != to.file || r != to.rank) {
            if (pieceAt(f, r) != null) return false
            f += stepF
            r += stepR
        }
        return true
    }

    private fun kingSquare(color: PieceColor): Square? {
        for (i in 0..63) {
            val p = squares[i]
            if (p != null && p.color == color && p.type == PieceType.KING) return Square(i % 8, i / 8)
        }
        return null
    }

    /** True if [color]'s king is currently attacked. */
    fun isKingAttacked(color: PieceColor): Boolean {
        val kingSq = kingSquare(color) ?: return false
        return isSquareAttacked(kingSq, color.opposite())
    }

    /** True if the side to move is in check. */
    fun isInCheck(): Boolean = isKingAttacked(sideToMove)

    fun isCheckmate(): Boolean = isInCheck() && legalMoves().isEmpty()

    fun isStalemate(): Boolean = !isInCheck() && legalMoves().isEmpty()

    /** Fifty-move rule: 100 halfmoves without a pawn move or capture. */
    fun isFiftyMoveDraw(): Boolean = halfmoveClock >= 100

    /** Neither side can possibly mate: K vs K, or K+minor vs K. */
    fun hasInsufficientMaterial(): Boolean {
        var nonKings = 0
        var allMinor = true
        for (p in squares) {
            if (p == null || p.type == PieceType.KING) continue
            nonKings++
            if (p.type != PieceType.BISHOP && p.type != PieceType.KNIGHT) allMinor = false
        }
        return nonKings == 0 || (nonKings == 1 && allMinor)
    }

    /** No legal moves (mate/stalemate), or a rule-based draw. Threefold repetition is
     *  judged by the caller, since it needs game history a single position doesn't have. */
    fun isGameOver(): Boolean = legalMoves().isEmpty() || isFiftyMoveDraw() || hasInsufficientMaterial()

    // ---- Apply a move ---------------------------------------------------------------

    /**
     * Returns the position after [move]. The move is assumed pseudo-legal; [legalMoves]
     * only ever hands out legal ones. Handles captures, en passant, promotion, castling
     * (incl. the rook hop), castling-right updates, the en passant target and the clocks.
     */
    fun applyMove(move: Move): Position {
        val board = squares.toMutableList()
        val mover = board[move.from.index] ?: error("no piece at ${move.from}")
        val color = mover.color

        // Whether this move captures (needed for the halfmove clock).
        val isCapture = move.isEnPassant || squares[move.to.index] != null

        // En passant removes the pawn beside the destination (same rank as `from`).
        if (move.isEnPassant) {
            board[Square(move.to.file, move.from.rank).index] = null
        }

        // Move the piece (promoting if requested).
        board[move.from.index] = null
        board[move.to.index] = if (move.promotion != null) Piece(color, move.promotion) else mover

        // Castling moves the rook too.
        val br = move.from.rank
        if (move.isCastleKingSide) {
            board[Square(7, br).index] = null
            board[Square(5, br).index] = Piece(color, PieceType.ROOK)
        } else if (move.isCastleQueenSide) {
            board[Square(0, br).index] = null
            board[Square(3, br).index] = Piece(color, PieceType.ROOK)
        }

        // Update castling rights.
        var wk = castling.whiteKing
        var wq = castling.whiteQueen
        var bk = castling.blackKing
        var bq = castling.blackQueen
        if (mover.type == PieceType.KING) {
            if (color == PieceColor.WHITE) {
                wk = false; wq = false
            } else {
                bk = false; bq = false
            }
        }
        // A rook leaving (from) or being captured on (to) a corner clears that side.
        for (sq in listOf(move.from, move.to)) {
            if (sq.rank == 0 && sq.file == 0) wq = false
            if (sq.rank == 0 && sq.file == 7) wk = false
            if (sq.rank == 7 && sq.file == 0) bq = false
            if (sq.rank == 7 && sq.file == 7) bk = false
        }

        // En passant target: only set on a two-square pawn push.
        val newEp: Square? = if (mover.type == PieceType.PAWN && abs(move.to.rank - move.from.rank) == 2) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else {
            null
        }

        val newHalf = if (mover.type == PieceType.PAWN || isCapture) 0 else halfmoveClock + 1
        val newFull = if (color == PieceColor.BLACK) fullmoveNumber + 1 else fullmoveNumber

        return Position(
            squares = board,
            sideToMove = color.opposite(),
            castling = CastlingRights(wk, wq, bk, bq),
            enPassantTarget = newEp,
            halfmoveClock = newHalf,
            fullmoveNumber = newFull,
        )
    }

    // ---- FEN ------------------------------------------------------------------------

    /** Full FEN string for this position. */
    fun toFen(): String {
        val sb = StringBuilder()
        for (rowIndex in 0..7) {
            val rank = 7 - rowIndex
            var empty = 0
            for (file in 0..7) {
                val p = pieceAt(file, rank)
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty); empty = 0
                    }
                    sb.append(p.fenChar)
                }
            }
            if (empty > 0) sb.append(empty)
            if (rowIndex != 7) sb.append('/')
        }
        sb.append(' ').append(if (sideToMove == PieceColor.WHITE) 'w' else 'b')
        sb.append(' ').append(castling.fen())
        sb.append(' ').append(enPassantTarget?.name ?: "-")
        sb.append(' ').append(halfmoveClock)
        sb.append(' ').append(fullmoveNumber)
        return sb.toString()
    }

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun startingPosition(): Position = fromFen(STARTING_FEN)

        /** Parses a full FEN string (placement, side, castling, en passant, clocks). */
        fun fromFen(fen: String): Position {
            val parts = fen.trim().split(Regex("\\s+"))
            require(parts.size >= 4) { "FEN needs at least 4 fields: $fen" }

            val board = arrayOfNulls<Piece>(64).toMutableList()
            val rows = parts[0].split("/")
            require(rows.size == 8) { "FEN placement must have 8 ranks: ${parts[0]}" }
            for ((rowIndex, row) in rows.withIndex()) {
                val rank = 7 - rowIndex
                var file = 0
                for (c in row) {
                    if (c.isDigit()) {
                        file += c - '0'
                    } else {
                        board[rank * 8 + file] = Piece.fromFenChar(c)
                        file++
                    }
                }
                require(file == 8) { "Rank ${rank + 1} does not describe exactly 8 files" }
            }

            val side = if (parts[1] == "w") PieceColor.WHITE else PieceColor.BLACK
            val cr = parts[2]
            val castling = CastlingRights(
                whiteKing = cr.contains('K'),
                whiteQueen = cr.contains('Q'),
                blackKing = cr.contains('k'),
                blackQueen = cr.contains('q'),
            )
            val ep = if (parts[3] == "-") null else Square.fromName(parts[3])
            val half = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val full = parts.getOrNull(5)?.toIntOrNull() ?: 1

            return Position(board, side, castling, ep, half, full)
        }
    }
}
