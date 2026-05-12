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

package cn.wj.android.cashbook.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * 饼图调色板 Design Token
 *
 * 按 Material 3 语义逐级展开 8 色，兼容多品类占比报表
 *
 * @param pieColors 扇区颜色
 * @param onPieColors 扇区上标签文字颜色，下标与 [pieColors] 对齐
 */
@Immutable
data class CbPieChartPalette(
    val pieColors: List<Color>,
    val onPieColors: List<Color>,
)

/**
 * 构造默认饼图调色板
 *
 * 取 colorScheme 的 primary/secondary/tertiary 三组 + [ExtendedColors.quaternary]，每组各 2 色（base/container），共 8 色
 *
 * 同一 composition 下依赖未变时复用同一实例，避免每次 recomposition 重建 List
 */
@Composable
fun rememberCbPieChartPalette(): CbPieChartPalette {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = LocalExtendedColors.current
    return remember(colorScheme, extendedColors) {
        CbPieChartPalette(
            pieColors = listOf(
                colorScheme.primary,
                colorScheme.primaryContainer,
                colorScheme.secondary,
                colorScheme.secondaryContainer,
                colorScheme.tertiary,
                colorScheme.tertiaryContainer,
                extendedColors.quaternary,
                extendedColors.quaternaryContainer,
            ),
            onPieColors = listOf(
                colorScheme.onPrimary,
                colorScheme.onPrimaryContainer,
                colorScheme.onSecondary,
                colorScheme.onSecondaryContainer,
                colorScheme.onTertiary,
                colorScheme.onTertiaryContainer,
                extendedColors.onQuaternary,
                extendedColors.onQuaternaryContainer,
            ),
        )
    }
}
