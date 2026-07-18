package com.checkmatey.feature.skilltree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkmatey.core.skilltree.NodeStatus
import com.checkmatey.core.skilltree.SkillProgress
import com.checkmatey.core.skilltree.SkillTree
import com.checkmatey.data.UserStore
import com.checkmatey.ui.theme.Accent

/**
 * The learning map — a vertical journey from rules to real games, each node coloured by whether
 * it's done, open, or still locked. It reads the same progress the rest of the app writes (lessons,
 * puzzles, drills, games), so "where am I?" always matches reality. Full-screen, launched from Home.
 */
@Composable
fun SkillTreeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { UserStore(context) }
    val progress = SkillProgress(
        completedLessons = store.completedLessons,
        puzzlesSolved = store.solvedCount,
        gamesPlayed = store.recentGames.size,
        completedDrills = store.completedDrills,
    )
    val statuses = SkillTree.statuses(progress)
    val percent = SkillTree.progressPercent(progress)

    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("나의 학습 지도", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("규칙부터 실전까지 — 한 걸음씩", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onBack) { Text("닫기") }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("전체 진행 $percent%", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(20.dp))
        SkillTree.ALL.forEachIndexed { i, node ->
            NodeRow(
                title = node.title,
                subtitle = node.subtitle,
                status = statuses[node.id] ?: NodeStatus.LOCKED,
                showConnector = i < SkillTree.ALL.lastIndex,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "잠긴 단계는 앞 단계를 끝내면 열립니다. 각 단계는 레슨 · 퍼즐 · 실습 · 대국 탭에서 채울 수 있어요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NodeRow(title: String, subtitle: String, status: NodeStatus, showConnector: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val (dotColor, badge) = when (status) {
        NodeStatus.DONE -> Accent to "✓"
        NodeStatus.AVAILABLE -> scheme.primary to "▶"
        NodeStatus.LOCKED -> scheme.outlineVariant to "🔒"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Status dot + vertical connector line to the next node.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = dotColor, modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(badge, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (showConnector) {
                Surface(
                    color = scheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 2.dp).size(width = 2.dp, height = 40.dp),
                ) {}
            }
        }
        Spacer(Modifier.size(14.dp))
        // Node card.
        Surface(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (status == NodeStatus.LOCKED) scheme.surface else scheme.surfaceContainerHigh,
            contentColor = if (status == NodeStatus.LOCKED) scheme.onSurfaceVariant else scheme.onSurface,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatusChip(status)
                }
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StatusChip(status: NodeStatus) {
    val scheme = MaterialTheme.colorScheme
    val (label, bg, fg) = when (status) {
        NodeStatus.DONE -> Triple("완료", Accent.copy(alpha = 0.16f), Accent)
        NodeStatus.AVAILABLE -> Triple("진행 가능", scheme.primaryContainer, scheme.onPrimaryContainer)
        NodeStatus.LOCKED -> Triple("잠김", scheme.surfaceVariant, scheme.onSurfaceVariant)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg, contentColor = fg) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}
