package com.checkmatey

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.checkmatey.core.plan.StepTarget
import com.checkmatey.data.UserStore
import com.checkmatey.feature.diagnostic.PlacementScreen
import com.checkmatey.feature.home.HomeScreen
import com.checkmatey.feature.skilltree.SkillTreeScreen
import com.checkmatey.feature.lesson.LessonScreen
import com.checkmatey.feature.onboarding.OnboardingScreen
import com.checkmatey.feature.play.PlayScreen
import com.checkmatey.feature.profile.ProfileScreen
import com.checkmatey.feature.puzzles.PuzzlesScreen
import com.checkmatey.feature.study.StudyScreen

/** Top-level tabs. */
private enum class TopDestination(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Filled.Home),
    LESSONS("레슨", Icons.Filled.Edit),
    LEARN("명국", Icons.Filled.Star),
    PLAY("대국", Icons.Filled.PlayArrow),
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
    val context = LocalContext.current
    val store = remember { UserStore(context) }
    var onboarded by rememberSaveable { mutableStateOf(store.onboardingSeen) }
    if (!onboarded) {
        OnboardingScreen(onDone = { store.onboardingSeen = true; onboarded = true })
        return
    }

    // Full-screen diagnostic placement overlay, launched from the home invite.
    var showPlacement by rememberSaveable { mutableStateOf(false) }
    if (showPlacement) {
        PlacementScreen(onDone = { showPlacement = false })
        return
    }

    // Full-screen learning-map overlay, launched from the home card.
    var showSkillTree by rememberSaveable { mutableStateOf(false) }
    if (showSkillTree) {
        SkillTreeScreen(onBack = { showSkillTree = false })
        return
    }

    var selectedIndex by rememberSaveable { mutableIntStateOf(TopDestination.HOME.ordinal) }
    val current = TopDestination.entries[selectedIndex]

    fun goTo(target: StepTarget) {
        selectedIndex = when (target) {
            StepTarget.LESSON -> TopDestination.LESSONS
            StepTarget.PUZZLE -> TopDestination.PUZZLES
            StepTarget.PLAY -> TopDestination.PLAY
            StepTarget.REVIEW -> TopDestination.PROFILE
        }.ordinal
    }

    // Apply the status bar / cutout / (tablet) rail insets *around* the scaffold, so they aren't
    // already consumed by the time content is laid out. The bottom nav bar keeps the bottom inset.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
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
            when (current) {
                TopDestination.HOME -> HomeScreen(
                    onGo = ::goTo,
                    onPlacement = { showPlacement = true },
                    onSkillTree = { showSkillTree = true },
                )
                TopDestination.LESSONS -> LessonScreen()
                TopDestination.LEARN -> StudyScreen()
                TopDestination.PLAY -> PlayScreen()
                TopDestination.PUZZLES -> PuzzlesScreen()
                TopDestination.PROFILE -> ProfileScreen()
            }
        }
    }
}
