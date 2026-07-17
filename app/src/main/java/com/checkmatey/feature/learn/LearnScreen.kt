package com.checkmatey.feature.learn

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.checkmatey.feature.common.InfoScreen

/** Learn tab: guided lessons on rules, openings and tactics (built in later iterations). */
@Composable
fun LearnScreen(modifier: Modifier = Modifier) {
    InfoScreen(
        title = "Learn",
        subtitle = "규칙 · 전술 · 오프닝 레슨\n(다음 업데이트에서 추가됩니다)",
        modifier = modifier,
    )
}
