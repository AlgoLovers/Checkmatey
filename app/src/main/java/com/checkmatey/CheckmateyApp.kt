package com.checkmatey

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.checkmatey.feature.play.PlayScreen
import com.checkmatey.feature.profile.ProfileScreen
import com.checkmatey.feature.puzzles.PuzzlesScreen
import com.checkmatey.feature.study.StudyScreen

/** Top-level tabs. */
private enum class TopDestination(val label: String, val icon: ImageVector) {
    LEARN("명국", Icons.Filled.Star),
    PLAY("Play", Icons.Filled.PlayArrow),
    PUZZLES("퍼즐", Icons.Filled.Search),
    PROFILE("분석", Icons.Filled.Person),
}

/**
 * App root. [NavigationSuiteScaffold] adapts automatically to the window size:
 * a bottom navigation bar on phones (compact width) and a navigation rail/drawer
 * on tablets and foldables (medium/expanded width). This is how one code path
 * supports both phone and tablet.
 */
@Composable
fun CheckmateyApp() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(TopDestination.PLAY.ordinal) }
    val current = TopDestination.entries[selectedIndex]

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopDestination.entries.forEach { dest ->
                item(
                    selected = dest == current,
                    onClick = { selectedIndex = dest.ordinal },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                )
            }
        },
    ) {
        // Keep content clear of the status bar / display cutout / (tablet) nav rail.
        // The bottom nav bar's inset is handled by the scaffold itself.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            when (current) {
                TopDestination.LEARN -> StudyScreen()
                TopDestination.PLAY -> PlayScreen()
                TopDestination.PUZZLES -> PuzzlesScreen()
                TopDestination.PROFILE -> ProfileScreen()
            }
        }
    }
}
