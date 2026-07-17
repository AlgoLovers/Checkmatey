package com.checkmatey.core.study

import com.checkmatey.core.chess.Move
import com.checkmatey.core.chess.Position
import com.checkmatey.core.chess.parseSan
import com.checkmatey.core.chess.toSan

/**
 * A master game turned into something the UI can replay: the moves, their SAN, and the position
 * after every ply. [positions] has [plyCount] + 1 entries — index 0 is the start.
 */
data class StudyGame(
    val meta: PgnGame,
    val moves: List<Move>,
    val sans: List<String>,
    val positions: List<Position>,
) {
    val plyCount: Int get() = moves.size
    val title: String get() = "${meta.white} vs ${meta.black}"
    val subtitle: String get() = listOf(meta.event, meta.year, meta.result).filter { it.isNotEmpty() }.joinToString(" · ")

    /** Position after [ply] half-moves (0 = start). */
    fun positionAt(ply: Int): Position = positions[ply.coerceIn(0, positions.lastIndex)]

    /** The move played from position [ply] (i.e. the move that produces position ply+1). */
    fun moveAt(ply: Int): Move? = moves.getOrNull(ply)
}

object StudyGames {

    /** All bundled master games, ready to replay. */
    fun all(): List<StudyGame> = PgnParser.parse(MasterGames.PGN).map { build(it) }

    fun build(pgn: PgnGame): StudyGame {
        var pos = Position.startingPosition()
        val moves = ArrayList<Move>()
        val sans = ArrayList<String>()
        val positions = ArrayList<Position>().apply { add(pos) }
        for (san in pgn.sanMoves) {
            val move = pos.parseSan(san) ?: break // clean data never breaks; guards bad input
            sans.add(pos.toSan(move))
            pos = pos.applyMove(move)
            moves.add(move)
            positions.add(pos)
        }
        return StudyGame(pgn, moves, sans, positions)
    }
}
