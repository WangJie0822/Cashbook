/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.feature.records.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookBackground
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.preview.PreviewTheme
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.TypeIcon
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.records.dialog.ConfirmDeleteRecordDialogRoute
import cn.wj.android.cashbook.feature.records.dialog.ImagePreviewDialog
import cn.wj.android.cashbook.feature.records.enums.RecordDetailsDialogEnum
import cn.wj.android.cashbook.feature.records.model.asViewModel
import cn.wj.android.cashbook.feature.records.preview.RecordDetailsSheetPreviewParameterProvider
import java.math.BigDecimal

/**
 * 记录详情 sheet 内容
 *
 * @param recordData 显示的记录数据
 * @param onRequestNaviToEditRecord 导航到编辑记录
 * @param onRequestNaviToAssetInfo 导航到资产信息
 * @param onRequestDismissSheet 隐藏 sheet
 */
@Composable
internal fun RecordDetailsSheet(
    recordData: RecordViewsEntity?,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogState: DialogState by remember {
        mutableStateOf(DialogState.Dismiss)
    }

    if (null != recordData) {
        (dialogState as? DialogState.Shown<*>)?.data?.run {
            when (this) {
                RecordDetailsDialogEnum.DELETE_CONFIRM -> {
                    // 删除确认弹窗
                    ConfirmDeleteRecordDialogRoute(
                        recordId = recordData.id,
                        onResult = { result ->
                            if (result.isSuccess) {
                                dialogState = DialogState.Dismiss
                                onRequestDismissSheet()
                            }
                        },
                        onDismissRequest = { dialogState = DialogState.Dismiss },
                    )
                }

                RecordDetailsDialogEnum.IMAGE_PREVIEW -> {
                    // 图片预览
                    ImagePreviewDialog(
                        onRequestDismissDialog = {
                            dialogState = DialogState.Dismiss
                        },
                        list = recordData.relatedImage.map { it.asViewModel() },
                    )
                }
            }
        }
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
                            CbTextButton(
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

                        CbTextButton(
                            onClick = {
                                dialogState =
                                    DialogState.Shown(RecordDetailsDialogEnum.DELETE_CONFIRM)
                            },
                        ) {
                            Text(
                                text = stringResource(id = R.string.delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    CbHorizontalDivider()

                    // 实际金额
                    CbListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.final_amount)) },
                        trailingContent = {
                            Text(
                                text = recordData.finalAmount.withCNY(),
                                color = recordData.typeCategory.typeColor,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )

                    // 金额
                    CbListItem(
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
                        CbListItem(
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
                        CbListItem(
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
                    CbListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.type)) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TypeIcon(
                                    painter = painterDrawableResource(idStr = recordData.typeIconResName),
                                    containerColor = recordData.typeCategory.typeColor,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = recordData.typeName,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        },
                    )

                    if (recordData.relatedRecord.isNotEmpty() &&
                        (
                            recordData.typeCategory == RecordTypeCategoryEnum.EXPENDITURE ||
                                recordData.typeCategory == RecordTypeCategoryEnum.INCOME
                            )
                    ) {
                        // 有关联记录，且是收入、支出类型
                        val list = recordData.relatedRecord
                        CbListItem(
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
                                            total.decimalFormat().withCNY(),
                                        )
                                    } else {
                                        // 支出类型，被退款 or 被报销，计算金额
                                        var total = BigDecimal.ZERO
                                        list.forEach {
                                            total += (it.amount.toBigDecimalOrZero() - it.charges.toBigDecimalOrZero())
                                        }
                                        stringResource(id = R.string.refund_reimbursed_format).format(
                                            total.decimalFormat().withCNY(),
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
                        CbListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.asset)) },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AssetIconText(
                                        painter = painterResource(id = recordData.assetIconResId!!),
                                        assetName = assetName,
                                        modifier = Modifier.clickable {
                                            recordData.assetId?.let(onRequestNaviToAssetInfo)
                                        },
                                    )
                                    // 关联资产
                                    recordData.relatedAssetName?.let { relatedName ->
                                        Text(
                                            text = "->",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                        )
                                        AssetIconText(
                                            painter = painterResource(id = recordData.relatedAssetIconResId!!),
                                            assetName = relatedName,
                                            modifier = Modifier.clickable {
                                                recordData.relatedAssetId?.let(
                                                    onRequestNaviToAssetInfo,
                                                )
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }

                    if (recordData.relatedTags.isNotEmpty()) {
                        // 标签
                        CbListItem(
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
                                            shape = MaterialTheme.shapes.small,
                                        )
                                        .padding(horizontal = 4.dp),
                                )
                            },
                        )
                    }

                    if (recordData.relatedImage.isNotEmpty()) {
                        // 关联图片
                        CbListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.related_image)) },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = CbIcons.PhotoLibrary,
                                        contentDescription = null,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = recordData.relatedImage.size.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                dialogState =
                                    DialogState.Shown(RecordDetailsDialogEnum.IMAGE_PREVIEW)
                            },
                        )
                    }

                    if (recordData.remark.isNotBlank()) {
                        // 备注
                        CbListItem(
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
                    CbListItem(
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

/**
 * 资产图标文本控件
 *
 * @param painter 图标
 * @param assetName 资产名
 */
@Composable
private fun AssetIconText(
    painter: Painter,
    assetName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.5f,
                    ),
                    shape = CircleShape,
                )
                .padding(6.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = assetName,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@PreviewLightDark
@Composable
fun RecordDetailsSheetAssetIconText() {
    PreviewTheme {
        AssetIconText(
            painter = painterResource(id = R.drawable.vector_wechat_circle_24),
            assetName = "微信",
        )
    }
}

@PreviewLightDark
@Composable
fun RecordDetailsSheetWithData(
    @PreviewParameter(RecordDetailsSheetPreviewParameterProvider::class)
    recordData: RecordViewsEntity?,
) {
    PreviewTheme(defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200)) {
        RecordDetailsSheet(
            recordData = recordData,
            onRequestNaviToEditRecord = {},
            onRequestNaviToAssetInfo = {},
            onRequestDismissSheet = {},
        )
    }
}
