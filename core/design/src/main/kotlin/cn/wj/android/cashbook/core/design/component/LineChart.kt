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

package cn.wj.android.cashbook.core.design.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * 折线图数据集
 *
 * @param label 数据集标签（用于图例）
 * @param entries 数据点列表
 * @param color 线条颜色
 * @param lineWidth 线宽
 */
data class LineDataSet(
    val label: String,
    val entries: List<LineEntry>,
    val color: Color,
    val lineWidth: Float = 2f,
)

/**
 * 折线图数据点
 *
 * @param x X 轴位置
 * @param y Y 轴数值
 * @param label X 轴标签
 */
data class LineEntry(
    val x: Float,
    val y: Float,
    val label: String = "",
)

/**
 * Compose 折线图组件
 *
 * @param dataSets 数据集列表（支持多条折线）
 * @param modifier Modifier
 * @param showZeroLine 是否显示 0 值参考线（虚线）
 * @param formatYValue Y 轴标签格式化函数
 */
@Composable
fun CbLineChart(
    dataSets: List<LineDataSet>,
    modifier: Modifier = Modifier,
    showZeroLine: Boolean = false,
    formatYValue: (Float) -> String = { formatLargeValue(it) },
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dataSets) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 250))
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline

    // 缓存 Paint 对象，避免每帧重新创建
    val yLabelPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }
    val xLabelPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val legendPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }

    // 预排序数据集，避免在 Canvas DrawScope 内每帧排序
    val sortedDataSets = remember(dataSets) {
        dataSets.map { it.copy(entries = it.entries.sortedBy { e -> e.x }) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (sortedDataSets.isEmpty() || sortedDataSets.all { it.entries.isEmpty() }) return@Canvas

        val progress = animationProgress.value

        // 计算绘图区域边距
        val leftPadding = 60f
        val rightPadding = 16f
        val topPadding = 30f
        val bottomPadding = 40f

        val plotWidth = size.width - leftPadding - rightPadding
        val plotHeight = size.height - topPadding - bottomPadding

        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        // 收集所有数据点，计算数值范围
        val allEntries = sortedDataSets.flatMap { it.entries }
        if (allEntries.isEmpty()) return@Canvas

        val minX = allEntries.minOf { it.x }
        val maxX = allEntries.maxOf { it.x }
        val minY = allEntries.minOf { it.y }
        val maxY = allEntries.maxOf { it.y }

        // Y 轴范围加间距
        val ySpan = (maxY - minY).coerceAtLeast(1f)
        val yPadding = ySpan * 0.15f
        val yMin = minY - yPadding
        val yMax = maxY + yPadding

        // 坐标转换
        fun toScreenX(x: Float): Float {
            return if (maxX == minX) {
                leftPadding + plotWidth / 2f
            } else {
                leftPadding + (x - minX) / (maxX - minX) * plotWidth
            }
        }

        fun toScreenY(y: Float): Float {
            return topPadding + (1f - (y - yMin) / (yMax - yMin)) * plotHeight
        }

        // 绘制 Y 轴标签和网格线
        val yTickCount = 4
        for (i in 0..yTickCount) {
            val yValue = yMin + (yMax - yMin) * i / yTickCount
            val screenY = toScreenY(yValue)
            val label = formatYValue(yValue)

            // Y 轴标签
            drawYLabel(
                text = label,
                x = leftPadding - 8f,
                y = screenY,
                color = onSurfaceColor,
                paint = yLabelPaint,
            )
        }

        // 绘制 X 轴标签
        val xLabels = sortedDataSets.first().entries
        val maxLabels = (plotWidth / 60f).toInt().coerceAtLeast(2)
        val step = (xLabels.size / maxLabels).coerceAtLeast(1)
        for (i in xLabels.indices step step) {
            val entry = xLabels[i]
            if (entry.label.isNotEmpty()) {
                val screenX = toScreenX(entry.x)
                drawXLabel(
                    text = entry.label,
                    x = screenX,
                    y = size.height - 8f,
                    color = onSurfaceColor,
                    paint = xLabelPaint,
                )
            }
        }

        // 绘制 0 参考线（虚线）
        if (showZeroLine && yMin < 0f && yMax > 0f) {
            val zeroY = toScreenY(0f)
            drawLine(
                color = outlineColor,
                start = Offset(leftPadding, zeroY),
                end = Offset(size.width - rightPadding, zeroY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
            )
        }

        // 绘制折线（使用预排序的数据集）
        for (dataSet in sortedDataSets) {
            if (dataSet.entries.isEmpty()) continue

            val path = Path()
            dataSet.entries.forEachIndexed { index, entry ->
                val screenX = toScreenX(entry.x)
                // 动画：Y 值从基线渐变到目标位置
                val baseY = toScreenY(0f).coerceIn(topPadding, topPadding + plotHeight)
                val targetY = toScreenY(entry.y)
                val animatedY = baseY + (targetY - baseY) * progress

                if (index == 0) {
                    path.moveTo(screenX, animatedY)
                } else {
                    path.lineTo(screenX, animatedY)
                }
            }

            drawPath(
                path = path,
                color = dataSet.color,
                style = Stroke(width = dataSet.lineWidth),
            )

            // 绘制数据点圆点
            for (entry in dataSet.entries) {
                val screenX = toScreenX(entry.x)
                val baseY = toScreenY(0f).coerceIn(topPadding, topPadding + plotHeight)
                val targetY = toScreenY(entry.y)
                val animatedY = baseY + (targetY - baseY) * progress

                drawCircle(
                    color = dataSet.color,
                    radius = 3f,
                    center = Offset(screenX, animatedY),
                )
            }
        }

        // 绘制图例
        drawLegend(
            dataSets = sortedDataSets,
            x = size.width - rightPadding,
            y = topPadding,
            textColor = onSurfaceColor,
            paint = legendPaint,
        )
    }
}

private fun DrawScope.drawYLabel(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    paint: android.graphics.Paint,
) {
    // 仅更新动态属性，不重新创建 Paint
    paint.color = android.graphics.Color.argb(
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
    )
    paint.textSize = 9.sp.toPx()
    drawContext.canvas.nativeCanvas.drawText(text, x, y + paint.textSize / 3f, paint)
}

private fun DrawScope.drawXLabel(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    paint: android.graphics.Paint,
) {
    // 仅更新动态属性，不重新创建 Paint
    paint.color = android.graphics.Color.argb(
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
    )
    paint.textSize = 9.sp.toPx()
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private fun DrawScope.drawLegend(
    dataSets: List<LineDataSet>,
    x: Float,
    y: Float,
    textColor: Color,
    paint: android.graphics.Paint,
) {
    if (dataSets.size <= 1) return
    // 仅更新动态属性，不重新创建 Paint
    paint.color = android.graphics.Color.argb(
        (textColor.alpha * 255).toInt(),
        (textColor.red * 255).toInt(),
        (textColor.green * 255).toInt(),
        (textColor.blue * 255).toInt(),
    )
    paint.textSize = 8.sp.toPx()

    var currentY = y
    for (dataSet in dataSets) {
        val lineY = currentY + paint.textSize / 2f
        // 颜色指示线
        drawLine(
            color = dataSet.color,
            start = Offset(x - paint.measureText(dataSet.label) - 16f, lineY),
            end = Offset(x - paint.measureText(dataSet.label) - 4f, lineY),
            strokeWidth = 2f,
        )
        // 文字
        drawContext.canvas.nativeCanvas.drawText(
            dataSet.label,
            x,
            currentY + paint.textSize,
            paint,
        )
        currentY += paint.textSize + 4f
    }
}

/**
 * 格式化大数值（类似 MPChart 的 LargeValueFormatter）
 */
fun formatLargeValue(value: Float): String {
    val absValue = abs(value)
    return when {
        absValue >= 1_000_000f -> String.format("%.1fM", value / 1_000_000f)
        absValue >= 1_000f -> String.format("%.1fK", value / 1_000f)
        absValue == 0f -> "0"
        else -> String.format("%.0f", value)
    }
}
