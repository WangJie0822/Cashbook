package cn.wj.android.cashbook.core.design.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

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

@Composable
fun fixedContentColorFor(backgroundColor: Color): Color {
    return MaterialTheme.colorScheme.contentColorFor(backgroundColor).takeOrElse {
        with(LocalExtendedColors.current) {
            when (backgroundColor) {
                quaternary -> onQuaternary
                quaternaryContainer -> onQuaternaryContainer
                income -> onIncome
                expenditure -> onExpenditure
                transfer -> onTransfer
                else -> LocalContentColor.current
            }
        }
    }
}
