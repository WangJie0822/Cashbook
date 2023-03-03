package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.PATTERN_SIGN_MONEY
import cn.wj.android.cashbook.core.data.helper.assetClassificationEnumBanks
import cn.wj.android.cashbook.core.data.helper.iconResId
import cn.wj.android.cashbook.core.data.helper.nameResId
import cn.wj.android.cashbook.core.design.component.CommonTopBar
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.feature.assets.R
import cn.wj.android.cashbook.feature.assets.enums.EditAssetBottomSheetEnum
import cn.wj.android.cashbook.feature.assets.viewmodel.EditAssetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun EditAssetRoute(
    onBackClick: () -> Unit,
    assetId: Long
) {
    EditAssetScreen(
        onBackClick = onBackClick,
        assetId = assetId,
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun EditAssetScreen(
    onBackClick: () -> Unit,
    assetId: Long,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = if (assetId == -1L) ModalBottomSheetValue.HalfExpanded else ModalBottomSheetValue.Hidden),
    viewModel: EditAssetViewModel = hiltViewModel(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    // 底部菜单类型
    val bottomSheet: EditAssetBottomSheetEnum by viewModel.bottomSheetData.collectAsStateWithLifecycle()
    // 资产类型
    val classification: AssetClassificationEnum by viewModel.classification.collectAsStateWithLifecycle()
    // 是否是信用卡
    val creditCard: Boolean by viewModel.creditCard.collectAsStateWithLifecycle()
    // 是否有银行信息
    val hasBankInfo: Boolean by viewModel.hasBankInfo.collectAsStateWithLifecycle()
    // 资产名提示
    val assetHint: String? by viewModel.assetHint.collectAsStateWithLifecycle()
    // 总金额
    val totalAmount: String by viewModel.totalAmount.collectAsStateWithLifecycle()
    // 总金额提示
    val totalAmountHint: String? by viewModel.totalAmountHint.collectAsStateWithLifecycle()
    // 余额
    val balance: String by viewModel.balance.collectAsStateWithLifecycle()
    // 开户行
    val openBank: String by viewModel.openBank.collectAsStateWithLifecycle()
    // 卡号
    val cardNo: String by viewModel.cardNo.collectAsStateWithLifecycle()
    // 备注
    val remark: String by viewModel.remark.collectAsStateWithLifecycle()
    // 账单日
    val billingDate: String by viewModel.billingDate.collectAsStateWithLifecycle()
    // 还款日
    val repaymentDate: String by viewModel.repaymentDate.collectAsStateWithLifecycle()

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            when (bottomSheet) {
                EditAssetBottomSheetEnum.CLASSIFICATION_TYPE -> {
                    SelectAssetClassificationTypeSheet(
                        onItemClick = { type, classification ->
                            viewModel.onTypeChanged(type)
                            if (classification == AssetClassificationEnum.BANK_CARD || classification == AssetClassificationEnum.CREDIT_CARD) {
                                // 银行卡和信用卡，继续选择银行
                                coroutineScope.launch {
                                    sheetState.hide()
                                    viewModel.onSelectBankCard()
                                    sheetState.show()
                                }
                            } else {
                                // 其它类型选择
                                viewModel.onClassificationChanged(classification)
                                coroutineScope.launch {
                                    sheetState.hide()
                                }
                            }
                        }
                    )
                }

                EditAssetBottomSheetEnum.ASSET_CLASSIFICATION -> {
                    SelectAssetClassificationSheet(
                        onItemClick = { classification ->
                            viewModel.onClassificationChanged(classification)
                            coroutineScope.launch {
                                sheetState.hide()
                            }
                        }
                    )
                }
            }
        }) {
        Scaffold(
            topBar = {
                CommonTopBar(
                    text = stringResource(id = if (assetId == -1L) R.string.new_asset else R.string.edit_asset),
                    onBackClick = onBackClick
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { viewModel.trySaveAsset { onBackClick() } }) {
                    Icon(imageVector = Icons.Default.SaveAs, contentDescription = null)
                }
            },
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable {
                            viewModel.onSelectTypeClick()
                            coroutineScope.launch {
                                sheetState.show()
                            }
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.asset_classification),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(id = classification.iconResId),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                    Text(
                        text = stringResource(id = classification.nameResId),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowRight,
                        contentDescription = null,
                    )
                }

                Divider(
                    modifier = Modifier.height(8.dp),
                    color = DividerDefaults.color.copy(alpha = 0.1f)
                )

                // 资产名
                var assetName: String by remember {
                    viewModel.assetName
                }
                val isError = !assetHint.isNullOrBlank()
                val supportingTextL: @Composable (() -> Unit)? = if (isError) {
                    { Text(text = assetHint.orEmpty()) }
                } else {
                    null
                }
                TextField(
                    value = assetName,
                    onValueChange = {
                        assetName = it
                        viewModel.onAssetNameChanged(it)
                    },
                    label = { Text(text = stringResource(id = R.string.asset_name)) },
                    supportingText = supportingTextL,
                    isError = isError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .padding(horizontal = 8.dp),
                )

                if (creditCard) {
                    // 信用卡总额度
                    EditAssetTextField(
                        initializedText = totalAmount,
                        label = stringResource(id = R.string.total_amount),
                        supportingText = totalAmountHint,
                        isError = !totalAmountHint.isNullOrBlank(),
                        onValueChange = viewModel::onTotalAmountChanged,
                        onValueVerify = {
                            // 只允许输入正负小数、整数
                            it.matches(Regex(PATTERN_SIGN_MONEY))
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                    )
                }

                // 余额、已用额度
                EditAssetTextField(
                    initializedText = balance,
                    label = stringResource(id = if (creditCard) R.string.arrears else R.string.balance),
                    onValueChange = viewModel::onBalanceChanged,
                    onValueVerify = {
                        // 只允许输入正负小数、整数
                        it.matches(Regex(PATTERN_SIGN_MONEY))
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal)
                )

                if (hasBankInfo) {
                    // 开户行
                    EditAssetTextField(
                        initializedText = openBank,
                        label = stringResource(id = R.string.open_bank),
                        onValueChange = viewModel::onOpenBankChanged
                    )

                    // 卡号
                    EditAssetTextField(
                        initializedText = cardNo,
                        label = stringResource(id = R.string.card_no),
                        onValueChange = viewModel::onCardNoChanged,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                }

                // 备注
                EditAssetTextField(
                    initializedText = remark,
                    label = stringResource(id = R.string.remark),
                    onValueChange = viewModel::onRemarkChanged
                )

            }
        }
    }

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
                .padding(16.dp)
        )
        Divider()

        LazyColumn(
            content = {
                ClassificationTypeEnum.values().forEach { type ->
                    stickyHeader {
                        Text(
                            text = stringResource(id = type.nameResId),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                .padding(16.dp)
        )

        Divider()

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

@Composable
internal fun EditAssetTextField(
    initializedText: String,
    label: String,
    supportingText: String? = null,
    isError: Boolean = false,
    onValueChange: (String) -> Unit,
    onValueVerify: ((String) -> Boolean)? = null,
    maxLength: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    CompatTextField(
        initializedText = initializedText,
        label = label,
        supportingText = supportingText,
        isError = isError,
        onValueChange = onValueChange,
        onValueVerify = onValueVerify,
        maxLength = maxLength,
        singleLine = true,
        keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .padding(horizontal = 8.dp),
    )
}