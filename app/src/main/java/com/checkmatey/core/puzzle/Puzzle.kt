package com.checkmatey.core.puzzle

import com.checkmatey.core.chess.Move
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
 *
 * Pure Kotlin (no Android): the asset read lives in `data/PuzzleAssets`, so this and the repository
 * are JVM-unit-testable.
 */
data class Puzzle(
    val id: String,
    val fen: String,
    val solution: List<String>,
    val theme: String,
    val rating: Int,
) {
    val position: Position get() = Position.fromFen(fen)

    /**
     * Is [move] the solver move expected at [step]? Compares origin+destination (the UCI prefix),
     * ignoring which piece a promotion turns into — the board applies the solution's exact move.
     * Pure and deterministic, so grading is covered by tests rather than recomputed in the UI.
     */
    fun isSolverMove(step: Int, move: Move): Boolean {
        val uci = solution.getOrNull(step) ?: return false
        return move.uci().take(4) == uci.take(4)
    }

    companion object {
        /** One CSV row -> Puzzle. Columns never contain commas (FEN/UCI use spaces), so split is safe. */
        fun parse(line: String): Puzzle? {
            val c = line.split(",")
            if (c.size < 5) return null
            val rating = c[3].toIntOrNull() ?: return null
            val solution = c[2].split(" ").filter { it.isNotEmpty() }
            if (solution.isEmpty()) return null
            return Puzzle(id = c[0], fen = c[1], solution = solution, theme = c[4], rating = rating)
        }
    }
}

/**
 * Serves the bundled puzzle set. Pure: it is handed the parsed [puzzles] (see `data/PuzzleAssets`
 * for the Android asset load), so difficulty selection and lookups are JVM-testable.
 */
class PuzzleRepository(private val puzzles: List<Puzzle>) {

    private val index: Map<String, Puzzle> by lazy { puzzles.associateBy { it.id } }

    fun all(): List<Puzzle> = puzzles

    fun themes(): List<String> = puzzles.map { it.theme }.distinct()

    /** Looks up a puzzle by id — used to re-serve a spaced-repetition review card (see core/srs). */
    fun byId(id: String): Puzzle? = index[id]

    /**
     * Picks the next *new* puzzle. Weakness focus: [themeFilter] restricts to one theme. Otherwise
     * an unsolved puzzle near [rating] is chosen so difficulty tracks the player. Review of missed
     * puzzles is handled separately by the SM-2 scheduler (core/srs), not here.
     */
    fun next(
        rating: Int,
        solved: Set<String>,
        random: Random = Random.Default,
        themeFilter: String? = null,
    ): Puzzle {
        var pool = puzzles.filter { it.id !in solved }
        if (themeFilter != null) pool = pool.filter { it.theme == themeFilter }.ifEmpty { pool }
        if (pool.isEmpty()) pool = puzzles
        // Among puzzles closest to the player's rating, pick randomly for variety.
        val closest = pool.sortedBy { abs(it.rating - rating) }.take(12)
        return closest[random.nextInt(closest.size)]
    }
}
