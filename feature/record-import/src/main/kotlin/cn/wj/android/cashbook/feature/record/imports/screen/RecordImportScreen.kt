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

package cn.wj.android.cashbook.feature.record.imports.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.model.model.BillDirection
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.record.imports.component.ImportPreviewList
import cn.wj.android.cashbook.feature.record.imports.component.ImportSummarySection
import cn.wj.android.cashbook.feature.record.imports.component.PaymentMappingSection
import cn.wj.android.cashbook.feature.record.imports.viewmodel.RecordImportUiState
import cn.wj.android.cashbook.feature.record.imports.viewmodel.RecordImportViewModel

/**
 * 账单导入界面入口
 */
@Composable
internal fun RecordImportRoute(
    onRequestPopBackStack: () -> Unit,
    onImportResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 资产选择弹窗状态
    var selectedMappingName by remember { mutableStateOf<String?>(null) }
    // 分类选择弹窗状态
    var selectedTypeIndex by remember { mutableStateOf(-1) }

    RecordImportScreen(
        uiState = uiState,
        onBookSelected = viewModel::selectBook,
        onMappingClick = { originalName -> selectedMappingName = originalName },
        onUpdatePaymentMapping = viewModel::updatePaymentMapping,
        onToggleSelection = viewModel::toggleItemSelection,
        onSelectAll = viewModel::selectAll,
        onTypeClick = { index -> selectedTypeIndex = index },
        onUpdateItemType = viewModel::updateItemType,
        onConfirmImport = viewModel::confirmImport,
        onBackClick = onRequestPopBackStack,
        onImportResult = onImportResult,
        selectedMappingName = selectedMappingName,
        onDismissMappingDialog = { selectedMappingName = null },
        selectedTypeIndex = selectedTypeIndex,
        onDismissTypeDialog = { selectedTypeIndex = -1 },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordImportScreen(
    uiState: RecordImportUiState,
    onBookSelected: (Long) -> Unit,
    onMappingClick: (String) -> Unit,
    onUpdatePaymentMapping: (String, Long, String) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onTypeClick: (Int) -> Unit,
    onUpdateItemType: (Int, Long, String) -> Unit,
    onConfirmImport: () -> Unit,
    onBackClick: () -> Unit,
    onImportResult: (String) -> Unit,
    selectedMappingName: String?,
    onDismissMappingDialog: () -> Unit,
    selectedTypeIndex: Int,
    onDismissTypeDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 处理导入完成和错误状态
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (uiState) {
            is RecordImportUiState.Done -> {
                onImportResult(
                    context.getString(R.string.import_success, uiState.imported, uiState.skipped),
                )
            }

            is RecordImportUiState.Error -> {
                onImportResult(context.getString(R.string.import_file_format_error))
            }

            else -> {}
        }
    }

    // 资产选择弹窗
    if (selectedMappingName != null && uiState is RecordImportUiState.Ready) {
        CbAlertDialog(
            onDismissRequest = onDismissMappingDialog,
            title = { Text(text = stringResource(R.string.import_select_asset)) },
            text = {
                LazyColumn {
                    items(uiState.visibleAssets.size) { index ->
                        val asset = uiState.visibleAssets[index]
                        Text(
                            text = asset.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdatePaymentMapping(selectedMappingName!!, asset.id, asset.name)
                                    onDismissMappingDialog()
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissMappingDialog) {
                    Text(text = stringResource(R.string.close))
                }
            },
        )
    }

    // 分类选择弹窗
    if (selectedTypeIndex >= 0 && uiState is RecordImportUiState.Ready) {
        val item = uiState.previewItems.getOrNull(selectedTypeIndex)
        val typeList = if (item?.billItem?.direction == BillDirection.EXPENDITURE) {
            uiState.expenditureTypes
        } else {
            uiState.incomeTypes
        }
        CbAlertDialog(
            onDismissRequest = onDismissTypeDialog,
            title = { Text(text = stringResource(R.string.import_select_category)) },
            text = {
                LazyColumn {
                    items(typeList.size) { index ->
                        val type = typeList[index]
                        Text(
                            text = type.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateItemType(selectedTypeIndex, type.id, type.name)
                                    onDismissTypeDialog()
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissTypeDialog) {
                    Text(text = stringResource(R.string.close))
                }
            },
        )
    }

    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(R.string.import_bill)) },
            )
        },
        bottomBar = {
            if (uiState is RecordImportUiState.Ready) {
                ImportBottomBar(
                    uiState = uiState,
                    onSelectAll = onSelectAll,
                    onConfirmImport = onConfirmImport,
                )
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            ) {
                when (uiState) {
                    RecordImportUiState.Parsing,
                    RecordImportUiState.Importing,
                    -> {
                        Loading(modifier = Modifier.align(Alignment.Center))
                    }

                    is RecordImportUiState.Ready -> {
                        ReadyContent(
                            uiState = uiState,
                            onBookSelected = onBookSelected,
                            onMappingClick = onMappingClick,
                            onToggleSelection = onToggleSelection,
                            onTypeClick = onTypeClick,
                        )
                    }

                    is RecordImportUiState.Done,
                    is RecordImportUiState.Error,
                    -> {
                        // 由 LaunchedEffect 处理
                    }
                }
            }
        },
    )
}

@Composable
private fun ReadyContent(
    uiState: RecordImportUiState.Ready,
    onBookSelected: (Long) -> Unit,
    onMappingClick: (String) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onTypeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        // 概览区域
        ImportSummarySection(
            fileName = uiState.fileName,
            summary = uiState.summary,
            selectedBooksId = uiState.selectedBooksId,
            booksList = uiState.booksList,
            onBookSelected = onBookSelected,
        )

        HorizontalDivider()

        // 支付方式映射
        if (uiState.paymentMappings.isNotEmpty()) {
            PaymentMappingSection(
                mappings = uiState.paymentMappings,
                onMappingClick = onMappingClick,
            )
            HorizontalDivider()
        }

        // 记录预览列表标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.import_preview_list),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // 记录预览列表
        ImportPreviewList(
            items = uiState.previewItems,
            onToggleSelection = onToggleSelection,
            onTypeClick = onTypeClick,
            listState = listState,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ImportBottomBar(
    uiState: RecordImportUiState.Ready,
    onSelectAll: (Boolean) -> Unit,
    onConfirmImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = uiState.previewItems.count { it.selected }
    val selectedAmount = uiState.previewItems
        .filter { it.selected }
        .sumOf { item ->
            if (item.billItem.direction == BillDirection.EXPENDITURE) item.billItem.amount else item.billItem.amount
        }
    val allSelected = uiState.previewItems.all { it.selected }
    val canImport = selectedCount > 0 && !uiState.hasUnmappedPayments

    BottomAppBar(modifier = modifier) {
        TextButton(
            onClick = { onSelectAll(!allSelected) },
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text(
                text = if (allSelected) {
                    stringResource(R.string.import_deselect_all)
                } else {
                    stringResource(R.string.import_select_all)
                },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.import_selected_count, selectedCount),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.import_selected_amount, "%.2f".format(selectedAmount)),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        FilledTonalButton(
            onClick = onConfirmImport,
            enabled = canImport,
            modifier = Modifier.padding(end = 16.dp),
        ) {
            Text(text = stringResource(R.string.import_confirm))
        }
    }
}
