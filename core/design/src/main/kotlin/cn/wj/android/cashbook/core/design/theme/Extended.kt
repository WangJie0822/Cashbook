package cn.wj.android.cashbook.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 拓展颜色数据类
 */
@Immutable
data class ExtendedColors(
    val quaternary: Color = Color.Unspecified,
    val onQuaternary: Color = Color.Unspecified,
    val quaternaryContainer: Color = Color.Unspecified,
    val onQuaternaryContainer: Color = Color.Unspecified,
    val selected: Color = Color.Unspecified,
    val unselected: Color = Color.Unspecified,
    val income: Color = Color.Unspecified,
    val onIncome: Color = Color.Unspecified,
    val expenditure: Color = Color.Unspecified,
    val onExpenditure: Color = Color.Unspecified,
    val transfer: Color = Color.Unspecified,
    val onTransfer: Color = Color.Unspecified,
    val github: Color = Color.Unspecified,
    val gitee: Color = Color.Unspecified,
)

/**
 * A composition local for [ExtendedColors].
 */
val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }
