package com.checkmatey.core.puzzle

import com.checkmatey.core.chess.Position
import kotlin.math.abs
import kotlin.random.Random

/**
 * A one-move tactic: from [fen], the side to move has a clearly best move. The solution isn't
 * stored — the engine computes it — so the data can't drift out of sync with the rules; a test
 * verifies every position really does contain a decisive tactic.
 */
data class Puzzle(val id: String, val fen: String, val theme: String, val rating: Int) {
    val position: Position get() = Position.fromFen(fen)
}

object Puzzles {

    val ALL: List<Puzzle> = listOf(
        Puzzle("p1", "4k3/8/8/8/3q4/8/8/3RK3 w - - 0 1", "기물 이득", 800),
        Puzzle("p2", "4k3/8/8/3b4/8/8/8/3RK3 w - - 0 1", "기물 이득", 600),
        Puzzle("p3", "4k3/8/8/8/8/8/4r3/4KR2 w - - 0 1", "기물 이득", 650),
        Puzzle("p4", "3qk3/8/8/8/8/8/8/3RK3 w - - 0 1", "기물 이득", 950),
        Puzzle("p5", "k3q3/8/4N3/8/8/8/8/6K1 w - - 0 1", "포크", 1100),
        Puzzle("p6", "6k1/5ppp/8/8/8/8/8/R6K w - - 0 1", "백랭크 메이트", 900),
        Puzzle("p7", "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1", "백랭크 메이트", 850),
        Puzzle("p8", "2r3k1/5ppp/8/8/8/8/5PPP/2Q3K1 w - - 0 1", "백랭크", 1000),
        Puzzle("p9", "6k1/5ppp/8/8/8/8/5PPP/1Q4K1 w - - 0 1", "백랭크 메이트", 1050),
        Puzzle("p10", "4k3/8/8/8/7b/8/6N1/4K3 w - - 0 1", "기물 이득", 950),
        Puzzle("p11", "3r2k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1", "백랭크 메이트", 1250),
        Puzzle("p12", "q3k3/8/4N3/8/8/8/8/4K3 w - - 0 1", "포크", 1150),
        Puzzle("p13", "2b1k3/8/1N6/8/8/8/8/4K3 w - - 0 1", "기물 이득", 900),
    )

    /**
     * Picks the next puzzle. Spaced repetition: a previously-missed puzzle ([reviewIds]) is
     * re-served part of the time. Weakness focus: [themeFilter] restricts to one theme. Otherwise
     * an unsolved puzzle near [rating] is chosen so difficulty tracks the player.
     */
    fun next(
        rating: Int,
        solved: Set<String>,
        random: Random = Random.Default,
        reviewIds: List<String> = emptyList(),
        themeFilter: String? = null,
    ): Puzzle {
        val reviewPool = ALL.filter { it.id in reviewIds }
        if (reviewPool.isNotEmpty() && random.nextDouble() < 0.5) {
            return reviewPool[random.nextInt(reviewPool.size)]
        }
        var pool = ALL.filter { it.id !in solved }
        if (themeFilter != null) pool = pool.filter { it.theme == themeFilter }.ifEmpty { pool }
        if (pool.isEmpty()) pool = ALL
        val closest = pool.sortedBy { abs(it.rating - rating) }.take(3)
        return closest[random.nextInt(closest.size)]
    }
}
