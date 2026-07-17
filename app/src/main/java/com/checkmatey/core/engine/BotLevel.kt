package com.checkmatey.core.engine

/**
 * Difficulty levels for the computer opponent, targeting up to ~1200.
 *
 * Strength is shaped by two knobs: [searchDepth] (how far it looks ahead) and
 * [blunderChance] (probability of playing a random legal move instead of the best one),
 * which makes weaker levels feel human by occasionally hanging material.
 *
 * These are approximate bands, not exact Elo. They can be recalibrated from real results,
 * and later a stronger engine (e.g. Stockfish) can back any level behind the Engine interface.
 */
enum class BotLevel(
    val displayName: String,
    val approxElo: Int,
    val searchDepth: Int,
    val blunderChance: Double,
) {
    SEEDLING("새싹", 400, 1, 0.60),
    BEGINNER("초급", 700, 2, 0.35),
    INTERMEDIATE("중급", 1000, 3, 0.15),
    CHALLENGER("도전", 1200, 4, 0.05);

    companion object {
        /** The level that best matches a player's rating — used for adaptive difficulty. */
        fun forRating(rating: Int): BotLevel = when {
            rating < 550 -> SEEDLING
            rating < 850 -> BEGINNER
            rating < 1100 -> INTERMEDIATE
            else -> CHALLENGER
        }
    }
}
