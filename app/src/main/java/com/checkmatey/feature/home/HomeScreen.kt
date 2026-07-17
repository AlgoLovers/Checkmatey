package com.checkmatey.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkmatey.core.plan.Planner
import com.checkmatey.core.plan.Progress
import com.checkmatey.core.plan.StepTarget
import com.checkmatey.data.UserStore
import com.checkmatey.ui.Sparkline

/**
 * Home — the app's front door. It answers "what should I do right now?" with one clear next step,
 * plus a small progress view, instead of dropping a beginner in front of five tabs.
 */
@Composable
fun HomeScreen(onGo: (StepTarget) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { UserStore(context) }

    val games = store.recentGames
    val progress = Progress(
        lessonsDone = store.completedLessons.size,
        lessonsTotal = 12,
        puzzlesSolved = store.solvedCount,
        gamesPlayed = games.size,
        hasWeakThemeToDrill = store.recommendedThemes.isNotEmpty(),
        unreviewedGame = games.isNotEmpty() && !store.reviewedLatestGame,
    )
    val step = Planner.next(progress)

    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("체스, 오늘도 한 걸음", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Checkmatey", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        // The one thing to do now.
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("👉 지금 할 일", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(step.subtitle, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onGo(step.target) }) { Text(step.cta) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("내 레이팅", style = MaterialTheme.typography.labelMedium)
                        Text("${store.puzzleRating}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Sparkline(
                        values = store.ratingHistory,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(40.dp).fillMaxWidth(0.5f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "레슨 ${progress.lessonsDone}/${progress.lessonsTotal}  ·  퍼즐 ${progress.puzzlesSolved}문제  ·  대국 ${progress.gamesPlayed}판  ·  연속 최고 ${store.bestStreak}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "아래 탭에서 언제든 원하는 걸 골라도 됩니다 — 레슨 · 명국 · 대국 · 퍼즐 · 분석",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
