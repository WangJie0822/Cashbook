package cn.wj.android.cashbook.core.ui.expand

import androidx.compose.ui.graphics.Color

val Color.colorInt: Int
    get() = android.graphics.Color.argb(
        (alpha * 255.0f + 0.5f).toInt(),
        (red * 255.0f + 0.5f).toInt(),
        (green * 255.0f + 0.5f).toInt(),
        (blue * 255.0f + 0.5f).toInt(),
    )