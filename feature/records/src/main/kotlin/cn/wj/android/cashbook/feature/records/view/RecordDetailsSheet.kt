package cn.wj.android.cashbook.feature.records.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookBackground
import cn.wj.android.cashbook.core.design.component.CommonDivider
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.TransparentListItem
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.records.dialog.ConfirmDeleteRecordDialogRoute
import java.math.BigDecimal

/**
 * 记录详情 sheet 内容
 *
 * @param recordData 显示的记录数据
 * @param onRequestNaviToEditRecord 导航到编辑记录
 * @param onRequestDismissSheet 隐藏 sheet
 */
@Composable
internal fun RecordDetailsSheet(
    recordData: RecordViewsEntity?,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var dialogState: DialogState by remember {
        mutableStateOf(DialogState.Dismiss)
    }

    if (null != recordData && dialogState is DialogState.Shown<*>) {
        // 删除确认弹窗
        ConfirmDeleteRecordDialogRoute(
            recordId = recordData.id,
            onResult = { result ->
                if (result.isSuccess) {
                    dialogState = DialogState.Dismiss
                    onRequestDismissSheet()
                }
            },
            onDialogDismiss = { dialogState = DialogState.Dismiss })
    }

    CashbookBackground {
        Box(
            modifier = modifier.fillMaxWidth(),
        ) {
            if (null == recordData) {
                // 无数据
                Empty(
                    modifier = Modifier.align(Alignment.TopCenter),
                    hintText = stringResource(id = R.string.no_record_data),
                )
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.record_details),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (!recordData.isBalanceAccount) {
                            TextButton(
                                onClick = {
                                    onRequestNaviToEditRecord(recordData.id)
                                    onRequestDismissSheet()
                                },
                            ) {
                                Text(
                                    text = stringResource(id = R.string.edit),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        TextButton(
                            onClick = { dialogState = DialogState.Shown(0) },
                        ) {
                            Text(
                                text = stringResource(id = R.string.delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    CommonDivider()

                    // 金额
                    TransparentListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.amount)) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (recordData.typeCategory == RecordTypeCategoryEnum.EXPENDITURE && recordData.reimbursable) {
                                    // 支出类型，并且可报销
                                    val text = if (recordData.relatedRecord.isEmpty()) {
                                        // 未报销
                                        stringResource(id = R.string.reimbursable)
                                    } else {
                                        // 已报销
                                        stringResource(id = R.string.reimbursed)
                                    }
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                }
                                Text(
                                    text = recordData.amount.withCNY(),
                                    color = recordData.typeCategory.typeColor,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        },
                    )

                    if (recordData.charges.toDoubleOrZero() > 0.0) {
                        // 手续费
                        TransparentListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.charges)) },
                            trailingContent = {
                                Text(
                                    text = "-${recordData.charges}".withCNY(),
                                    color = LocalExtendedColors.current.expenditure,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }

                    if (recordData.typeCategory != RecordTypeCategoryEnum.INCOME && recordData.concessions.toDoubleOrZero() > 0.0) {
                        // 优惠
                        TransparentListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.concessions)) },
                            trailingContent = {
                                Text(
                                    text = "+${recordData.concessions.withCNY()}",
                                    color = LocalExtendedColors.current.income,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }

                    // 类型
                    TransparentListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.type)) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterDrawableResource(idStr = recordData.typeIconResName),
                                    contentDescription = null,
                                    tint = recordData.typeCategory.typeColor,
                                    modifier = Modifier
                                        .background(
                                            color = recordData.typeCategory.typeColor.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        )
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .padding(4.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = recordData.typeName,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        },
                    )

                    if (recordData.relatedRecord.isNotEmpty()
                        && (recordData.typeCategory == RecordTypeCategoryEnum.EXPENDITURE
                                || recordData.typeCategory == RecordTypeCategoryEnum.INCOME)
                    ) {
                        // 有关联记录，且是收入、支出类型
                        val list = recordData.relatedRecord
                        TransparentListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.related_record)) },
                            trailingContent = {
                                val text =
                                    if (recordData.typeCategory == RecordTypeCategoryEnum.INCOME) {
                                        // 收入类型，报销 or 退款，计算关联记录的金额
                                        var total = BigDecimal.ZERO
                                        list.forEach {
                                            total += (it.amount.toBigDecimalOrZero() + it.charges.toBigDecimalOrZero() - it.concessions.toBigDecimalOrZero())
                                        }
                                        stringResource(id = R.string.related_record_display_format).format(
                                            list.size,
                                            total.decimalFormat().withCNY()
                                        )
                                    } else {
                                        // 支出类型，被退款 or 被报销，计算金额
                                        var total = BigDecimal.ZERO
                                        list.forEach {
                                            total += (it.amount.toBigDecimalOrZero() - it.charges.toBigDecimalOrZero())
                                        }
                                        stringResource(id = R.string.refund_reimbursed_format).format(
                                            total.decimalFormat().withCNY()
                                        )
                                    }
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }

                    recordData.assetName?.let { assetName ->
                        // 资产
                        TransparentListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.asset)) },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = recordData.assetIconResId!!),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.2f
                                                ),
                                                shape = CircleShape
                                            )
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .padding(4.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = assetName,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    // 关联资产
                                    recordData.relatedAssetName?.let { relatedName ->
                                        Text(
                                            text = "->",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                        )
                                        Icon(
                                            painter = painterResource(id = recordData.relatedAssetIconResId!!),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                        Text(
                                            text = relatedName,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            },
                        )
                    }

                    if (recordData.relatedTags.isNotEmpty()) {
                        // 标签
                        TransparentListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.tags)) },
                            trailingContent = {
                                val tagsText = with(StringBuilder()) {
                                    recordData.relatedTags.forEach { tag ->
                                        if (isNotBlank()) {
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
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 4.dp),
                                )
                            },
                        )
                    }

                    if (recordData.remark.isNotBlank()) {
                        // 备注
                        TransparentListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.remark)) },
                            trailingContent = {
                                Text(
                                    text = recordData.remark,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }

                    // 时间
                    TransparentListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.time)) },
                        trailingContent = {
                            Text(
                                text = recordData.recordTime,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }
        }
    }
}