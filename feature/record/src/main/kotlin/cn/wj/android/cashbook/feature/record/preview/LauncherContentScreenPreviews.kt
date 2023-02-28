package cn.wj.android.cashbook.feature.record.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.record.screen.LauncherCollapsedTitleScreen
import cn.wj.android.cashbook.feature.record.screen.LauncherContentScreen
import cn.wj.android.cashbook.feature.record.screen.LauncherPinnedTitleScreen

/** 首页标题固定部分预览 */
@DevicePreviews
@Composable
internal fun LauncherPinnedTitleScreenPreview() {
    CashbookTheme {
        Surface(color = MaterialTheme.colorScheme.primary) {
            LauncherPinnedTitleScreen()
        }
    }
}

/** 首页标题可折叠部分预览 */
@DevicePreviews
@Composable
internal fun LauncherCollapsedTitleScreenPreview() {
    CashbookTheme {
        Surface(color = MaterialTheme.colorScheme.primary) {
            LauncherCollapsedTitleScreen()
        }
    }
}

/** 首页内容部分预览 */
@DevicePreviews
@Composable
internal fun LauncherContentScreenPreview() {
    CashbookTheme {
        LauncherContentScreen()
    }
}