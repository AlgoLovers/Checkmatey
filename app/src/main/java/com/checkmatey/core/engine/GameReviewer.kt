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

    /**
     * Puzzle themes worth drilling based on the mistakes/blunders in [annotations] — derived from
     * the reason text of the move that would have been better. Lets the app turn "you missed a
     * fork here" into "practice fork puzzles".
     */
    fun weakThemes(annotations: List<MoveAnnotation>): List<String> {
        val themes = LinkedHashSet<String>()
        for (a in annotations) {
            if (a.quality.ordinal < MoveQuality.MISTAKE.ordinal) continue
            val reason = a.reason
            when {
                "포크" in reason -> themes += "포크"
                "핀" in reason -> themes += "핀"
                "체크메이트" in reason -> themes += "백랭크 메이트"
                "잡" in reason || "이득" in reason -> themes += "기물 이득"
            }
        }
        return themes.toList()
    }

    private fun weight(quality: MoveQuality): Double = when (quality) {
        MoveQuality.BEST -> 1.0
        MoveQuality.GOOD -> 0.9
        MoveQuality.INACCURACY -> 0.6
        MoveQuality.MISTAKE -> 0.3
        MoveQuality.BLUNDER -> 0.0
    }
}
