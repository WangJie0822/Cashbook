package cn.wj.android.cashbook.feature.records.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity
import cn.wj.android.cashbook.core.ui.expand.colorInt
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

@Composable
internal fun AnalyticsPieChart(
    barCenterText: String,
    dataList: List<AnalyticsRecordPieEntity>,
    pieColorsCompose: List<Color>,
    onPieColorsCompose: List<Color>,
    modifier: Modifier = Modifier,
) {
    val pieColors = pieColorsCompose.map { it.colorInt }
    val onPieColors = onPieColorsCompose.map { it.colorInt }

    var tempDataList: List<AnalyticsRecordPieEntity>? by remember {
        mutableStateOf(null)
    }
    var tempBarCenterText: String? by remember {
        mutableStateOf(null)
    }

    AndroidView(
        modifier = modifier,
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
                        private var tempCenterText: CharSequence = ""
                        override fun onValueSelected(e: Entry, h: Highlight) {
                            if (!centerText.contains(Symbol.CNY)) {
                                tempCenterText = centerText
                            }
                            (e.data as? AnalyticsRecordPieEntity)?.let { data ->
                                centerText = "${data.typeName}\n${data.totalAmount.withCNY()}"
                            }
                        }

                        override fun onNothingSelected() {
                            centerText = tempCenterText
                        }
                    })
            }
        },
        update = { chart ->
            if (dataList != tempDataList || barCenterText != tempBarCenterText) {
                tempDataList = dataList
                tempBarCenterText = barCenterText
                val pieEntryList = dataList.filter { it.percent > 0.03f }
                    .map {
                        PieEntry(it.percent, it.typeName, it)
                    }
                chart.centerText = barCenterText
                chart.data = PieData(PieDataSet(pieEntryList, "").apply {
                    sliceSpace = 3f
                    selectionShift = 5f
                    colors = pieColors
                    setLabelColors(onPieColors)
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
}