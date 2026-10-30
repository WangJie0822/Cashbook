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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.design.theme.rememberHapticOnClick
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.recordFilterContribution
import cn.wj.android.cashbook.core.model.model.shouldShowFilterContribution
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.screen.RecordListItem

@Composable
fun SearchFilterRecordItem(record: RecordViewsEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().clickable(onClick = rememberHapticOnClick(onClick = onClick))) {
        RecordListItem(item = record)
        if (shouldShowFilterContribution(record)) {
            val contribution = recordFilterContribution(record)
            val explanation = if (record.typeId == -1101L || record.typeId == -1102L) {
                stringResource(R.string.search_filter_balance_excluded)
            } else if (record.typeCategory == RecordTypeCategoryEnum.INCOME) {
                stringResource(R.string.search_filter_contribution_income, contribution.income.toMoneyCNY())
            } else {
                stringResource(R.string.search_filter_contribution_expenditure, contribution.expenditure.toMoneyCNY())
            }
            Text(explanation, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp).testTag("filter_contribution_${record.id}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
