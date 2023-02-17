@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class
)

package cn.wj.android.cashbook.feature.record.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SaveAs
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.RecordTypeEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.record.R
import cn.wj.android.cashbook.feature.record.model.TabItem

/**
 * 编辑记录界面
 *
 * @param onBackClick 返回点击
 */
@Composable
internal fun EditRecordScreen(
    onBackClick: () -> Unit,
) {
    // TODO 默认选中标签
    val selectedTabIndex = remember { mutableStateOf(RecordTypeEnum.EXPENDITURE.position) }
    Scaffold(
        topBar = {
            EditRecordTopBar(
                selectedTabIndex = selectedTabIndex,
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = Icons.Default.SaveAs, contentDescription = null)
            }
        }
    ) { paddingValues ->
        EditRecordContent(
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
internal fun EditRecordTopBar(
    selectedTabIndex: MutableState<Int>,
    onBackClick: () -> Unit,
) {
    // 顶部标签列表
    val tabs = arrayListOf(
        TabItem(title = stringResource(id = R.string.expend), type = RecordTypeEnum.EXPENDITURE),
        TabItem(title = stringResource(id = R.string.income), type = RecordTypeEnum.INCOME),
        TabItem(title = stringResource(id = R.string.transfer), type = RecordTypeEnum.TRANSFER),
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
                selectedTabIndex = selectedTabIndex.value,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex.value]),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                divider = {},
            ) {
                tabs.forEach { tabItem ->
                    Tab(
                        selected = selectedTabIndex.value == tabItem.type.position,
                        onClick = { selectedTabIndex.value = tabItem.type.position },
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
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        EditRecordCategory()
    }
}

@Composable
internal fun EditRecordCategory(
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
    ) {
        items(20) {
            CategoryItem(selected = it == 0)
        }
    }
}

@Composable
internal fun CategoryItem(selected: Boolean) {
    ConstraintLayout {
        val (iconBg, iconMore, text) = createRefs()
        val color =
            if (selected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
        OutlinedIconButton(
            modifier = Modifier
                .size(32.dp)
                .constrainAs(iconBg) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            onClick = { /*TODO*/ },
            content = {
                Icon(imageVector = Icons.Default.SaveAs, contentDescription = null, tint = color)
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
            text = "分类",
            color = color,
        )
    }
}

@DevicePreviews
@Composable
internal fun CategoryItemPreview() {
    CashbookTheme {
        Column {
            CategoryItem(true)
            CategoryItem(false)
        }
    }
}

@DevicePreviews
@Composable
internal fun EditRecordCategoryPreview() {
    CashbookTheme {
        EditRecordCategory()
    }
}

@DevicePreviews
@Composable
internal fun EditRecordTopBarPreview() {
    CashbookTheme {
        Column {
            EditRecordTopBar(
                selectedTabIndex = remember { mutableStateOf(RecordTypeEnum.EXPENDITURE.position) },
                onBackClick = {},
            )
            EditRecordTopBar(
                selectedTabIndex = remember { mutableStateOf(RecordTypeEnum.INCOME.position) },
                onBackClick = {},
            )
            EditRecordTopBar(
                selectedTabIndex = remember { mutableStateOf(RecordTypeEnum.TRANSFER.position) },
                onBackClick = {},
            )
        }
    }
}

@DevicePreviews
@Composable
internal fun EditRecordScreenPreview() {
    CashbookTheme {
        EditRecordScreen(
            onBackClick = {},
        )
    }
}