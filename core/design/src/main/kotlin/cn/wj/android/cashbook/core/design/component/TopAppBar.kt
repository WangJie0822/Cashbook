@file:OptIn(ExperimentalMaterial3Api::class)

package cn.wj.android.cashbook.core.design.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.CashbookTheme

/**
 * 通用顶部标题栏
 *
 * @param onBackClick 返回事件
 * @param title 标题
 * @param actions 标题菜单
 * @param colors 标题控件颜色
 */
@Composable
fun CashbookTopAppBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = CashbookIcons.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun CommonTopBarPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            Column {
                CashbookTopAppBar(onBackClick = { })
                CashbookTopAppBar(title = { Text(text = "标题") }, onBackClick = { })
                CashbookTopAppBar(onBackClick = { }, actions = {
                    IconButton(onClick = { }) {
                        Icon(imageVector = CashbookIcons.Menu, contentDescription = null)
                    }
                })
            }
        }
    }
}