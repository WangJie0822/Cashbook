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

package cn.wj.android.cashbook.feature.assets.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import java.math.BigDecimal

/**
 * 资产列表 Item
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/29
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AssetListItem(
    type: ClassificationTypeEnum,
    iconPainter: Painter,
    name: String,
    balance: String,
    totalAmount: String,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    onItemLongClick: (() -> Unit)? = null,
) {
    val itemModifier = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 70.dp)
        .combinedClickable(
            onClick = onItemClick,
            onLongClick = onItemLongClick,
        )
        .padding(horizontal = 16.dp, vertical = 8.dp)
    if (type.isCreditCard) {
        // 信用卡
        CreditCardAssetListItem(
            iconPainter = iconPainter,
            name = name,
            balance = balance,
            totalAmount = totalAmount,
            modifier = itemModifier,
        )
    } else {
        // 其它类型
        CapitalAssetListItem(
            iconPainter = iconPainter,
            name = name,
            balance = balance,
            modifier = itemModifier,
        )
    }
}

@Composable
internal fun NotAssociatedAssetListItem(
    onNotAssociatedAssetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .clickable(onClick = onNotAssociatedAssetClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.vector_baseline_not_select_24),
            contentDescription = null,
            tint = LocalContentColor.current,
        )
        Text(
            modifier = Modifier
                .padding(start = 8.dp),
            text = stringResource(id = R.string.not_associated_asset),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun CapitalAssetListItem(
    iconPainter: Painter,
    name: String,
    balance: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = balance.withCNY(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CreditCardAssetListItem(
    iconPainter: Painter,
    name: String,
    balance: String,
    totalAmount: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Column(
            modifier = Modifier
                .padding(start = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f),
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = balance.withCNY(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val totalAmountDecimal = totalAmount.toBigDecimalOrZero()
            val balanceDecimal = balance.toBigDecimalOrZero()
            val progress =
                if (balanceDecimal > totalAmountDecimal || totalAmountDecimal <= BigDecimal.ZERO) {
                    0f
                } else {
                    1f - (balanceDecimal / totalAmountDecimal).toFloat()
                }
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    text = stringResource(id = R.string.total_amount_with_colon) + totalAmount.withCNY(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun AssetListPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            Column {
                NotAssociatedAssetListItem(
                    onNotAssociatedAssetClick = {},
                )
                AssetListItem(
                    type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
                    iconPainter = painterResource(id = R.drawable.vector_bank_js_24),
                    balance = "1000",
                    totalAmount = "",
                    onItemClick = {},
                    name = "建设银行",
                )
                AssetListItem(
                    type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
                    iconPainter = painterResource(id = R.drawable.vector_bank_zs_24),
                    balance = "1000",
                    totalAmount = "2000",
                    onItemClick = {},
                    name = "招商银行",
                )
            }
        }
    }
}
