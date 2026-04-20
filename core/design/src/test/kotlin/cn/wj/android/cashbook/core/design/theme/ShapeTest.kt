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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Design Token 约束测试：保证 [CashbookShapes] 的取值不被意外改动
 *
 * - Density 取 1f，1dp = 1px，便于直接与 dp 数值比较
 * - 数值与 Material 3 默认保持一致；如需调整需同步更新本测试
 */
class ShapeTest {

    private val density = Density(density = 1f)
    private val boundingSize = Size(100f, 100f)

    @Test
    fun extraSmall_corner_is_4dp() {
        assertCornerSize(CashbookShapes.extraSmall as RoundedCornerShape, expectedDp = 4f)
    }

    @Test
    fun small_corner_is_8dp() {
        assertCornerSize(CashbookShapes.small as RoundedCornerShape, expectedDp = 8f)
    }

    @Test
    fun medium_corner_is_12dp() {
        assertCornerSize(CashbookShapes.medium as RoundedCornerShape, expectedDp = 12f)
    }

    @Test
    fun large_corner_is_16dp() {
        assertCornerSize(CashbookShapes.large as RoundedCornerShape, expectedDp = 16f)
    }

    @Test
    fun extraLarge_corner_is_28dp() {
        assertCornerSize(CashbookShapes.extraLarge as RoundedCornerShape, expectedDp = 28f)
    }

    private fun assertCornerSize(shape: RoundedCornerShape, expectedDp: Float) {
        val topStart = shape.topStart.toPx(boundingSize, density)
        val topEnd = shape.topEnd.toPx(boundingSize, density)
        val bottomEnd = shape.bottomEnd.toPx(boundingSize, density)
        val bottomStart = shape.bottomStart.toPx(boundingSize, density)
        assertThat(topStart).isEqualTo(expectedDp)
        assertThat(topEnd).isEqualTo(expectedDp)
        assertThat(bottomEnd).isEqualTo(expectedDp)
        assertThat(bottomStart).isEqualTo(expectedDp)
    }
}
