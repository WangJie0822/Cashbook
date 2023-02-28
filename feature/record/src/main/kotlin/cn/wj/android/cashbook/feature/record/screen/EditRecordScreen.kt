@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalMaterialApi::class,
)

package cn.wj.android.cashbook.feature.record.screen

import cn.wj.android.cashbook.core.ui.R as UIR
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetLayout
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.RECORD_TYPE_COLUMNS
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RECORD_TYPE_SETTINGS
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.record.R
import cn.wj.android.cashbook.feature.record.enums.AssetListTypeEnum
import cn.wj.android.cashbook.feature.record.enums.BottomSheetEnum
import cn.wj.android.cashbook.feature.record.model.TabItem
import cn.wj.android.cashbook.feature.record.viewmodel.EditRecordViewModel
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun EditRecordRoute(
    onBackClick: () -> Unit,
    onTypeSettingClick: () -> Unit,
    onAddAssetClick: () -> Unit,
    viewModel: EditRecordViewModel = hiltViewModel(),
) {
    // 选中分类
    val selectedTypeCategory: RecordTypeCategoryEnum by viewModel.typeCategory.collectAsStateWithLifecycle()
    // 金额数据
    val amount: String by viewModel.amountData.collectAsStateWithLifecycle()
    // 类型列表数据
    val typeList: List<RecordTypeEntity> by viewModel.typeListData.collectAsStateWithLifecycle()
    // 备注数据
    val remark: String by viewModel.remarkData.collectAsStateWithLifecycle()
    // 资产名
    val assetName: String by viewModel.assetData.collectAsStateWithLifecycle()
    // 关联资产名
    val relatedAssetName: String by viewModel.relatedAssetData.collectAsStateWithLifecycle()
    // 时间
    val dateTime: String by viewModel.dateTimeData.collectAsStateWithLifecycle()
    // 标签
    val tags: String by viewModel.tagsData.collectAsStateWithLifecycle()
    // 手续费
    val charges: String by viewModel.chargesData.collectAsStateWithLifecycle()
    // 优惠
    val concessions: String by viewModel.concessionsData.collectAsStateWithLifecycle()
    // 是否可报销
    val reimbursable: Boolean by viewModel.reimbursableData.collectAsStateWithLifecycle()

    // 底部菜单状态
    val bottomSheetEnum: BottomSheetEnum by viewModel.showBottomSheet.collectAsStateWithLifecycle()

    // 资产数据
    val assetList: List<AssetEntity> by viewModel.assetListData.collectAsStateWithLifecycle()

    EditRecordScreen(
        onBackClick = onBackClick,
        selectedTypeCategory = selectedTypeCategory,
        onTabSelected = viewModel::onTypeCategoryTabSelected,
        amount = amount,
        onAmountClick = { viewModel.onBottomSheetAction(BottomSheetEnum.AMOUNT) },
        typeList = typeList,
        onTypeClick = viewModel::onTypeClick,
        onTypeSettingClick = onTypeSettingClick,
        remark = remark,
        onRemarkTextChanged = viewModel::onRemarkTextChanged,
        assetName = assetName,
        onAssetClick = { viewModel.onBottomSheetAction(BottomSheetEnum.ASSETS) },
        relatedAssetName = relatedAssetName,
        onRelatedAssetClick = { viewModel.onBottomSheetAction(BottomSheetEnum.ASSETS) },
        dateTime = dateTime,
        onDateTimeClick = {},
        tags = tags,
        onTagsClick = { viewModel.onBottomSheetAction(BottomSheetEnum.TAGS) },
        charges = charges,
        onChargesClick = {},
        concessions = concessions,
        onConcessionsClick = {},
        reimbursable = reimbursable,
        onReimbursableClick = {},
        bottomSheetEnum = bottomSheetEnum,
        assetList = assetList,
        onAssetItemClick = viewModel::onAssetItemClick,
        onAddAssetClick = onAddAssetClick,
        onSaveClick = viewModel::trySaveRecord,
    )
}

/**
 * 编辑记录界面
 *
 * @param onBackClick 返回点击
 */
@Composable
internal fun EditRecordScreen(
    onBackClick: () -> Unit,
    selectedTypeCategory: RecordTypeCategoryEnum,
    onTabSelected: (RecordTypeCategoryEnum) -> Unit,
    amount: String,
    onAmountClick: () -> Unit,
    typeList: List<RecordTypeEntity>,
    onTypeClick: (RecordTypeEntity) -> Unit,
    onTypeSettingClick: () -> Unit,
    remark: String,
    onRemarkTextChanged: (String) -> Unit,
    assetName: String,
    onAssetClick: () -> Unit,
    relatedAssetName: String,
    onRelatedAssetClick: () -> Unit,
    dateTime: String,
    onDateTimeClick: () -> Unit,
    tags: String,
    onTagsClick: () -> Unit,
    charges: String,
    onChargesClick: () -> Unit,
    concessions: String,
    onConcessionsClick: () -> Unit,
    reimbursable: Boolean,
    onReimbursableClick: () -> Unit,
    bottomSheetEnum: BottomSheetEnum,
    onSaveClick: () -> Unit,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
    assetList: List<AssetEntity>,
    onAssetItemClick: (AssetEntity?) -> Unit,
    onAddAssetClick: () -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    // 主色调
    val primaryColor = when (selectedTypeCategory) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            // TODO 底部弹窗
            when (bottomSheetEnum) {
                BottomSheetEnum.NONE -> Spacer(modifier = Modifier.height(1.dp))

                BottomSheetEnum.AMOUNT -> Text(
                    text = "amount",
                    modifier = Modifier.defaultMinSize(minHeight = 400.dp)
                )

                BottomSheetEnum.ASSETS -> AssetBottomSheet(
                    assetList = assetList,
                    onAssetItemClick = {
                        onAssetItemClick(it)
                        coroutineScope.launch {
                            sheetState.hide()
                        }
                    },
                    onAddAssetClick = {
                        onAddAssetClick()
                        coroutineScope.launch {
                            sheetState.hide()
                        }
                    },
                )

                BottomSheetEnum.TAGS -> Text(
                    text = "tags",
                    modifier = Modifier.defaultMinSize(minHeight = 800.dp)
                )
            }

        }) {
        Scaffold(topBar = {
            EditRecordTopBar(
                selectedTabIndex = selectedTypeCategory.position,
                onTabSelected = onTabSelected,
                onBackClick = onBackClick,
            )
        }, floatingActionButton = {
            FloatingActionButton(onClick = onSaveClick) {
                Icon(imageVector = Icons.Default.SaveAs, contentDescription = null)
            }
        }) { paddingValues ->
            EditRecordLazyGrid(modifier = Modifier.padding(paddingValues)) {
                item(
                    span = {
                        GridItemSpan(maxLineSpan)
                    },
                ) {
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
                                onAmountClick()
                                coroutineScope.launch {
                                    sheetState.show()
                                }
                            },
                        )
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(id = R.string.record_type),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        )
                    }
                }
                // 分类列表
                items(typeList) {
                    if (it == RECORD_TYPE_SETTINGS) {
                        // 设置项
                        TypeItem(
                            modifier = Modifier.animateItemPlacement(),
                            first = true,
                            shapeType = it.shapeType,
                            iconResId = UIR.drawable.vector_baseline_settings_24,
                            showMore = false,
                            title = stringResource(id = R.string.settings),
                            selected = true,
                            onTypeClick = onTypeSettingClick,
                        )
                    } else {
                        TypeItem(
                            modifier = Modifier.animateItemPlacement(),
                            first = it.parentId == -1L,
                            shapeType = it.shapeType,
                            iconResId = it.iconResId,
                            showMore = it.child.isNotEmpty(),
                            title = it.name,
                            selected = it.selected,
                            onTypeClick = { onTypeClick(it) },
                        )
                    }
                }
                item(
                    span = {
                        GridItemSpan(maxLineSpan)
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                            .animateItemPlacement(),
                    ) {
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                        )
                        // 备注信息
                        Remark(remark = remark, onRemarkTextChanged = onRemarkTextChanged)

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
                                    onAssetClick()
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
                                        onRelatedAssetClick()
                                        coroutineScope.launch {
                                            sheetState.show()
                                        }
                                    },
                                    label = { Text(text = stringResource(id = R.string.related_asset) + if (hasRelatedAsset) ":$relatedAssetName" else "") },
                                )
                            }

                            // 记录时间
                            FilterChip(
                                selected = true,
                                onClick = onDateTimeClick,
                                label = { Text(text = dateTime) },
                            )

                            // 标签
                            val hasTags = tags.isNotBlank()
                            FilterChip(
                                selected = hasTags,
                                onClick = {
                                    onTagsClick()
                                    coroutineScope.launch {
                                        sheetState.show()
                                    }
                                },
                                label = { Text(text = stringResource(id = R.string.tags) + if (hasTags) ":$tags" else "") },
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
                                label = { Text(text = stringResource(id = R.string.charges) + if (hasCharges) ":$charges" else "") },
                            )

                            // 优惠
                            val hasConcessions = concessions.isNotBlank()
                            FilterChip(
                                selected = hasConcessions,
                                onClick = onConcessionsClick,
                                label = { Text(text = stringResource(id = R.string.concessions) + if (hasConcessions) ":$concessions" else "") },
                            )
                        }
                    }
                }
            }
        }
    }
}

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

@Composable
internal fun EditRecordLazyGrid(modifier: Modifier = Modifier, content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(RECORD_TYPE_COLUMNS),
        content = content,
    )
}

@Composable
internal fun Remark(
    remark: String,
    onRemarkTextChanged: (String) -> Unit,
) {
    TextField(
        value = remark,
        onValueChange = onRemarkTextChanged,
        label = {
            Text(
                text = stringResource(id = R.string.remark),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
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
            text = Symbol.rmb,
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
internal fun TypeItem(
    modifier: Modifier = Modifier,
    first: Boolean,
    shapeType: Int,
    iconResId: Int,
    showMore: Boolean,
    title: String,
    selected: Boolean,
    onTypeClick: () -> Unit
) {
    // 背景颜色，用于区分一级分类、二级分类
    val backgroundColor =
        if (first) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val backgroundShape = if (first) {
        RectangleShape
    } else {
        when (shapeType) {
            -1 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
            1 -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
            else -> RectangleShape
        }
    }
    // 列表数据
    ConstraintLayout(
        modifier = modifier
            .padding(top = 4.dp, bottom = 4.dp)
            .background(color = backgroundColor, shape = backgroundShape)
            .clickable(onClick = onTypeClick)
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        // 约束条件
        val (iconBg, iconMore, text) = createRefs()
        // 根据选中状态显示主要颜色
        val color =
            if (selected) LocalExtendedColors.current.selected else LocalExtendedColors.current.unselected
        // 记录类型对应的图标，使用圆形边框
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(32.dp)
                .border(border = BorderStroke(1.dp, color), shape = CircleShape)
                .clip(CircleShape)
                .padding(4.dp)
                .constrainAs(iconBg) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
        if (showMore) {
            // 横向菜单标记，一级分类有二级分类时显示
            Icon(
                modifier = Modifier
                    .size(12.dp)
                    .border(border = BorderStroke(1.dp, backgroundColor), shape = CircleShape)
                    .clip(CircleShape)
                    .constrainAs(iconMore) {
                        bottom.linkTo(iconBg.bottom)
                        end.linkTo(iconBg.end)
                    },
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                tint = color
            )
        }
        // 类型名称
        Text(
            modifier = Modifier.constrainAs(text) {
                top.linkTo(iconBg.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            }, text = title, color = color, style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * 选择资产菜单
 */
@Composable
internal fun AssetBottomSheet(
    assetList: List<AssetEntity>,
    onAssetItemClick: (AssetEntity?) -> Unit,
    onAddAssetClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        content = {
            item {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    val (title, subTitle, add) = createRefs()
                    Text(
                        text = "选择一个资产账户",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.constrainAs(title) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                        },
                    )
                    Text(
                        text = "无法选择隐藏资产",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.constrainAs(subTitle) {
                            top.linkTo(title.bottom, 8.dp)
                            start.linkTo(parent.start)
                        },
                    )
                    TextButton(
                        modifier = Modifier.constrainAs(add) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        },
                        onClick = onAddAssetClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Text(text = "添加")
                    }
                }
            }
            item {
                AssetItem(
                    type = AssetListTypeEnum.NO_SELECT,
                    name = "不选择账户",
                    iconResId = R.drawable.vector_baseline_not_select_24,
                    balance = "",
                    totalAmount = "",
                    onAssetItemClick = { onAssetItemClick(null) },
                )
            }
            items(assetList) {
                val type = if (it.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    AssetListTypeEnum.CREDIT_CARD
                } else {
                    AssetListTypeEnum.CAPITAL
                }
                AssetItem(
                    type = type,
                    name = it.name,
                    iconResId = it.iconResId,
                    balance = it.balance,
                    totalAmount = it.totalAmount,
                    onAssetItemClick = { onAssetItemClick(it) },
                )
            }
        },
    )
}

@Composable
internal fun AssetItem(
    type: AssetListTypeEnum,
    name: String,
    iconResId: Int,
    balance: String,
    totalAmount: String,
    onAssetItemClick: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .clickable {
                onAssetItemClick()
            }
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        val isNoSelect = type == AssetListTypeEnum.NO_SELECT
        val isCreditCard = type == AssetListTypeEnum.CREDIT_CARD

        val (iconRef, nameRef, balanceRef, progressRef, usedRef) = createRefs()

        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.constrainAs(iconRef) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            },
        )
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.constrainAs(nameRef) {
                start.linkTo(iconRef.end, 8.dp)
                top.linkTo(parent.top)
                if (!isCreditCard) {
                    bottom.linkTo(parent.bottom)
                } else {
                    bottom.linkTo(progressRef.top)
                }
            },
        )

        if (!isNoSelect) {
            Text(
                text = Symbol.rmb + if (isCreditCard) totalAmount else balance,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.constrainAs(balanceRef) {
                    end.linkTo(parent.end)
                    top.linkTo(nameRef.top)
                    bottom.linkTo(nameRef.bottom)
                },
            )
        }

        if (isCreditCard) {
            // 信用卡类型
            var floatTotalAmount = totalAmount.toFloatOrNull() ?: 1f
            if (floatTotalAmount == 0f) {
                floatTotalAmount = 1f
            }
            val progress = (balance.toFloatOrNull() ?: 0f) / floatTotalAmount
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.constrainAs(progressRef) {
                    start.linkTo(nameRef.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            )
            Text(
                text = stringResource(id = R.string.used_with_colon) + Symbol.rmb + balance,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.constrainAs(usedRef) {
                    start.linkTo(progressRef.start)
                    top.linkTo(progressRef.bottom)
                },
            )
        }
    }
}