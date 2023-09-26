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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
@OptIn(ExperimentalMaterial3Api::class)
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
            var onBackClicked by remember {
                mutableStateOf(false)
            }
            IconButton(onClick = {
                if (!onBackClicked) {
                    /* 从 [NavHost] 二级界面连续快速点击返回按钮，由于动画效果，会多次响应返回事件，
                    [NavController.popBackStack] 在栈里只有一个页面的时候推出了多次，导致 [NavHost] 无法正常显示，
                    因此在这里添加处理，返回事件只能执行一次 */
                    onBackClicked = true
                    onBackClick()
                }
            }) {
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

@OptIn(ExperimentalMaterial3Api::class)
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