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

package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.theme.rememberHapticOnClick
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.ReimbursementUiState
import cn.wj.android.cashbook.feature.records.viewmodel.ReimbursementViewModel

@Composable
internal fun ReimbursementRoute(
    recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReimbursementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReimbursementScreen(
        uiState = uiState,
        viewRecord = viewModel.viewRecord,
        recordDetailSheetContent = { record ->
            recordDetailSheetContent(record, viewModel::dismissRecordDetailSheet)
        },
        onRecordItemClick = viewModel::showRecordDetailsSheet,
        onRequestDismissSheet = viewModel::dismissRecordDetailSheet,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReimbursementScreen(
    uiState: ReimbursementUiState,
    viewRecord: RecordViewsEntity?,
    recordDetailSheetContent: @Composable (RecordViewsEntity?) -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
    onRequestDismissSheet: () -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onRequestPopBackStack,
                title = { Text(text = stringResource(id = R.string.pending_reimbursement)) },
            )
        },
    ) { paddingValues ->
        if (viewRecord != null) {
            CbModalBottomSheet(
                onDismissRequest = onRequestDismissSheet,
                content = { recordDetailSheetContent(viewRecord) },
            )
        }
        Column(modifier = Modifier.padding(paddingValues)) {
            when (uiState) {
                ReimbursementUiState.Loading -> {
                    Loading(modifier = Modifier.fillMaxWidth())
                }

                is ReimbursementUiState.Success -> {
                    Text(
                        text = stringResource(
                            id = R.string.reimbursement_summary_format,
                            uiState.count,
                            uiState.totalAmount.toMoneyCNY(),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    if (uiState.records.isEmpty()) {
                        Empty(
                            hintText = stringResource(id = R.string.no_record_data),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = uiState.records, key = { it.id }) { item ->
                                RecordListItem(
                                    item = item,
                                    modifier = Modifier.clickable(
                                        onClick = rememberHapticOnClick { onRecordItemClick(item) },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
