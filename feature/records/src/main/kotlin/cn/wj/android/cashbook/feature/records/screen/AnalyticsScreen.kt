package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toFloatOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.common.ext.yearMonth
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordBarEntity
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.SelectDateDialog
import cn.wj.android.cashbook.core.ui.expand.colorInt
import cn.wj.android.cashbook.core.ui.expand.text
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.records.viewmodel.AnalyticsUiState
import cn.wj.android.cashbook.feature.records.viewmodel.AnalyticsViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.DateData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

/**
 * 数据分析界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/20
 */
@Composable
internal fun AnalyticsRoute(
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
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    ),
                                )
                            }

                            if (!uiState.crossYear) {
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
                        })
                    }
                }
            }
        },
    )
}

@Composable
private fun SplitReports(uiState: AnalyticsUiState.Success, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Text(
            text = if (uiState.year) {
                stringResource(id = R.string.monthly_report)
            } else {
                stringResource(id = R.string.daily_report)
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp),
        )
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
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
        Divider()
        val barDataList = uiState.barDataList.filter {
            it.date.parseDateLong(DATE_FORMAT_DATE) <= System.currentTimeMillis()
        }.reversed()
        repeat(barDataList.size) { i ->
            val item = barDataList[i]
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            ) {
                val date = if (uiState.year) {
                    item.date.split("-")[1] + stringResource(id = R.string.month)
                } else {
                    item.date.split("-").run {
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
                    text = item.expenditure.withCNY(),
                    textAlign = TextAlign.Center,
                    color = LocalExtendedColors.current.expenditure,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.income.withCNY(),
                    textAlign = TextAlign.Center,
                    color = LocalExtendedColors.current.income,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.balance.withCNY(),
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
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = R.string.category_report),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        )
        var tempUiState: AnalyticsUiState.Success? by remember {
            mutableStateOf(null)
        }
        var selectedTab by remember {
            mutableStateOf(RecordTypeCategoryEnum.EXPENDITURE)
        }
        var tempTab: RecordTypeCategoryEnum? by remember {
            mutableStateOf(null)
        }
        val pieColors = listOf(
            MaterialTheme.colorScheme.primary.colorInt,
            MaterialTheme.colorScheme.primaryContainer.colorInt,
            MaterialTheme.colorScheme.secondary.colorInt,
            MaterialTheme.colorScheme.secondaryContainer.colorInt,
            MaterialTheme.colorScheme.tertiary.colorInt,
            MaterialTheme.colorScheme.tertiaryContainer.colorInt,
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            factory = { context ->
                PieChart(context).apply {
                    setUsePercentValues(true)
                    description.isEnabled = false
                    setExtraOffsets(5f, 10f, 5f, 5f)
                    dragDecelerationFrictionCoef = 0.95f
                    isDrawHoleEnabled = true
                    setHoleColor(Color.White.colorInt)
                    setTransparentCircleColor(Color.White.colorInt)
                    setTransparentCircleAlpha(110)
                    holeRadius = 58f
                    transparentCircleRadius = 61f
                    setDrawCenterText(true)
                    rotationAngle = 0f
                    isRotationEnabled = true
                    isHighlightPerTapEnabled = true

                    with(legend) {
                        verticalAlignment = Legend.LegendVerticalAlignment.TOP
                        horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                        orientation = Legend.LegendOrientation.VERTICAL
                        setDrawInside(false)
                        isEnabled = false
                    }

                    setOnChartValueSelectedListener(
                        object : OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry, h: Highlight) {
                                (e.data as? AnalyticsRecordPieEntity)?.let { data ->
                                    centerText = "${data.typeName}\n${data.totalAmount.withCNY()}"
                                }
                            }

                            override fun onNothingSelected() {
                                centerText = ""
                            }
                        })
                }
            },
            update = { chart ->
                if (uiState != tempUiState || selectedTab != tempTab) {
                    tempUiState = uiState
                    tempTab = selectedTab
                    val pieEntryList = when (selectedTab) {
                        RecordTypeCategoryEnum.EXPENDITURE -> uiState.expenditurePieDataList
                        RecordTypeCategoryEnum.INCOME -> uiState.incomePieDataList
                        RecordTypeCategoryEnum.TRANSFER -> uiState.transferPieDataList
                    }.filter { it.percent > 0.03f }
                        .map {
                            PieEntry(it.percent, it.typeName, it)
                        }
                    chart.centerText = ""
                    chart.data = PieData(PieDataSet(pieEntryList, "").apply {
                        sliceSpace = 3f
                        selectionShift = 5f
                        colors = pieColors
                        valueLinePart1OffsetPercentage = 80f
                        valueLinePart1Length = 0.2f
                        valueLinePart2Length = 0.4f
                        yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                        valueFormatter = PercentFormatter()
                    })
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
                .width((RecordTypeCategoryEnum.size * 50).dp)
                .padding(vertical = 8.dp),
            divider = {},
        ) {
            RecordTypeCategoryEnum.entries.forEach { enum ->
                Tab(
                    selected = selectedTab == enum,
                    onClick = { selectedTab = enum },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = LocalContentColor.current,
                ) {
                    Text(
                        text = enum.text,
                        modifier = Modifier.padding(8.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 70.dp)
                        .clickable {
                            // TODO 展示二级分类
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterDrawableResource(idStr = item.typeIconResName),
                        contentDescription = null,
                        tint = item.typeCategory.typeColor,
                        modifier = Modifier
                            .background(
                                color = item.typeCategory.typeColor.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .padding(2.dp)
                            .clip(CircleShape)
                            .padding(4.dp)
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(text = item.typeName)
                            Text(
                                text = (item.percent * 100f).decimalFormat("###,###,##0.0") + "%",
                                color = LocalContentColor.current.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .weight(1f),
                            )
                            Text(
                                text = item.totalAmount.withCNY(),
                                color = item.typeCategory.typeColor,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        LinearProgressIndicator(
                            progress = item.percent,
                            color = item.typeCategory.typeColor,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsBarChart(
    uiState: AnalyticsUiState.Success,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Text(
            text = if (uiState.year) {
                stringResource(id = R.string.monthly_statistics)
            } else {
                stringResource(id = R.string.daily_statistics)
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        )
        var tempUiState: AnalyticsUiState.Success? by remember {
            mutableStateOf(null)
        }
        var selectedTab by remember {
            mutableStateOf(AnalyticsBarTypeEnum.EXPENDITURE)
        }
        var tempTab: AnalyticsBarTypeEnum? by remember {
            mutableStateOf(null)
        }
        val expenditureColor = LocalExtendedColors.current.expenditure
        val expenditureText = AnalyticsBarTypeEnum.EXPENDITURE.text
        val incomeColor = LocalExtendedColors.current.income
        val incomeText = AnalyticsBarTypeEnum.INCOME.text
        val balanceColor = LocalExtendedColors.current.transfer
        val balanceText = AnalyticsBarTypeEnum.BALANCE.text
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
                LineChart(context).apply {
                    description.isEnabled = false
                    setDrawGridBackground(false)

                    with(legend) {
                        verticalAlignment = Legend.LegendVerticalAlignment.TOP
                        horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                        orientation = Legend.LegendOrientation.VERTICAL
                        setDrawInside(true)
                        yOffset = -20f
                        xOffset = 10f
                        yEntrySpace = 0f
                        textSize = 8f
                    }

                    with(xAxis) {
                        granularity = 1f
                        setCenterAxisLabels(true)
                        position = XAxis.XAxisPosition.BOTTOM
                        offsetLeftAndRight(50)
                        setAvoidFirstLastClipping(true)
                        valueFormatter = IAxisValueFormatter { value, _ ->
                            val position = value.toInt()
                            val dataSet = data.dataSets.first()
                            if (position in 0 until dataSet.entryCount) {
                                val entry = dataSet.getEntryForIndex(position)
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
                    }

                    axisRight.isEnabled = false
                }
            },
            update = { chart ->
                if (uiState != tempUiState || selectedTab != tempTab) {
                    tempUiState = uiState
                    tempTab = selectedTab
                    val expenditureLs = mutableListOf<Entry>()
                    val incomeLs = mutableListOf<Entry>()
                    val balanceLs = mutableListOf<Entry>()
                    dataList.forEachIndexed { index, entity ->
                        val expenditure = entity.expenditure.toFloatOrZero()
                        if (expenditure > 0f) {
                            expenditureLs.add(
                                Entry(
                                    (index + 1).toFloat(),
                                    expenditure * -1f,
                                    entity
                                )
                            )
                        }
                        val income = entity.income.toFloatOrZero()
                        if (income > 0f) {
                            incomeLs.add(
                                Entry(
                                    (index + 1).toFloat(),
                                    income,
                                    entity
                                )
                            )
                        }
                        val balance = entity.balance.toFloatOrZero()
                        if (balance > 0f) {
                            balanceLs.add(
                                Entry(
                                    (index + 1).toFloat(),
                                    balance,
                                    entity
                                )
                            )
                        }
                    }
                    val dataSets = mutableListOf<ILineDataSet>()
                    val expenditureSet = LineDataSet(expenditureLs, expenditureText).apply {
                        color = expenditureColor.colorInt
                        lineWidth = 2f
                    }
                    val incomeSet = LineDataSet(incomeLs, incomeText).apply {
                        color = incomeColor.colorInt
                        lineWidth = 2f
                    }
                    val balanceSet = LineDataSet(balanceLs, balanceText).apply {
                        color = balanceColor.colorInt
                        lineWidth = 2f
                    }
                    val spaceValue = 1000f
                    when (selectedTab) {
                        AnalyticsBarTypeEnum.EXPENDITURE -> {
                            with(chart.axisLeft) {
                                axisMinimum = expenditureLs.minOf { it.y } - spaceValue
                                axisMaximum = expenditureLs.maxOf { it.y } + spaceValue
                                removeAllLimitLines()
                            }
                            dataSets.add(expenditureSet)
                        }

                        AnalyticsBarTypeEnum.INCOME -> {
                            with(chart.axisLeft) {
                                axisMinimum = incomeLs.minOf { it.y } - spaceValue
                                axisMaximum = incomeLs.maxOf { it.y } + spaceValue
                                removeAllLimitLines()
                            }
                            dataSets.add(incomeSet)
                        }

                        AnalyticsBarTypeEnum.BALANCE -> {
                            with(chart.axisLeft) {
                                axisMinimum = balanceLs.minOf { it.y } - spaceValue
                                axisMaximum = balanceLs.maxOf { it.y } + spaceValue
                                addLimitLine(LimitLine(0f).apply {
                                    lineWidth = 4f
                                    enableDashedLine(10f, 10f, 0f)
                                })
                            }
                            dataSets.add(balanceSet)
                        }

                        AnalyticsBarTypeEnum.ALL -> {
                            with(chart.axisLeft) {
                                axisMinimum = expenditureLs.minOf { it.y } - spaceValue
                                axisMaximum = incomeLs.maxOf { it.y } + spaceValue
                                addLimitLine(LimitLine(0f).apply {
                                    lineWidth = 4f
                                    enableDashedLine(10f, 10f, 0f)
                                })
                            }
                            dataSets.add(expenditureSet)
                            dataSets.add(incomeSet)
                            dataSets.add(balanceSet)
                        }
                    }
                    chart.data = LineData(dataSets).apply {
                        setValueFormatter { _, entry, _, _ ->
                            entry.y.decimalFormat().withCNY()
                        }
                    }
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
                .width((AnalyticsBarTypeEnum.size * 50).dp)
                .padding(vertical = 8.dp),
            divider = {},
        ) {
            AnalyticsBarTypeEnum.entries.forEach { enum ->
                Tab(
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