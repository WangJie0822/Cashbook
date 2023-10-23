package cn.wj.android.cashbook.core.ui.expand

import androidx.compose.ui.graphics.Color

val Color.colorInt: Int
    get() = -0x1000000 or
            ((red * 255.0f + 0.5f).toInt() shl 16) or
            ((green * 255.0f + 0.5f).toInt() shl 8) or
            (blue * 255.0f + 0.5f).toInt()