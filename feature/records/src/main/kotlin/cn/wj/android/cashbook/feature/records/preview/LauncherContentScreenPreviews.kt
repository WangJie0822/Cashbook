package cn.wj.android.cashbook.feature.records.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.BackdropValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.records.screen.LauncherContentScreen
import cn.wj.android.cashbook.feature.records.screen.LauncherTopBar

/** 首页标题固定部分预览 */
@OptIn(ExperimentalMaterialApi::class)
@DevicePreviews
@Composable
internal fun LauncherTopTitleBarPreview() {
    CashbookTheme {
        Column(modifier = Modifier.background(color = MaterialTheme.colorScheme.primary)) {
            LauncherTopBar(
                booksName = "默认账本",
                backdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Revealed),
                onMenuClick = {},
                onSearchClick = {},
                onCalendarClick = {},
                onMyAssetClick = {},
            )
            LauncherTopBar(
                booksName = "默认账本",
                backdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Concealed),
                onMenuClick = {},
                onSearchClick = {},
                onCalendarClick = {},
                onMyAssetClick = {},
            )
        }
    }
}

/** 首页内容部分预览 */
@OptIn(ExperimentalMaterialApi::class)
@DevicePreviews
@Composable
internal fun LauncherContentScreenPreview() {
    CashbookTheme {
        LauncherContentScreen(
            onMenuClick = {},
            onAddClick = {},
            onSearchClick = {},
            onCalendarClick = {},
            onMyAssetClick = {},
            onRecordItemEditClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
        )
    }
}