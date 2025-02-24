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

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.Calculator
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTab
import cn.wj.android.cashbook.core.design.component.CbTabRow
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.component.rememberSnackbarHostState
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.expand.text
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import cn.wj.android.cashbook.feature.records.model.DateTimePickerModel
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordUiState
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.util.Calendar

/**
 * 编辑记录
 *
 * @param recordId 记录id，`-1` 为新建
 * @param assetId 资产 id，默认为 `-1`
 * @param typeListContent 类型列表布局，参数：(类型大类, 默认类型 id, 类型选择回调) -> [Unit]
 * @param assetBottomSheetContent 选择资产抽屉布局，参数：(已选择类型id, 已选择资产id, 是否是关联资产, 资产选择回调) -> [Unit]
 * @param tagBottomSheetContent 选择标签抽屉布局，参数：(已选择标签id列表, 标签id列表变化回调) -> [Unit]
 * @param onRequestPopBackStack 导航到上一级
 */
@Composable
internal fun EditRecordRoute(
    recordId: Long,
    assetId: Long,
    typeListContent: @Composable (RecordTypeCategoryEnum, Long, (Long) -> Unit) -> Unit,
    assetBottomSheetContent: @Composable (Long, Long, Boolean, (Long) -> Unit) -> Unit,
    tagBottomSheetContent: @Composable (List<Long>, (List<Long>) -> Unit, () -> Unit) -> Unit,
    onRequestNaviToSelectRelatedRecord: () -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRecordViewModel = hiltViewModel<EditRecordViewModel>().apply {
        initRecordId(recordId)
        initAssetId(assetId)
    },
) {
    val savingHintText = stringResource(id = R.string.record_saving)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultTypeId by viewModel.defaultTypeIdData.collectAsStateWithLifecycle()
    val selectedTypeCategory by viewModel.selectedTypeCategoryData.collectAsStateWithLifecycle()
    val tagText by viewModel.tagTextData.collectAsStateWithLifecycle()
    val imageList by viewModel.displayImageData.collectAsStateWithLifecycle()
    val selectedTagIdList by viewModel.displayTagIdListData.collectAsStateWithLifecycle()

    EditRecordScreen(
        uiState = uiState,
        dialogState = viewModel.dialogState,
        onRequestDismissDialog = viewModel::dismissDialog,
        onRecordTimeClick = viewModel::displayDatePickerDialog,
        onDateSelected = viewModel::onDateSelected,
        onTimeSelected = viewModel::onTimeSelected,
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
        onImageListChange = viewModel::updateImageData,
        typeListContent = {
            (uiState as? EditRecordUiState.Success)?.run {
                typeListContent(
                    selectedTypeCategory,
                    defaultTypeId,
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
                    viewModel::updateAsset,
                )
            }
        },
        selectRelatedAssetBottomSheetContent = {
            (uiState as? EditRecordUiState.Success)?.run {
                assetBottomSheetContent(
                    selectedTypeId,
                    selectedAssetId,
                    true,
                    viewModel::updateRelatedAsset,
                )
            }
        },
        tagText = tagText,
        imageList = imageList,
        onTagClick = viewModel::displayTagSheet,
        onImageClick = viewModel::displayImageSheet,
        selectTagBottomSheetContent = {
            tagBottomSheetContent(
                selectedTagIdList,
                viewModel::updateTag,
                viewModel::dismissBottomSheet,
            )
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
 * @param onDateSelected 日期选择确认回调
 * @param onTimeSelected 时间选择确认回调
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
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (String) -> Unit,
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
    onImageListChange: (List<ImageModel>) -> Unit,
    typeListContent: @Composable () -> Unit,
    onRemarkChange: (String) -> Unit,
    onAssetClick: () -> Unit,
    onRelatedAssetClick: () -> Unit,
    selectAssetBottomSheetContent: @Composable () -> Unit,
    selectRelatedAssetBottomSheetContent: @Composable () -> Unit,
    tagText: String,
    imageList: List<ImageModel>,
    onTagClick: () -> Unit,
    onImageClick: () -> Unit,
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

    CbScaffold(
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
                CbFloatingActionButton(
                    onClick = onSaveClick,
                    content = {
                        Icon(imageVector = CbIcons.SaveAs, contentDescription = null)
                    },
                )
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier.padding(paddingValues),
            ) {
                if (bottomSheetType != EditRecordBottomSheetEnum.NONE) {
                    CbModalBottomSheet(
                        onDismissRequest = onRequestDismissBottomSheet,
                        sheetState = rememberModalBottomSheetState(
                            confirmValueChange = {
                                if (it == SheetValue.Hidden) {
                                    onRequestDismissBottomSheet()
                                }
                                true
                            },
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
                                bottomSheetType = bottomSheetType,
                                uiState = uiState,
                                imageList = imageList,
                                primaryColor = selectedTypeCategory.typeColor,
                                onAmountChange = onAmountChange,
                                onChargesChange = onChargesChange,
                                onConcessionsChange = onConcessionsChange,
                                onImageListChange = onImageListChange,
                                selectAssetBottomSheetContent = selectAssetBottomSheetContent,
                                selectRelatedAssetBottomSheetContent = selectRelatedAssetBottomSheetContent,
                                selectTagBottomSheetContent = selectTagBottomSheetContent,
                            )
                        },
                    )
                }

                ((dialogState as? DialogState.Shown<*>)?.data as? DateTimePickerModel)?.let { model ->
                    when (model) {
                        is DateTimePickerModel.DatePicker -> {
                            val datePickerState = rememberDatePickerState(model.dateMs)
                            DatePickerDialog(
                                onDismissRequest = onRequestDismissDialog,
                                confirmButton = {
                                    CbTextButton(
                                        onClick = {
                                            val selectedDateMillis =
                                                datePickerState.selectedDateMillis
                                            if (null == selectedDateMillis) {
                                                onRequestDismissDialog()
                                            } else {
                                                onDateSelected(selectedDateMillis)
                                            }
                                        },
                                    ) {
                                        Text(text = stringResource(id = R.string.confirm))
                                    }
                                },
                                dismissButton = {
                                    CbTextButton(onClick = onRequestDismissDialog) {
                                        Text(text = stringResource(id = R.string.cancel))
                                    }
                                },
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }

                        is DateTimePickerModel.TimePicker -> {
                            val currentTime = Calendar.getInstance().apply {
                                timeInMillis = model.timeMs
                            }
                            val timePickerState = rememberTimePickerState(
                                initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
                                initialMinute = currentTime.get(Calendar.MINUTE),
                                is24Hour = true,
                            )
                            CbAlertDialog(
                                onDismissRequest = onRequestDismissDialog,
                                confirmButton = {
                                    CbTextButton(
                                        onClick = {
                                            onTimeSelected("${timePickerState.hour.completeZero()}:${timePickerState.minute.completeZero()}")
                                        },
                                    ) {
                                        Text(text = stringResource(id = R.string.confirm))
                                    }
                                },
                                dismissButton = {
                                    CbTextButton(onClick = onRequestDismissDialog) {
                                        Text(text = stringResource(id = R.string.cancel))
                                    }
                                },
                                text = {
                                    TimePicker(state = timePickerState)
                                },
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
                    imageList = imageList,
                    onTagClick = onTagClick,
                    onImageClick = onImageClick,
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
@OptIn(ExperimentalLayoutApi::class)
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
    imageList: List<ImageModel>,
    onTagClick: () -> Unit,
    onImageClick: () -> Unit,
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
                        .verticalScroll(state = rememberScrollState())
                        .padding(top = 8.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    // 金额显示
                    Amount(
                        amount = uiState.amountText,
                        primaryColor = typeColor,
                        onAmountClick = onAmountClick,
                    )
                    CbHorizontalDivider()
                    Text(
                        text = stringResource(id = R.string.record_type),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )

                    // 类型列表
                    typeListContent()

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
                    CbTextField(
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
                        ElevatedFilterChip(
                            selected = hasAsset,
                            onClick = onAssetClick,
                            label = { Text(text = stringResource(id = R.string.target_asset) + if (hasAsset) ":${uiState.assetText}" else "") },
                        )

                        if (selectedTypeCategory == RecordTypeCategoryEnum.TRANSFER) {
                            // 只有转账类型显示关联资产
                            val hasRelatedAsset =
                                uiState.relatedAssetText.isNotBlank()
                            ElevatedFilterChip(
                                selected = hasRelatedAsset,
                                onClick = onRelatedAssetClick,
                                label = { Text(text = stringResource(id = R.string.related_asset) + if (hasRelatedAsset) ":${uiState.relatedAssetText}" else "") },
                            )
                        }

                        // 记录时间
                        val dateTime = uiState.dateTimeText
                        ElevatedFilterChip(
                            selected = true,
                            onClick = onRecordTimeClick,
                            label = { Text(text = dateTime) },
                        )

                        // 标签
                        val hasTag = tagText.isNotBlank()
                        ElevatedFilterChip(
                            selected = hasTag,
                            onClick = onTagClick,
                            label = { Text(text = stringResource(id = R.string.tags) + if (hasTag) ":$tagText" else "") },
                        )

                        // 关联图片
                        val imageCount = imageList.size
                        ElevatedFilterChip(
                            selected = imageCount > 0,
                            onClick = {
                                // TODO 显示选择图片
                                onImageClick.invoke()
                            },
                            leadingIcon = {
                                Icon(imageVector = CbIcons.PhotoLibrary, contentDescription = null)
                            },
                            label = { Text(text = imageCount.toString()) },
                        )

                        if (selectedTypeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                            // 只有支出类型显示是否可报销
                            val reimbursable = uiState.reimbursable
                            ElevatedFilterChip(
                                selected = reimbursable,
                                onClick = onReimbursableClick,
                                leadingIcon = {
                                    if (reimbursable) {
                                        Icon(
                                            imageVector = CbIcons.Check,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                label = { Text(text = stringResource(id = R.string.reimbursable)) },
                            )
                        }

                        // 手续费
                        val hasCharges = uiState.chargesText.isNotBlank()
                        ElevatedFilterChip(
                            selected = hasCharges,
                            onClick = onChargesClick,
                            label = { Text(text = stringResource(id = R.string.charges) + if (hasCharges) ":${uiState.chargesText.withCNY()}" else "") },
                        )

                        if (selectedTypeCategory != RecordTypeCategoryEnum.INCOME) {
                            // 非收入类型才有优惠
                            val hasConcessions =
                                uiState.concessionsText.isNotBlank()
                            ElevatedFilterChip(
                                selected = hasConcessions,
                                onClick = onConcessionsClick,
                                label = { Text(text = stringResource(id = R.string.concessions) + if (hasConcessions) ":${uiState.concessionsText.withCNY()}" else "") },
                            )
                        }

                        // 关联的支出记录
                        if (uiState.needRelated) {
                            val hasRelated = uiState.relatedCount > 0
                            ElevatedFilterChip(
                                selected = hasRelated,
                                onClick = onRelatedRecordClick,
                                label = {
                                    Text(
                                        text = if (hasRelated) {
                                            stringResource(id = R.string.related_record_display_format).format(
                                                uiState.relatedCount,
                                                uiState.relatedAmount.withCNY(),
                                            )
                                        } else {
                                            stringResource(id = R.string.related_record)
                                        },
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
    imageList: List<ImageModel>,
    primaryColor: Color,
    onAmountChange: (String) -> Unit,
    onChargesChange: (String) -> Unit,
    onConcessionsChange: (String) -> Unit,
    onImageListChange: (List<ImageModel>) -> Unit,
    selectAssetBottomSheetContent: @Composable () -> Unit,
    selectRelatedAssetBottomSheetContent: @Composable () -> Unit,
    selectTagBottomSheetContent: @Composable () -> Unit,
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

        EditRecordBottomSheetEnum.IMAGES -> {
            // TODO 选择照片
            SelectImageSheetContent(
                imageList = imageList,
                onImageListChange = onImageListChange,
            )
        }

        EditRecordBottomSheetEnum.NONE -> {
            // empty block
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun SelectImageSheetContent(
    imageList: List<ImageModel>,
    onImageListChange: (List<ImageModel>) -> Unit,
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var pictureUri: Uri? = null
    val takePictureResult =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result) {
                onImageListChange.invoke(
                    arrayListOf<ImageModel>().apply {
                        addAll(imageList)
                        add(ImageModel(-1L, -1L, pictureUri?.toString().orEmpty(), byteArrayOf()))
                    },
                )
            }
            pictureUri = null
        }
    val pickMultiplePicture =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uriList ->
            if (uriList.isNotEmpty()) {
                onImageListChange.invoke(
                    arrayListOf<ImageModel>().apply {
                        addAll(imageList)
                        val pathList = imageList.map { it.path }
                        addAll(
                            uriList.filter { !pathList.contains(it.toString()) }.map {
                                ImageModel(-1L, -1L, it.toString(), byteArrayOf())
                            },
                        )
                    },
                )
            }
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            val (title, subTitle, takePicture, pickImage) = createRefs()
            Text(
                text = stringResource(id = R.string.please_select_images),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                },
            )
            Text(
                text = stringResource(id = R.string.allow_multiple_choices),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.constrainAs(subTitle) {
                    top.linkTo(title.bottom, 8.dp)
                    start.linkTo(parent.start)
                },
            )
            CbTextButton(
                modifier = Modifier.constrainAs(takePicture) {
                    top.linkTo(parent.top)
                    end.linkTo(pickImage.start)
                    bottom.linkTo(parent.bottom)
                },
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        val tempFile = File.createTempFile(
                            "CB_IMG_${System.currentTimeMillis()}",
                            ".jpg",
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        ).apply {
                            createNewFile()
                        }
                        pictureUri = FileProvider.getUriForFile(
                            context,
                            "${ApplicationInfo.applicationId}.FileProvider",
                            tempFile,
                        )
                        takePictureResult.launch(pictureUri!!)
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
            ) {
                Text(
                    text = stringResource(id = R.string.take_picture),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            CbTextButton(
                modifier = Modifier.constrainAs(pickImage) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                },
                onClick = {
                    // 从相册选择图片
                    pickMultiplePicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            ) {
                Text(
                    text = stringResource(id = R.string.pick_image),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        CbHorizontalDivider()

        if (imageList.isEmpty()) {
            Empty(
                hintText = stringResource(id = R.string.image_empty_hint),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                columns = GridCells.Fixed(3),
                content = {
                    items(items = imageList) { image ->
                        ConstraintLayout(
                            modifier = Modifier.padding(8.dp),
                        ) {
                            val (iv, delete) = createRefs()
                            AsyncImage(
                                model = image.path.toUri(),
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                modifier = Modifier
                                    .constrainAs(iv) {
                                        centerTo(parent)
                                    }
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Image(
                                imageVector = CbIcons.RemoveCircle,
                                contentDescription = null,
                                modifier = Modifier
                                    .constrainAs(delete) {
                                        top.linkTo(iv.top)
                                        bottom.linkTo(iv.top)
                                        start.linkTo(iv.end)
                                        end.linkTo(iv.end)
                                    }
                                    .clickable {
                                        onImageListChange.invoke(imageList.filter { image.path != it.path })
                                    },
                            )
                        }
                    }
                },
            )
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
    CbTopAppBar(
        onBackClick = onBackClick,
        title = {
            if (uiState is EditRecordUiState.Success) {
                CbTabRow(
                    modifier = Modifier.fillMaxSize(),
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Unspecified,
                    contentColor = Color.Unspecified,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = selectedTab.typeColor,
                        )
                    },
                    divider = {},
                ) {
                    RecordTypeCategoryEnum.entries.forEach { enum ->
                        CbTab(
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
