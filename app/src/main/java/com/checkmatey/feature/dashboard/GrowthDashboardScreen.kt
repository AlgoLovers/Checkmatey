package com.checkmatey.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkmatey.core.lesson.Lessons
import com.checkmatey.core.progress.Growth
import com.checkmatey.core.progress.GrowthReport
import com.checkmatey.core.progress.ThemeMastery
import com.checkmatey.core.skilltree.SkillProgress
import com.checkmatey.core.skilltree.SkillTree
import com.checkmatey.data.UserStore
import com.checkmatey.ui.Sparkline
import com.checkmatey.ui.components.StatTile
import com.checkmatey.ui.theme.Accent
import com.checkmatey.ui.theme.Amber
import com.checkmatey.ui.theme.Coral
import com.checkmatey.ui.theme.Info

/**
 * Growth dashboard — makes improvement visible: rating gained since you started, totals, curriculum
 * mastery, the review deck, and which tactics you're strong/weak at. Seeing the line go up is the
 * emotional reason to come back and, eventually, to subscribe. Full-screen overlay from the home card.
 */
@Composable
fun GrowthDashboardScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { UserStore(context) }
    val today = remember { System.currentTimeMillis() / 86_400_000L }

    val report = remember {
        val games = store.recentGames
        Growth.report(
            rating = store.puzzleRating,
            ratingHistory = store.ratingHistory,
            puzzlesSolved = store.solvedCount,
            lessonsDone = store.completedLessons.size,
            lessonsTotal = Lessons.ALL.size,
            gamesPlayed = games.size,
            bestStreak = store.bestStreak,
            masteryPercent = SkillTree.progressPercent(
                SkillProgress(
                    completedLessons = store.completedLessons,
                    puzzlesSolved = store.solvedCount,
                    gamesPlayed = games.size,
                    completedDrills = store.completedDrills,
                ),
            ),
            dueReviews = store.dueReviewCount(today),
            reviewDeck = store.srsCards.size,
            themeStats = store.themeStats(),
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 뒤로") }
        }
        Text("나의 성장 리포트", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        HeroCard(report)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(label = "푼 퍼즐", value = "${report.puzzlesSolved}", icon = "🧩", iconTint = Info, modifier = Modifier.weight(1f))
            StatTile(label = "최고 연속", value = "${report.bestStreak}", icon = "🔥", iconTint = Amber, modifier = Modifier.weight(1f))
            StatTile(label = "학습 완성", value = "${report.masteryPercent}%", icon = "🗺️", iconTint = Accent, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        ReviewDeckCard(report)
        Spacer(Modifier.height(16.dp))

        if (report.strengths.isEmpty() && report.weaknesses.isEmpty()) {
            EmptyHint()
        } else {
            if (report.strengths.isNotEmpty()) {
                MasterySection("💪 잘하는 전술", report.strengths, tintForRate = ::rateColor)
                Spacer(Modifier.height(16.dp))
            }
            if (report.weaknesses.isNotEmpty()) {
                MasterySection(
                    "🎯 더 훈련하면 좋은 전술",
                    report.weaknesses,
                    tintForRate = ::rateColor,
                    footer = "이 테마들을 [퍼즐] 탭에서 집중 훈련하면 레이팅이 가장 빨리 오릅니다.",
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroCard(r: GrowthReport) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("내 퍼즐 레이팅", style = MaterialTheme.typography.labelMedium)
            Text("${r.rating}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            val line = when {
                r.ratingHistory.size < 2 -> "퍼즐을 풀면 여기에 성장 곡선이 그려집니다"
                r.ratingDelta > 0 -> "시작 ${r.startRating}에서 ▲ +${r.ratingDelta} 올랐어요 — 잘하고 있어요!"
                r.ratingDelta < 0 -> "시작 ${r.startRating} · 지금은 조정 중 (${r.ratingDelta}) — 복습으로 다시 올려봐요"
                else -> "시작 ${r.startRating} · 이제 막 출발했어요"
            }
            Text(line, style = MaterialTheme.typography.bodyMedium)
            if (r.ratingHistory.size >= 2) {
                Spacer(Modifier.height(12.dp))
                Sparkline(
                    values = r.ratingHistory,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                )
            }
        }
    }
}

@Composable
private fun ReviewDeckCard(r: GrowthReport) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("🔁", style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f)) {
                Text("간격 반복 복습 덱", style = MaterialTheme.typography.titleSmall)
                val sub = if (r.reviewDeck == 0) {
                    "아직 없음 — 틀린 퍼즐이 여기 모여 잊을 때쯤 다시 나옵니다"
                } else {
                    "총 ${r.reviewDeck}개 · 오늘 복습할 것 ${r.dueReviews}개"
                }
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (r.dueReviews > 0) {
                Text("${r.dueReviews}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MasterySection(
    title: String,
    items: List<ThemeMastery>,
    tintForRate: (Int) -> Color,
    footer: String? = null,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            for (m in items) MasteryBar(m, tintForRate(m.rate))
            if (footer != null) {
                Text(footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MasteryBar(m: ThemeMastery, tint: Color) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(m.theme, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${m.rate}%  (${m.solved}/${m.attempts})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(m.rate / 100f)
                    .height(8.dp)
                    .background(tint, RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            "퍼즐을 몇 개 풀면 여기에 전술별 숙련도가 그려집니다.\n어떤 전술이 강하고 약한지 한눈에 보여드릴게요.",
            Modifier.fillMaxWidth().padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Green when strong, amber mid, coral when weak — an instant read on each theme. */
private fun rateColor(rate: Int): Color = when {
    rate >= 70 -> Accent
    rate >= 45 -> Amber
    else -> Coral
}
