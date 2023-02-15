package cn.wj.android.cashbook.feature.record.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.feature.record.screen.LauncherCollapsedTitleScreen
import cn.wj.android.cashbook.feature.record.screen.LauncherContentScreen
import cn.wj.android.cashbook.feature.record.screen.LauncherPinnedTitleScreen

@Composable
fun LauncherPinnedTitleContent() {
    LauncherPinnedTitleScreen()
}

@Composable
fun LauncherCollapsedTitleContent() {
    LauncherCollapsedTitleScreen()
}

@Composable
fun LauncherContent(modifier: Modifier) {
    LauncherContentScreen(modifier)
}