package cn.wj.android.cashbook.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 拓展颜色数据类
 */
@Immutable
data class ExtendedColors(
    val selected: Color = Color.Unspecified,
    val unselected: Color = Color.Unspecified,
    val income: Color = Color.Unspecified,
    val expenditure: Color = Color.Unspecified,
    val transfer: Color = Color.Unspecified,
    val github: Color = Color(0xFF212121),
    val gitee: Color =  Color(0xFFD7072F),
)

/**
 * A composition local for [ExtendedColors].
 */
val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }
