@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class
)

package cn.wj.android.cashbook.feature.record.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.RECORD_TYPE_COLUMNS
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.record.R
import cn.wj.android.cashbook.feature.record.model.TabItem
import cn.wj.android.cashbook.feature.record.viewmodel.EditRecordViewModel

@Composable
internal fun EditRecordRoute(
    onBackClick: () -> Unit,
    viewModel: EditRecordViewModel = hiltViewModel(),
) {
    // 选中分类
    val selectedTypeCategory: RecordTypeCategoryEnum by viewModel.typeCategory.collectAsStateWithLifecycle()
    // 主色调
    val primaryColor = when (selectedTypeCategory) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
    }
    // 金额数据
    val amount: String by viewModel.amountData.collectAsStateWithLifecycle()
    // 分类列表数据
    val typeList: List<RecordTypeEntity> by viewModel.typeListData.collectAsStateWithLifecycle()

    EditRecordScreen(
        selectedTypeCategory = selectedTypeCategory,
        primaryColor = primaryColor,
        amount = amount,
        typeList = typeList,
        onTypeClick = { viewModel.onTypeClick(it) },
        onTabSelected = { viewModel.onTypeCategoryTabSelected(it) },
        onBackClick = onBackClick,
        onSaveClick = { /*TODO*/ },
    )
}

/**
 * 编辑记录界面
 *
 * @param onBackClick 返回点击
 */
@Composable
internal fun EditRecordScreen(
    selectedTypeCategory: RecordTypeCategoryEnum,
    typeList: List<RecordTypeEntity>,
    primaryColor: Color,
    amount: String,
    onTypeClick: (RecordTypeEntity) -> Unit,
    onTabSelected: (RecordTypeCategoryEnum) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            EditRecordTopBar(
                selectedTabIndex = selectedTypeCategory.position,
                onTabSelected = onTabSelected,
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSaveClick) {
                Icon(imageVector = Icons.Default.SaveAs, contentDescription = null)
            }
        }
    ) { paddingValues ->
        EditRecordContent(
            modifier = Modifier.padding(paddingValues),
            primaryColor = primaryColor,
            amount = amount,
            typeList = typeList,
            onTypeClick = onTypeClick,
        )
    }
}

@Composable
internal fun EditRecordTopBar(
    selectedTabIndex: Int,
    onTabSelected: (RecordTypeCategoryEnum) -> Unit,
    onBackClick: () -> Unit,
) {
    // 顶部标签列表
    val tabs = arrayListOf(
        TabItem(
            title = stringResource(id = R.string.expend),
            type = RecordTypeCategoryEnum.EXPENDITURE
        ),
        TabItem(title = stringResource(id = R.string.income), type = RecordTypeCategoryEnum.INCOME),
        TabItem(
            title = stringResource(id = R.string.transfer),
            type = RecordTypeCategoryEnum.TRANSFER
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
internal fun EditRecordContent(
    modifier: Modifier = Modifier,
    primaryColor: Color,
    amount: String,
    typeList: List<RecordTypeEntity>,
    onTypeClick: (RecordTypeEntity) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(RECORD_TYPE_COLUMNS),
        modifier = modifier
            .fillMaxWidth(),
    ) {
        item(
            span = {
                GridItemSpan(maxLineSpan)
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "￥",
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
                Divider(
                    modifier = Modifier
                        .fillMaxWidth(),
                )
                Text(
                    text = stringResource(id = R.string.record_type),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
        }
        items(typeList) {
            TypeItem(
                iconResId = it.iconResId,
                title = it.name,
                selected = it.selected,
                onTypeClick = { onTypeClick(it) }
            )
        }
    }
}

@Composable
internal fun TypeItem(
    iconResId: Int,
    title: String,
    selected: Boolean,
    onTypeClick: () -> Unit
) {
    if (iconResId == 0) {
        // 补齐数据
        Spacer(modifier = Modifier.fillMaxSize())
    } else {
        ConstraintLayout(
            modifier = Modifier
                .clickable(onClick = onTypeClick)
                .padding(top = 8.dp, bottom = 8.dp)

        ) {
            val (iconBg, iconMore, text) = createRefs()
            val color =
                if (selected) LocalExtendedColors.current.selected else LocalExtendedColors.current.unselected
            OutlinedIconButton(
                modifier = Modifier
                    .size(32.dp)
                    .constrainAs(iconBg) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                onClick = onTypeClick,
                content = {
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        tint = color
                    )
                },
                border = BorderStroke(1.dp, color)
            )
            Icon(
                modifier = Modifier
                    .size(12.dp)
                    .constrainAs(iconMore) {
                        bottom.linkTo(iconBg.bottom)
                        end.linkTo(iconBg.end)
                    },
                imageVector = Icons.Default.MoreHoriz, contentDescription = null, tint = color
            )
            Text(
                modifier = Modifier
                    .constrainAs(text) {
                        top.linkTo(iconBg.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                text = title,
                color = color,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@DevicePreviews
@Composable
internal fun CategoryItemPreview() {
    CashbookTheme {
        Column {
            TypeItem(
                iconResId = R.drawable.vector_type_books_education_24,
                title = "标题",
                selected = false,
                onTypeClick = {},
            )
            TypeItem(
                iconResId = R.drawable.vector_type_books_education_24,
                title = "标题",
                selected = true,
                onTypeClick = {},
            )
        }
    }
}

@DevicePreviews
@Composable
internal fun EditRecordTopBarPreview() {
    CashbookTheme {
        Column {
            EditRecordTopBar(
                selectedTabIndex = RecordTypeCategoryEnum.EXPENDITURE.position,
                onTabSelected = {},
                onBackClick = {},
            )
            EditRecordTopBar(
                selectedTabIndex = RecordTypeCategoryEnum.INCOME.position,
                onTabSelected = {},
                onBackClick = {},
            )
            EditRecordTopBar(
                selectedTabIndex = RecordTypeCategoryEnum.TRANSFER.position,
                onTabSelected = {},
                onBackClick = {},
            )
        }
    }
}

@DevicePreviews
@Composable
internal fun EditRecordScreenPreview() {
    CashbookTheme {
        val typeList = listOf(
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_books_education_24,
                0,
                listOf(),
                true
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_electrical_appliance_24,
                0,
                listOf(),
                false
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_bag_24,
                0,
                listOf(),
                false
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_salary_24,
                0,
                listOf(),
                false
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_digital_product_24,
                0,
                listOf(),
                false
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_bicycle_24,
                0,
                listOf(),
                false
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_at_24,
                0,
                listOf(),
                false
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_call_charge_24,
                0,
                listOf(),
                false
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_borrow_24,
                0,
                listOf(),
                false
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_hat_24,
                0,
                listOf(),
                false
            ),
        )
        EditRecordScreen(
            selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            primaryColor = LocalExtendedColors.current.expenditure,
            amount = "1000",
            typeList = typeList,
            onTypeClick = {},
            onTabSelected = {},
            onBackClick = {},
            onSaveClick = {},
        )
    }
}