package com.checkmatey.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.checkmatey.feature.common.InfoScreen

/** Profile tab: progress, rating and settings (built in later iterations). */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    InfoScreen(
        title = "Profile",
        subtitle = "내 레이팅 · 진행 · 설정\n(다음 업데이트에서 추가됩니다)",
        modifier = modifier,
    )
}
