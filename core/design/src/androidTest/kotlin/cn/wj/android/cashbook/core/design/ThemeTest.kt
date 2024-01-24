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

package cn.wj.android.cashbook.core.design

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.theme.BackgroundTheme
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.design.theme.DarkAndroidColorScheme
import cn.wj.android.cashbook.core.design.theme.DarkExtendedColors
import cn.wj.android.cashbook.core.design.theme.GradientColors
import cn.wj.android.cashbook.core.design.theme.LightAndroidColorScheme
import cn.wj.android.cashbook.core.design.theme.LightExtendedColors
import cn.wj.android.cashbook.core.design.theme.LocalBackgroundTheme
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.design.theme.LocalGradientColors
import cn.wj.android.cashbook.core.design.theme.LocalTintTheme
import cn.wj.android.cashbook.core.design.theme.TintTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * 验证不同配置下应用主题是否符合预期
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/1/18
 */
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun darkThemeFalse_dynamicColorFalse() {
        composeTestRule.setContent {
            CashbookTheme(
                darkTheme = false,
                disableDynamicTheming = true,
            ) {
                val colorScheme = LightAndroidColorScheme
                assertColorSchemesEqual(colorScheme, MaterialTheme.colorScheme)
                val extendColors = LightExtendedColors
                assertEquals(extendColors, LocalExtendedColors.current)
                val gradientColors = defaultGradientColors(colorScheme)
                assertEquals(gradientColors, LocalGradientColors.current)
                val backgroundTheme = defaultBackgroundTheme(colorScheme)
                assertEquals(backgroundTheme, LocalBackgroundTheme.current)
                val tintTheme = defaultTintTheme()
                assertEquals(tintTheme, LocalTintTheme.current)
            }
        }
    }

    @Test
    fun darkThemeTrue_dynamicColorFalse() {
        composeTestRule.setContent {
            CashbookTheme(
                darkTheme = true,
                disableDynamicTheming = true,
            ) {
                val colorScheme = DarkAndroidColorScheme
                assertColorSchemesEqual(colorScheme, MaterialTheme.colorScheme)
                val extendColors = DarkExtendedColors
                assertEquals(extendColors, LocalExtendedColors.current)
                val gradientColors = defaultGradientColors(colorScheme)
                assertEquals(gradientColors, LocalGradientColors.current)
                val backgroundTheme = defaultBackgroundTheme(colorScheme)
                assertEquals(backgroundTheme, LocalBackgroundTheme.current)
                val tintTheme = defaultTintTheme()
                assertEquals(tintTheme, LocalTintTheme.current)
            }
        }
    }

    @Test
    fun darkThemeFalse_dynamicColorTrue() {
        composeTestRule.setContent {
            CashbookTheme(
                darkTheme = false,
                disableDynamicTheming = false,
            ) {
                val colorScheme = dynamicLightColorSchemeWithFallback()
                assertColorSchemesEqual(colorScheme, MaterialTheme.colorScheme)
                val extendColors = LightExtendedColors
                assertEquals(extendColors, LocalExtendedColors.current)
                val gradientColors = dynamicGradientColorsWithFallback(colorScheme)
                assertEquals(gradientColors, LocalGradientColors.current)
                val backgroundTheme = defaultBackgroundTheme(colorScheme)
                assertEquals(backgroundTheme, LocalBackgroundTheme.current)
                val tintTheme = dynamicTintThemeWithFallback(colorScheme)
                assertEquals(tintTheme, LocalTintTheme.current)
            }
        }
    }

    @Test
    fun darkThemeTrue_dynamicColorTrue() {
        composeTestRule.setContent {
            CashbookTheme(
                darkTheme = true,
                disableDynamicTheming = false,
            ) {
                val colorScheme = dynamicDarkColorSchemeWithFallback()
                assertColorSchemesEqual(colorScheme, MaterialTheme.colorScheme)
                val extendColors = DarkExtendedColors
                assertEquals(extendColors, LocalExtendedColors.current)
                val gradientColors = dynamicGradientColorsWithFallback(colorScheme)
                assertEquals(gradientColors, LocalGradientColors.current)
                val backgroundTheme = defaultBackgroundTheme(colorScheme)
                assertEquals(backgroundTheme, LocalBackgroundTheme.current)
                val tintTheme = dynamicTintThemeWithFallback(colorScheme)
                assertEquals(tintTheme, LocalTintTheme.current)
            }
        }
    }

    /**
     * Workaround for the fact that the NiA design system specify all color scheme values.
     */
    private fun assertColorSchemesEqual(
        expectedColorScheme: ColorScheme,
        actualColorScheme: ColorScheme,
    ) {
        assertEquals(expectedColorScheme.primary, actualColorScheme.primary)
        assertEquals(expectedColorScheme.onPrimary, actualColorScheme.onPrimary)
        assertEquals(expectedColorScheme.primaryContainer, actualColorScheme.primaryContainer)
        assertEquals(expectedColorScheme.onPrimaryContainer, actualColorScheme.onPrimaryContainer)
        assertEquals(expectedColorScheme.secondary, actualColorScheme.secondary)
        assertEquals(expectedColorScheme.onSecondary, actualColorScheme.onSecondary)
        assertEquals(expectedColorScheme.secondaryContainer, actualColorScheme.secondaryContainer)
        assertEquals(
            expectedColorScheme.onSecondaryContainer,
            actualColorScheme.onSecondaryContainer,
        )
        assertEquals(expectedColorScheme.tertiary, actualColorScheme.tertiary)
        assertEquals(expectedColorScheme.onTertiary, actualColorScheme.onTertiary)
        assertEquals(expectedColorScheme.tertiaryContainer, actualColorScheme.tertiaryContainer)
        assertEquals(expectedColorScheme.onTertiaryContainer, actualColorScheme.onTertiaryContainer)
        assertEquals(expectedColorScheme.error, actualColorScheme.error)
        assertEquals(expectedColorScheme.onError, actualColorScheme.onError)
        assertEquals(expectedColorScheme.errorContainer, actualColorScheme.errorContainer)
        assertEquals(expectedColorScheme.onErrorContainer, actualColorScheme.onErrorContainer)
        assertEquals(expectedColorScheme.background, actualColorScheme.background)
        assertEquals(expectedColorScheme.onBackground, actualColorScheme.onBackground)
        assertEquals(expectedColorScheme.surface, actualColorScheme.surface)
        assertEquals(expectedColorScheme.onSurface, actualColorScheme.onSurface)
        assertEquals(expectedColorScheme.surfaceVariant, actualColorScheme.surfaceVariant)
        assertEquals(expectedColorScheme.onSurfaceVariant, actualColorScheme.onSurfaceVariant)
        assertEquals(expectedColorScheme.inverseSurface, actualColorScheme.inverseSurface)
        assertEquals(expectedColorScheme.inverseOnSurface, actualColorScheme.inverseOnSurface)
        assertEquals(expectedColorScheme.outline, actualColorScheme.outline)
    }

    @Composable
    private fun dynamicLightColorSchemeWithFallback(): ColorScheme {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(LocalContext.current)
        } else {
            LightAndroidColorScheme
        }
    }

    @Composable
    private fun dynamicDarkColorSchemeWithFallback(): ColorScheme {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(LocalContext.current)
        } else {
            DarkAndroidColorScheme
        }
    }

    private fun dynamicGradientColorsWithFallback(colorScheme: ColorScheme): GradientColors {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            emptyGradientColors(colorScheme)
        } else {
            defaultGradientColors(colorScheme)
        }
    }

    private fun defaultGradientColors(colorScheme: ColorScheme): GradientColors {
        return GradientColors(
            top = colorScheme.surface,
            bottom = colorScheme.tertiaryContainer,
            container = colorScheme.surface,
        )
    }

    private fun emptyGradientColors(colorScheme: ColorScheme): GradientColors {
        return GradientColors(container = colorScheme.surfaceColorAtElevation(2.dp))
    }

    private fun defaultBackgroundTheme(colorScheme: ColorScheme): BackgroundTheme {
        return BackgroundTheme(
            color = colorScheme.surface,
            tonalElevation = 2.dp,
        )
    }

    private fun defaultTintTheme(): TintTheme {
        return TintTheme()
    }

    private fun dynamicTintThemeWithFallback(colorScheme: ColorScheme): TintTheme {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            TintTheme(colorScheme.primary)
        } else {
            TintTheme()
        }
    }
}
