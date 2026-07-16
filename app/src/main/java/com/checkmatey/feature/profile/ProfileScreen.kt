package com.checkmatey.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.checkmatey.feature.common.InfoScreen

/** Profile tab: progress, rating and settings (built in later iterations). */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    InfoScreen(
        title = "Profile",
        subtitle = "Your progress, rating and settings will live here.",
        modifier = modifier,
    )
}
