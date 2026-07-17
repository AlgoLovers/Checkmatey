package com.checkmatey.core.engine

import com.checkmatey.core.chess.PieceColor
import com.checkmatey.core.study.StudyGames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameReviewerTest {

    private val reviewer = GameReviewer(Annotator(KotlinMinimaxEngine(), depth = 2))
    private val opera = StudyGames.all().first { it.meta.white.contains("Morphy") }

    @Test
    fun oneAnnotationPerMove() {
        assertEquals(opera.plyCount, reviewer.review(opera).size)
    }

    @Test
    fun findsMistakesInADecisiveGame() {
        // The Opera Game is a brilliancy against weak defence — the losing side errs.
        val annotations = reviewer.review(opera)
        assertTrue(annotations.any { it.quality.ordinal >= MoveQuality.MISTAKE.ordinal })
    }

    @Test
    fun perSideSummariesCoverEveryMove() {
        val annotations = reviewer.review(opera)
        val white = reviewer.summarize(opera, annotations, PieceColor.WHITE)
        val black = reviewer.summarize(opera, annotations, PieceColor.BLACK)
        assertEquals(opera.plyCount, white.total + black.total)
        assertTrue(white.accuracy in 0..100 && black.accuracy in 0..100)
    }
}
