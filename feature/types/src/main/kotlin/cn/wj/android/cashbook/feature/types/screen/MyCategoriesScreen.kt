package cn.wj.android.cashbook.feature.types.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.RECORD_TYPE_COLUMNS
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TransparentListItem
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.types.enums.MyCategoriesBookmarkEnum
import cn.wj.android.cashbook.feature.types.model.ExpandableRecordTypeModel
import cn.wj.android.cashbook.feature.types.viewmodel.MyCategoriesDialogData
import cn.wj.android.cashbook.feature.types.viewmodel.MyCategoriesUiState
import cn.wj.android.cashbook.feature.types.viewmodel.MyCategoriesViewModel

@Composable
internal fun MyCategoriesRoute(
    onRequestNaviToEdiType: (Long, Long) -> Unit,
    onRequestNaviToTypeStatistics: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyCategoriesViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyCategoriesScreen(
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        onRequestDismissBookmark = viewModel::dismissBookmark,
        dialogState = viewModel.dialogState,
        onRequestDismissDialog = viewModel::dismissDialog,
        uiState = uiState,
        onRequestSelectTypeCategory = viewModel::selectTypeCategory,
        onRequestEditType = { onRequestNaviToEdiType(it, -1L) },
        onRequestChangeFirstTypeToSecond = viewModel::requestChangeFirstTypeToSecond,
        onRequestAddFirstType = { onRequestNaviToEdiType(-1L, -1L) },
        onRequestAddSecondType = { onRequestNaviToEdiType(-1L, it) },
        changeFirstTypeToSecond = viewModel::changeTypeToSecond,
        onRequestChangeSecondTypeToFirst = viewModel::changeSecondTypeToFirst,
        onRequestMoveSecondTypeToAnother = viewModel::requestMoveSecondTypeToAnother,
        onRequestNaviToTypeStatistics = onRequestNaviToTypeStatistics,
        onRequestDeleteType = viewModel::requestDeleteType,
        changeRecordTypeBeforeDelete = viewModel::changeRecordTypeBeforeDeleteType,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@Composable
internal fun MyCategoriesScreen(
    shouldDisplayBookmark: MyCategoriesBookmarkEnum,
    onRequestDismissBookmark: () -> Unit,
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    uiState: MyCategoriesUiState,
    onRequestSelectTypeCategory: (RecordTypeCategoryEnum) -> Unit,
    onRequestEditType: (Long) -> Unit,
    onRequestChangeFirstTypeToSecond: (Long) -> Unit,
    onRequestAddFirstType: () -> Unit,
    onRequestAddSecondType: (Long) -> Unit,
    changeFirstTypeToSecond: (Long, Long) -> Unit,
    onRequestChangeSecondTypeToFirst: (Long) -> Unit,
    onRequestMoveSecondTypeToAnother: (Long, Long) -> Unit,
    onRequestNaviToTypeStatistics: (Long) -> Unit,
    onRequestDeleteType: (Long) -> Unit,
    changeRecordTypeBeforeDelete: (Long, Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // 提示文本
    val changeFirstTypeHasChildHintText =
        stringResource(id = R.string.change_first_type_has_child_hint)
    val deleteFirstTypeHasChildHintText =
        stringResource(id = R.string.delete_first_type_has_child_hint)
    val protectedTypeHintText = stringResource(id = R.string.protected_type_hint)
    val deleteSuccessHintText = stringResource(id = R.string.delete_success)
    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark != MyCategoriesBookmarkEnum.DISMISS) {
            val hintText = when (shouldDisplayBookmark) {
                MyCategoriesBookmarkEnum.CHANGE_FIRST_TYPE_HAS_CHILD -> changeFirstTypeHasChildHintText
                MyCategoriesBookmarkEnum.DELETE_FIRST_TYPE_HAS_CHILD -> deleteFirstTypeHasChildHintText
                MyCategoriesBookmarkEnum.PROTECTED_TYPE -> protectedTypeHintText
                MyCategoriesBookmarkEnum.DELETE_SUCCESS -> deleteSuccessHintText
                else -> ""
            }
            if (hintText.isNotBlank()) {
                val result = snackbarHostState.showSnackbar(hintText)
                if (result == SnackbarResult.Dismissed) {
                    onRequestDismissBookmark()
                }
            } else {
                onRequestDismissBookmark()
            }
        }
    }

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            MyCategoriesTopBar(
                uiState = uiState,
                onTabSelected = onRequestSelectTypeCategory,
                onBackClick = onRequestPopBackStack,
            )
        },
        floatingActionButton = {
            CashbookFloatingActionButton(onClick = onRequestAddFirstType) {
                Icon(imageVector = CashbookIcons.Add, contentDescription = null)
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            (dialogState as? DialogState.Shown<*>)?.data?.let { data ->
                if (data is MyCategoriesDialogData.SelectFirstType) {
                    SelectFirstTypeDialog(
                        onRequestDismissDialog = onRequestDismissDialog,
                        data = data,
                        changeFirstTypeToSecond = changeFirstTypeToSecond,
                    )
                } else if (data is MyCategoriesDialogData.DeleteType) {
                    DeleteTypeDialog(
                        onRequestDismissDialog = onRequestDismissDialog,
                        data = data,
                        changeRecordTypeBeforeDelete = changeRecordTypeBeforeDelete,
                    )
                }
            }

            when (uiState) {
                MyCategoriesUiState.Loading -> {
                    Loading(modifier = Modifier.align(Alignment.Center))
                }

                is MyCategoriesUiState.Success -> {
                    if (uiState.typeList.isEmpty()) {
                        Empty(hintText = stringResource(id = R.string.type_no_data))
                    } else {
                        Column {
                            Text(
                                text = stringResource(id = R.string.click_arrow_expand_more),
                                color = LocalContentColor.current.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            ExpandableTypeList(
                                typeList = uiState.typeList,
                                onRequestEditType = onRequestEditType,
                                onRequestDeleteType = onRequestDeleteType,
                                onRequestChangeFirstTypeToSecond = onRequestChangeFirstTypeToSecond,
                                onRequestAddSecondType = onRequestAddSecondType,
                                onRequestNaviToTypeStatistics = onRequestNaviToTypeStatistics,
                                onRequestChangeSecondTypeToFirst = onRequestChangeSecondTypeToFirst,
                                onRequestMoveSecondTypeToAnother = onRequestMoveSecondTypeToAnother,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteTypeDialog(
    onRequestDismissDialog: () -> Unit,
    data: MyCategoriesDialogData.DeleteType,
    changeRecordTypeBeforeDelete: (Long, Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onRequestDismissDialog,
        title = {
            Text(
                text = stringResource(id = R.string.select_type_to_move_before_delete_format).format(
                    data.recordSize
                )
            )
        },
        text = {
            DialogExpandableTypeList(
                typeList = data.expandableTypeList,
                onTypeItemClick = {
                    changeRecordTypeBeforeDelete(data.id, it)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onRequestDismissDialog) {
                Text(text = stringResource(id = R.string.cancel))
            }
        })
}

@Composable
private fun SelectFirstTypeDialog(
    onRequestDismissDialog: () -> Unit,
    data: MyCategoriesDialogData.SelectFirstType,
    changeFirstTypeToSecond: (Long, Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onRequestDismissDialog,
        title = { Text(text = stringResource(id = R.string.select_first_type_to_move)) },
        text = {
            LazyColumn(content = {
                items(items = data.typeList) { first ->
                    TransparentListItem(
                        leadingContent = {
                            Icon(
                                painter = painterDrawableResource(idStr = first.iconName),
                                contentDescription = null,
                            )
                        },
                        headlineContent = { Text(text = first.name) },
                        modifier = Modifier.clickable {
                            changeFirstTypeToSecond(data.id, first.id)
                        },
                    )
                }
            })
        },
        confirmButton = {
            TextButton(onClick = onRequestDismissDialog) {
                Text(text = stringResource(id = R.string.cancel))
            }
        })
}

@Composable
private fun ExpandableTypeList(
    typeList: List<ExpandableRecordTypeModel>,
    onRequestEditType: (Long) -> Unit,
    onRequestDeleteType: (Long) -> Unit,
    onRequestChangeFirstTypeToSecond: (Long) -> Unit,
    onRequestAddSecondType: (Long) -> Unit,
    onRequestNaviToTypeStatistics: (Long) -> Unit,
    onRequestChangeSecondTypeToFirst: (Long) -> Unit,
    onRequestMoveSecondTypeToAnother: (Long, Long) -> Unit,
) {
    LazyColumn(
        content = {
            typeList.forEach { first ->
                val hasChild = first.list.isNotEmpty()
                item {
                    FirstTypeItem(
                        first = first,
                        hasChild = hasChild,
                        onRequestEditType = onRequestEditType,
                        onRequestDeleteType = onRequestDeleteType,
                        onRequestChangeFirstTypeToSecond = onRequestChangeFirstTypeToSecond,
                        onRequestAddSecondType = onRequestAddSecondType,
                        onRequestNaviToTypeStatistics = onRequestNaviToTypeStatistics
                    )
                    if (first.expanded && hasChild) {
                        SecondTypeList(
                            first = first,
                            onRequestEditType = onRequestEditType,
                            onRequestDeleteType = onRequestDeleteType,
                            onRequestChangeSecondTypeToFirst = onRequestChangeSecondTypeToFirst,
                            onRequestMoveSecondTypeToAnother = onRequestMoveSecondTypeToAnother,
                            onRequestNaviToTypeStatistics = onRequestNaviToTypeStatistics,
                        )
                    }
                }
            }
            item {
                Footer(hintText = stringResource(id = R.string.footer_hint_default))
            }
        },
    )
}

@Composable
private fun DialogExpandableTypeList(
    typeList: List<ExpandableRecordTypeModel>,
    onTypeItemClick: (Long) -> Unit,
) {
    LazyColumn(
        content = {
            typeList.forEach { first ->
                val hasChild = first.list.isNotEmpty()
                item {
                    TransparentListItem(
                        leadingContent = {
                            Icon(
                                painter = painterDrawableResource(idStr = first.data.iconName),
                                contentDescription = null,
                            )
                        },
                        headlineContent = { Text(text = first.data.name) },
                        trailingContent = {
                            if (hasChild) {
                                IconButton(onClick = { first.expanded = !first.expanded }) {
                                    Icon(
                                        imageVector = if (first.expanded) {
                                            CashbookIcons.KeyboardArrowDown
                                        } else {
                                            CashbookIcons.KeyboardArrowRight
                                        },
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onTypeItemClick(first.data.id) },
                    )
                    if (first.expanded && hasChild) {
                        ElevatedCard(Modifier.padding(horizontal = 8.dp)) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val col = if (first.list.size % RECORD_TYPE_COLUMNS == 0) {
                                    first.list.size / RECORD_TYPE_COLUMNS
                                } else {
                                    first.list.size / RECORD_TYPE_COLUMNS + 1
                                }
                                for (c in 0 until col) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        for (r in 0 until RECORD_TYPE_COLUMNS) {
                                            val index = c * RECORD_TYPE_COLUMNS + r
                                            if (index >= first.list.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            } else {
                                                val second = first.list[index]
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(8.dp)
                                                        .clickable {
                                                            onTypeItemClick(second.id)
                                                        },
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                ) {
                                                    Icon(
                                                        painter = painterDrawableResource(idStr = second.iconName),
                                                        contentDescription = null,
                                                        tint = LocalExtendedColors.current.unselected,
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(
                                                                color = Color.Unspecified,
                                                                shape = CircleShape
                                                            )
                                                            .clip(CircleShape)
                                                            .padding(4.dp),
                                                    )
                                                    Text(
                                                        text = second.name,
                                                        color = LocalExtendedColors.current.unselected,
                                                        style = MaterialTheme.typography.labelMedium,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Footer(hintText = stringResource(id = R.string.footer_hint_default))
            }
        },
    )
}

@Composable
private fun FirstTypeItem(
    first: ExpandableRecordTypeModel,
    hasChild: Boolean,
    onRequestEditType: (Long) -> Unit,
    onRequestDeleteType: (Long) -> Unit,
    onRequestChangeFirstTypeToSecond: (Long) -> Unit,
    onRequestAddSecondType: (Long) -> Unit,
    onRequestNaviToTypeStatistics: (Long) -> Unit
) {
    Box {
        var expandedMenu by remember {
            mutableStateOf(false)
        }
        TransparentListItem(
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            ),
            leadingContent = {
                Icon(
                    painter = painterDrawableResource(idStr = first.data.iconName),
                    contentDescription = null,
                )
            },
            headlineContent = { Text(text = first.data.name) },
            trailingContent = {
                if (hasChild) {
                    IconButton(onClick = { first.expanded = !first.expanded }) {
                        Icon(
                            imageVector = if (first.expanded) {
                                CashbookIcons.KeyboardArrowDown
                            } else {
                                CashbookIcons.KeyboardArrowRight
                            },
                            contentDescription = null
                        )
                    }
                }
            },
            modifier = Modifier.clickable { expandedMenu = true },
        )
        DropdownMenu(
            expanded = expandedMenu,
            onDismissRequest = { expandedMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.edit)) },
                onClick = {
                    expandedMenu = false
                    onRequestEditType(first.data.id)
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.delete)) },
                onClick = {
                    expandedMenu = false
                    onRequestDeleteType(first.data.id)
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.change_to_second_type)) },
                onClick = {
                    expandedMenu = false
                    onRequestChangeFirstTypeToSecond(first.data.id)
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.add_second_type)) },
                onClick = {
                    expandedMenu = false
                    onRequestAddSecondType(first.data.id)
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.statistic_data)) },
                onClick = {
                    expandedMenu = false
                    onRequestNaviToTypeStatistics(first.data.id)
                },
            )
        }
    }
}

@Composable
private fun SecondTypeList(
    first: ExpandableRecordTypeModel,
    onRequestEditType: (Long) -> Unit,
    onRequestDeleteType: (Long) -> Unit,
    onRequestChangeSecondTypeToFirst: (Long) -> Unit,
    onRequestMoveSecondTypeToAnother: (Long, Long) -> Unit,
    onRequestNaviToTypeStatistics: (Long) -> Unit,
) {
    ElevatedCard(Modifier.padding(horizontal = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val col = if (first.list.size % RECORD_TYPE_COLUMNS == 0) {
                first.list.size / RECORD_TYPE_COLUMNS
            } else {
                first.list.size / RECORD_TYPE_COLUMNS + 1
            }
            for (c in 0 until col) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (r in 0 until RECORD_TYPE_COLUMNS) {
                        val index = c * RECORD_TYPE_COLUMNS + r
                        if (index >= first.list.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val second = first.list[index]
                            SecondTypeItem(
                                second = second,
                                onRequestEditType = onRequestEditType,
                                onRequestDeleteType = onRequestDeleteType,
                                onRequestChangeSecondTypeToFirst = onRequestChangeSecondTypeToFirst,
                                onRequestMoveSecondTypeToAnother = onRequestMoveSecondTypeToAnother,
                                onRequestNaviToTypeStatistics = onRequestNaviToTypeStatistics,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondTypeItem(
    second: RecordTypeModel,
    onRequestEditType: (Long) -> Unit,
    onRequestDeleteType: (Long) -> Unit,
    onRequestChangeSecondTypeToFirst: (Long) -> Unit,
    onRequestMoveSecondTypeToAnother: (Long, Long) -> Unit,
    onRequestNaviToTypeStatistics: (Long) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
    ) {
        var expandedMenu by remember {
            mutableStateOf(false)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable {
                    expandedMenu = true
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterDrawableResource(idStr = second.iconName),
                contentDescription = null,
                tint = LocalExtendedColors.current.unselected,
                modifier = Modifier
                    .size(32.dp)
                    .background(color = Color.Unspecified, shape = CircleShape)
                    .clip(CircleShape)
                    .padding(4.dp),
            )
            Text(
                text = second.name,
                color = LocalExtendedColors.current.unselected,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        DropdownMenu(
            expanded = expandedMenu,
            onDismissRequest = {
                expandedMenu = false
            },
        ) {
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(id = R.string.edit))
                },
                onClick = {
                    expandedMenu = false
                    onRequestEditType(second.id)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(id = R.string.delete))
                },
                onClick = {
                    expandedMenu = false
                    onRequestDeleteType(second.id)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(id = R.string.change_to_first_type))
                },
                onClick = {
                    expandedMenu = false
                    onRequestChangeSecondTypeToFirst(second.id)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(id = R.string.move_to_another_first_type))
                },
                onClick = {
                    expandedMenu = false
                    onRequestMoveSecondTypeToAnother(second.id, second.parentId)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(id = R.string.statistic_data))
                },
                onClick = {
                    expandedMenu = false
                    onRequestNaviToTypeStatistics(second.id)
                },
            )
        }
    }
}

private val RecordTypeCategoryEnum.text: String
    @Composable get() = stringResource(
        id = when (this) {
            RecordTypeCategoryEnum.EXPENDITURE -> R.string.expend
            RecordTypeCategoryEnum.INCOME -> R.string.income
            RecordTypeCategoryEnum.TRANSFER -> R.string.transfer
        }
    )

/**
 * 编辑记录标题栏
 *
 * @param uiState 界面 UI 状态
 * @param onTabSelected 大类变化回调
 * @param onBackClick 返回点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyCategoriesTopBar(
    uiState: MyCategoriesUiState,
    onTabSelected: (RecordTypeCategoryEnum) -> Unit,
    onBackClick: () -> Unit,
) {
    CashbookTopAppBar(
        onBackClick = onBackClick,
        title = {
            if (uiState is MyCategoriesUiState.Success) {
                val selectedTab = uiState.selectedTab
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
                    RecordTypeCategoryEnum.entries.forEach { enum ->
                        Tab(
                            selected = selectedTab == enum,
                            onClick = { onTabSelected(enum) },
                            text = { Text(text = enum.text) },
                            selectedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            unselectedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        },
    )
}