package com.checkmatey.core.daily

import com.checkmatey.core.puzzle.Puzzle

/**
 * The "puzzle of the day" — one tactic everyone gets on a given date, so a daily reminder has a
 * concrete thing to pull the player back to. Deterministic from the epoch day (a multiplicative hash
 * spreads consecutive days across the set instead of walking it in order), so it's the same for the
 * whole day and identical for every install — pure and unit-tested.
 */
object DailyPuzzle {
    fun forDay(epochDay: Long, puzzles: List<Puzzle>): Puzzle {
        require(puzzles.isNotEmpty()) { "no puzzles to pick a daily from" }
        val hash = epochDay * 2654435761L // Knuth multiplicative — scatters adjacent days
        val idx = ((hash % puzzles.size) + puzzles.size) % puzzles.size
        return puzzles[idx.toInt()]
    }
}
