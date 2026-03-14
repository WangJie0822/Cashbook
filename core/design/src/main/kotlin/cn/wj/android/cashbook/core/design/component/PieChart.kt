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

import android.text.TextUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 饼图切片数据
 *
 * @param label 标签文字
 * @param value 百分比值（0-1）
 * @param color 切片颜色
 * @param labelColor 标签文字颜色
 */
data class PieSlice(
    val label: String,
    val value: Float,
    val color: Color,
    val labelColor: Color = Color.Unspecified,
)

/**
 * 标记线布局数据
 */
private data class LeaderLineLayout(
    val edgePoint: Offset,
    val elbowPoint: Offset,
    val labelAnchor: Offset,
    val labelText: String,
    val isRightSide: Boolean,
    val color: Color,
)

/**
 * Compose 饼图（环形图）组件
 *
 * @param slices 饼图切片数据列表
 * @param centerText 中心文字
 * @param modifier Modifier
 * @param holeRatio 中心孔洞比例（相对于半径，0-1）
 * @param sliceSpaceDegrees 切片间距角度
 * @param selectedIndex 当前选中的切片索引，-1 表示无选中
 * @param showLeaderLines 是否显示标记线和外部标签
 * @param onSliceSelected 切片被点击时的回调，参数为切片索引，-1 表示点击空白区域
 */
@Composable
fun CbPieChart(
    slices: List<PieSlice>,
    centerText: String,
    modifier: Modifier = Modifier,
    holeRatio: Float = 0.58f,
    sliceSpaceDegrees: Float = 2f,
    selectedIndex: Int = -1,
    showLeaderLines: Boolean = false,
    onSliceSelected: (Int) -> Unit = {},
) {
    // 入场动画
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 250))
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // 缓存 Paint 对象，避免每帧重新创建
    val labelPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val ellipsizePaint = remember {
        android.text.TextPaint().apply {
            isAntiAlias = true
        }
    }

    // 预计算每个切片的角度范围
    val totalValue = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)
    val sweepAngles = remember(slices) {
        slices.map { (it.value / totalValue) * 360f }
    }
    val startAngles = remember(sweepAngles) {
        val starts = mutableListOf<Float>()
        var current = -90f // 从12点方向开始
        for (sweep in sweepAngles) {
            starts.add(current)
            current += sweep
        }
        starts
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices, showLeaderLines) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val canvasSize = min(size.width, size.height).toFloat()
                        val pieRadius = if (showLeaderLines) {
                            canvasSize / 2f * PIE_RADIUS_RATIO
                        } else {
                            canvasSize / 2f
                        }
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        val holeRadius = pieRadius * holeRatio

                        if (distance < holeRadius || distance > pieRadius) {
                            onSliceSelected(-1)
                            return@detectTapGestures
                        }

                        // 计算点击角度（从12点方向顺时针）
                        var angle = Math.toDegrees(
                            atan2(dy.toDouble(), dx.toDouble()),
                        ).toFloat()
                        // 转换为从-90度开始的坐标系
                        angle = (angle + 360f) % 360f
                        val normalizedAngle = (angle + 90f) % 360f

                        var accumulated = 0f
                        for (i in sweepAngles.indices) {
                            accumulated += sweepAngles[i]
                            if (normalizedAngle <= accumulated) {
                                onSliceSelected(i)
                                return@detectTapGestures
                            }
                        }
                        onSliceSelected(-1)
                    }
                },
        ) {
            val canvasSize = min(size.width, size.height)
            val selectionShift = 5f

            // 标记线模式下缩小饼图半径，为标签留出空间
            val pieRadius = if (showLeaderLines) {
                canvasSize / 2f * PIE_RADIUS_RATIO
            } else {
                canvasSize / 2f
            }
            val holeRadius = pieRadius * holeRatio
            val arcSize = Size(pieRadius * 2f, pieRadius * 2f)
            val pieCenter = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(
                pieCenter.x - pieRadius,
                pieCenter.y - pieRadius,
            )

            val progress = animationProgress.value

            for (i in slices.indices) {
                val slice = slices[i]
                val startAngle = startAngles[i]
                val sweepAngle = (sweepAngles[i] - sliceSpaceDegrees).coerceAtLeast(0f) * progress
                val isSelected = i == selectedIndex

                // 选中时向外偏移
                val shiftedTopLeft = if (isSelected) {
                    val midAngle = startAngle + sweepAngles[i] / 2f
                    val shiftX = cos(midAngle * PI / 180f).toFloat() * selectionShift
                    val shiftY = sin(midAngle * PI / 180f).toFloat() * selectionShift
                    Offset(topLeft.x + shiftX, topLeft.y + shiftY)
                } else {
                    topLeft
                }

                // 绘制扇形
                drawArc(
                    color = slice.color,
                    startAngle = startAngle + sliceSpaceDegrees / 2f,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = shiftedTopLeft,
                    size = arcSize,
                )

                // 非标记线模式：绘制内部百分比标签
                if (!showLeaderLines && progress > 0.9f && slice.value > 0.03f) {
                    drawPercentLabel(
                        slice = slice,
                        startAngle = startAngle + sliceSpaceDegrees / 2f,
                        sweepAngle = sweepAngles[i] - sliceSpaceDegrees,
                        center = Offset(
                            shiftedTopLeft.x + arcSize.width / 2f,
                            shiftedTopLeft.y + arcSize.height / 2f,
                        ),
                        radius = pieRadius,
                        paint = labelPaint,
                    )
                }
            }

            // 绘制中心孔洞
            drawCircle(
                color = surfaceColor,
                radius = holeRadius * progress,
                center = pieCenter,
            )

            // 标记线模式：在动画接近完成时绘制标记线和标签
            if (showLeaderLines && progress > 0.9f) {
                val layouts = calculateLeaderLineLayouts(
                    slices = slices,
                    startAngles = startAngles,
                    sweepAngles = sweepAngles,
                    pieCenter = pieCenter,
                    pieRadius = pieRadius,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                    ellipsizePaint = ellipsizePaint,
                )
                drawLeaderLines(
                    layouts = layouts,
                    paint = labelPaint,
                    textColor = onSurfaceColor,
                )
            }
        }

        // 中心文字
        if (animationProgress.value > 0.5f) {
            Text(
                text = centerText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceColor,
            )
        }
    }
}

/** 标记线模式下饼图半径缩放比例 */
private const val PIE_RADIUS_RATIO = 0.55f

/** 显示标记线的最小百分比阈值 */
private const val LEADER_LINE_MIN_PERCENT = 0.02f

/** 径向延伸段长度比例（相对于饼图半径） */
private const val LEADER_LINE_RADIAL_RATIO = 0.15f

/** 标签与拐点之间最小水平间距 */
private val LEADER_LINE_TEXT_GAP = 4.dp

/** 水平段最小长度 */
private val LEADER_LINE_MIN_HORIZONTAL = 8.dp

/** 标签文字大小 */
private val LEADER_LINE_TEXT_SIZE = 10.sp

/** 标记线宽度 */
private val LEADER_LINE_STROKE_WIDTH = 1.dp

/**
 * 计算所有标记线的布局位置
 */
private fun DrawScope.calculateLeaderLineLayouts(
    slices: List<PieSlice>,
    startAngles: List<Float>,
    sweepAngles: List<Float>,
    pieCenter: Offset,
    pieRadius: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    ellipsizePaint: android.text.TextPaint,
): List<LeaderLineLayout> {
    val radialLength = pieRadius * LEADER_LINE_RADIAL_RATIO
    val textGap = LEADER_LINE_TEXT_GAP.toPx()
    val minHorizontal = LEADER_LINE_MIN_HORIZONTAL.toPx()
    val textSize = LEADER_LINE_TEXT_SIZE.toPx()
    val minSpacing = textSize * 1.4f

    ellipsizePaint.textSize = textSize

    // 固定列对齐：所有右侧标签从同一 X 位置开始，左侧同理
    // 使用最小水平段长度确保列位置有足够偏移
    val rightColumnX = pieCenter.x + pieRadius + radialLength + minHorizontal
    val leftColumnX = pieCenter.x - pieRadius - radialLength - minHorizontal

    val rightLayouts = mutableListOf<LeaderLineLayout>()
    val leftLayouts = mutableListOf<LeaderLineLayout>()

    for (i in slices.indices) {
        val slice = slices[i]
        if (slice.value < LEADER_LINE_MIN_PERCENT) continue

        val midAngleDeg = startAngles[i] + sweepAngles[i] / 2f
        val midAngleRad = (midAngleDeg * PI / 180f).toFloat()

        // 圆弧边缘起点
        val edgeX = pieCenter.x + cos(midAngleRad) * pieRadius
        val edgeY = pieCenter.y + sin(midAngleRad) * pieRadius

        // 径向延伸到拐点
        val elbowX = pieCenter.x + cos(midAngleRad) * (pieRadius + radialLength)
        val elbowY = pieCenter.y + sin(midAngleRad) * (pieRadius + radialLength)

        // 判断左右侧：角度在 -90~90 度范围（即右半边）
        val normalizedDeg = ((midAngleDeg % 360f) + 360f) % 360f
        val isRightSide = normalizedDeg < 90f || normalizedDeg > 270f

        // 水平段终点：对齐到固定列位置，若拐点已超过列位置则加小间距
        val labelX = if (isRightSide) {
            maxOf(elbowX + textGap, rightColumnX)
        } else {
            minOf(elbowX - textGap, leftColumnX)
        }

        val percent = (slice.value * 100f)
        @Suppress("DefaultLocale")
        val labelText = "${slice.label} ${String.format("%.1f%%", percent)}"

        val layout = LeaderLineLayout(
            edgePoint = Offset(edgeX, edgeY),
            elbowPoint = Offset(elbowX, elbowY),
            labelAnchor = Offset(labelX, elbowY),
            labelText = labelText,
            isRightSide = isRightSide,
            color = slice.color,
        )

        if (isRightSide) {
            rightLayouts.add(layout)
        } else {
            leftLayouts.add(layout)
        }
    }

    // 分侧防重叠
    resolveOverlaps(rightLayouts, minSpacing, canvasHeight)
    resolveOverlaps(leftLayouts, minSpacing, canvasHeight)

    val allLayouts = rightLayouts + leftLayouts
    return allLayouts.map { layout ->
        // 截断过长文字
        val availableWidth = if (layout.isRightSide) {
            canvasWidth - layout.labelAnchor.x - 4.dp.toPx()
        } else {
            layout.labelAnchor.x - 4.dp.toPx()
        }.coerceAtLeast(0f)
        val ellipsized = TextUtils.ellipsize(
            layout.labelText,
            ellipsizePaint,
            availableWidth,
            TextUtils.TruncateAt.END,
        ).toString()
        layout.copy(labelText = ellipsized)
    }
}

/**
 * 防重叠：对同侧标签按 Y 坐标排序，保证最小间距
 */
private fun resolveOverlaps(
    layouts: MutableList<LeaderLineLayout>,
    minSpacing: Float,
    canvasHeight: Float,
) {
    if (layouts.size <= 1) return
    layouts.sortBy { it.labelAnchor.y }

    // 从上到下推移
    for (i in 1 until layouts.size) {
        val prevY = layouts[i - 1].labelAnchor.y
        val currY = layouts[i].labelAnchor.y
        val overlap = (prevY + minSpacing) - currY
        if (overlap > 0) {
            val curr = layouts[i]
            layouts[i] = curr.copy(
                labelAnchor = curr.labelAnchor.copy(y = currY + overlap),
            )
        }
    }

    // 如果底部超出边界，从下往上回推
    val margin = minSpacing / 2f
    val lastY = layouts.last().labelAnchor.y
    if (lastY > canvasHeight - margin) {
        val shift = lastY - (canvasHeight - margin)
        for (i in layouts.indices.reversed()) {
            val curr = layouts[i]
            layouts[i] = curr.copy(
                labelAnchor = curr.labelAnchor.copy(y = curr.labelAnchor.y - shift),
            )
            // 后续标签也需要检查间距
            if (i > 0) {
                val above = layouts[i - 1]
                val gap = layouts[i].labelAnchor.y - above.labelAnchor.y
                if (gap < minSpacing) {
                    layouts[i - 1] = above.copy(
                        labelAnchor = above.labelAnchor.copy(
                            y = layouts[i].labelAnchor.y - minSpacing,
                        ),
                    )
                }
            }
        }
    }

    // 如果顶部超出边界，从上往下推回
    val firstY = layouts.first().labelAnchor.y
    if (firstY < margin) {
        val shift = margin - firstY
        for (i in layouts.indices) {
            val curr = layouts[i]
            layouts[i] = curr.copy(
                labelAnchor = curr.labelAnchor.copy(y = curr.labelAnchor.y + shift),
            )
            if (i < layouts.size - 1) {
                val below = layouts[i + 1]
                val gap = below.labelAnchor.y - layouts[i].labelAnchor.y
                if (gap < minSpacing) {
                    layouts[i + 1] = below.copy(
                        labelAnchor = below.labelAnchor.copy(
                            y = layouts[i].labelAnchor.y + minSpacing,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * 绘制标记线和标签文字
 */
private fun DrawScope.drawLeaderLines(
    layouts: List<LeaderLineLayout>,
    paint: android.graphics.Paint,
    textColor: Color,
) {
    val strokeWidth = LEADER_LINE_STROKE_WIDTH.toPx()
    val textSize = LEADER_LINE_TEXT_SIZE.toPx()

    for (layout in layouts) {
        val adjustedElbow = Offset(layout.elbowPoint.x, layout.labelAnchor.y)

        // 绘制径向段（边缘 → 原始拐点）
        drawLine(
            color = layout.color,
            start = layout.edgePoint,
            end = layout.elbowPoint,
            strokeWidth = strokeWidth,
        )
        // 防重叠导致 Y 偏移时，绘制过渡段（原始拐点 → 调整后拐点）
        if (layout.elbowPoint.y != layout.labelAnchor.y) {
            drawLine(
                color = layout.color,
                start = layout.elbowPoint,
                end = adjustedElbow,
                strokeWidth = strokeWidth,
            )
        }
        // 绘制水平段（调整后拐点 → 标签锚点）
        drawLine(
            color = layout.color,
            start = adjustedElbow,
            end = layout.labelAnchor,
            strokeWidth = strokeWidth,
        )

        // 绘制标签文字
        paint.textSize = textSize
        paint.color = android.graphics.Color.argb(
            (textColor.alpha * 255).toInt(),
            (textColor.red * 255).toInt(),
            (textColor.green * 255).toInt(),
            (textColor.blue * 255).toInt(),
        )
        paint.textAlign = if (layout.isRightSide) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
        drawContext.canvas.nativeCanvas.drawText(
            layout.labelText,
            layout.labelAnchor.x,
            layout.labelAnchor.y + textSize / 3f,
            paint,
        )
    }
}

/**
 * 绘制百分比标签（内部模式）
 *
 * @param paint 复用的 Paint 对象，在此函数内仅更新动态属性（textSize、color）
 */
private fun DrawScope.drawPercentLabel(
    slice: PieSlice,
    startAngle: Float,
    sweepAngle: Float,
    center: Offset,
    radius: Float,
    paint: android.graphics.Paint,
) {
    val midAngle = startAngle + sweepAngle / 2f
    val labelRadius = radius * 0.78f
    val labelX = center.x + cos(midAngle * PI / 180f).toFloat() * labelRadius
    val labelY = center.y + sin(midAngle * PI / 180f).toFloat() * labelRadius

    val percent = (slice.value * 100f)
    val text = String.format("%.1f%%", percent)
    val textColor = if (slice.labelColor != Color.Unspecified) {
        slice.labelColor
    } else {
        Color.White
    }

    // 仅更新动态属性，不重新创建 Paint
    paint.textAlign = android.graphics.Paint.Align.CENTER
    paint.color = android.graphics.Color.argb(
        (textColor.alpha * 255).toInt(),
        (textColor.red * 255).toInt(),
        (textColor.green * 255).toInt(),
        (textColor.blue * 255).toInt(),
    )
    paint.textSize = 10.sp.toPx()

    drawContext.canvas.nativeCanvas.drawText(
        text,
        labelX,
        labelY + paint.textSize / 3f,
        paint,
    )
}
