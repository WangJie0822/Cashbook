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

@file:OptIn(ExperimentalMaterial3Api::class)

package cn.wj.android.cashbook.feature.budget.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.common.ext.toMoneyString
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.model.entity.BUDGET_TYPE_ID_TOTAL
import cn.wj.android.cashbook.core.model.entity.BudgetItem
import cn.wj.android.cashbook.core.model.entity.BudgetStateEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.parseBudgetAmountCent
import cn.wj.android.cashbook.feature.budget.R
import cn.wj.android.cashbook.feature.budget.viewmodel.BudgetUiState
import cn.wj.android.cashbook.feature.budget.viewmodel.BudgetViewModel

/** 接近预算阈值的橙色（80%~100%） */
private val NearColor = Color(0xFFFF9800)

/**
 * 预算管理界面入口
 */
@Composable
internal fun BudgetRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSetBudget = viewModel::onSetBudget,
        onDeleteBudget = viewModel::onDeleteBudget,
        modifier = modifier,
    )
}

/** 编辑限额对话框目标 */
private data class BudgetEditTarget(val typeId: Long, val name: String, val limit: Long)

@Composable
internal fun BudgetScreen(
    uiState: BudgetUiState,
    onBackClick: () -> Unit,
    onSetBudget: (typeId: Long, input: String) -> Unit,
    onDeleteBudget: (typeId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editTarget by remember { mutableStateOf<BudgetEditTarget?>(null) }
    var showTypePicker by remember { mutableStateOf(false) }

    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                title = { Text(text = stringResource(id = R.string.budget_title)) },
                onBackClick = onBackClick,
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                when (uiState) {
                    BudgetUiState.Loading -> Loading(modifier = Modifier.align(Alignment.Center))

                    is BudgetUiState.Success -> {
                        val progress = uiState.progress
                        // 始终渲染列表：无预算时显「设置总体预算」+「添加分类预算」作为引导（避免空态死路）
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // 总体预算
                            item {
                                BudgetSectionTitle(text = stringResource(id = R.string.budget_overall))
                                val overall = progress.overall
                                if (overall != null) {
                                    BudgetProgressRow(
                                        name = stringResource(id = R.string.budget_overall),
                                        item = overall,
                                        onClick = {
                                            editTarget = BudgetEditTarget(BUDGET_TYPE_ID_TOTAL, "", overall.limit)
                                        },
                                    )
                                } else {
                                    CbListItem(
                                        headlineContent = { Text(text = stringResource(id = R.string.budget_set_overall)) },
                                        modifier = Modifier.clickable {
                                            editTarget = BudgetEditTarget(BUDGET_TYPE_ID_TOTAL, "", 0L)
                                        },
                                    )
                                }
                            }
                            // 分类预算标题 + 添加
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.budget_category),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    CbTextButton(onClick = { showTypePicker = true }) {
                                        Text(text = stringResource(id = R.string.budget_add_category))
                                    }
                                }
                            }
                            items(progress.categoryList) { item ->
                                BudgetProgressRow(
                                    name = item.typeName,
                                    item = item,
                                    onClick = {
                                        editTarget = BudgetEditTarget(item.typeId, item.typeName, item.limit)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    )

    editTarget?.let { target ->
        BudgetEditDialog(
            initialText = if (target.limit > 0) target.limit.toMoneyString() else "",
            canDelete = target.limit > 0,
            onConfirm = { input ->
                onSetBudget(target.typeId, input)
                editTarget = null
            },
            onDelete = {
                onDeleteBudget(target.typeId)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }

    if (showTypePicker && uiState is BudgetUiState.Success) {
        BudgetTypePickerDialog(
            types = uiState.addableTypes,
            onSelect = { type ->
                showTypePicker = false
                editTarget = BudgetEditTarget(type.id, type.name, 0L)
            },
            onDismiss = { showTypePicker = false },
        )
    }
}

@Composable
private fun BudgetSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun BudgetProgressRow(
    name: String,
    item: BudgetItem,
    onClick: () -> Unit,
) {
    val color = when (item.state) {
        BudgetStateEnum.NORMAL -> MaterialTheme.colorScheme.primary
        BudgetStateEnum.NEAR -> NearColor
        BudgetStateEnum.OVER -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${item.spent.toMoneyCNY()} / ${item.limit.toMoneyCNY()}",
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
        }
        LinearProgressIndicator(
            progress = { item.progress ?: 0f },
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
        if (item.state == BudgetStateEnum.OVER) {
            Text(
                text = stringResource(id = R.string.budget_over, item.overAmount.toMoneyCNY()),
                style = MaterialTheme.typography.bodySmall,
                color = color,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun BudgetEditDialog(
    initialText: String,
    canDelete: Boolean,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val invalidHint = stringResource(id = R.string.budget_invalid_amount)
    val amountState = remember {
        TextFieldState(
            defaultText = initialText,
            validator = { parseBudgetAmountCent(it) != null },
            errorFor = { invalidHint },
        )
    }
    CbAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.budget_limit_hint)) },
        text = {
            CbTextField(
                textFieldState = amountState,
                label = { Text(text = stringResource(id = R.string.budget_limit_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = {
            CbTextButton(onClick = {
                if (amountState.isValid) {
                    onConfirm(amountState.text)
                } else {
                    amountState.requestErrors()
                }
            }) {
                Text(text = stringResource(id = R.string.budget_confirm))
            }
        },
        dismissButton = {
            Row {
                if (canDelete) {
                    CbTextButton(onClick = onDelete) {
                        Text(text = stringResource(id = R.string.budget_delete))
                    }
                }
                CbTextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.budget_cancel))
                }
            }
        },
    )
}

@Composable
private fun BudgetTypePickerDialog(
    types: List<RecordTypeModel>,
    onSelect: (RecordTypeModel) -> Unit,
    onDismiss: () -> Unit,
) {
    CbAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.budget_select_category)) },
        text = {
            LazyColumn {
                items(types) { type ->
                    CbListItem(
                        headlineContent = { Text(text = type.name) },
                        modifier = Modifier.clickable { onSelect(type) },
                    )
                }
            }
        },
        confirmButton = {
            CbTextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.budget_cancel))
            }
        },
    )
}
