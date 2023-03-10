package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.records.R
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentViewModel
import java.util.Calendar

/** 首页标题固定部分 */
@Composable
internal fun LauncherPinnedTitleScreen(
    viewModel: LauncherContentViewModel = hiltViewModel()
) {
    // 账本名称
    val bookName by viewModel.bookName.collectAsStateWithLifecycle()

    Text(
        text = bookName,
    )
}

/** 首页标题可折叠部分 */
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
                text = "${stringResource(id = R.string.current_month_income)} ${Symbol.rmb}$monthIncome",
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "${stringResource(id = R.string.current_month_balance)} ${Symbol.rmb}$monthBalance",
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = "${stringResource(id = R.string.current_month_expend)} ${Symbol.rmb}$monthExpand",
        )
    }
}

/** 首页内容部分 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherContentScreen(
    modifier: Modifier = Modifier,
    onRecordItemEditClick: (Long) -> Unit,
    viewModel: LauncherContentViewModel = hiltViewModel(),
) {
    val recordMap by viewModel.currentMonthRecordListData.collectAsStateWithLifecycle()
    val todayInt = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    LazyColumn(
        modifier = modifier,
    ) {
        recordMap.keys.reversed().forEach { key ->
            stickyHeader {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    text = when (key.toInt()) {
                        todayInt -> "今天"
                        todayInt - 1 -> "昨天"
                        todayInt - 2 -> "前天"
                        else -> key
                    }
                )
            }
            items(recordMap[key] ?: listOf(), key = { it.id }) {
                ListItem(
                    modifier = Modifier.clickable {
                        // FIXME
                        onRecordItemEditClick(it.id)
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = it.type.iconResId),
                            contentDescription = null
                        )
                    },
                    headlineText = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = it.type.name)
                            val tags = it.relatedTags
                            if (tags.isNotEmpty()) {
                                val tagsText = with(StringBuilder()) {
                                    tags.forEach { tag ->
                                        if (!isBlank()) {
                                            append(",")
                                        }
                                        append(tag.name)
                                    }
                                    var result = toString()
                                    if (result.length > 12) {
                                        result = result.substring(0, 12) + "…"
                                    }
                                    result
                                }
                                Text(
                                    text = tagsText,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 4.dp),
                                )
                            }
                        }
                    },
                    supportingText = {
                        Text(
                            text = "${
                                it.recordTime.split(" ").first()
                            } ${it.remark}"
                        )
                    },
                    trailingContent = {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            // TODO 关联记录
                            Text(
                                text = buildAnnotatedString {
                                    append("${Symbol.rmb}${it.amount}")
                                },
                                color = when (it.type.typeCategory) {
                                    RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
                                    RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
                                    RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                            it.asset?.let { asset ->
                                Text(text = buildAnnotatedString {
                                    if (it.charges.toDoubleOrZero() != 0.0 || it.concessions.toDoubleOrZero() != 0.0) {
                                        // 有手续费、优惠信息
                                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                            append("(")
                                        }
                                        if (it.charges.isNotBlank()) {
                                            withStyle(style = SpanStyle(color = LocalExtendedColors.current.expenditure)) {
                                                append("-${Symbol.rmb}${it.charges}")
                                            }
                                        }
                                        if (it.concessions.isNotBlank()) {
                                            withStyle(style = SpanStyle(color = LocalExtendedColors.current.income)) {
                                                append("+${Symbol.rmb}${it.concessions}")
                                            }
                                        }
                                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                            append(") ")
                                        }
                                    }
                                    append(asset.name)
                                    if (it.type.typeCategory == RecordTypeCategoryEnum.TRANSFER) {
                                        append(" -> ${it.relatedAsset?.name}")
                                    }
                                })
                            }
                        }
                    },
                )
            }
        }
    }
}
