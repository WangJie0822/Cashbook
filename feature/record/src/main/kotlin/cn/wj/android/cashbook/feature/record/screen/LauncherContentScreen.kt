package cn.wj.android.cashbook.feature.record.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.feature.record.R
import cn.wj.android.cashbook.feature.record.viewmodel.LauncherContentViewModel

@Composable
internal fun LauncherPinnedTitleScreen(
    viewModel: LauncherContentViewModel = hiltViewModel()
) {
    // 账本名称
    val bookName by viewModel.bookName.collectAsStateWithLifecycle()

    Text(
        text = bookName,
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
internal fun LauncherCollapsedTitleScreen(
    viewModel: LauncherContentViewModel = hiltViewModel()
) {
    // 月收入
    val monthIncome by viewModel.monthIncome.collectAsStateWithLifecycle()
    // 月支出
    val monthExpand by viewModel.monthExpand.collectAsStateWithLifecycle()
    // 月结余
    val monthBalance by viewModel.monthBalance.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "${stringResource(id = R.string.record_current_month_income)} ${Symbol.rmb}$monthIncome",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "${stringResource(id = R.string.record_current_month_balance)} ${Symbol.rmb}$monthBalance",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = "${stringResource(id = R.string.record_current_month_expend)} ${Symbol.rmb}$monthExpand",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
internal fun LauncherContentScreen(modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(60) {
            Text(text = "列表数据 $it")
        }
    }
}