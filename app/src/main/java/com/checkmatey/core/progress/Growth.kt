package com.checkmatey.core.progress

/**
 * How well the player does at one tactical theme — attempts and how many were solved. [rate] is the
 * success percentage, used to rank strengths and weaknesses on the growth dashboard.
 */
data class ThemeMastery(val theme: String, val attempts: Int, val solved: Int) {
    val rate: Int get() = if (attempts == 0) 0 else solved * 100 / attempts
}

/**
 * A snapshot of the player's improvement — the whole point of a learning app and the reason to keep
 * subscribing: it makes progress *visible*. Rating gained since they started, totals, mastery of the
 * curriculum, the review deck, and which themes they're strong/weak at. Pure data assembled by
 * [Growth.report] so it stays testable and Android-free.
 */
data class GrowthReport(
    val rating: Int,
    val startRating: Int,
    val ratingDelta: Int,
    val puzzlesSolved: Int,
    val lessonsDone: Int,
    val lessonsTotal: Int,
    val gamesPlayed: Int,
    val bestStreak: Int,
    val masteryPercent: Int,
    val dueReviews: Int,
    val reviewDeck: Int,
    val strengths: List<ThemeMastery>,
    val weaknesses: List<ThemeMastery>,
    val ratingHistory: List<Int>,
)

object Growth {
    /** Ignore themes with too few attempts — a 1/1 fluke shouldn't read as "100% mastered". */
    const val MIN_ATTEMPTS = 3

    /** Themes with enough attempts, ranked by success rate (highest first). */
    fun themeMastery(stats: Map<String, Pair<Int, Int>>, minAttempts: Int = MIN_ATTEMPTS): List<ThemeMastery> =
        stats.filter { it.value.first >= minAttempts }
            .map { ThemeMastery(it.key, it.value.first, it.value.second) }
            .sortedWith(compareByDescending<ThemeMastery> { it.rate }.thenByDescending { it.attempts })

    fun report(
        rating: Int,
        ratingHistory: List<Int>,
        puzzlesSolved: Int,
        lessonsDone: Int,
        lessonsTotal: Int,
        gamesPlayed: Int,
        bestStreak: Int,
        masteryPercent: Int,
        dueReviews: Int,
        reviewDeck: Int,
        themeStats: Map<String, Pair<Int, Int>>,
    ): GrowthReport {
        val ranked = themeMastery(themeStats)
        val strengths = ranked.take(3)
        // Weakest first, and never repeat a theme already shown as a strength.
        val weaknesses = ranked.asReversed().filter { it !in strengths }.take(3)
        val start = ratingHistory.firstOrNull() ?: rating
        return GrowthReport(
            rating = rating,
            startRating = start,
            ratingDelta = rating - start,
            puzzlesSolved = puzzlesSolved,
            lessonsDone = lessonsDone,
            lessonsTotal = lessonsTotal,
            gamesPlayed = gamesPlayed,
            bestStreak = bestStreak,
            masteryPercent = masteryPercent,
            dueReviews = dueReviews,
            reviewDeck = reviewDeck,
            strengths = strengths,
            weaknesses = weaknesses,
            ratingHistory = ratingHistory,
        )
    }
}
