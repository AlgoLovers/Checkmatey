package com.checkmatey.feature.learn

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.checkmatey.feature.common.InfoScreen

/** Learn tab: guided lessons on rules, openings and tactics (built in later iterations). */
@Composable
fun LearnScreen(modifier: Modifier = Modifier) {
    InfoScreen(
        title = "Learn",
        subtitle = "Step-by-step chess lessons will live here.",
        modifier = modifier,
    )
}
