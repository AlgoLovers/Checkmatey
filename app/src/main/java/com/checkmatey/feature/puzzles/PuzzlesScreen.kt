package com.checkmatey.feature.puzzles

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.checkmatey.feature.common.InfoScreen

/** Puzzles tab: tactical puzzles graded by rating (built in later iterations). */
@Composable
fun PuzzlesScreen(modifier: Modifier = Modifier) {
    InfoScreen(
        title = "Puzzles",
        subtitle = "Daily tactics and rated puzzles will live here.",
        modifier = modifier,
    )
}
