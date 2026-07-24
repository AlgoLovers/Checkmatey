package com.checkmatey.core.engine

import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.Position

/**
 * A small, curated book of the openings a beginner should actually meet — mainline moves for both
 * sides of the classics (Italian, Spanish, Sicilian, French, Caro-Kann, Queen's Gambit, London…).
 *
 * Two jobs:
 * 1. **Variety & realism for the bot** — in a book position the bot picks among book moves instead
 *    of always playing the engine's single favourite, so every game starts differently and the
 *    student experiences real openings.
 * 2. **Teaching labels for the coach** — when the student plays a book move, the coach can name it
 *    ("이탈리안 게임 정석") instead of a generic evaluation.
 *
 * Pure Kotlin, built once from UCI lines; every entry is validated legal by the data test.
 */
object OpeningBook {

    /** Opening name to its mainline (UCI from the start position). Both colours' moves teach. */
    private val LINES: List<Pair<String, String>> = listOf(
        "이탈리안 게임" to "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6 d2d3 d7d6",
        "투 나이트 디펜스" to "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 d2d3 f8c5",
        "스페인 게임 (루이 로페즈)" to "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1 f8e7",
        "스카치 게임" to "e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 g8f6",
        "시실리안 디펜스" to "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6",
        "시실리안 (닫힌 변화)" to "e2e4 c7c5 b1c3 b8c6 g2g3 g7g6 f1g2 f8g7",
        "프렌치 디펜스" to "e2e4 e7e6 d2d4 d7d5 b1c3 g8f6 e4e5 f6d7",
        "카로칸 디펜스" to "e2e4 c7c6 d2d4 d7d5 b1c3 d5e4 c3e4 c8f5",
        "스칸디나비안 디펜스" to "e2e4 d7d5 e4d5 d8d5 b1c3 d5a5 d2d4 g8f6",
        "퀸즈 갬빗 거절" to "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7",
        "슬라브 디펜스" to "d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3 d5c4",
        "런던 시스템" to "d2d4 d7d5 g1f3 g8f6 c1f4 e7e6 e2e3 f8d6",
        "킹즈 인디언 디펜스" to "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6",
        "님조 인디언 디펜스" to "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4",
        "잉글리시 오프닝" to "c2c4 e7e5 b1c3 g8f6 g1f3 b8c6 g2g3 d7d5",
    )

    /** One book option from a position: the move to play and the opening it belongs to. */
    data class BookMove(val uci: String, val opening: String)

    // Position (first four FEN fields) -> distinct book continuations.
    private val book: Map<String, List<BookMove>> by lazy {
        val map = HashMap<String, MutableList<BookMove>>()
        for ((name, line) in LINES) {
            var pos = Position.startingPosition()
            for (uci in line.split(" ")) {
                val move = pos.findMove(uci) ?: break // guarded by the data test; never break silently in prod
                val key = key(pos)
                val entries = map.getOrPut(key) { mutableListOf() }
                if (entries.none { it.uci == uci }) entries.add(BookMove(uci, name))
                pos = pos.applyMove(move)
            }
        }
        map
    }

    /** All book continuations from [position] (empty once out of book). */
    fun moves(position: Position): List<BookMove> = book[key(position)].orEmpty()

    /** The opening name if playing [move] from [position] follows a book line, else null. */
    fun openingOf(position: Position, move: Move): String? =
        moves(position).firstOrNull { it.uci == move.uci() }?.opening

    val lineCount: Int get() = LINES.size

    /** Every line, for the data test to replay. */
    internal fun allLines(): List<Pair<String, String>> = LINES

    private fun key(position: Position): String = position.toFen().split(" ").take(4).joinToString(" ")
}
