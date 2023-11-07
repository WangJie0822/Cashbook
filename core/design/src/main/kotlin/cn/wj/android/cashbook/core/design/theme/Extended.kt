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
    val onSelected: Color = Color.Unspecified,
    val selectedContainer: Color = Color.Unspecified,
    val onSelectedContainer: Color = Color.Unspecified,
    val unselected: Color = Color.Unspecified,
    val onUnselected: Color = Color.Unspecified,
    val unselectedContainer: Color = Color.Unspecified,
    val onUnselectedContainer: Color = Color.Unspecified,
    val income: Color = Color.Unspecified,
    val onIncome: Color = Color.Unspecified,
    val incomeContainer: Color = Color.Unspecified,
    val onIncomeContainer: Color = Color.Unspecified,
    val expenditure: Color = Color.Unspecified,
    val onExpenditure: Color = Color.Unspecified,
    val expenditureContainer: Color = Color.Unspecified,
    val onExpenditureContainer: Color = Color.Unspecified,
    val transfer: Color = Color.Unspecified,
    val onTransfer: Color = Color.Unspecified,
    val transferContainer: Color = Color.Unspecified,
    val onTransferContainer: Color = Color.Unspecified,
    val github: Color = Color.Unspecified,
    val onGithub: Color = Color.Unspecified,
    val githubContainer: Color = Color.Unspecified,
    val onGithubContainer: Color = Color.Unspecified,
    val gitee: Color = Color.Unspecified,
    val onGitee: Color = Color.Unspecified,
    val giteeContainer: Color = Color.Unspecified,
    val onGiteeContainer: Color = Color.Unspecified,
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
                selected -> onSelected
                selectedContainer -> onSelectedContainer
                unselected -> onUnselected
                unselectedContainer -> onUnselectedContainer
                income -> onIncome
                incomeContainer -> onIncomeContainer
                expenditure -> onExpenditure
                expenditureContainer -> onExpenditureContainer
                transfer -> onTransfer
                transferContainer -> onTransferContainer
                gitee -> onGitee
                giteeContainer -> onGiteeContainer
                github -> onGithub
                githubContainer -> onGithubContainer
                else -> LocalContentColor.current
            }
        }
    }
}

@Composable
fun fixedContainerColorFor(color: Color): Color {
    return with(MaterialTheme.colorScheme) {
        when (color) {
            primary -> primaryContainer
            secondary -> secondaryContainer
            tertiary -> tertiaryContainer
            error -> errorContainer
            else -> Color.Unspecified
        }
    }.takeOrElse {
        with(LocalExtendedColors.current) {
            when (color) {
                quaternary -> quaternaryContainer
                selected -> selectedContainer
                unselected -> unselectedContainer
                income -> incomeContainer
                expenditure -> expenditureContainer
                transfer -> transferContainer
                gitee -> giteeContainer
                github -> githubContainer
                else -> LocalContentColor.current
            }
        }
    }
}
