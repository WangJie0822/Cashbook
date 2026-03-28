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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CbElevatedCard
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbLineChart
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTab
import cn.wj.android.cashbook.core.design.component.CbTabRow
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.component.LineEntry
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarGranularity
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.LocalProgressDialogController
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.DateSelectionPopup
import cn.wj.android.cashbook.core.ui.expand.percentText
import cn.wj.android.cashbook.core.ui.expand.text
import cn.wj.android.cashbook.feature.records.view.AnalyticsPieChart
import cn.wj.android.cashbook.feature.records.view.AnalyticsPieListItem
import cn.wj.android.cashbook.feature.records.viewmodel.AnalyticsUiState
import cn.wj.android.cashbook.feature.records.viewmodel.AnalyticsViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.ShowSheetData

/**
 * 数据分析界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/20
 */
@Composable
internal fun AnalyticsRoute(
    onRequestNaviToTypeAnalytics: (Long, String?, Boolean) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val progressDialogController = LocalProgressDialogController.current
    viewModel.setProgressDialogController(progressDialogController)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateSelection by viewModel.dateSelection.collectAsStateWithLifecycle()

    AnalyticsScreen(
        dateSelection = dateSelection,
        showDatePopup = viewModel.showDatePopup,
        onDateClick = viewModel::displayDatePopup,
        onDateSelected = viewModel::updateDateSelection,
        onDismissDatePopup = viewModel::dismissDatePopup,
        sheetData = viewModel.sheetData,
        onRequestShowBottomSheet = { typeId -> viewModel.showSheet(progressDialogController, typeId) },
        onRequestDismissBottomSheet = viewModel::dismissSheet,
        uiState = uiState,
        onRequestNaviToTypeAnalytics = { typeId, includeChildTypes ->
            viewModel.dismissSheet()
            onRequestNaviToTypeAnalytics(
                typeId,
                (uiState as? AnalyticsUiState.Success)?.titleText,
                includeChildTypes,
            )
        },
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsScreen(
    dateSelection: DateSelectionEntity,
    showDatePopup: Boolean,
    onDateClick: () -> Unit,
    onDateSelected: (DateSelectionEntity) -> Unit,
    onDismissDatePopup: () -> Unit,
    sheetData: ShowSheetData?,
    onRequestShowBottomSheet: (Long) -> Unit,
    onRequestDismissBottomSheet: () -> Unit,
    uiState: AnalyticsUiState,
    onRequestNaviToTypeAnalytics: (Long, Boolean) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                title = {
                    Box {
                        Row(
                            modifier = Modifier.clickable(onClick = onDateClick),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (dateSelection is DateSelectionEntity.DateRange) {
                                Column {
                                    Text(
                                        text = dateSelection.from.let {
                                            "${it.year}-${it.monthValue.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Text(
                                        text = dateSelection.to.let {
                                            "${it.year}-${it.monthValue.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            } else {
                                Text(
                                    text = dateSelection.getDisplayText(),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                            Icon(
                                imageVector = CbIcons.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        DateSelectionPopup(
                            expanded = showDatePopup,
                            onDismissRequest = onDismissDatePopup,
                            currentSelection = dateSelection,
                            onDateSelected = onDateSelected,
                        )
                    }
                },
                onBackClick = onRequestPopBackStack,
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            ) {
                if (null != sheetData) {
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
                        content = {
                            ConstraintLayout(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val (title, icon) = createRefs()
                                Text(
                                    text = stringResource(id = R.string.category_report),
                                    modifier = Modifier.constrainAs(title) {
                                        centerTo(parent)
                                    },
                                )
                                CbIconButton(
                                    onClick = {
                                        onRequestNaviToTypeAnalytics(
                                            sheetData.typeId,
                                            true,
                                        )
                                    },
                                    modifier = Modifier.constrainAs(icon) {
                                        end.linkTo(parent.end)
                                        centerVerticallyTo(parent)
                                    },
                                ) {
                                    Icon(
                                        imageVector = CbIcons.DonutSmall,
                                        contentDescription = stringResource(id = R.string.cd_analytics),
                                    )
                                }
                            }

                            val colorScheme = MaterialTheme.colorScheme
                            val extendedColors = LocalExtendedColors.current
                            val pieColorsCompose = remember(colorScheme, extendedColors) {
                                listOf(
                                    colorScheme.primary,
                                    colorScheme.primaryContainer,
                                    colorScheme.secondary,
                                    colorScheme.secondaryContainer,
                                    colorScheme.tertiary,
                                    colorScheme.tertiaryContainer,
                                    extendedColors.quaternary,
                                    extendedColors.quaternaryContainer,
                                )
                            }
                            val onPieColorsCompose = remember(colorScheme, extendedColors) {
                                listOf(
                                    colorScheme.onPrimary,
                                    colorScheme.onPrimaryContainer,
                                    colorScheme.onSecondary,
                                    colorScheme.onSecondaryContainer,
                                    colorScheme.onTertiary,
                                    colorScheme.onTertiaryContainer,
                                    extendedColors.onQuaternary,
                                    extendedColors.onQuaternaryContainer,
                                )
                            }
                            AnalyticsPieChart(
                                barCenterText = sheetData.typeName,
                                dataList = sheetData.dataList,
                                pieColorsCompose = pieColorsCompose,
                                onPieColorsCompose = onPieColorsCompose,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            repeat(sheetData.dataList.size) { i ->
                                val item = sheetData.dataList[i]
                                AnalyticsPieListItem(
                                    item = item,
                                    tintColor = pieColorsCompose[i % pieColorsCompose.size],
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 70.dp)
                                        .clickable {
                                            onRequestNaviToTypeAnalytics(item.typeId, false)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        },
                    )
                }

                when (uiState) {
                    AnalyticsUiState.Loading -> {
                        Loading(modifier = Modifier.align(Alignment.Center))
                    }

                    is AnalyticsUiState.Success -> {
                        LazyColumn(
                            content = {
                                item {
                                    CbElevatedCard(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp,
                                        ),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp,
                                            ),
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.overview_of_revenue_and_expenditure),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = stringResource(id = R.string.total_income))
                                            Text(
                                                text = uiState.totalIncome.withCNY(),
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Row {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = stringResource(id = R.string.total_expenditure))
                                                    Text(
                                                        text = uiState.totalExpenditure.withCNY(),
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = stringResource(id = R.string.total_balance))
                                                    Text(
                                                        text = uiState.totalBalance.withCNY(),
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!uiState.noData) {
                                    item {
                                        AnalyticsBarChart(
                                            uiState = uiState,
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp,
                                            ),
                                        )
                                    }

                                    item {
                                        AnalyticsPieChart(
                                            uiState = uiState,
                                            onRequestShowSheet = onRequestShowBottomSheet,
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp,
                                            ),
                                        )
                                    }

                                    item {
                                        SplitReports(
                                            uiState = uiState,
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp,
                                            ),
                                        )
                                    }
                                }

                                item {
                                    Footer(hintText = stringResource(id = R.string.footer_hint_default))
                                }
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SplitReports(uiState: AnalyticsUiState.Success, modifier: Modifier = Modifier) {
    CbElevatedCard(
        modifier = modifier,
    ) {
        Text(
            text = when (uiState.granularity) {
                AnalyticsBarGranularity.YEAR -> stringResource(id = R.string.yearly_report)
                AnalyticsBarGranularity.MONTH -> stringResource(id = R.string.monthly_report)
                AnalyticsBarGranularity.DAY -> stringResource(id = R.string.daily_report)
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp),
        )
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        ) {
            Text(
                text = stringResource(id = R.string.date),
                textAlign = TextAlign.Center,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(id = R.string.expend),
                textAlign = TextAlign.Center,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(id = R.string.income),
                textAlign = TextAlign.Center,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(id = R.string.surplus),
                textAlign = TextAlign.Center,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
        CbHorizontalDivider()
        val barDataList = remember(uiState.barDataList) { uiState.barDataList.reversed() }
        repeat(barDataList.size) { i ->
            val item = barDataList[i]
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            ) {
                val date = when (uiState.granularity) {
                    AnalyticsBarGranularity.YEAR -> item.date + stringResource(id = R.string.year)
                    AnalyticsBarGranularity.MONTH -> item.date.split("-")[1] + stringResource(id = R.string.month)
                    AnalyticsBarGranularity.DAY -> item.date.split("-").run {
                        "${this[1]}-${this[2]}"
                    }
                }
                Text(
                    text = date,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.expenditure.toMoneyCNY(),
                    textAlign = TextAlign.Center,
                    color = LocalExtendedColors.current.expenditure,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.income.toMoneyCNY(),
                    textAlign = TextAlign.Center,
                    color = LocalExtendedColors.current.income,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.balance.toMoneyCNY(),
                    textAlign = TextAlign.Center,
                    color = LocalExtendedColors.current.transfer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AnalyticsPieChart(
    uiState: AnalyticsUiState.Success,
    onRequestShowSheet: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    CbElevatedCard(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = R.string.category_report),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        )

        val colorScheme = MaterialTheme.colorScheme
        val extendedColors = LocalExtendedColors.current
        val pieColorsCompose = remember(colorScheme, extendedColors) {
            listOf(
                colorScheme.primary,
                colorScheme.primaryContainer,
                colorScheme.secondary,
                colorScheme.secondaryContainer,
                colorScheme.tertiary,
                colorScheme.tertiaryContainer,
                extendedColors.quaternary,
                extendedColors.quaternaryContainer,
            )
        }

        var selectedTab by remember {
            mutableStateOf(RecordTypeCategoryEnum.EXPENDITURE)
        }

        AnalyticsPieChart(
            barCenterText = selectedTab.percentText,
            dataList = when (selectedTab) {
                RecordTypeCategoryEnum.EXPENDITURE -> uiState.expenditurePieDataList
                RecordTypeCategoryEnum.INCOME -> uiState.incomePieDataList
                RecordTypeCategoryEnum.TRANSFER -> uiState.transferPieDataList
            },
            pieColorsCompose = pieColorsCompose,
            onPieColorsCompose = remember(colorScheme, extendedColors) {
                listOf(
                    colorScheme.onPrimary,
                    colorScheme.onPrimaryContainer,
                    colorScheme.onSecondary,
                    colorScheme.onSecondaryContainer,
                    colorScheme.onTertiary,
                    colorScheme.onTertiaryContainer,
                    extendedColors.onQuaternary,
                    extendedColors.onQuaternaryContainer,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        CbTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Unspecified,
            contentColor = Color.Unspecified,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width((RecordTypeCategoryEnum.size * 50).dp)
                .padding(vertical = 8.dp),
            divider = {},
        ) {
            RecordTypeCategoryEnum.entries.forEach { enum ->
                CbTab(
                    selected = selectedTab == enum,
                    onClick = { selectedTab = enum },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = LocalContentColor.current,
                ) {
                    Text(
                        text = enum.text,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
        Column {
            val pieDataList = when (selectedTab) {
                RecordTypeCategoryEnum.EXPENDITURE -> uiState.expenditurePieDataList
                RecordTypeCategoryEnum.INCOME -> uiState.incomePieDataList
                RecordTypeCategoryEnum.TRANSFER -> uiState.transferPieDataList
            }
            repeat(pieDataList.size) { i ->
                val item = pieDataList[i]
                AnalyticsPieListItem(
                    item = item,
                    tintColor = pieColorsCompose[i % pieColorsCompose.size],
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 70.dp)
                        .clickable {
                            onRequestShowSheet(item.typeId)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AnalyticsBarChart(
    uiState: AnalyticsUiState.Success,
    modifier: Modifier = Modifier,
) {
    CbElevatedCard(
        modifier = modifier,
    ) {
        Text(
            text = when (uiState.granularity) {
                AnalyticsBarGranularity.YEAR -> stringResource(id = R.string.yearly_statistics)
                AnalyticsBarGranularity.MONTH -> stringResource(id = R.string.monthly_statistics)
                AnalyticsBarGranularity.DAY -> stringResource(id = R.string.daily_statistics)
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        )
        var selectedTab by remember {
            mutableStateOf(AnalyticsBarTypeEnum.EXPENDITURE)
        }
        val expenditureColor = LocalExtendedColors.current.expenditure
        val expenditureText = AnalyticsBarTypeEnum.EXPENDITURE.text
        val incomeColor = LocalExtendedColors.current.income
        val incomeText = AnalyticsBarTypeEnum.INCOME.text
        val balanceColor = LocalExtendedColors.current.transfer
        val balanceText = AnalyticsBarTypeEnum.BALANCE.text
        val dataList = uiState.barDataList

        // 构建折线图数据
        val chartDataSets = remember(dataList, selectedTab) {
            val expenditureLs = mutableListOf<LineEntry>()
            val incomeLs = mutableListOf<LineEntry>()
            val balanceLs = mutableListOf<LineEntry>()
            dataList.forEachIndexed { index, entity ->
                val xLabel = when (entity.granularity) {
                    AnalyticsBarGranularity.YEAR -> entity.date
                    AnalyticsBarGranularity.MONTH -> entity.date.split("-").last()
                    AnalyticsBarGranularity.DAY -> entity.date.split("-").run {
                        "${this[1]}-${this[2]}"
                    }
                }
                val expenditure = entity.expenditure / 100f
                if (expenditure > 0f) {
                    expenditureLs.add(
                        LineEntry(
                            x = (index + 1).toFloat(),
                            y = expenditure * -1f,
                            label = xLabel,
                        ),
                    )
                }
                val income = entity.income / 100f
                if (income > 0f) {
                    incomeLs.add(
                        LineEntry(
                            x = (index + 1).toFloat(),
                            y = income,
                            label = xLabel,
                        ),
                    )
                }
                val balance = entity.balance / 100f
                if (balance > 0f) {
                    balanceLs.add(
                        LineEntry(
                            x = (index + 1).toFloat(),
                            y = balance,
                            label = xLabel,
                        ),
                    )
                }
            }

            val result = mutableListOf<cn.wj.android.cashbook.core.design.component.LineDataSet>()
            when (selectedTab) {
                AnalyticsBarTypeEnum.EXPENDITURE -> {
                    if (expenditureLs.isNotEmpty()) {
                        result.add(
                            cn.wj.android.cashbook.core.design.component.LineDataSet(
                                label = expenditureText,
                                entries = expenditureLs,
                                color = expenditureColor,
                            ),
                        )
                    }
                }

                AnalyticsBarTypeEnum.INCOME -> {
                    if (incomeLs.isNotEmpty()) {
                        result.add(
                            cn.wj.android.cashbook.core.design.component.LineDataSet(
                                label = incomeText,
                                entries = incomeLs,
                                color = incomeColor,
                            ),
                        )
                    }
                }

                AnalyticsBarTypeEnum.BALANCE -> {
                    if (balanceLs.isNotEmpty()) {
                        result.add(
                            cn.wj.android.cashbook.core.design.component.LineDataSet(
                                label = balanceText,
                                entries = balanceLs,
                                color = balanceColor,
                            ),
                        )
                    }
                }

                AnalyticsBarTypeEnum.ALL -> {
                    if (expenditureLs.isNotEmpty()) {
                        result.add(
                            cn.wj.android.cashbook.core.design.component.LineDataSet(
                                label = expenditureText,
                                entries = expenditureLs,
                                color = expenditureColor,
                            ),
                        )
                    }
                    if (incomeLs.isNotEmpty()) {
                        result.add(
                            cn.wj.android.cashbook.core.design.component.LineDataSet(
                                label = incomeText,
                                entries = incomeLs,
                                color = incomeColor,
                            ),
                        )
                    }
                    if (balanceLs.isNotEmpty()) {
                        result.add(
                            cn.wj.android.cashbook.core.design.component.LineDataSet(
                                label = balanceText,
                                entries = balanceLs,
                                color = balanceColor,
                            ),
                        )
                    }
                }
            }
            result
        }

        val showZeroLine = selectedTab == AnalyticsBarTypeEnum.BALANCE ||
            selectedTab == AnalyticsBarTypeEnum.ALL

        CbLineChart(
            dataSets = chartDataSets,
            showZeroLine = showZeroLine,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
        )
        CbTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Unspecified,
            contentColor = Color.Unspecified,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width((AnalyticsBarTypeEnum.size * 50).dp)
                .padding(vertical = 8.dp),
            divider = {},
        ) {
            AnalyticsBarTypeEnum.entries.forEach { enum ->
                CbTab(
                    selected = selectedTab == enum,
                    onClick = { selectedTab = enum },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = LocalContentColor.current,
                ) {
                    Text(text = enum.text, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
