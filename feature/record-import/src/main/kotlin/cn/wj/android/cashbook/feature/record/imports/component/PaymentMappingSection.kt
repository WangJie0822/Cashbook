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

package cn.wj.android.cashbook.feature.record.imports.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.model.model.PaymentMethodMapping
import cn.wj.android.cashbook.core.ui.R

/**
 * 支付方式映射区域（可折叠）
 *
 * @param mappings 支付方式映射列表
 * @param onMappingClick 点击某个映射项，弹出资产选择
 */
@Composable
internal fun PaymentMappingSection(
    mappings: List<PaymentMethodMapping>,
    onMappingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.import_payment_mapping),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = CbIcons.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (expanded) 180f else 0f
                },
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                mappings.forEach { mapping ->
                    PaymentMappingItem(
                        mapping = mapping,
                        onClick = { onMappingClick(mapping.originalName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMappingItem(
    mapping: PaymentMethodMapping,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUnmapped = mapping.matchedAssetId == -1L
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mapping.originalName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = " → ",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (isUnmapped) {
                stringResource(R.string.import_payment_unmapped)
            } else {
                mapping.matchedAssetName
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUnmapped) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}
