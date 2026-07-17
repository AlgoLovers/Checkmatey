package com.checkmatey.core.engine

import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.study.StudyGame
import kotlin.math.roundToInt

/** Per-side breakdown of move quality across a game, plus an accuracy score (0–100). */
data class ReviewSummary(val counts: Map<MoveQuality, Int>, val accuracy: Int) {
    fun count(quality: MoveQuality): Int = counts[quality] ?: 0
    val total: Int get() = counts.values.sum()
}

/**
 * Runs the [Annotator] over a whole game — an imported PGN or a game you just played — so you can
 * see, move by move, where it went wrong and what was better. This is the "review your game" tool.
 */
class GameReviewer(private val annotator: Annotator) {

    /** Annotate every move in order (one [MoveAnnotation] per ply). */
    fun review(game: StudyGame): List<MoveAnnotation> =
        game.moves.indices.map { i -> annotator.annotate(game.positions[i], game.moves[i]) }

    /** Aggregate the annotations for the moves [side] made. */
    fun summarize(game: StudyGame, annotations: List<MoveAnnotation>, side: PieceColor): ReviewSummary {
        val mine = annotations.filterIndexed { i, _ -> game.positions[i].sideToMove == side }
        val counts = MoveQuality.entries.associateWith { q -> mine.count { it.quality == q } }
        val accuracy = if (mine.isEmpty()) 100 else (mine.sumOf { weight(it.quality) } / mine.size * 100).roundToInt()
        return ReviewSummary(counts, accuracy)
    }

    private fun weight(quality: MoveQuality): Double = when (quality) {
        MoveQuality.BEST -> 1.0
        MoveQuality.GOOD -> 0.9
        MoveQuality.INACCURACY -> 0.6
        MoveQuality.MISTAKE -> 0.3
        MoveQuality.BLUNDER -> 0.0
    }
}
