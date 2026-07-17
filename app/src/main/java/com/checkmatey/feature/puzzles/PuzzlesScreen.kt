package com.checkmatey.feature.puzzles

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.checkmatey.feature.common.InfoScreen

/** Puzzles tab: tactical puzzles graded by rating (built in later iterations). */
@Composable
fun PuzzlesScreen(modifier: Modifier = Modifier) {
    InfoScreen(
        title = "Puzzles",
        subtitle = "레이팅 · 테마별 전술 퍼즐 (Lichess CC0)\n(다음 업데이트에서 추가됩니다)",
        modifier = modifier,
    )
}
