package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.toFloatOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.common.ext.yearMonth
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordBarEntity
import cn.wj.android.cashbook.core.model.enums.AnalyticsIncomeExpenditureEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.SelectDateDialog
import cn.wj.android.cashbook.core.ui.expand.colorInt
import cn.wj.android.cashbook.core.ui.expand.text
import cn.wj.android.cashbook.feature.records.viewmodel.AnalyticsUiState
import cn.wj.android.cashbook.feature.records.viewmodel.AnalyticsViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.DateData
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet

/**
 * TODO 数据分析界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/20
 */
@Composable
internal fun AnalyticsRoute(
    typeId: Long,
    tagId: Long,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsScreen(
        dialogState = viewModel.dialogState,
        onRequestShowSelectDateDialog = viewModel::showSelectDateDialog,
        onRequestDismissDialog = viewModel::dismissDialog,
        onDateSelect = viewModel::onDateSelect,
        uiState = uiState,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsScreen(
    dialogState: DialogState,
    onRequestShowSelectDateDialog: () -> Unit,
    onRequestDismissDialog: () -> Unit,
    onDateSelect: (DateData) -> Unit,
    uiState: AnalyticsUiState,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                title = {
                    if (uiState is AnalyticsUiState.Success) {
                        Text(
                            text = uiState.titleText,
                            modifier = Modifier.clickable(onClick = onRequestShowSelectDateDialog),
                        )
                    }
                },
                onBackClick = onRequestPopBackStack,
            )
        },
        content = { paddingValues ->

            (dialogState as? DialogState.Shown<*>)?.data?.let { data ->
                if (data is DateData) {
                    SelectDateDialog(
                        onDialogDismiss = onRequestDismissDialog,
                        currentDate = data.from.yearMonth,
                        yearSelectable = true,
                        yearSelected = data.year,
                        onDateSelected = { ym, year ->
                            onDateSelect(
                                DateData(
                                    from = ym.atDay(1),
                                    year = year
                                )
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            ) {
                when (uiState) {
                    AnalyticsUiState.Loading -> {
                        Loading(modifier = Modifier.align(Alignment.Center))
                    }

                    is AnalyticsUiState.Success -> {
                        LazyColumn(content = {
                            item {
                                ElevatedCard(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp,
                                        )
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.overview_of_revenue_and_expenditure),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth(),
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

                            item {
                                ElevatedCard(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    )
                                ) {
                                    Text(
                                        text = if (uiState.year) {
                                            stringResource(id = R.string.monthly_statistics)
                                        } else {
                                            stringResource(id = R.string.daily_statistics)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .padding(top = 8.dp),
                                    )
                                    var selectedTab by remember {
                                        mutableStateOf(AnalyticsIncomeExpenditureEnum.EXPENDITURE)
                                    }
                                    val expenditureColor = LocalExtendedColors.current.expenditure
                                    val incomeColor = LocalExtendedColors.current.income
                                    val expenditureText =
                                        AnalyticsIncomeExpenditureEnum.EXPENDITURE.text
                                    val incomeText = AnalyticsIncomeExpenditureEnum.INCOME.text
                                    val dataList = uiState.barDataList
                                    AndroidView(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp,
                                            ),
                                        factory = { context ->
                                            BarChart(context).apply {
                                                description.isEnabled = false
                                                setScaleEnabled(false)
                                                setDrawBarShadow(false)
                                                setDrawGridBackground(false)

                                                with(legend) {
                                                    verticalAlignment =
                                                        Legend.LegendVerticalAlignment.TOP
                                                    horizontalAlignment =
                                                        Legend.LegendHorizontalAlignment.RIGHT
                                                    orientation = Legend.LegendOrientation.VERTICAL
                                                    setDrawInside(true)
                                                    yOffset = 0f
                                                    xOffset = 10f
                                                    yEntrySpace = 0f
                                                    textSize = 8f
                                                }

                                                with(xAxis) {
                                                    granularity = 1f
                                                    setCenterAxisLabels(true)
                                                    position = XAxis.XAxisPosition.BOTTOM
                                                    offsetLeftAndRight(50)
                                                    valueFormatter =
                                                        IAxisValueFormatter { value, _ ->
                                                            val position = value.toInt()
                                                            val dataSet = data.dataSets.first()
                                                            if (position in 0 until dataSet.entryCount) {
                                                                val entry =
                                                                    dataSet.getEntryForIndex(
                                                                        position
                                                                    )
                                                                val data = entry.data
                                                                if (data is AnalyticsRecordBarEntity) {
                                                                    val date = data.date
                                                                    if (data.year) {
                                                                        date.split("-").last()
                                                                    } else {
                                                                        date.split("-").run {
                                                                            "${this[1]}-${this[2]}"
                                                                        }
                                                                    }
                                                                } else {
                                                                    ""
                                                                }
                                                            } else {
                                                                ""
                                                            }
                                                        }
                                                }

                                                with(axisLeft) {
                                                    valueFormatter = LargeValueFormatter()
                                                    setDrawGridLines(false)
                                                    spaceTop = 35f
                                                    axisMinimum = 0f
                                                }

                                                axisRight.isEnabled = false
                                            }
                                        },
                                        update = { chart ->
                                            val expenditureLs = mutableListOf<BarEntry>()
                                            val incomeLs = mutableListOf<BarEntry>()
                                            dataList.forEachIndexed { index, entity ->
                                                expenditureLs.add(
                                                    BarEntry(
                                                        (index + 1).toFloat(),
                                                        entity.expenditure.toFloatOrZero(),
                                                        entity
                                                    )
                                                )
                                                incomeLs.add(
                                                    BarEntry(
                                                        (index + 1).toFloat(),
                                                        entity.income.toFloatOrZero(),
                                                        entity
                                                    )
                                                )
                                            }
                                            val dataSet = chart.data?.dataSets?.first()
                                            val ls =
                                                if (selectedTab == AnalyticsIncomeExpenditureEnum.INCOME) {
                                                    incomeLs
                                                } else {
                                                    expenditureLs
                                                }
                                            if (dataSet?.entryCount != ls.size
                                                || dataSet.getEntryForIndex(0).y != ls.first().y
                                            ) {
                                                val barDataSets = mutableListOf<IBarDataSet>()
                                                when (selectedTab) {
                                                    AnalyticsIncomeExpenditureEnum.EXPENDITURE -> {
                                                        barDataSets.add(BarDataSet(
                                                            expenditureLs,
                                                            expenditureText
                                                        ).apply {
                                                            color = expenditureColor.colorInt
                                                        })
                                                    }

                                                    AnalyticsIncomeExpenditureEnum.INCOME -> {
                                                        barDataSets.add(BarDataSet(
                                                            incomeLs,
                                                            incomeText
                                                        ).apply {
                                                            color = incomeColor.colorInt
                                                        })
                                                    }

                                                    AnalyticsIncomeExpenditureEnum.ALL -> {
                                                        barDataSets.add(BarDataSet(
                                                            expenditureLs,
                                                            expenditureText
                                                        ).apply {
                                                            color = expenditureColor.colorInt
                                                        })
                                                        barDataSets.add(BarDataSet(
                                                            incomeLs,
                                                            incomeText
                                                        ).apply {
                                                            color = incomeColor.colorInt
                                                        })
                                                    }
                                                }
                                                chart.data = BarData(barDataSets).apply {
                                                    barWidth = 0.5f
                                                }
                                                if (selectedTab == AnalyticsIncomeExpenditureEnum.ALL) {
                                                    chart.groupBars(0f, 0.5f, 0.2f)
                                                }
                                                chart.data.notifyDataChanged()
                                                chart.notifyDataSetChanged()
                                                chart.invalidate()
                                                chart.animateY(250)
                                            }
                                        }
                                    )
                                    TabRow(
                                        selectedTabIndex = selectedTab.ordinal,
                                        containerColor = Color.Unspecified,
                                        contentColor = Color.Unspecified,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .width(200.dp)
                                            .padding(vertical = 8.dp),
                                        indicator = {},
                                        divider = {},
                                    ) {
                                        AnalyticsIncomeExpenditureEnum.entries.forEach { enum ->
                                            Tab(
                                                selected = selectedTab == enum,
                                                onClick = { selectedTab = enum },
                                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                                unselectedContentColor = LocalContentColor.current,
                                            ) {
                                                Text(text = enum.text)
                                            }
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
            }
        },
    )
}