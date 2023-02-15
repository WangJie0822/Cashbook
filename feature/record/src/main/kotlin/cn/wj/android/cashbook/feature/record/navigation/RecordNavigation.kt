package cn.wj.android.cashbook.feature.record.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.feature.record.screen.LauncherContentRoute

@Composable
fun LauncherContent(
    modifier: Modifier = Modifier,
    onMenuIconClick: () -> Unit,
    onSearchIconClick: () -> Unit,
    onCalendarIconClick: () -> Unit,
    onAssetIconClick: () -> Unit,
) {
    LauncherContentRoute(
        modifier = modifier,
        onMenuIconClick = onMenuIconClick,
        onSearchIconClick = onSearchIconClick,
        onCalendarIconClick = onCalendarIconClick,
        onAssetIconClick = onAssetIconClick,
    )
}