package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.Calculator
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.design.component.DatePickerDialog
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.component.TimePickerDialog
import cn.wj.android.cashbook.core.design.component.rememberSnackbarHostState
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import cn.wj.android.cashbook.feature.records.model.DateTimePickerModel
import cn.wj.android.cashbook.feature.records.model.TabItem
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordUiState
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordViewModel
import com.google.accompanist.flowlayout.FlowRow

@Composable
internal fun EditRecordRoute(
    recordId: Long,
    typeId: Long,
    typeListContent: @Composable (
        modifier: Modifier,
        typeCategory: RecordTypeCategoryEnum,
        selectedTypeId: Long,
        onTypeSelect: (Long) -> Unit,
        headerContent: @Composable (modifier: Modifier) -> Unit,
        footerContent: @Composable (modifier: Modifier) -> Unit,
    ) -> Unit,
    selectAssetBottomSheetContent: @Composable (
        currentTypeId: Long,
        isRelated: Boolean,
        onAssetChange: (Long) -> Unit,
    ) -> Unit,
    selectTagBottomSheetContent: @Composable (
        selectedTagIdList: List<Long>,
        onTagIdListChange: (List<Long>) -> Unit,
    ) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRecordViewModel = hiltViewModel<EditRecordViewModel>().apply {
        updateRecordId(recordId)
        onTypeSelect(typeId)
    },
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTypeCategory by viewModel.selectedTypeCategoryData.collectAsStateWithLifecycle()
    val tagText by viewModel.tagTextData.collectAsStateWithLifecycle()
    val selectedTagIdList by viewModel.displayTagIdListData.collectAsStateWithLifecycle()

    val selectedTypeId = uiState.selectedTypeId

    EditRecordScreen(
        dialogState = viewModel.dialogState,
        showDatePicker = viewModel::showDatePickerDialog,
        datePickerConfirm = viewModel::datePickerConfirm,
        timePickerConfirm = viewModel::timePickerConfirm,
        dismissDialog = viewModel::dismissDialog,
        uiState = uiState,
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        dismissBookmark = viewModel::dismissBookmark,
        onBackClick = onBackClick,
        selectedTypeCategory = selectedTypeCategory,
        onTypeCategorySelect = viewModel::onTypeCategorySelect,
        bottomSheetType = viewModel.bottomSheetType,
        dismissBottomSheet = viewModel::dismissBottomSheet,
        onAmountClick = viewModel::onAmountClick,
        onAmountChange = viewModel::onAmountChange,
        onChargesClick = viewModel::onChargesClick,
        onChargesChange = viewModel::onChargeChange,
        onConcessionsClick = viewModel::onConcessionsClick,
        onConcessionsChange = viewModel::onConcessionsChange,
        typeListContent = typeListContent,
        onTypeSelect = viewModel::onTypeSelect,
        onRemarkChange = viewModel::onRemarkChange,
        onAssetClick = viewModel::onAssetClick,
        onRelatedAssetClick = viewModel::onRelatedAssetClick,
        selectAssetBottomSheetContent = {
            selectAssetBottomSheetContent(
                currentTypeId = selectedTypeId,
                isRelated = false,
                onAssetChange = viewModel::onAssetChange,
            )
        },
        selectRelatedAssetBottomSheetContent = {
            selectAssetBottomSheetContent(
                currentTypeId = selectedTypeId,
                isRelated = true,
                onAssetChange = viewModel::onRelatedAssetChange,
            )
        },
        tagText = tagText,
        onTagClick = viewModel::onTagClick,
        selectTagBottomSheetContent = {
            selectTagBottomSheetContent(
                selectedTagIdList = selectedTagIdList,
                onTagIdListChange = viewModel::onTagChange,
            )
        },
        onReimbursableClick = viewModel::onReimbursableClick,
        onSaveClick = { viewModel.onSaveClick(onSuccess = onBackClick) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditRecordScreen(
    dialogState: DialogState,
    showDatePicker: () -> Unit,
    datePickerConfirm: (String) -> Unit,
    timePickerConfirm: (String) -> Unit,
    dismissDialog: () -> Unit,
    uiState: EditRecordUiState,
    // Snackbar 提示
    shouldDisplayBookmark: EditRecordBookmarkEnum,
    dismissBookmark: () -> Unit,
    // 标题栏
    onBackClick: () -> Unit,
    selectedTypeCategory: RecordTypeCategoryEnum,
    onTypeCategorySelect: (RecordTypeCategoryEnum) -> Unit,
    // bottom sheet
    bottomSheetType: EditRecordBottomSheetEnum,
    dismissBottomSheet: () -> Unit,
    // 金额
    onAmountClick: () -> Unit,
    onAmountChange: (String) -> Unit,
    // 手续费
    onChargesClick: () -> Unit,
    onChargesChange: (String) -> Unit,
    // 优惠
    onConcessionsClick: () -> Unit,
    onConcessionsChange: (String) -> Unit,
    // 类型列表
    typeListContent: @Composable (
        modifier: Modifier,
        typeCategory: RecordTypeCategoryEnum,
        selectedTypeId: Long,
        onTypeSelect: (Long) -> Unit,
        headerContent: @Composable (modifier: Modifier) -> Unit,
        footerContent: @Composable (modifier: Modifier) -> Unit,
    ) -> Unit,
    onTypeSelect: (Long) -> Unit,
    // 备注
    onRemarkChange: (String) -> Unit,
    // 资产
    onAssetClick: () -> Unit,
    // 关联资产
    onRelatedAssetClick: () -> Unit,
    selectAssetBottomSheetContent: @Composable () -> Unit,
    selectRelatedAssetBottomSheetContent: @Composable () -> Unit,
    // 标签
    tagText: String,
    onTagClick: () -> Unit,
    selectTagBottomSheetContent: @Composable () -> Unit,
    // 报销
    onReimbursableClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
) {

    // 提示文本
    val amountMustNotBeNullText = stringResource(id = R.string.amount_must_not_be_zero)
    val typeErrorText = stringResource(id = R.string.please_select_type)
    val saveFailedText = stringResource(id = R.string.save_failure)
    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark != EditRecordBookmarkEnum.NONE) {
            val tipText = when (shouldDisplayBookmark) {
                EditRecordBookmarkEnum.AMOUNT_MUST_NOT_BE_ZERO -> amountMustNotBeNullText
                EditRecordBookmarkEnum.TYPE_NOT_MATCH_CATEGORY, EditRecordBookmarkEnum.TYPE_MUST_NOT_BE_NULL -> typeErrorText
                EditRecordBookmarkEnum.SAVE_FAILED -> saveFailedText
                else -> ""
            }
            val showSnackbarResult = snackbarHostState.showSnackbar(tipText)
            if (SnackbarResult.Dismissed == showSnackbarResult) {
                dismissBookmark()
            }
        }
    }

    // 主色调
    val primaryColor = when (selectedTypeCategory) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
    }

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            EditRecordTopBar(
                uiState = uiState,
                selectedTab = selectedTypeCategory,
                onTabSelected = onTypeCategorySelect,
                onBackClick = onBackClick,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is EditRecordUiState.Success) {
                CashbookFloatingActionButton(
                    onClick = onSaveClick,
                    content = {
                        Icon(imageVector = CashbookIcons.SaveAs, contentDescription = null)
                    },
                )
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier.padding(paddingValues),
            ) {
                if (bottomSheetType != EditRecordBottomSheetEnum.NONE) {
                    CashbookModalBottomSheet(
                        onDismissRequest = dismissBottomSheet,
                        sheetState = rememberModalBottomSheetState(
                            confirmValueChange = {
                                if (it == SheetValue.Hidden) {
                                    dismissBottomSheet()
                                }
                                true
                            }
                        ),
                        dragHandle = if (bottomSheetType.isCalculator) {
                            null
                        } else {
                            @Composable {
                                BottomSheetDefaults.DragHandle(modifier = Modifier.statusBarsPadding())
                            }
                        },
                        content = {
                            EditRecordBottomSheetContent(
                                bottomSheetType,
                                uiState,
                                primaryColor,
                                onAmountChange,
                                onChargesChange,
                                onConcessionsChange,
                                selectAssetBottomSheetContent,
                                selectRelatedAssetBottomSheetContent,
                                selectTagBottomSheetContent
                            )
                        },
                    )
                }

                ((dialogState as? DialogState.Shown<*>)?.data as? DateTimePickerModel)?.let { model ->
                    when (model) {
                        is DateTimePickerModel.DatePicker -> {
                            DatePickerDialog(
                                onDismissRequest = dismissDialog,
                                onPositiveButtonClick = datePickerConfirm,
                                onNegativeButtonClick = dismissDialog,
                                selection = model.dateMs,
                            )
                        }

                        is DateTimePickerModel.TimePicker -> {
                            TimePickerDialog(
                                onDismissRequest = dismissDialog,
                                onPositiveButtonClick = timePickerConfirm,
                                onNegativeButtonClick = dismissDialog,
                                selection = model.timeMs,
                            )
                        }
                    }
                }

                EditRecordScaffoldContent(
                    uiState = uiState,
                    typeListContent = typeListContent,
                    selectedTypeCategory = selectedTypeCategory,
                    onTypeSelect = onTypeSelect,
                    primaryColor = primaryColor,
                    onAmountClick = onAmountClick,
                    onRemarkChange = onRemarkChange,
                    onAssetClick = onAssetClick,
                    onRelatedAssetClick = onRelatedAssetClick,
                    tagText = tagText,
                    onTagClick = onTagClick,
                    onReimbursableClick = onReimbursableClick,
                    onChargesClick = onChargesClick,
                    onConcessionsClick = onConcessionsClick,
                    showDatePicker = showDatePicker,
                )
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditRecordScaffoldContent(
    uiState: EditRecordUiState,
    typeListContent: @Composable (
        modifier: Modifier,
        typeCategory: RecordTypeCategoryEnum,
        selectedTypeId: Long,
        onTypeSelect: (Long) -> Unit,
        headerContent: @Composable (modifier: Modifier) -> Unit,
        footerContent: @Composable (modifier: Modifier) -> Unit,
    ) -> Unit,
    selectedTypeCategory: RecordTypeCategoryEnum,
    onTypeSelect: (Long) -> Unit,
    primaryColor: Color,
    onAmountClick: () -> Unit,
    onRemarkChange: (String) -> Unit,
    onAssetClick: () -> Unit,
    onRelatedAssetClick: () -> Unit,
    tagText: String,
    onTagClick: () -> Unit,
    onReimbursableClick: () -> Unit,
    onChargesClick: () -> Unit,
    onConcessionsClick: () -> Unit,
    showDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (uiState) {
            EditRecordUiState.Loading -> {
                Loading(modifier = Modifier.align(Alignment.Center))
            }

            is EditRecordUiState.Success -> {
                typeListContent(
                    modifier = Modifier.fillMaxSize(),
                    typeCategory = selectedTypeCategory,
                    selectedTypeId = uiState.selectedTypeId,
                    onTypeSelect = onTypeSelect,
                    headerContent = { modifier ->
                        Column(
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            // 金额显示
                            Amount(
                                amount = uiState.amountText,
                                primaryColor = primaryColor,
                                onAmountClick = onAmountClick,
                            )
                            Divider()
                            Text(
                                text = stringResource(id = R.string.record_type),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                            )
                        }
                    },
                    footerContent = { modifier ->
                        Column(
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Divider()

                            // 备注文本
                            val remarkTextState = remember {
                                TextFieldState(
                                    defaultText = uiState.remarkText,
                                    filter = { text ->
                                        onRemarkChange(text)
                                        true
                                    },
                                )
                            }

                            // 备注信息
                            CompatTextField(
                                textFieldState = remarkTextState,
                                label = { Text(text = stringResource(id = R.string.remark)) },
                                colors = OutlinedTextFieldDefaults.colors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )

                            // 其他选项
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                mainAxisSpacing = 8.dp,
                                crossAxisSpacing = 4.dp,
                            ) {
                                // 目标资产
                                val hasAsset = uiState.assetText.isNotBlank()
                                FilterChip(
                                    selected = hasAsset,
                                    onClick = onAssetClick,
                                    label = { Text(text = stringResource(id = R.string.target_asset) + if (hasAsset) ":${uiState.assetText}" else "") },
                                )

                                if (selectedTypeCategory == RecordTypeCategoryEnum.TRANSFER) {
                                    // 只有转账类型显示关联资产
                                    val hasRelatedAsset =
                                        uiState.relatedAssetText.isNotBlank()
                                    FilterChip(
                                        selected = hasRelatedAsset,
                                        onClick = onRelatedAssetClick,
                                        label = { Text(text = stringResource(id = R.string.related_asset) + if (hasRelatedAsset) ":${uiState.relatedAssetText}" else "") },
                                    )
                                }

                                // 记录时间
                                val dateTime = uiState.dateTimeText
                                FilterChip(
                                    selected = true,
                                    onClick = showDatePicker,
                                    label = { Text(text = dateTime) },
                                )

                                // 标签
                                val hasTag = tagText.isNotBlank()
                                FilterChip(
                                    selected = hasTag,
                                    onClick = onTagClick,
                                    label = { Text(text = stringResource(id = R.string.tags) + if (hasTag) ":$tagText" else "") },
                                )

                                if (selectedTypeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                                    // 只有支出类型显示是否可报销
                                    val reimbursable = uiState.reimbursable
                                    FilterChip(
                                        selected = reimbursable,
                                        onClick = onReimbursableClick,
                                        leadingIcon = {
                                            if (reimbursable) {
                                                Icon(
                                                    imageVector = CashbookIcons.Check,
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        label = { Text(text = stringResource(id = R.string.reimbursable)) },
                                    )
                                }

                                // 手续费
                                val hasCharges = uiState.chargesText.isNotBlank()
                                FilterChip(
                                    selected = hasCharges,
                                    onClick = onChargesClick,
                                    label = { Text(text = stringResource(id = R.string.charges) + if (hasCharges) ":${uiState.chargesText.withCNY()}" else "") },
                                )

                                if (selectedTypeCategory != RecordTypeCategoryEnum.INCOME) {
                                    // 非收入类型才有优惠
                                    val hasConcessions =
                                        uiState.concessionsText.isNotBlank()
                                    FilterChip(
                                        selected = hasConcessions,
                                        onClick = onConcessionsClick,
                                        label = { Text(text = stringResource(id = R.string.concessions) + if (hasConcessions) ":${uiState.concessionsText.withCNY()}" else "") },
                                    )
                                }

                                // TODO 关联的支出记录
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun EditRecordBottomSheetContent(
    bottomSheetType: EditRecordBottomSheetEnum,
    uiState: EditRecordUiState,
    primaryColor: Color,
    onAmountChange: (String) -> Unit,
    onChargesChange: (String) -> Unit,
    onConcessionsChange: (String) -> Unit,
    selectAssetBottomSheetContent: @Composable () -> Unit,
    selectRelatedAssetBottomSheetContent: @Composable () -> Unit,
    selectTagBottomSheetContent: @Composable () -> Unit
) {
    when (bottomSheetType) {
        EditRecordBottomSheetEnum.AMOUNT -> {
            (uiState as? EditRecordUiState.Success)?.let { data ->
                Calculator(
                    defaultText = data.amountText,
                    primaryColor = primaryColor,
                    onConfirmClick = onAmountChange,
                )
            }
        }

        EditRecordBottomSheetEnum.CHARGES -> {
            (uiState as? EditRecordUiState.Success)?.let { data ->
                Calculator(
                    defaultText = data.chargesText,
                    primaryColor = primaryColor,
                    onConfirmClick = onChargesChange,
                )
            }
        }

        EditRecordBottomSheetEnum.CONCESSIONS -> {
            (uiState as? EditRecordUiState.Success)?.let { data ->
                Calculator(
                    defaultText = data.concessionsText,
                    primaryColor = primaryColor,
                    onConfirmClick = onConcessionsChange,
                )
            }
        }

        EditRecordBottomSheetEnum.ASSETS -> {
            // 显示选择资产弹窗
            selectAssetBottomSheetContent()
        }

        EditRecordBottomSheetEnum.RELATED_ASSETS -> {
            // 显示选择关联资产弹窗
            selectRelatedAssetBottomSheetContent()
        }

        EditRecordBottomSheetEnum.TAGS -> {
            // 显示选择标签弹窗
            selectTagBottomSheetContent()
        }

        EditRecordBottomSheetEnum.NONE -> {
            // empty block
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditRecordTopBar(
    uiState: EditRecordUiState,
    onBackClick: () -> Unit,
    selectedTab: RecordTypeCategoryEnum,
    onTabSelected: (RecordTypeCategoryEnum) -> Unit,
) {
    // 顶部标签列表
    val tabs = arrayListOf(
        TabItem(
            title = stringResource(id = R.string.expend),
            type = RecordTypeCategoryEnum.EXPENDITURE,
        ),
        TabItem(title = stringResource(id = R.string.income), type = RecordTypeCategoryEnum.INCOME),
        TabItem(
            title = stringResource(id = R.string.transfer), type = RecordTypeCategoryEnum.TRANSFER
        ),
    )
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = CashbookIcons.ArrowBack, contentDescription = null)
            }
        },
        title = {
            if (uiState is EditRecordUiState.Success) {
                TabRow(
                    modifier = Modifier.fillMaxSize(),
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Unspecified,
                    contentColor = Color.Unspecified,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    },
                    divider = {},
                ) {
                    tabs.forEach { tabItem ->
                        Tab(
                            selected = selectedTab == tabItem.type,
                            onClick = { onTabSelected(tabItem.type) },
                            text = { Text(text = tabItem.title) },
                            selectedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            unselectedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        },
    )
}

/**
 * 金额显示框
 *
 * @param amount 金额
 * @param primaryColor 显示颜色
 * @param onAmountClick 点击回调
 */
@Composable
internal fun Amount(
    amount: String,
    primaryColor: Color,
    onAmountClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAmountClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Symbol.CNY,
            color = primaryColor,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = amount,
            color = primaryColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@DevicePreviews
@Composable
private fun EditRecordScreenPreview() {
    PreviewTheme {
        EditRecordScreen(
            dialogState = DialogState.Dismiss,
            showDatePicker = {},
            datePickerConfirm = {},
            timePickerConfirm = {},
            dismissDialog = {},
            uiState = EditRecordUiState.Success(
                amountText = "100",
                chargesText = "10",
                concessionsText = "",
                remarkText = "备注",
                assetText = "微信(￥1000)",
                relatedAssetText = "",
                dateTimeText = "2023-07-01 11:30",
                reimbursable = false,
                selectedTypeId = -1L,
            ),
            shouldDisplayBookmark = EditRecordBookmarkEnum.NONE,
            dismissBookmark = {},
            selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            onTypeCategorySelect = {},
            onBackClick = {},
            bottomSheetType = EditRecordBottomSheetEnum.NONE,
            dismissBottomSheet = {},
            onAmountClick = {},
            onAmountChange = {},
            onChargesClick = {},
            onChargesChange = {},
            onConcessionsClick = {},
            onConcessionsChange = {},
            typeListContent = { modifier, _, _, _, headerContent, footerContent ->
                Column(
                    modifier = modifier.padding(horizontal = 16.dp),
                ) {
                    headerContent(Modifier)
                    Text(
                        text = "分类列表",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                    )
                    footerContent(Modifier)
                }
            },
            onTypeSelect = {},
            onAssetClick = {},
            onRelatedAssetClick = {},
            selectAssetBottomSheetContent = {},
            selectRelatedAssetBottomSheetContent = {},
            tagText = "",
            onTagClick = {},
            selectTagBottomSheetContent = { },
            onRemarkChange = {},
            onReimbursableClick = {},
            onSaveClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EditRecordLoadingScreenPreview() {
    PreviewTheme {
        EditRecordScreen(
            dialogState = DialogState.Dismiss,
            showDatePicker = {},
            datePickerConfirm = {},
            timePickerConfirm = {},
            dismissDialog = {},
            uiState = EditRecordUiState.Loading,
            shouldDisplayBookmark = EditRecordBookmarkEnum.NONE,
            dismissBookmark = {},
            selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            onTypeCategorySelect = {},
            onBackClick = {},
            bottomSheetType = EditRecordBottomSheetEnum.NONE,
            dismissBottomSheet = {},
            onAmountClick = {},
            onAmountChange = {},
            onChargesClick = {},
            onChargesChange = {},
            onConcessionsClick = {},
            onConcessionsChange = {},
            typeListContent = { modifier, _, _, _, headerContent, footerContent ->
                Column(
                    modifier = modifier.padding(horizontal = 16.dp),
                ) {
                    headerContent(Modifier)
                    Text(
                        text = "分类列表",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                    )
                    footerContent(Modifier)
                }
            },
            onTypeSelect = {},
            onAssetClick = {},
            onRelatedAssetClick = {},
            selectAssetBottomSheetContent = {},
            selectRelatedAssetBottomSheetContent = {},
            tagText = "",
            onTagClick = {},
            selectTagBottomSheetContent = { },
            onRemarkChange = {},
            onReimbursableClick = {},
            onSaveClick = {},
        )
    }
}