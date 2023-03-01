@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class
)

package cn.wj.android.cashbook.feature.settings.preview

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.design.component.TopAppBarDefaults
import cn.wj.android.cashbook.feature.settings.screen.LauncherScreen
import cn.wj.android.cashbook.feature.settings.screen.LauncherSheet
import cn.wj.android.cashbook.feature.settings.screen.LauncherTopBar

/** 首页 top bar 样式预览 */
@DevicePreviews
@Composable
internal fun LauncherTopBarPreview() {
    CashbookTheme {
        LauncherTopBar(
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            onMenuClick = {},
            pinnedTitle = {
                Text(
                    text = "固定菜单",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            collapsedTitle = {
                Text(
                    text = "可折叠菜单",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
        )
    }
}

/** 首页抽屉菜单预览 */
@DevicePreviews
@Composable
internal fun LauncherSheetPreview() {
    CashbookTheme {
        LauncherSheet(onMenuClick = {})
    }
}

/** 首页抽屉菜单预览 */
@DevicePreviews
@Composable
internal fun LauncherScreenPreview() {
    CashbookTheme {
        LauncherScreen(
            onMenuClick = {},
            pinnedTitle = {
                Text(
                    text = "固定菜单",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            collapsedTitle = {
                Text(
                    text = "可折叠菜单",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            content = { modifier ->
                LazyColumn(modifier = modifier) {
                    items(60) {
                        Text(text = "列表数据 $it")
                    }
                }
            },
        )
    }
}