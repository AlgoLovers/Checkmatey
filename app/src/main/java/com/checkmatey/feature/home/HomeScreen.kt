package com.checkmatey.feature.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.checkmatey.core.habit.DailyGoal
import com.checkmatey.reminder.DailyReminder
import com.checkmatey.core.plan.Planner
import com.checkmatey.core.plan.Progress
import com.checkmatey.core.plan.StepTarget
import com.checkmatey.core.skilltree.SkillProgress
import com.checkmatey.core.skilltree.SkillTree
import com.checkmatey.data.UserStore
import com.checkmatey.ui.Sparkline
import com.checkmatey.ui.components.GradientPrimaryButton
import com.checkmatey.ui.components.StatTile
import com.checkmatey.ui.theme.Accent
import com.checkmatey.ui.theme.Amber
import com.checkmatey.ui.theme.Info

/**
 * Home — the app's front door. It answers "what should I do right now?" with one clear next step,
 * plus a small progress view, instead of dropping a beginner in front of five tabs.
 */
@Composable
fun HomeScreen(
    onGo: (StepTarget) -> Unit,
    onPlacement: () -> Unit,
    onSkillTree: () -> Unit,
    onDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember { UserStore(context) }

    val games = store.recentGames
    val today = remember { System.currentTimeMillis() / 86_400_000L }
    val progress = Progress(
        lessonsDone = store.completedLessons.size,
        lessonsTotal = 12,
        puzzlesSolved = store.solvedCount,
        gamesPlayed = games.size,
        hasWeakThemeToDrill = store.recommendedThemes.isNotEmpty(),
        unreviewedGame = games.isNotEmpty() && !store.reviewedLatestGame,
        dueReviews = store.dueReviewCount(today),
    )
    val step = Planner.next(progress)

    // Daily reminder toggle: on Android 13+ enabling it asks for the notification permission first.
    var reminderOn by remember { mutableStateOf(store.reminderOn && DailyReminder.hasPermission(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminderOn = granted
        store.reminderOn = granted
        if (granted) DailyReminder.schedule(context) else DailyReminder.cancel(context)
    }
    fun toggleReminder(on: Boolean) {
        if (on && !DailyReminder.hasPermission(context)) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            reminderOn = on
            store.reminderOn = on
            if (on) DailyReminder.schedule(context) else DailyReminder.cancel(context)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("체스, 오늘도 한 걸음", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Checkmatey", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        // The one thing to do now — soft brand card with a raised gradient CTA.
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("👉 지금 할 일", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text(step.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(step.subtitle, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(18.dp))
                GradientPrimaryButton(onClick = { onGo(step.target) }, modifier = Modifier.fillMaxWidth()) {
                    Text(step.cta, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Daily habit — a streak that only survives if you practise every day, and today's goal.
        val daily = store.dailyState(today)
        val goalMet = DailyGoal.metToday(daily)
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = { onGo(StepTarget.PUZZLE) },
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (goalMet) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (goalMet) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("🔥", style = MaterialTheme.typography.headlineMedium)
                Column(Modifier.weight(1f)) {
                    Text(
                        if (daily.dayStreak > 0) "${daily.dayStreak}일 연속 학습 중!" else "오늘 시작해 연속 기록을 만들어요",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (daily.solvedToday.coerceAtMost(DailyGoal.GOAL)) / DailyGoal.GOAL.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (goalMet) "✓ 오늘 목표 달성! (${daily.solvedToday}문제)" else "오늘의 목표  ${daily.solvedToday}/${DailyGoal.GOAL} 퍼즐",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Puzzle of the day — one shared tactic, tapped from here into the Puzzles tab.
        val dailyDone = store.dailySolvedDay == today
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = { store.pendingDailyDay = today; onGo(StepTarget.PUZZLE) },
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("🧩", style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f)) {
                    Text("오늘의 퍼즐", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (dailyDone) "✓ 오늘의 퍼즐 완료 — 내일 또 만나요" else "매일 새로운 한 문제로 감각을 유지해요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(if (dailyDone) "✓" else "풀기 →", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Reminder — the trigger that brings the player back each day (the retention lever).
        val h = store.reminderHour
        val timeLabel = if (h < 12) "오전 ${if (h == 0) 12 else h}시" else "오후 ${if (h == 12) 12 else h - 12}시"
        Spacer(Modifier.height(12.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("🔔 매일 학습 알림", style = MaterialTheme.typography.titleSmall)
                    Text("$timeLabel 에 오늘의 퍼즐을 알려드려요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = reminderOn, onCheckedChange = { toggleReminder(it) })
            }
        }

        // One-time invite: place the player at the right level if they already know chess.
        if (!store.placementDone) {
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = onPlacement,
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("🎯", style = MaterialTheme.typography.headlineSmall)
                    Column(Modifier.weight(1f)) {
                        Text("실력 진단으로 시작점 찾기", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "체스를 둘 줄 안다면 7문제로 딱 맞는 난이도부터 — 처음이라면 건너뛰어도 돼요",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text("진단 →", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Colourful at-a-glance stats.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "내 레이팅",
                value = "${store.puzzleRating}",
                icon = "♟️",
                iconTint = Accent,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "푼 퍼즐",
                value = "${progress.puzzlesSolved}",
                icon = "🧩",
                iconTint = Info,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "최고 연속",
                value = "${store.bestStreak}",
                icon = "🔥",
                iconTint = Amber,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Rating trend + progress line.
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("레이팅 추이", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${store.puzzleRating}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Sparkline(
                        values = store.ratingHistory,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(40.dp).fillMaxWidth(0.5f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "레슨 ${progress.lessonsDone}/${progress.lessonsTotal}  ·  대국 ${progress.gamesPlayed}판",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Learning map — a tap into the full journey view.
        val treePercent = SkillTree.progressPercent(
            SkillProgress(
                completedLessons = store.completedLessons,
                puzzlesSolved = progress.puzzlesSolved,
                gamesPlayed = games.size,
                completedDrills = store.completedDrills,
            ),
        )
        Surface(
            onClick = onSkillTree,
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🗺️", style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f)) {
                    Text("나의 학습 지도", style = MaterialTheme.typography.titleSmall)
                    Text("규칙 → 전술 → 실전, 전체 진행 $treePercent%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("지도 →", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Growth report — the "how far have I come?" view that keeps players (and subscribers) coming back.
        val trend = store.ratingHistory.let { if (it.size >= 2) it.last() - it.first() else 0 }
        Surface(
            onClick = onDashboard,
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("📈", style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f)) {
                    Text("나의 성장 리포트", style = MaterialTheme.typography.titleSmall)
                    val sub = if (trend > 0) "시작보다 ▲ +$trend · 전술별 강약점 한눈에" else "레이팅 추이 · 전술별 강약점 한눈에"
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("리포트 →", style = MaterialTheme.typography.labelLarge)
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
