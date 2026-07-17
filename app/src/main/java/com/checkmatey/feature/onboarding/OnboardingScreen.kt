package com.checkmatey.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class OnboardPage(val emoji: String, val title: String, val body: String)

private val PAGES = listOf(
    OnboardPage("♟️", "체스, 제대로 배우기", "Checkmatey는 오프라인·무료로 코치와 함께 실력을 키우는 학습 앱입니다. 계정도 필요 없어요."),
    OnboardPage("🤖", "코치와 함께 두기", "컴퓨터와 두면서 힌트와 실시간 채점으로 '왜'를 배웁니다. 난이도는 내 실력에 맞춰 적응해요."),
    OnboardPage("🧩", "레이팅으로 성장", "전술 퍼즐을 풀면 내 레이팅이 오르고, 약한 부분을 집중 훈련합니다."),
    OnboardPage("📊", "명국 공부 & 복기", "유명 명국을 따라 두고, 내 게임을 분석해 실수를 짚어 실력을 끌어올립니다."),
)

/** First-launch intro. Swipe through, then "시작하기". [onDone] marks it seen. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == PAGES.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text("건너뛰기") }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val p = PAGES[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(p.emoji, fontSize = 72.sp)
                Spacer(Modifier.height(24.dp))
                Text(p.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    p.body,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(PAGES.size) { i ->
                val selected = i == pagerState.currentPage
                Box(
                    Modifier
                        .size(if (selected) 10.dp else 8.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (isLast) onDone() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLast) "시작하기" else "다음")
        }
    }
}
