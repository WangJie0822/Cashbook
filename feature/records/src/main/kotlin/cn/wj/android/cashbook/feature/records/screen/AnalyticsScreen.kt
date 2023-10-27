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
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.util.Pair
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toFloatOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.common.ext.yearMonth
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.common.tools.toLocalDate
import cn.wj.android.cashbook.core.common.tools.toMs
import cn.wj.android.cashbook.core.design.component.CashbookModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.DateRangePickerDialog
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordBarEntity
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.SelectDateDialog
import cn.wj.android.cashbook.core.ui.expand.colorInt
import cn.wj.android.cashbook.core.ui.expand.percentText
import cn.wj.android.cashbook.core.ui.expand.text
import cn.wj.android.cashbook.feature.records.view.AnalyticsPieChart
import cn.wj.android.cashbook.feature.records.view.AnalyticsPieListItem
import cn.wj.android.cashbook.feature.records.viewmodel.AnalyticsUiState
import cn.wj.android.cashbook.feature.records.viewmodel.AnalyticsViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.DateData
import cn.wj.android.cashbook.feature.records.viewmodel.ShowSelectDateDialogData
import cn.wj.android.cashbook.feature.records.viewmodel.ShowSheetData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

/**
 * 数据分析界面
 * TODO ChartView 夜间模式适配、文本大小、选中label、全屏查看
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/20
 */
@Composable
internal fun AnalyticsRoute(
    onRequestNaviToTypeAnalytics: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsScreen(
        dialogState = viewModel.dialogState,
        onRequestShowSelectDateDialog = viewModel::showSelectDateDialog,
        onRequestShowSelectDateRangeDialog = viewModel::showSelectDateRangeDialog,
        onRequestDismissDialog = viewModel::dismissDialog,
        onDateSelect = viewModel::selectDate,
        sheetData = viewModel.sheetData,
        onRequestShowBottomSheet = viewModel::showSheet,
        onRequestDismissBottomSheet = viewModel::dismissSheet,
        uiState = uiState,
        onRequestNaviToTypeAnalytics = {
            viewModel.dismissSheet()
            onRequestNaviToTypeAnalytics(it)
                                       },
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsScreen(
    dialogState: DialogState,
    onRequestShowSelectDateDialog: () -> Unit,
    onRequestShowSelectDateRangeDialog: () -> Unit,
    onRequestDismissDialog: () -> Unit,
    onDateSelect: (DateData) -> Unit,
    sheetData: ShowSheetData?,
    onRequestShowBottomSheet: (Long) -> Unit,
    onRequestDismissBottomSheet: () -> Unit,
    uiState: AnalyticsUiState,
    onRequestNaviToTypeAnalytics: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                title = {
                    if (uiState is AnalyticsUiState.Success) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val titleText = uiState.titleText
                            Text(
                                text = titleText,
                                style = if (titleText.contains("\n")) {
                                    MaterialTheme.typography.titleSmall
                                } else {
                                    MaterialTheme.typography.titleLarge
                                },
                                modifier = Modifier.clickable(onClick = onRequestShowSelectDateDialog),
                            )
                            Icon(
                                imageVector = CashbookIcons.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    }
                },
                onBackClick = onRequestPopBackStack,
                actions = {
                    IconButton(onClick = onRequestShowSelectDateRangeDialog) {
                        Icon(imageVector = CashbookIcons.DateRange, contentDescription = null)
                    }
                },
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            ) {

                ((dialogState as? DialogState.Shown<*>)?.data as? ShowSelectDateDialogData)?.let { data ->
                    val date = data.date
                    if (data is ShowSelectDateDialogData.SelectDate) {
                        SelectDateDialog(
                            onDialogDismiss = onRequestDismissDialog,
                            currentDate = date.from.yearMonth,
                            yearSelectable = true,
                            yearSelected = date.year,
                            onDateSelected = { ym, year ->
                                onDateSelect(
                                    DateData(
                                        from = ym.atDay(1),
                                        year = year
                                    )
                                )
                            }
                        )
                    } else {
                        DateRangePickerDialog(
                            onDismissRequest = onRequestDismissDialog,
                            selection = Pair.create(
                                date.from.toMs(),
                                date.to?.toMs()
                            ),
                            onPositiveButtonClick = { pair ->
                                onDateSelect(
                                    DateData(
                                        from = pair.first.toLocalDate(),
                                        to = pair.second.toLocalDate(),
                                    )
                                )
                            },
                            onNegativeButtonClick = onRequestDismissDialog
                        )
                    }
                }

                if (null != sheetData) {
                    CashbookModalBottomSheet(
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
                                IconButton(
                                    onClick = { onRequestNaviToTypeAnalytics(sheetData.typeId) },
                                    modifier = Modifier.constrainAs(icon) {
                                        end.linkTo(parent.end)
                                        centerVerticallyTo(parent)
                                    },
                                ) {
                                    Icon(
                                        imageVector = CashbookIcons.DonutSmall,
                                        contentDescription = null
                                    )
                                }
                            }

                            val pieColorsCompose = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.tertiaryContainer,
                                LocalExtendedColors.current.quaternary,
                                LocalExtendedColors.current.quaternaryContainer,
                            )
                            AnalyticsPieChart(
                                barCenterText = sheetData.typeName,
                                dataList = sheetData.dataList,
                                pieColorsCompose = pieColorsCompose,
                                onPieColorsCompose = listOf(
                                    MaterialTheme.colorScheme.onPrimary,
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                    MaterialTheme.colorScheme.onSecondary,
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                    MaterialTheme.colorScheme.onTertiary,
                                    MaterialTheme.colorScheme.onTertiaryContainer,
                                    LocalExtendedColors.current.onQuaternary,
                                    LocalExtendedColors.current.onQuaternaryContainer,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
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
                                            onRequestNaviToTypeAnalytics(item.typeId)
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
    onRequestShowSheet: (Long) -> Unit,
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

        val pieColorsCompose = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer,
            LocalExtendedColors.current.quaternary,
            LocalExtendedColors.current.quaternaryContainer,
        )

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
            onPieColorsCompose = listOf(
                MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.colorScheme.onPrimaryContainer,
                MaterialTheme.colorScheme.onSecondary,
                MaterialTheme.colorScheme.onSecondaryContainer,
                MaterialTheme.colorScheme.onTertiary,
                MaterialTheme.colorScheme.onTertiaryContainer,
                LocalExtendedColors.current.onQuaternary,
                LocalExtendedColors.current.onQuaternaryContainer,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                if (expenditureLs.isEmpty()) {
                                    resetAxisMinimum()
                                    resetAxisMaximum()
                                } else {
                                    axisMinimum = expenditureLs.minOf { it.y } - spaceValue
                                    axisMaximum = expenditureLs.maxOf { it.y } + spaceValue
                                }
                                removeAllLimitLines()
                            }
                            if (expenditureLs.isNotEmpty()) {
                                dataSets.add(expenditureSet)
                            }
                        }

                        AnalyticsBarTypeEnum.INCOME -> {
                            with(chart.axisLeft) {
                                if (incomeLs.isEmpty()) {
                                    resetAxisMinimum()
                                    resetAxisMaximum()
                                } else {
                                    axisMinimum = incomeLs.minOf { it.y } - spaceValue
                                    axisMaximum = incomeLs.maxOf { it.y } + spaceValue
                                }
                                removeAllLimitLines()
                            }
                            if (incomeLs.isNotEmpty()) {
                                dataSets.add(incomeSet)
                            }
                        }

                        AnalyticsBarTypeEnum.BALANCE -> {
                            with(chart.axisLeft) {
                                if (balanceLs.isEmpty()) {
                                    resetAxisMinimum()
                                    resetAxisMaximum()
                                    removeAllLimitLines()
                                } else {
                                    axisMinimum = balanceLs.minOf { it.y } - spaceValue
                                    axisMaximum = balanceLs.maxOf { it.y } + spaceValue
                                    addLimitLine(LimitLine(0f).apply {
                                        lineWidth = 4f
                                        enableDashedLine(10f, 10f, 0f)
                                    })
                                }
                            }
                            if (balanceLs.isNotEmpty()) {
                                dataSets.add(balanceSet)
                            }
                        }

                        AnalyticsBarTypeEnum.ALL -> {
                            with(chart.axisLeft) {
                                if (expenditureLs.isEmpty() && incomeLs.isEmpty() && balanceLs.isEmpty()) {
                                    resetAxisMinimum()
                                    resetAxisMaximum()
                                    removeAllLimitLines()
                                } else {
                                    axisMinimum = when {
                                        expenditureLs.isNotEmpty() -> expenditureLs
                                        balanceLs.isNotEmpty() -> balanceLs
                                        else -> incomeLs
                                    }.minOf { it.y } - spaceValue
                                    axisMaximum = when {
                                        incomeLs.isNotEmpty() -> incomeLs
                                        balanceLs.isNotEmpty() -> balanceLs
                                        else -> expenditureLs
                                    }.maxOf { it.y } + spaceValue
                                }
                                if (expenditureLs.isEmpty() && balanceLs.isEmpty()) {
                                    removeAllLimitLines()
                                } else {
                                    addLimitLine(LimitLine(0f).apply {
                                        lineWidth = 4f
                                        enableDashedLine(10f, 10f, 0f)
                                    })
                                }
                            }
                            if (expenditureLs.isNotEmpty()) {
                                dataSets.add(expenditureSet)
                            }
                            if (incomeLs.isNotEmpty()) {
                                dataSets.add(incomeSet)
                            }
                            if (balanceLs.isNotEmpty()) {
                                dataSets.add(balanceSet)
                            }
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