package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.Calculator
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.design.component.DatePickerDialog
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.component.TimePickerDialog
import cn.wj.android.cashbook.core.design.component.rememberSnackbarHostState
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import cn.wj.android.cashbook.feature.records.model.DateTimePickerModel
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordUiState
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordViewModel

/**
 * 编辑记录
 *
 * @param recordId 记录id，`-1` 为新建
 * @param typeId 类型 id，默认为 `-1`
 * @param typeListContent 类型列表布局，参数：(类型大类, 类型大类选择回调, 类型选择回调) -> [Unit]
 * @param assetBottomSheetContent 选择资产抽屉布局，参数：(已选择类型id, 已选择资产id, 是否是关联资产, 资产选择回调) -> [Unit]
 * @param tagBottomSheetContent 选择标签抽屉布局，参数：(已选择标签id列表, 标签id列表变化回调) -> [Unit]
 * @param onRequestPopBackStack 导航到上一级
 */
@Composable
internal fun EditRecordRoute(
    recordId: Long,
    typeId: Long,
    typeListContent: @Composable (RecordTypeCategoryEnum, (RecordTypeCategoryEnum) -> Unit, (Long) -> Unit) -> Unit,
    assetBottomSheetContent: @Composable (Long, Long, Boolean, (Long) -> Unit) -> Unit,
    tagBottomSheetContent: @Composable (List<Long>, (List<Long>) -> Unit) -> Unit,
    onRequestNaviToSelectRelatedRecord: () -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRecordViewModel = hiltViewModel<EditRecordViewModel>().apply {
        updateRecordId(recordId)
        updateType(typeId)
    },
) {

    val savingHintText = stringResource(id = R.string.record_saving)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTypeCategory by viewModel.selectedTypeCategoryData.collectAsStateWithLifecycle()
    val tagText by viewModel.tagTextData.collectAsStateWithLifecycle()
    val selectedTagIdList by viewModel.displayTagIdListData.collectAsStateWithLifecycle()

    EditRecordScreen(
        uiState = uiState,
        dialogState = viewModel.dialogState,
        onRequestDismissDialog = viewModel::dismissDialog,
        onRecordTimeClick = viewModel::displayDatePickerDialog,
        onDatePickerConfirm = viewModel::pickDate,
        onTimePickerConfirm = viewModel::pickTime,
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        onRequestDismissBookmark = viewModel::dismissBookmark,
        onBackClick = onRequestPopBackStack,
        selectedTypeCategory = selectedTypeCategory,
        onTypeCategorySelect = viewModel::updateTypeCategory,
        bottomSheetType = viewModel.bottomSheetType,
        onRequestDismissBottomSheet = viewModel::dismissBottomSheet,
        onAmountClick = viewModel::displayAmountSheet,
        onAmountChange = viewModel::updateAmount,
        onChargesClick = viewModel::displayChargesSheet,
        onChargesChange = viewModel::updateCharge,
        onConcessionsClick = viewModel::displayConcessions,
        onConcessionsChange = viewModel::updateConcessions,
        typeListContent = {
            (uiState as? EditRecordUiState.Success)?.run {
                typeListContent(
                    selectedTypeCategory,
                    viewModel::updateTypeCategory,
                    viewModel::updateType,
                )
            }

        },
        onRemarkChange = viewModel::updateRemark,
        onAssetClick = viewModel::displayAssetSheet,
        onRelatedAssetClick = viewModel::displayRelatedAssetSheet,
        selectAssetBottomSheetContent = {
            (uiState as? EditRecordUiState.Success)?.run {
                assetBottomSheetContent(
                    selectedTypeId,
                    selectedAssetId,
                    false,
                    viewModel::updateAsset
                )
            }
        },
        selectRelatedAssetBottomSheetContent = {
            (uiState as? EditRecordUiState.Success)?.run {
                assetBottomSheetContent(
                    selectedTypeId,
                    selectedAssetId,
                    true,
                    viewModel::updateRelatedAsset
                )
            }
        },
        tagText = tagText,
        onTagClick = viewModel::displayTagSheet,
        selectTagBottomSheetContent = {
            tagBottomSheetContent(selectedTagIdList, viewModel::updateTag)
        },
        onReimbursableClick = viewModel::switchReimbursable,
        onRelatedRecordClick = onRequestNaviToSelectRelatedRecord,
        onSaveClick = {
            viewModel.trySave(
                hintText = savingHintText,
                onSuccess = onRequestPopBackStack,
            )
        },
        modifier = modifier,
    )
}

/**
 * 编辑记录
 *
 * @param uiState 界面 UI 状态
 * @param shouldDisplayBookmark 显示提示数据
 * @param onRequestDismissBookmark 隐藏提示
 * @param dialogState 弹窗状态
 * @param onRequestDismissDialog 隐藏弹窗
 * @param onRecordTimeClick 记录时间点击回调
 * @param onDatePickerConfirm 日期选择确认回调
 * @param onTimePickerConfirm 时间选择确认回调
 * @param selectedTypeCategory 已选择大类
 * @param onTypeCategorySelect 记录大类修改回调
 * @param bottomSheetType 底部抽屉类型
 * @param onRequestDismissBottomSheet 隐藏底部抽屉
 * @param onAmountClick 金额点击回调
 * @param onAmountChange 金额变化回调
 * @param onChargesClick 手续费点击回调
 * @param onChargesChange 手续费变化回调
 * @param onConcessionsClick 优惠点击回调
 * @param onConcessionsChange 优惠变化回调
 * @param typeListContent 类型列表布局，参数：(头布局, 脚布局) -> [Unit]
 * @param onRemarkChange 备注变化回调
 * @param onAssetClick 资产点击回调
 * @param onRelatedAssetClick 关联资产点击回调
 * @param selectAssetBottomSheetContent 选择资产底部抽屉
 * @param selectRelatedAssetBottomSheetContent 选择关联资产底部抽屉
 * @param tagText 标签显示文本
 * @param onTagClick 标签点击回调
 * @param selectTagBottomSheetContent 选择标签底部抽屉
 * @param onReimbursableClick 可报销点击回调
 * @param onSaveClick 保存点击回调
 * @param onBackClick 返回点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditRecordScreen(
    uiState: EditRecordUiState,
    shouldDisplayBookmark: EditRecordBookmarkEnum,
    onRequestDismissBookmark: () -> Unit,
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    onRecordTimeClick: () -> Unit,
    onDatePickerConfirm: (String) -> Unit,
    onTimePickerConfirm: (String) -> Unit,
    selectedTypeCategory: RecordTypeCategoryEnum,
    onTypeCategorySelect: (RecordTypeCategoryEnum) -> Unit,
    bottomSheetType: EditRecordBottomSheetEnum,
    onRequestDismissBottomSheet: () -> Unit,
    onAmountClick: () -> Unit,
    onAmountChange: (String) -> Unit,
    onChargesClick: () -> Unit,
    onChargesChange: (String) -> Unit,
    onConcessionsClick: () -> Unit,
    onRelatedRecordClick: () -> Unit,
    onConcessionsChange: (String) -> Unit,
    typeListContent: @Composable () -> Unit,
    onRemarkChange: (String) -> Unit,
    onAssetClick: () -> Unit,
    onRelatedAssetClick: () -> Unit,
    selectAssetBottomSheetContent: @Composable () -> Unit,
    selectRelatedAssetBottomSheetContent: @Composable () -> Unit,
    tagText: String,
    onTagClick: () -> Unit,
    selectTagBottomSheetContent: @Composable () -> Unit,
    onReimbursableClick: () -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
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
            if (tipText.isBlank()) {
                onRequestDismissBookmark()
            } else {
                val showSnackbarResult = snackbarHostState.showSnackbar(tipText)
                if (SnackbarResult.Dismissed == showSnackbarResult) {
                    onRequestDismissBookmark()
                }
            }
        }
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
                        onDismissRequest = onRequestDismissBottomSheet,
                        sheetState = rememberModalBottomSheetState(
                            confirmValueChange = {
                                if (it == SheetValue.Hidden) {
                                    onRequestDismissBottomSheet()
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
                                selectedTypeCategory.typeColor,
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
                                onDismissRequest = onRequestDismissDialog,
                                onPositiveButtonClick = onDatePickerConfirm,
                                onNegativeButtonClick = onRequestDismissDialog,
                                selection = model.dateMs,
                            )
                        }

                        is DateTimePickerModel.TimePicker -> {
                            TimePickerDialog(
                                onDismissRequest = onRequestDismissDialog,
                                onPositiveButtonClick = onTimePickerConfirm,
                                onNegativeButtonClick = onRequestDismissDialog,
                                selection = model.timeMs,
                            )
                        }
                    }
                }

                EditRecordScaffoldContent(
                    uiState = uiState,
                    typeListContent = typeListContent,
                    selectedTypeCategory = selectedTypeCategory,
                    typeColor = selectedTypeCategory.typeColor,
                    onAmountClick = onAmountClick,
                    onRemarkChange = onRemarkChange,
                    onAssetClick = onAssetClick,
                    onRelatedAssetClick = onRelatedAssetClick,
                    tagText = tagText,
                    onTagClick = onTagClick,
                    onReimbursableClick = onReimbursableClick,
                    onChargesClick = onChargesClick,
                    onConcessionsClick = onConcessionsClick,
                    onRelatedRecordClick = onRelatedRecordClick,
                    onRecordTimeClick = onRecordTimeClick,
                )
            }
        },
    )
}

/**
 * 编辑记录
 *
 * @param uiState 界面 UI 状态
 * @param selectedTypeCategory 已选择大类
 * @param typeColor 分类主色调
 * @param onAmountClick 金额点击回调
 * @param onChargesClick 手续费点击回调
 * @param onConcessionsClick 优惠点击回调
 * @param typeListContent 类型列表布局，参数：(头布局, 脚布局) -> [Unit]
 * @param onRemarkChange 备注变化回调
 * @param onAssetClick 资产点击回调
 * @param onRelatedAssetClick 关联资产点击回调
 * @param tagText 标签显示文本
 * @param onTagClick 标签点击回调
 * @param onReimbursableClick 可报销点击回调
 * @param onRecordTimeClick 记录时间点击回调
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun EditRecordScaffoldContent(
    uiState: EditRecordUiState,
    typeListContent: @Composable () -> Unit,
    selectedTypeCategory: RecordTypeCategoryEnum,
    typeColor: Color,
    onAmountClick: () -> Unit,
    onRemarkChange: (String) -> Unit,
    onAssetClick: () -> Unit,
    onRelatedAssetClick: () -> Unit,
    tagText: String,
    onTagClick: () -> Unit,
    onReimbursableClick: () -> Unit,
    onChargesClick: () -> Unit,
    onConcessionsClick: () -> Unit,
    onRelatedRecordClick: () -> Unit,
    onRecordTimeClick: () -> Unit,
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
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // 金额显示
                    Amount(
                        amount = uiState.amountText,
                        primaryColor = typeColor,
                        onAmountClick = onAmountClick,
                    )
                    Divider()
                    Text(
                        text = stringResource(id = R.string.record_type),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )

                    typeListContent()

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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                            onClick = onRecordTimeClick,
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

                        // 关联的支出记录
                        if (uiState.needRelated) {
                            val hasRelated = uiState.relatedCount > 0
                            FilterChip(
                                selected = hasRelated,
                                onClick = onRelatedRecordClick,
                                label = {
                                    Text(
                                        text = if (hasRelated) {
                                            stringResource(id = R.string.related_record_display_format).format(
                                                uiState.relatedCount,
                                                uiState.relatedAmount.withCNY()
                                            )
                                        } else {
                                            stringResource(id = R.string.related_record)
                                        }
                                    )
                                },
                            )
                        }
                    }
                }

            }
        }
    }
}

/**
 * 编辑记录底部抽屉
 *
 * @param bottomSheetType 抽屉类型
 * @param uiState 界面 UI 状态
 * @param primaryColor 主色调
 * @param onAmountChange 金额变化回调
 * @param onChargesChange 手续费变化回调
 * @param onConcessionsChange 优惠变化回调
 * @param selectAssetBottomSheetContent 选择资产抽屉
 * @param selectRelatedAssetBottomSheetContent 选择关联资产抽屉
 * @param selectTagBottomSheetContent 选择标签抽屉
 */
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

/**
 * 编辑记录标题栏
 *
 * @param uiState 界面 UI 状态
 * @param selectedTab 已选择大类
 * @param onTabSelected 大类变化回调
 * @param onBackClick 返回点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditRecordTopBar(
    uiState: EditRecordUiState,
    selectedTab: RecordTypeCategoryEnum,
    onTabSelected: (RecordTypeCategoryEnum) -> Unit,
    onBackClick: () -> Unit,
) {
    CashbookTopAppBar(
        onBackClick = onBackClick,
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
                            color = selectedTab.typeColor,
                        )
                    },
                    divider = {},
                ) {
                    RecordTypeCategoryEnum.entries.forEach { enum ->
                        Tab(
                            selected = selectedTab == enum,
                            onClick = { onTabSelected(enum) },
                            text = { Text(text = enum.text) },
                            selectedContentColor = selectedTab.typeColor,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
    )
}

internal val RecordTypeCategoryEnum.text: String
    @Composable get() = stringResource(
        id = when (this) {
            RecordTypeCategoryEnum.EXPENDITURE -> R.string.expend
            RecordTypeCategoryEnum.INCOME -> R.string.income
            RecordTypeCategoryEnum.TRANSFER -> R.string.transfer
        }
    )

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