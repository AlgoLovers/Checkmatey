package com.checkmatey.core.puzzle

import kotlin.math.pow
import kotlin.math.roundToInt

/** Standard Elo update, used to move the player's puzzle rating after each attempt. */
object Rating {
    private const val K = 32.0
    const val DEFAULT = 800

    /** New rating after attempting a [puzzleRating] puzzle. */
    fun update(userRating: Int, puzzleRating: Int, solved: Boolean): Int {
        val expected = 1.0 / (1.0 + 10.0.pow((puzzleRating - userRating) / 400.0))
        val score = if (solved) 1.0 else 0.0
        return (userRating + K * (score - expected)).roundToInt()
    }
}
