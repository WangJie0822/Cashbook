package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetScaffold
import androidx.compose.material3.ModalBottomSheetState
import androidx.compose.material3.ModalBottomSheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.toIntOrZero
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_TIME
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.design.component.Calculator
import cn.wj.android.cashbook.core.design.component.CashbookBottomSheetScaffold
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.component.rememberSnackbarHostState
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.BackPressHandler
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import cn.wj.android.cashbook.feature.records.model.TabItem
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordNewViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordViewModel
import com.google.accompanist.flowlayout.FlowRow
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun EditRecordRoute(
    recordId: Long,
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    selectTypeList: @Composable (RecordTypeCategoryEnum, RecordTypeEntity?, @Composable LazyGridItemScope.() -> Unit, @Composable LazyGridItemScope.() -> Unit, (RecordTypeEntity?) -> Unit) -> Unit,
    selectAssetBottomSheet: @Composable (RecordTypeEntity?, Boolean, (AssetEntity?) -> Unit) -> Unit,
    selectTagBottomSheet: @Composable (List<Long>, (TagEntity) -> Unit) -> Unit,
    onSelectRelatedRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    EditRecordScreen(
        recordId = recordId,
        onBackClick = onBackClick,
        selectTypeList = selectTypeList,
        onShowSnackbar = onShowSnackbar,
        selectAssetBottomSheet = selectAssetBottomSheet,
        selectTagBottomSheet = selectTagBottomSheet,
        onSelectRelatedRecordClick = onSelectRelatedRecordClick,
        modifier = modifier,
    )
}

/**
 * 编辑记录界面
 *
 * @param onBackClick 返回点击
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun EditRecordScreen(
    recordId: Long,
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    selectTypeList: @Composable (RecordTypeCategoryEnum, RecordTypeEntity?, @Composable LazyGridItemScope.() -> Unit, @Composable LazyGridItemScope.() -> Unit, (RecordTypeEntity?) -> Unit) -> Unit,
    selectAssetBottomSheet: @Composable (RecordTypeEntity?, Boolean, (AssetEntity?) -> Unit) -> Unit,
    selectTagBottomSheet: @Composable (List<Long>, (TagEntity) -> Unit) -> Unit,
    onSelectRelatedRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    viewModel: EditRecordViewModel = hiltViewModel<EditRecordViewModel>().apply {
        updateRecordId(recordId)
    },
) {

    // 选中分类
    val selectedTypeCategory: RecordTypeCategoryEnum by viewModel.typeCategory.collectAsStateWithLifecycle()
    // 金额数据
    val amount: String by viewModel.amountData.collectAsStateWithLifecycle()
    // 选中类型数据
    val selectedType: RecordTypeEntity? by viewModel.selectedTypeData.collectAsStateWithLifecycle()
    // 备注数据
    val remark: String by viewModel.remarkData.collectAsStateWithLifecycle()
    // 资产名
    val assetName: String by viewModel.assetData.collectAsStateWithLifecycle()
    // 关联资产名
    val relatedAssetName: String by viewModel.relatedAssetData.collectAsStateWithLifecycle()
    // 时间
    val dateTime: String by viewModel.dateTimeData.collectAsStateWithLifecycle()
    // 标签
    val tagsIdList: List<Long> by viewModel.tagsIdData.collectAsStateWithLifecycle()
    val tagsText: String by viewModel.tagsTextData.collectAsStateWithLifecycle()
    // 手续费
    val charges: String by viewModel.chargesData.collectAsStateWithLifecycle()
    // 优惠
    val concessions: String by viewModel.concessionsData.collectAsStateWithLifecycle()
    // 是否可报销
    val reimbursable: Boolean by viewModel.reimbursableData.collectAsStateWithLifecycle()
    // 关联支出记录
    val relatedRecordList: Map<Long, String> by viewModel.relatedRecordTextListData.collectAsStateWithLifecycle()

    // 底部菜单状态
    val bottomSheetEnum: EditRecordBottomSheetEnum by viewModel.bottomSheetData.collectAsStateWithLifecycle()

    // 主色调
    val primaryColor = when (selectedTypeCategory) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
    }

    // 提示文本
    val amountMustNotBeNullText = stringResource(id = R.string.amount_must_not_be_zero)
    val typeErrorText = stringResource(id = R.string.please_select_type)
    val saveFailedText = stringResource(id = R.string.save_failure)

    LaunchedEffect(viewModel.shouldDisplayBookmark) {
        if (viewModel.shouldDisplayBookmark != EditRecordBookmarkEnum.NONE) {
            val tipText = when (viewModel.shouldDisplayBookmark) {
                EditRecordBookmarkEnum.AMOUNT_MUST_NOT_BE_ZERO -> amountMustNotBeNullText
                EditRecordBookmarkEnum.TYPE_NOT_MATCH_CATEGORY, EditRecordBookmarkEnum.TYPE_MUST_NOT_BE_NULL -> typeErrorText
                EditRecordBookmarkEnum.SAVE_FAILED -> saveFailedText
                else -> ""
            }
            val showSnackbarResult = onShowSnackbar(tipText, null)
            if (SnackbarResult.Dismissed == showSnackbarResult) {
                viewModel.onBookmarkDismiss()
            }
        }
    }

    ModalBottomSheetScaffold(
        modifier = modifier,
        topBar = {
            EditRecordTopBar(
                selectedTabIndex = selectedTypeCategory.position,
                onTabSelected = viewModel::onTypeCategoryTabSelected,
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            CashbookFloatingActionButton(onClick = {
                coroutineScope.launch {
                    viewModel.saveRecord(onSaveSuccess = onBackClick)
                }
            }) {
                Icon(imageVector = Icons.Default.SaveAs, contentDescription = null)
            }
        },
        sheetState = sheetState,
        sheetContent = {
            // 底部弹窗
            when (bottomSheetEnum) {
                EditRecordBottomSheetEnum.NONE -> Spacer(modifier = Modifier.height(1.dp))

                EditRecordBottomSheetEnum.AMOUNT, EditRecordBottomSheetEnum.CHARGES, EditRecordBottomSheetEnum.CONCESSIONS -> {
                    // 显示计算器弹窗
                    when (bottomSheetEnum) {
                        EditRecordBottomSheetEnum.AMOUNT -> {
                            Calculator(defaultText = amount,
                                primaryColor = primaryColor,
                                onConfirmClick = {
                                    viewModel.onAmountChanged(it)
                                    coroutineScope.launch {
                                        sheetState.hide()
                                    }
                                }
                            )
                        }

                        EditRecordBottomSheetEnum.CHARGES -> {
                            Calculator(defaultText = charges,
                                primaryColor = primaryColor,
                                onConfirmClick = {
                                    viewModel.onChargesChanged(it)
                                    coroutineScope.launch {
                                        sheetState.hide()
                                    }
                                }
                            )
                        }

                        EditRecordBottomSheetEnum.CONCESSIONS -> {
                            Calculator(defaultText = concessions,
                                primaryColor = primaryColor,
                                onConfirmClick = {
                                    viewModel.onConcessionsChanged(it)
                                    coroutineScope.launch {
                                        sheetState.hide()
                                    }
                                }
                            )
                        }

                        else -> {
                            // 无用逻辑
                        }
                    }
                }

                EditRecordBottomSheetEnum.ASSETS, EditRecordBottomSheetEnum.RELATED_ASSETS -> {
                    // 显示选择资产弹窗
                    selectAssetBottomSheet(
                        selectedType,
                        bottomSheetEnum == EditRecordBottomSheetEnum.RELATED_ASSETS
                    ) {
                        if (bottomSheetEnum == EditRecordBottomSheetEnum.ASSETS) {
                            viewModel.onAssetItemClick(it)
                        } else {
                            viewModel.onRelatedAssetItemClick(it)
                        }
                        coroutineScope.launch {
                            sheetState.hide()
                        }
                    }
                }

                EditRecordBottomSheetEnum.TAGS -> {
                    // 显示选择标签弹窗
                    selectTagBottomSheet(tagsIdList) {
                        viewModel.onTagItemClick(it)
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            selectTypeList(
                selectedTypeCategory,
                selectedType,
                {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                            .animateItemPlacement(),
                    ) {
                        // 金额显示
                        Amount(
                            amount = amount,
                            primaryColor = primaryColor,
                            onAmountClick = {
                                viewModel.onBottomSheetAction(EditRecordBottomSheetEnum.AMOUNT)
                                coroutineScope.launch {
                                    sheetState.show()
                                }
                            },
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
                {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                            .animateItemPlacement(),
                    ) {
                        Divider()
                        // 备注信息
                        Remark(
                            remark = remark,
                            onRemarkTextChanged = viewModel::onRemarkTextChanged
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
                            val hasAsset = assetName.isNotBlank()
                            FilterChip(
                                selected = hasAsset,
                                onClick = {
                                    viewModel.onBottomSheetAction(EditRecordBottomSheetEnum.ASSETS)
                                    coroutineScope.launch {
                                        sheetState.show()
                                    }
                                },
                                label = { Text(text = stringResource(id = R.string.target_asset) + if (hasAsset) ":$assetName" else "") },
                            )

                            if (selectedTypeCategory == RecordTypeCategoryEnum.TRANSFER) {
                                // 只有转账类型显示关联资产
                                val hasRelatedAsset = relatedAssetName.isNotBlank()
                                FilterChip(
                                    selected = hasRelatedAsset,
                                    onClick = {
                                        viewModel.onBottomSheetAction(EditRecordBottomSheetEnum.RELATED_ASSETS)
                                        coroutineScope.launch {
                                            sheetState.show()
                                        }
                                    },
                                    label = { Text(text = stringResource(id = R.string.related_asset) + if (hasRelatedAsset) ":$relatedAssetName" else "") },
                                )
                            }

                            val activity = LocalContext.current as FragmentActivity
                            // 记录时间
                            FilterChip(
                                selected = true,
                                onClick = {
                                    // FIXME 临时方案，后续需要修改
                                    var date = dateTime.parseDateLong().dateFormat(
                                        DATE_FORMAT_DATE
                                    )
                                    var time = dateTime.parseDateLong().dateFormat(
                                        DATE_FORMAT_TIME
                                    )
                                    val datePicker = MaterialDatePicker.Builder.datePicker()
                                        .setSelection(dateTime.parseDateLong())
                                        .build()
                                    val timePicker = MaterialTimePicker.Builder()
                                        .setTimeFormat(CLOCK_24H)
                                        .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                                        .setHour(time.split(":").first().toIntOrZero())
                                        .setMinute(time.split(":").last().toIntOrZero())
                                        .build()
                                    timePicker.addOnPositiveButtonClickListener {
                                        time =
                                            (timePicker.hour.completeZero() + ":" + timePicker.minute.completeZero())
                                        viewModel.onDateTimePicked("$date $time")
                                    }
                                    datePicker.addOnPositiveButtonClickListener {
                                        date = it.dateFormat(DATE_FORMAT_DATE)
                                        timePicker.show(activity.supportFragmentManager, null)
                                    }
                                    datePicker.show(activity.supportFragmentManager, null)
                                },
                                label = { Text(text = dateTime) },
                            )

                            // 标签
                            val hasTags = tagsText.isNotBlank()
                            FilterChip(
                                selected = hasTags,
                                onClick = {
                                    viewModel.onBottomSheetAction(EditRecordBottomSheetEnum.TAGS)
                                    coroutineScope.launch {
                                        sheetState.show()
                                    }
                                },
                                label = { Text(text = stringResource(id = R.string.tags) + if (hasTags) ":$tagsText" else "") },
                            )

                            if (selectedTypeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                                // 只有支出类型显示是否可报销
                                FilterChip(
                                    selected = reimbursable,
                                    onClick = viewModel::onReimbursableClick,
                                    leadingIcon = {
                                        if (reimbursable) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    label = { Text(text = stringResource(id = R.string.reimbursable)) },
                                )
                            }

                            // 手续费
                            val hasCharges = charges.isNotBlank()
                            FilterChip(
                                selected = hasCharges,
                                onClick = {
                                    viewModel.onBottomSheetAction(EditRecordBottomSheetEnum.CHARGES)
                                    coroutineScope.launch {
                                        sheetState.show()
                                    }
                                },
                                label = { Text(text = stringResource(id = R.string.charges) + if (hasCharges) ":${Symbol.CNY}$charges" else "") },
                            )

                            if (selectedTypeCategory != RecordTypeCategoryEnum.INCOME) {
                                // 非收入类型才有优惠
                                val hasConcessions = concessions.isNotBlank()
                                FilterChip(
                                    selected = hasConcessions,
                                    onClick = {
                                        viewModel.onBottomSheetAction(EditRecordBottomSheetEnum.CONCESSIONS)
                                        coroutineScope.launch {
                                            sheetState.show()
                                        }
                                    },
                                    label = { Text(text = stringResource(id = R.string.concessions) + if (hasConcessions) ":${Symbol.CNY}$concessions" else "") },
                                )
                            }

                            // 关联的支出记录
                            if (selectedTypeCategory == RecordTypeCategoryEnum.INCOME/* FIXME && selectedType?.needRelated == true*/) {
                                if (relatedRecordList.isEmpty()) {
                                    FilterChip(
                                        selected = false,
                                        onClick = onSelectRelatedRecordClick,
                                        label = { Text(text = stringResource(id = R.string.related_record)) },
                                    )
                                } else {
                                    relatedRecordList.forEach {
                                        FilterChip(
                                            selected = true,
                                            onClick = { /*TODO*/ },
                                            label = { Text(text = it.value) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                viewModel::onTypeClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditRecordTopBar(
    onBackClick: () -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (RecordTypeCategoryEnum) -> Unit,
) {
    // 顶部标签列表
    val tabs = arrayListOf(
        TabItem(
            title = stringResource(id = R.string.expend), type = RecordTypeCategoryEnum.EXPENDITURE
        ),
        TabItem(title = stringResource(id = R.string.income), type = RecordTypeCategoryEnum.INCOME),
        TabItem(
            title = stringResource(id = R.string.transfer), type = RecordTypeCategoryEnum.TRANSFER
        ),
    )
    TopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        title = {
            TabRow(
                modifier = Modifier.fillMaxSize(),
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                divider = {},
            ) {
                tabs.forEach { tabItem ->
                    Tab(
                        selected = selectedTabIndex == tabItem.type.position,
                        onClick = { onTabSelected(tabItem.type) },
                        text = { Text(text = tabItem.title) },
                        selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Remark(
    remark: String,
    onRemarkTextChanged: (String) -> Unit,
) {
    CompatTextField(
        initializedText = remark,
        label = stringResource(id = R.string.remark),
        onValueChange = onRemarkTextChanged,
        colors = TextFieldDefaults.outlinedTextFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
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
            .clickable { onAmountClick() },
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

@Composable
internal fun EditRecordRouteNew(
    recordId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRecordNewViewModel = hiltViewModel<EditRecordNewViewModel>().apply {
        updateRecordId(recordId)
    },
) {

//    EditRecordScreenNew(
//        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
//        dismissBookmark = viewModel::dismissBookmark,
//        onBackClick = onBackClick,
//        selectedTypeCategory =,
//        onTypeCategorySelect =,
//        bottomSheetType =,
//        dismissBottomSheet = { /*TODO*/ },
//        amount =,
//        onAmountClick = { /*TODO*/ },
//        onAmountChange =,
//        charges =,
//        onChargesClick = { /*TODO*/ },
//        onChargesChange =,
//        concessions =,
//        onConcessionsClick = { /*TODO*/ },
//        onConcessionsChange =,
//        typeListContent =,
//        selectedTypeId =,
//        onTypeSelect =,
//        remark =,
//        onRemarkChange =,
//        assetName =,
//        onAssetClick = { /*TODO*/ },
//        onAssetChange =,
//        relatedAssetName =,
//        onRelatedAssetClick = { /*TODO*/ },
//        onRelatedAssetChange =,
//        dateTime =,
//        onDateTimeChange =,
//        tagText =,
//        onTagClick = { /*TODO*/ },
//        onTagChange = { /*TODO*/ },
//        reimbursable =,
//        onReimbursableClick = { /*TODO*/ },
//        modifier = modifier,
//    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditRecordScreenNew(
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
    amount: String,
    onAmountClick: () -> Unit,
    onAmountChange: (String) -> Unit,
    // 手续费
    charges: String,
    onChargesClick: () -> Unit,
    onChargesChange: (String) -> Unit,
    // 优惠
    concessions: String,
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
    selectedTypeId: Long,
    onTypeSelect: (Long) -> Unit,
    // 备注
    remark: String,
    onRemarkChange: (String) -> Unit,
    // 资产
    assetName: String,
    onAssetClick: () -> Unit,
    onAssetChange: (Long) -> Unit,
    // 关联资产
    relatedAssetName: String,
    onRelatedAssetClick: () -> Unit,
    onRelatedAssetChange: (Long) -> Unit,
    // 时间
    dateTime: String,
    onDateTimeChange: (String) -> Unit,
    // 标签
    tagText: String,
    onTagClick: () -> Unit,
    onTagChange: () -> Unit,
    // 报销
    reimbursable: Boolean,
    onReimbursableClick: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { value ->
            if (value == ModalBottomSheetValue.Hidden) {
                dismissBottomSheet()
            }
            true
        }),
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

    // sheet 状态
    if (sheetState.isVisible) {
        // sheet 显示时，返回隐藏 sheet
        BackPressHandler {
            dismissBottomSheet()
        }
    }
    LaunchedEffect(bottomSheetType) {
        if (bottomSheetType == EditRecordBottomSheetEnum.NONE) {
            sheetState.hide()
        } else {
            sheetState.show()
        }
    }

    // 主色调
    val primaryColor = when (selectedTypeCategory) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
    }

    // 备注文本
    val remarkTextState = remember {
        TextFieldState(
            defaultText = remark,
            filter = { text ->
                onRemarkChange(text)
                true
            },
        )
    }

    CashbookBottomSheetScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EditRecordTopBarNew(
                selectedTab = selectedTypeCategory,
                onTabSelected = onTypeCategorySelect,
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            CashbookFloatingActionButton(onClick = {
                // TODO
            }) {
                Icon(imageVector = Icons.Default.SaveAs, contentDescription = null)
            }
        },
        sheetState = sheetState,
        sheetContent = {
            when (bottomSheetType) {

                EditRecordBottomSheetEnum.AMOUNT, EditRecordBottomSheetEnum.CHARGES, EditRecordBottomSheetEnum.CONCESSIONS -> {
                    // 显示计算器弹窗
                    when (bottomSheetType) {
                        EditRecordBottomSheetEnum.AMOUNT -> {
                            Calculator(
                                defaultText = amount,
                                primaryColor = primaryColor,
                                onConfirmClick = onAmountChange,
                            )
                        }

                        EditRecordBottomSheetEnum.CHARGES -> {
                            Calculator(
                                defaultText = charges,
                                primaryColor = primaryColor,
                                onConfirmClick = onChargesChange,
                            )
                        }

                        EditRecordBottomSheetEnum.CONCESSIONS -> {
                            Calculator(
                                defaultText = concessions,
                                primaryColor = primaryColor,
                                onConfirmClick = onConcessionsChange,
                            )
                        }

                        else -> {
                            // empty block
                        }
                    }
                }

                EditRecordBottomSheetEnum.ASSETS, EditRecordBottomSheetEnum.RELATED_ASSETS -> {
                    // TODO 显示选择资产弹窗

                }

                EditRecordBottomSheetEnum.TAGS -> {
                    // TODO 显示选择标签弹窗

                }

                EditRecordBottomSheetEnum.NONE -> {
                    // empty block
                }
            }
        },
        content = {
            typeListContent(
                modifier = Modifier.padding(it),
                typeCategory = selectedTypeCategory,
                selectedTypeId = selectedTypeId,
                onTypeSelect = onTypeSelect,
                headerContent = { modifier ->
                    Column(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    ) {
                        // 金额显示
                        Amount(
                            amount = amount,
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
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Divider()

                        // 备注信息
                        CompatTextField(
                            textFieldState = remarkTextState,
                            label = { Text(text = stringResource(id = R.string.remark)) },
                            colors = TextFieldDefaults.outlinedTextFieldColors(),
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
                            val hasAsset = assetName.isNotBlank()
                            FilterChip(
                                selected = hasAsset,
                                onClick = onAssetClick,
                                label = { Text(text = stringResource(id = R.string.target_asset) + if (hasAsset) ":$assetName" else "") },
                            )

                            if (selectedTypeCategory == RecordTypeCategoryEnum.TRANSFER) {
                                // 只有转账类型显示关联资产
                                val hasRelatedAsset = relatedAssetName.isNotBlank()
                                FilterChip(
                                    selected = hasRelatedAsset,
                                    onClick = onRelatedAssetClick,
                                    label = { Text(text = stringResource(id = R.string.related_asset) + if (hasRelatedAsset) ":$relatedAssetName" else "") },
                                )
                            }

                            // 记录时间
                            (LocalContext.current as? FragmentActivity)?.supportFragmentManager?.let { fm ->
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        var date = dateTime.parseDateLong().dateFormat(
                                            DATE_FORMAT_DATE
                                        )
                                        var time = dateTime.parseDateLong().dateFormat(
                                            DATE_FORMAT_TIME
                                        )
                                        val datePicker = MaterialDatePicker.Builder.datePicker()
                                            .setSelection(dateTime.parseDateLong())
                                            .build()
                                        val timePicker = MaterialTimePicker.Builder()
                                            .setTimeFormat(CLOCK_24H)
                                            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                                            .setHour(time.split(":").first().toIntOrZero())
                                            .setMinute(time.split(":").last().toIntOrZero())
                                            .build()
                                        timePicker.addOnPositiveButtonClickListener {
                                            time =
                                                (timePicker.hour.completeZero() + ":" + timePicker.minute.completeZero())
                                            onDateTimeChange("$date $time")
                                        }
                                        datePicker.addOnPositiveButtonClickListener { timeMs ->
                                            date = timeMs.dateFormat(DATE_FORMAT_DATE)
                                            timePicker.show(fm, "timePicker")
                                        }
                                        datePicker.show(fm, "datePicker")
                                    },
                                    label = { Text(text = dateTime) },
                                )
                            }


                            // 标签
                            val hasTag = tagText.isNotBlank()
                            FilterChip(
                                selected = hasTag,
                                onClick = onTagClick,
                                label = { Text(text = stringResource(id = R.string.tags) + if (hasTag) ":$tagText" else "") },
                            )

                            if (selectedTypeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                                // 只有支出类型显示是否可报销
                                FilterChip(
                                    selected = reimbursable,
                                    onClick = onReimbursableClick,
                                    leadingIcon = {
                                        if (reimbursable) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    label = { Text(text = stringResource(id = R.string.reimbursable)) },
                                )
                            }

                            // 手续费
                            val hasCharges = charges.isNotBlank()
                            FilterChip(
                                selected = hasCharges,
                                onClick = onChargesClick,
                                label = { Text(text = stringResource(id = R.string.charges) + if (hasCharges) ":${Symbol.CNY}$charges" else "") },
                            )

                            if (selectedTypeCategory != RecordTypeCategoryEnum.INCOME) {
                                // 非收入类型才有优惠
                                val hasConcessions = concessions.isNotBlank()
                                FilterChip(
                                    selected = hasConcessions,
                                    onClick = onConcessionsClick,
                                    label = { Text(text = stringResource(id = R.string.concessions) + if (hasConcessions) ":${Symbol.CNY}$concessions" else "") },
                                )
                            }

                            // TODO 关联的支出记录
                            if (selectedTypeCategory == RecordTypeCategoryEnum.INCOME/* FIXME && selectedType?.needRelated == true*/) {
//                                if (relatedRecordList.isEmpty()) {
//                                    FilterChip(
//                                        selected = false,
//                                        onClick = onSelectRelatedRecordClick,
//                                        label = { Text(text = stringResource(id = R.string.related_record)) },
//                                    )
//                                } else {
//                                    relatedRecordList.forEach {
//                                        FilterChip(
//                                            selected = true,
//                                            onClick = { /*TODO*/ },
//                                            label = { Text(text = it.value) },
//                                        )
//                                    }
//                                }
                            }
                        }
                    }
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditRecordTopBarNew(
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
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
            }
        },
        title = {
            TabRow(
                modifier = Modifier.fillMaxSize(),
                selectedTabIndex = selectedTab.position,
                containerColor = Color.Unspecified,
                contentColor = Color.Unspecified,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.position]),
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
        },
    )
}

@DevicePreviews
@Composable
private fun EditRecordScreenPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            EditRecordScreenNew(
                shouldDisplayBookmark = EditRecordBookmarkEnum.NONE,
                dismissBookmark = {},
                selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                onTypeCategorySelect = {},
                onBackClick = {},
                bottomSheetType = EditRecordBottomSheetEnum.NONE,
                dismissBottomSheet = {},
                amount = "100",
                onAmountClick = {},
                onAmountChange = {},
                charges = "10",
                onChargesClick = {},
                onChargesChange = {},
                concessions = "13",
                onConcessionsClick = {},
                onConcessionsChange = {},
                typeListContent = { modifier, _, _, _, headerContent, footerContent ->
                    Column(
                        modifier = modifier,
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
                selectedTypeId = -1L,
                onTypeSelect = {},
                assetName = "微信",
                onAssetClick = {},
                onAssetChange = {},
                relatedAssetName = "",
                onRelatedAssetClick = {},
                onRelatedAssetChange = {},
                dateTime = "2023-07-01 11:30",
                onDateTimeChange = {},
                tagText = "",
                onTagClick = {},
                onTagChange = {},
                remark = "",
                onRemarkChange = {},
                reimbursable = false,
                onReimbursableClick = {},
            )
        }
    }
}