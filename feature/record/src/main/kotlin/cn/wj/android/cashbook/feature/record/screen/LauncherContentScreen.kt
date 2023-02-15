@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package cn.wj.android.cashbook.feature.record.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.Icons
import cn.wj.android.cashbook.core.ui.LargeTopAppBar
import cn.wj.android.cashbook.core.ui.TextStyles
import cn.wj.android.cashbook.core.ui.TopAppBarDefaults
import cn.wj.android.cashbook.core.ui.TopAppBarScrollBehavior
import cn.wj.android.cashbook.core.ui.onPrimary
import cn.wj.android.cashbook.core.ui.primary
import cn.wj.android.cashbook.feature.record.R
import cn.wj.android.cashbook.feature.record.viewmodel.LauncherContentViewModel

/**
 * 首页内容路由
 *
 * @param onMenuIconClick 菜单按钮点击事件
 * @param onSearchIconClick 搜索按钮点击事件
 * @param onCalendarIconClick 日历按钮点击事件
 * @param onAssetIconClick 资产按钮点击事件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/15
 */
@Composable
internal fun LauncherContentRoute(
    modifier: Modifier = Modifier,
    onMenuIconClick: () -> Unit,
    onSearchIconClick: () -> Unit,
    onCalendarIconClick: () -> Unit,
    onAssetIconClick: () -> Unit,
) {
    LauncherContentScreen(
        modifier = modifier,
        onMenuIconClick = onMenuIconClick,
        onSearchIconClick = onSearchIconClick,
        onCalendarIconClick = onCalendarIconClick,
        onAssetIconClick = onAssetIconClick,
    )
}

/**
 * 显示内容
 *
 * @param onMenuIconClick 菜单按钮点击事件
 * @param onSearchIconClick 搜索按钮点击事件
 * @param onCalendarIconClick 日历按钮点击事件
 * @param onAssetIconClick 资产按钮点击事件
 */
@Composable
internal fun LauncherContentScreen(
    modifier: Modifier = Modifier,
    onMenuIconClick: () -> Unit,
    onSearchIconClick: () -> Unit,
    onCalendarIconClick: () -> Unit,
    onAssetIconClick: () -> Unit,
) {
    LauncherScaffold(
        modifier = modifier,
        onMenuIconClick = onMenuIconClick,
        onSearchIconClick = onSearchIconClick,
        onCalendarIconClick = onCalendarIconClick,
        onAssetIconClick = onAssetIconClick,
    )
}

/**
 * 启动页脚手架
 *
 * @param onMenuIconClick 菜单按钮点击事件
 * @param onSearchIconClick 搜索按钮点击事件
 * @param onCalendarIconClick 日历按钮点击事件
 * @param onAssetIconClick 资产按钮点击事件
 */
@Composable
fun LauncherScaffold(
    modifier: Modifier = Modifier,
    viewModel: LauncherContentViewModel = hiltViewModel(),
    onMenuIconClick: () -> Unit,
    onSearchIconClick: () -> Unit,
    onCalendarIconClick: () -> Unit,
    onAssetIconClick: () -> Unit,
) {
    // 滚动行为
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // 账本名称
    val bookName by viewModel.bookName.collectAsStateWithLifecycle()
    // 月收入
    val monthIncome by viewModel.monthIncome.collectAsStateWithLifecycle()
    // 月支出
    val monthExpand by viewModel.monthExpand.collectAsStateWithLifecycle()
    // 月结余
    val monthBalance by viewModel.monthBalance.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LauncherTopBar(
                scrollBehavior = scrollBehavior,
                bookName = bookName,
                monthIncome = monthIncome,
                monthExpand = monthExpand,
                monthBalance = monthBalance,
                onMenuIconClick = onMenuIconClick,
                onSearchIconClick = onSearchIconClick,
                onCalendarIconClick = onCalendarIconClick,
                onAssetIconClick = onAssetIconClick,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            items(60) {
                Text(text = "列表数据 $it")
            }
        }
    }
}

/**
 * 启动页顶部菜单栏
 *
 * @param monthIncome 本月收入
 * @param monthExpand 本月支出
 * @param monthBalance 本月结余
 * @param onMenuIconClick 菜单按钮点击事件
 * @param onSearchIconClick 搜索按钮点击事件
 * @param onCalendarIconClick 日历按钮点击事件
 * @param onAssetIconClick 资产按钮点击事件
 */
@Composable
fun LauncherTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    bookName: String,
    monthIncome: String,
    monthExpand: String,
    monthBalance: String,
    onMenuIconClick: () -> Unit,
    onSearchIconClick: () -> Unit,
    onCalendarIconClick: () -> Unit,
    onAssetIconClick: () -> Unit,
) {
    LargeTopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.primary),
        pinnedTitle = {
            Text(
                text = bookName,
                color = Color.onPrimary,
                style = TextStyles.titleSmall,
            )
        },
        collapsedTitle = {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "${stringResource(id = R.string.record_current_month_income)} ${Symbol.rmb}$monthIncome",
                        color = Color.onPrimary,
                        style = TextStyles.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "${stringResource(id = R.string.record_current_month_balance)} ${Symbol.rmb}$monthBalance",
                        color = Color.onPrimary,
                        style = TextStyles.titleSmall,
                    )
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${stringResource(id = R.string.record_current_month_expend)} ${Symbol.rmb}$monthExpand",
                    color = Color.onPrimary,
                    style = TextStyles.titleSmall,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuIconClick) {
                Icon(
                    imageVector = Icons.menu,
                    contentDescription = null,
                    tint = Color.onPrimary,
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchIconClick) {
                Icon(
                    imageVector = Icons.search,
                    contentDescription = null,
                    tint = Color.onPrimary,
                )
            }
            IconButton(onClick = onCalendarIconClick) {
                Icon(
                    imageVector = Icons.calendar,
                    contentDescription = null,
                    tint = Color.onPrimary,
                )
            }
            IconButton(onClick = onAssetIconClick) {
                Icon(
                    imageVector = Icons.asset,
                    contentDescription = null,
                    tint = Color.onPrimary,
                )
            }
        },
    )
}

@DevicePreviews
@Composable
fun LauncherTopBarPreview() {
    CashbookTheme {
        LauncherTopBar(
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
            bookName = "我的账单",
            monthIncome = "3000",
            monthExpand = "2000",
            monthBalance = "1000",
            onMenuIconClick = {},
            onSearchIconClick = {},
            onCalendarIconClick = {},
            onAssetIconClick = {},
        )
    }
}
