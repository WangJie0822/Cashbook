package cn.wj.android.cashbook.core.design.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularNodata
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.LocalDefaultEmptyImagePainter
import cn.wj.android.cashbook.core.design.component.LocalDefaultLoadingHint

/**
 * 应用主题
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/7
 */

/**
 * 白色 Android 主题配色
 */
@VisibleForTesting
val LightAndroidColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

/**
 * 黑色 Android 主题配色
 */
@VisibleForTesting
val DarkAndroidColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

val LightExtendedColors = ExtendedColors(
    quaternary = light_quaternary,
    onQuaternary = light_onQuaternary,
    quaternaryContainer = light_quaternaryContainer,
    onQuaternaryContainer = light_onQuaternaryContainer,
    selected = light_selected,
    onSelected = light_onSelected,
    selectedContainer = light_selectedContainer,
    onSelectedContainer = light_onSelectedContainer,
    unselected = light_unselected,
    onUnselected = light_onUnselected,
    unselectedContainer = light_unselectedContainer,
    onUnselectedContainer = light_onUnselectedContainer,
    income = light_income,
    onIncome = light_onIncome,
    incomeContainer = light_incomeContainer,
    onIncomeContainer = light_onIncomeContainer,
    expenditure = light_expenditure,
    onExpenditure = light_onExpenditure,
    expenditureContainer = light_expenditureContainer,
    onExpenditureContainer = light_onExpenditureContainer,
    transfer = light_transfer,
    onTransfer = light_onTransfer,
    transferContainer = light_transferContainer,
    onTransferContainer = light_onTransferContainer,
    github = light_github,
    onGithub = light_onGithub,
    githubContainer = light_githubContainer,
    onGithubContainer = light_onGithubContainer,
    gitee = light_gitee,
    onGitee = light_onGitee,
    giteeContainer = light_giteeContainer,
    onGiteeContainer = light_onGiteeContainer,
)

val DarkExtendedColors = ExtendedColors(
    quaternary = dark_quaternary,
    onQuaternary = dark_onQuaternary,
    quaternaryContainer = dark_quaternaryContainer,
    onQuaternaryContainer = dark_onQuaternaryContainer,
    selected = dark_selected,
    onSelected = dark_onSelected,
    selectedContainer = dark_selectedContainer,
    onSelectedContainer = dark_onSelectedContainer,
    unselected = dark_unselected,
    onUnselected = dark_onUnselected,
    unselectedContainer = dark_unselectedContainer,
    onUnselectedContainer = dark_onUnselectedContainer,
    income = dark_income,
    onIncome = dark_onIncome,
    incomeContainer = dark_incomeContainer,
    onIncomeContainer = dark_onIncomeContainer,
    expenditure = dark_expenditure,
    onExpenditure = dark_onExpenditure,
    expenditureContainer = dark_expenditureContainer,
    onExpenditureContainer = dark_onExpenditureContainer,
    transfer = dark_transfer,
    onTransfer = dark_onTransfer,
    transferContainer = dark_transferContainer,
    onTransferContainer = dark_onTransferContainer,
    github = dark_github,
    onGithub = dark_onGithub,
    githubContainer = dark_githubContainer,
    onGithubContainer = dark_onGithubContainer,
    gitee = dark_gitee,
    onGitee = dark_onGitee,
    giteeContainer = dark_giteeContainer,
    onGiteeContainer = dark_onGiteeContainer,
)

/**
 * 应用主题
 *
 * @param darkTheme 是否使用深色方案，默认跟随系统
 * @param disableDynamicTheming 是否支持动态主题
 */
@Composable
fun CashbookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    disableDynamicTheming: Boolean = true,
    content: @Composable () -> Unit,
) {
    // 配色
    val colorScheme = when {
        !disableDynamicTheming && supportsDynamicTheming() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> if (darkTheme) DarkAndroidColorScheme else LightAndroidColorScheme
    }
    // 拓展颜色
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    // 渐变色
    val emptyGradientColors = GradientColors(container = colorScheme.surfaceColorAtElevation(2.dp))
    val defaultGradientColors = GradientColors(
        top = colorScheme.surface,
        bottom = colorScheme.tertiaryContainer,
        container = colorScheme.surface,
    )
    val gradientColors = when {
        !disableDynamicTheming && supportsDynamicTheming() -> emptyGradientColors
        else -> defaultGradientColors
    }
    // 背景
    val backgroundTheme = BackgroundTheme(
        color = colorScheme.surface,
        tonalElevation = 2.dp,
    )

    // 色彩
    val tintTheme = when {
        !disableDynamicTheming && supportsDynamicTheming() -> TintTheme(colorScheme.primary)
        else -> TintTheme()
    }
    // 组合
    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalGradientColors provides gradientColors,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CashbookTypography,
            content = content,
        )
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun PreviewTheme(
    defaultEmptyImagePainter: Painter = rememberVectorPainter(image = Icons.Filled.SignalCellularNodata),
    content: @Composable () -> Unit
) {
    CashbookTheme {
        CashbookGradientBackground {
            CompositionLocalProvider(
                LocalDefaultEmptyImagePainter provides defaultEmptyImagePainter,
                LocalDefaultLoadingHint provides "数据加载中",
                content = content
            )
        }
    }
}