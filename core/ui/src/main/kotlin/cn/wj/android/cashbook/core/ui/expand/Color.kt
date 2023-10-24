package cn.wj.android.cashbook.core.ui.expand

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

fun generateDistinctColors(count: Int): List<Int> = mutableListOf<Int>().apply {
    repeat(count) { i ->
        add(android.graphics.Color.rgb(i % 256, (i * 7) % 256, (i * 13) % 256))
    }
}

val Color.colorInt: Int
    get() = android.graphics.Color.argb(
        (alpha * 255.0f + 0.5f).toInt(),
        (red * 255.0f + 0.5f).toInt(),
        (green * 255.0f + 0.5f).toInt(),
        (blue * 255.0f + 0.5f).toInt(),
    )