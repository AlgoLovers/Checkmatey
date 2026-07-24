package com.checkmatey.core.engine

/**
 * Difficulty levels for the computer opponent.
 *
 * Strength is shaped by [searchDepth] (how far it looks ahead) and [blunderChance] (probability of
 * a random legal move instead of the best one). With quiescence search in the engine, the higher
 * levels play a genuinely strong game; the low levels stay beatable and human-feeling.
 */
enum class BotLevel(
    val displayName: String,
    val approxElo: Int,
    val searchDepth: Int,
    val blunderChance: Double,
) {
    SEEDLING("새싹", 400, 1, 0.60),
    BEGINNER("초급", 700, 2, 0.40),
    INTERMEDIATE("중급", 1000, 3, 0.30),
    CHALLENGER("도전", 1300, 4, 0.20),
    EXPERT("전문가", 1600, 5, 0.0),
    MASTER("마스터", 1900, 6, 0.0);

    companion object {
        /** The level that best matches a player's rating — used for adaptive difficulty. */
        fun forRating(rating: Int): BotLevel = when {
            rating < 550 -> SEEDLING
            rating < 850 -> BEGINNER
            rating < 1100 -> INTERMEDIATE
            rating < 1400 -> CHALLENGER
            rating < 1750 -> EXPERT
            else -> MASTER
        }
    }
}
