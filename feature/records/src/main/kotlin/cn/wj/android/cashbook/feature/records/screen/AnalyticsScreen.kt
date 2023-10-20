package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar

/**
 * TODO 数据分析界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/20
 */
@Composable
internal fun AnalyticsRoute(
    typeId: Long,
    tagId: Long,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    AnalyticsScreen(
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsScreen(
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                title = { Text(text = "数据分析") },
                onBackClick = onRequestPopBackStack,
            )
        },
        content = {},
    )
}