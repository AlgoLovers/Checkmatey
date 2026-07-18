package com.checkmatey.core.puzzle

import android.content.Context
import com.checkmatey.core.chess.Position
import kotlin.math.abs
import kotlin.random.Random

/**
 * A tactic from the Lichess CC0 puzzle database (curated for beginners in tools/puzzles).
 *
 * [fen] is the position the solver faces (Lichess's setup move is already applied by the build
 * script). [solution] is the forced line in UCI, **solver move first**, then the opponent's forced
 * reply, then the solver again, … so even indices are the moves the player must find. Storing the
 * verified solution (rather than recomputing it) means our engine's strength never limits which
 * puzzles we can offer, and a test replays every sampled line to prove it's legal.
 */
data class Puzzle(
    val id: String,
    val fen: String,
    val solution: List<String>,
    val theme: String,
    val rating: Int,
) {
    val position: Position get() = Position.fromFen(fen)

    /** The move the player must find first, in UCI. */
    val firstMoveUci: String get() = solution.first()
}

/**
 * Loads and serves the bundled puzzle set. The CSV (assets/puzzles.csv) is parsed once and cached
 * for the process; 15k rows load in well under a second and the app never repeats a puzzle.
 */
class PuzzleRepository(private val context: Context) {

    fun all(): List<Puzzle> = cached ?: load(context).also { cached = it }

    fun themes(): List<String> = all().map { it.theme }.distinct()

    /**
     * Picks the next puzzle. Spaced-repetition-lite: a previously-missed puzzle ([reviewIds]) is
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
        val puzzles = all()
        val reviewPool = puzzles.filter { it.id in reviewIds }
        if (reviewPool.isNotEmpty() && random.nextDouble() < 0.4) {
            return reviewPool[random.nextInt(reviewPool.size)]
        }
        var pool = puzzles.filter { it.id !in solved }
        if (themeFilter != null) pool = pool.filter { it.theme == themeFilter }.ifEmpty { pool }
        if (pool.isEmpty()) pool = puzzles
        // Among puzzles closest to the player's rating, pick randomly for variety.
        val closest = pool.sortedBy { abs(it.rating - rating) }.take(12)
        return closest[random.nextInt(closest.size)]
    }

    companion object {
        @Volatile private var cached: List<Puzzle>? = null

        private fun load(context: Context): List<Puzzle> =
            context.assets.open("puzzles.csv").bufferedReader().useLines { lines ->
                lines.drop(1).mapNotNull { parse(it) }.toList()
            }

        /** One CSV row -> Puzzle. Columns never contain commas (FEN/UCI use spaces), so split is safe. */
        private fun parse(line: String): Puzzle? {
            val c = line.split(",")
            if (c.size < 5) return null
            val rating = c[3].toIntOrNull() ?: return null
            val solution = c[2].split(" ").filter { it.isNotEmpty() }
            if (solution.isEmpty()) return null
            return Puzzle(id = c[0], fen = c[1], solution = solution, theme = c[4], rating = rating)
        }
    }
}
