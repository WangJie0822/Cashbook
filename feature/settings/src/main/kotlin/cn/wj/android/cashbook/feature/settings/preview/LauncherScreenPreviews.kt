package cn.wj.android.cashbook.feature.settings.preview

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.settings.screen.LauncherScreen
import cn.wj.android.cashbook.feature.settings.screen.LauncherSheet

/** 首页抽屉菜单预览 */
@DevicePreviews
@Composable
internal fun LauncherSheetPreview() {
    CashbookTheme {
        LauncherSheet(
            onMyAssetClick = { },
            onMyBookClick = { },
            onMyCategoryClick = { },
            onMyTagClick = { },
            onSettingClick = { },
            onAboutUsClick = { },
        )
    }
}

/** 首页抽屉菜单预览 */
@OptIn(ExperimentalMaterial3Api::class)
@DevicePreviews
@Composable
internal fun LauncherScreenPreview() {
    CashbookTheme {
        LauncherScreen(
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
            onMyAssetClick = { },
            onMyBookClick = { },
            onMyCategoryClick = { },
            onMyTagClick = { },
            onSettingClick = { },
            onAboutUsClick = { },
            content = {},
        )
    }
}