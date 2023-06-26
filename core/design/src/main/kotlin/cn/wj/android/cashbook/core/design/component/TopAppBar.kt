@file:OptIn(ExperimentalMaterial3Api::class)

package cn.wj.android.cashbook.core.design.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
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
import cn.wj.android.cashbook.core.design.theme.CashbookTheme

/**
 * 通用顶部标题栏
 *
 * @param text 标题文本
 * @param onBackClick 返回事件
 * @param actions 标题菜单
 * @param colors 标题控件颜色
 */
@Composable
fun CashbookTopAppBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { text?.let { Text(text = it) } },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
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

@Preview(showBackground = true, name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, name = "night", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CommonTopBarPreview() {
    CashbookTheme {
        Column {
            CashbookTopAppBar(onBackClick = { })
            CashbookTopAppBar(text = "标题", onBackClick = { })
            CashbookTopAppBar(onBackClick = { }, actions = {
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                }
            })
        }
    }
}