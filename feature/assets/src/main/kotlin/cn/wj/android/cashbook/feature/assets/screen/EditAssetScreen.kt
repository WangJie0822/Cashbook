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

package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.PATTERN_SIGN_MONEY
import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.data.helper.assetClassificationEnumBanks
import cn.wj.android.cashbook.core.data.helper.iconResId
import cn.wj.android.cashbook.core.data.helper.nameResId
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbDivider
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.assets.enums.EditAssetBottomSheetEnum
import cn.wj.android.cashbook.feature.assets.viewmodel.EditAssetUiState
import cn.wj.android.cashbook.feature.assets.viewmodel.EditAssetViewModel

/**
 * 编辑资产界面
 *
 * @param assetId 资产 id
 * @param onRequestPopBackStack 导航到上一级
 */
@Composable
internal fun EditAssetRoute(
    assetId: Long,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAssetViewModel = hiltViewModel<EditAssetViewModel>().apply {
        updateAssetId(assetId)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    EditAssetScreen(
        isCreate = assetId == -1L,
        uiState = uiState,
        onSelectClassificationClick = viewModel::showSelectClassificationSheet,
        onClassificationChange = { type, classification ->
            viewModel.updateClassification(
                type,
                classification,
                classification.nameResId.string(context),
            )
        },
        onBillingDateClick = viewModel::showSelectBillingDateDialog,
        onRepaymentDateClick = viewModel::showSelectRepaymentDateDialog,
        onInvisibleChange = viewModel::updateInvisible,
        bottomSheet = viewModel.bottomSheetData,
        onBottomSheetDismiss = viewModel::dismissBottomSheet,
        dialogState = viewModel.dialogState,
        onDialogDismiss = viewModel::dismissDialog,
        onDaySelect = viewModel::updateDay,
        onSaveClick = viewModel::save,
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditAssetScreen(
    isCreate: Boolean,
    uiState: EditAssetUiState,
    onSelectClassificationClick: () -> Unit,
    onClassificationChange: (ClassificationTypeEnum?, AssetClassificationEnum) -> Unit,
    onBillingDateClick: () -> Unit,
    onRepaymentDateClick: () -> Unit,
    onInvisibleChange: (Boolean) -> Unit,
    bottomSheet: EditAssetBottomSheetEnum,
    onBottomSheetDismiss: () -> Unit,
    dialogState: DialogState,
    onDialogDismiss: () -> Unit,
    onDaySelect: (String) -> Unit,
    onSaveClick: (String, String, String, String, String, String, () -> Unit) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 标记 - 是否有银行信息
    val hasBankInfo = uiState.classification.hasBankInfo

    // 提示文本
    val assetErrorText = stringResource(id = R.string.please_enter_asset_name)
    val totalAmountErrorText = stringResource(id = R.string.please_enter_total_amount)

    // 资产名
    val assetNameTextState = remember(uiState.assetName) {
        TextFieldState(
            defaultText = uiState.assetName,
            validator = { it.isNotBlank() },
            errorFor = { assetErrorText },
        )
    }
    // 信用卡 - 总额度
    val totalAmountTextState = remember(uiState.totalAmount) {
        TextFieldState(
            defaultText = uiState.totalAmount,
            validator = { it.isNotBlank() },
            filter = { it.matches(Regex(PATTERN_SIGN_MONEY)) },
            errorFor = { totalAmountErrorText },
        )
    }
    // 余额 or 信用卡 - 已用额度
    val balanceTextState = remember(uiState.balance) {
        TextFieldState(
            defaultText = uiState.balance,
            filter = { it.matches(Regex(PATTERN_SIGN_MONEY)) },
            errorFor = { totalAmountErrorText },
        )
    }
    // 开户行
    val openBankTextState = remember(uiState.openBank) {
        TextFieldState(
            defaultText = uiState.openBank,
        )
    }
    // 卡号
    val cardNoTextState = remember(uiState.cardNo) {
        TextFieldState(
            defaultText = uiState.cardNo,
        )
    }
    // 备注
    val remarkTextState = remember(uiState.remark) {
        TextFieldState(
            defaultText = uiState.remark,
        )
    }

    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = if (isCreate) R.string.new_asset else R.string.edit_asset)) },
            )
        },
        floatingActionButton = {
            if (uiState is EditAssetUiState.Success) {
                CbFloatingActionButton(onClick = {
                    if (!assetNameTextState.isValid || (uiState.isCreditCard && !totalAmountTextState.isValid)) {
                        // 不满足必要条件
                        assetNameTextState.requestErrors()
                        totalAmountTextState.requestErrors()
                    } else {
                        onSaveClick(
                            assetNameTextState.text,
                            totalAmountTextState.text,
                            balanceTextState.text,
                            openBankTextState.text,
                            cardNoTextState.text,
                            remarkTextState.text,
                            onBackClick,
                        )
                    }
                }) {
                    Icon(imageVector = CbIcons.SaveAs, contentDescription = null)
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            if (bottomSheet != EditAssetBottomSheetEnum.DISMISS) {
                CbModalBottomSheet(
                    onDismissRequest = onBottomSheetDismiss,
                    sheetState = rememberModalBottomSheetState(
                        confirmValueChange = {
                            if (it == SheetValue.Hidden) {
                                onBottomSheetDismiss()
                            }
                            true
                        },
                    ),
                    content = {
                        EditAssetSheetContent(
                            bottomSheet = bottomSheet,
                            onClassificationChange = onClassificationChange,
                        )
                    },
                )
            }

            if (dialogState != DialogState.Dismiss) {
                SelectDayDialog(
                    onDismiss = onDialogDismiss,
                    onDaySelect = onDaySelect,
                )
            }

            when (uiState) {
                EditAssetUiState.Loading -> {
                    Loading(modifier = Modifier.align(Alignment.Center))
                }

                is EditAssetUiState.Success -> {
                    Column(
                        modifier = Modifier.verticalScroll(state = rememberScrollState()),
                    ) {
                        CbListItem(
                            modifier = Modifier.clickable(
                                enabled = uiState.typeEnable,
                                onClick = onSelectClassificationClick,
                            ),
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.asset_classification),
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        painter = painterResource(id = uiState.classification.iconResId),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                    )
                                    Text(
                                        text = stringResource(id = uiState.classification.nameResId),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                    if (uiState.typeEnable) {
                                        Icon(
                                            imageVector = CbIcons.KeyboardArrowRight,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            },
                        )

                        CbDivider(
                            modifier = Modifier.height(8.dp),
                            color = DividerDefaults.color.copy(alpha = 0.1f),
                        )

                        CbTextField(
                            textFieldState = assetNameTextState,
                            label = { Text(text = stringResource(id = R.string.asset_name)) },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .padding(horizontal = 16.dp),
                        )

                        if (uiState.isCreditCard) {
                            // 信用卡账号，显示总额度
                            CbTextField(
                                textFieldState = totalAmountTextState,
                                label = { Text(text = stringResource(id = R.string.total_amount)) },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Next,
                                    keyboardType = KeyboardType.Decimal,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .padding(horizontal = 16.dp),
                            )
                        }

                        // 余额 or 信用卡-已用额度
                        CbTextField(
                            textFieldState = balanceTextState,
                            label = { Text(text = stringResource(id = if (uiState.isCreditCard) R.string.arrears else R.string.balance)) },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Decimal,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .padding(horizontal = 16.dp),
                        )

                        if (hasBankInfo) {
                            // 开户行
                            CbTextField(
                                textFieldState = openBankTextState,
                                label = { Text(text = stringResource(id = R.string.open_bank)) },
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .padding(horizontal = 16.dp),
                            )
                            // 卡号
                            CbTextField(
                                textFieldState = cardNoTextState,
                                label = { Text(text = stringResource(id = R.string.card_no)) },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Next,
                                    keyboardType = KeyboardType.Number,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .padding(horizontal = 16.dp),
                            )
                        }

                        // 备注
                        CbTextField(
                            textFieldState = remarkTextState,
                            label = { Text(text = stringResource(id = R.string.remark)) },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .padding(horizontal = 16.dp),
                        )

                        CbDivider(
                            modifier = Modifier.height(8.dp),
                            color = DividerDefaults.color.copy(alpha = 0.1f),
                        )

                        if (uiState.isCreditCard) {
                            CbListItem(
                                modifier = Modifier.clickable(onClick = onBillingDateClick),
                                headlineContent = {
                                    Text(
                                        text = stringResource(id = R.string.billing_date),
                                        modifier = Modifier.padding(start = 16.dp),
                                    )
                                },
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val billingDateText = if (uiState.billingDate.isBlank()) {
                                            stringResource(id = R.string.un_set)
                                        } else {
                                            uiState.billingDate + stringResource(id = R.string.day)
                                        }
                                        Text(
                                            text = billingDateText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                        )
                                        Icon(
                                            imageVector = CbIcons.KeyboardArrowRight,
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )

                            CbListItem(
                                modifier = Modifier.clickable(onClick = onRepaymentDateClick),
                                headlineContent = {
                                    Text(
                                        text = stringResource(id = R.string.repayment_date),
                                        modifier = Modifier.padding(start = 16.dp),
                                    )
                                },
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val repaymentDateText =
                                            if (uiState.repaymentDate.isBlank()) {
                                                stringResource(id = R.string.un_set)
                                            } else {
                                                uiState.repaymentDate + stringResource(id = R.string.day)
                                            }
                                        Text(
                                            text = repaymentDateText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                        )
                                        Icon(
                                            imageVector = CbIcons.KeyboardArrowRight,
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )
                        }

                        CbListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.invisible_asset),
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(id = R.string.invisible_asset_hint),
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = uiState.invisible,
                                    onCheckedChange = onInvisibleChange,
                                )
                            },
                        )

                        Footer(hintText = stringResource(id = R.string.footer_hint_default))
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditAssetSheetContent(
    bottomSheet: EditAssetBottomSheetEnum,
    onClassificationChange: (ClassificationTypeEnum?, AssetClassificationEnum) -> Unit,
) {
    when (bottomSheet) {
        EditAssetBottomSheetEnum.CLASSIFICATION_TYPE -> {
            SelectAssetClassificationTypeSheet(
                onItemClick = onClassificationChange,
            )
        }

        EditAssetBottomSheetEnum.ASSET_CLASSIFICATION -> {
            SelectAssetClassificationSheet(onItemClick = { classification ->
                onClassificationChange.invoke(null, classification)
            })
        }

        EditAssetBottomSheetEnum.DISMISS -> {
            // empty block
        }
    }
}

@Composable
internal fun SelectDayDialog(
    onDismiss: () -> Unit,
    onDaySelect: (String) -> Unit,
) {
    CbAlertDialog(onDismissRequest = onDismiss, text = {
        Column {
            for (c in 0 until 6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    for (r in 0 until 5) {
                        val text = (c * 5 + r + 1).toString()
                        Text(
                            modifier = Modifier
                                .clickable { onDaySelect.invoke(text) }
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            text = text,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }, confirmButton = {
        CbTextButton(onClick = { onDaySelect.invoke("") }) {
            Text(text = stringResource(id = R.string.clear))
        }
    }, dismissButton = {
        CbTextButton(onClick = onDismiss) {
            Text(text = stringResource(id = R.string.cancel))
        }
    })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SelectAssetClassificationTypeSheet(onItemClick: (ClassificationTypeEnum, AssetClassificationEnum) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(id = R.string.select_asset_classification),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        CbDivider()

        LazyColumn(
            content = {
                ClassificationTypeEnum.entries.forEach { type ->
                    stickyHeader {
                        Text(
                            text = stringResource(id = type.nameResId),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(type.array) {
                        SingleLineListItem(
                            iconResId = it.iconResId,
                            nameResId = it.nameResId,
                            onItemClick = { onItemClick(type, it) },
                        )
                    }
                }
            },
        )
    }
}

@Composable
internal fun SelectAssetClassificationSheet(onItemClick: (AssetClassificationEnum) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(id = R.string.select_bank),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        CbDivider()

        LazyColumn(
            content = {
                items(assetClassificationEnumBanks) {
                    SingleLineListItem(
                        iconResId = it.iconResId,
                        nameResId = it.nameResId,
                        onItemClick = { onItemClick(it) },
                    )
                }
            },
        )
    }
}

@Composable
internal fun SingleLineListItem(iconResId: Int, nameResId: Int, onItemClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable { onItemClick() },
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .align(Alignment.CenterVertically),
        )
        Text(
            text = stringResource(id = nameResId),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}
