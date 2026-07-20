package com.checkmatey.data

import android.content.Context
import com.checkmatey.core.puzzle.Puzzle

/**
 * The Android side of the puzzle set: reads the bundled CSV (assets/puzzles.csv) and parses it into
 * pure [Puzzle] rows. Kept out of `core/puzzle` so that package stays Android-free and JVM-testable.
 * Parsed once and cached for the process — 15k rows load in well under a second.
 */
object PuzzleAssets {

    @Volatile private var cached: List<Puzzle>? = null

    fun load(context: Context): List<Puzzle> =
        cached ?: parse(context).also { cached = it }

    private fun parse(context: Context): List<Puzzle> =
        context.assets.open("puzzles.csv").bufferedReader().useLines { lines ->
            lines.drop(1).mapNotNull { Puzzle.parse(it) }.toList()
        }
}
