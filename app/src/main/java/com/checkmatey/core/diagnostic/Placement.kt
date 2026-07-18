package com.checkmatey.core.diagnostic

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Diagnostic placement — estimates a new player's puzzle rating from a short adaptive quiz, so the
 * app can start them at the right level instead of guessing. This is the diagnostic half of the
 * adaptive-learning moat and the natural extension of "everything is rating-based": each question is
 * served near the running estimate, and an Elo update with a **decaying K** moves the estimate fast
 * at first, then fine-tunes. Pure, deterministic Kotlin — the whole thing is unit-tested.
 */
object Placement {
    /** Number of questions in a placement quiz. */
    const val LENGTH = 7

    /** Where the quiz starts before any evidence — the beginner band midpoint. */
    const val START = 800

    /** Ratings never leave the beginner band this app curates puzzles for. */
    const val MIN = 400
    const val MAX = 1600

    /** The difficulty (puzzle rating) to probe next, given the current [estimate]. */
    fun target(estimate: Int): Int = estimate.coerceIn(MIN, MAX)

    /**
     * Update the [estimate] after a question of [questionRating] was [solved] or not. [answered] is
     * how many questions came before this one (0-based): K shrinks as it grows, so early answers
     * swing the estimate hard and later answers only nudge it.
     */
    fun update(estimate: Int, questionRating: Int, solved: Boolean, answered: Int): Int {
        val k = (120.0 - answered * 14).coerceAtLeast(24.0)
        val expected = 1.0 / (1.0 + 10.0.pow((questionRating - estimate) / 400.0))
        val score = if (solved) 1.0 else 0.0
        return (estimate + k * (score - expected)).roundToInt().coerceIn(MIN, MAX)
    }

    fun isDone(answered: Int): Boolean = answered >= LENGTH
}
