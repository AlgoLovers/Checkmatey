package com.checkmatey.core.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthTest {

    @Test
    fun masteryIgnoresLowAttemptThemesAndRanksByRate() {
        val stats = mapOf(
            "포크" to (10 to 9),   // 90%
            "핀" to (10 to 3),     // 30%
            "스큐어" to (2 to 2),  // 100% but too few attempts -> dropped
        )
        val ranked = Growth.themeMastery(stats)
        assertEquals(listOf("포크", "핀"), ranked.map { it.theme })
        assertEquals(90, ranked.first().rate)
    }

    @Test
    fun strengthsAndWeaknessesDoNotOverlap() {
        val stats = mapOf(
            "포크" to (10 to 10), "핀" to (10 to 8), "스큐어" to (10 to 6),
            "백랭크 메이트" to (10 to 4), "질식 메이트" to (10 to 1),
        )
        val r = Growth.report(
            rating = 1000, ratingHistory = listOf(800, 1000), puzzlesSolved = 50,
            lessonsDone = 6, lessonsTotal = 12, gamesPlayed = 4, bestStreak = 7,
            masteryPercent = 40, dueReviews = 2, reviewDeck = 9, themeStats = stats,
        )
        assertEquals(listOf("포크", "핀", "스큐어"), r.strengths.map { it.theme })
        assertEquals(listOf("질식 메이트", "백랭크 메이트"), r.weaknesses.map { it.theme })
        val overlap = r.strengths.map { it.theme }.intersect(r.weaknesses.map { it.theme }.toSet())
        assertTrue("strengths and weaknesses must not overlap", overlap.isEmpty())
    }

    @Test
    fun ratingDeltaComesFromHistoryStart() {
        val r = Growth.report(
            rating = 1120, ratingHistory = listOf(800, 900, 1120), puzzlesSolved = 30,
            lessonsDone = 3, lessonsTotal = 12, gamesPlayed = 1, bestStreak = 4,
            masteryPercent = 20, dueReviews = 0, reviewDeck = 0, themeStats = emptyMap(),
        )
        assertEquals(800, r.startRating)
        assertEquals(320, r.ratingDelta)
    }

    @Test
    fun emptyHistoryFallsBackToCurrentRatingSoDeltaIsZero() {
        val r = Growth.report(
            rating = 800, ratingHistory = emptyList(), puzzlesSolved = 0,
            lessonsDone = 0, lessonsTotal = 12, gamesPlayed = 0, bestStreak = 0,
            masteryPercent = 0, dueReviews = 0, reviewDeck = 0, themeStats = emptyMap(),
        )
        assertEquals(800, r.startRating)
        assertEquals(0, r.ratingDelta)
        assertTrue(r.strengths.isEmpty() && r.weaknesses.isEmpty())
    }
}
