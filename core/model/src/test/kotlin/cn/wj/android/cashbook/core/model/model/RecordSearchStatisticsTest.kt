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

package cn.wj.android.cashbook.core.model.model

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class RecordSearchStatisticsTest {
    @Test fun negativeOnly_keepsSignAndUsesAbsoluteMagnitude() {
        val statistics = statistics(category(1, -300))
        assertThat(statistics.categories.single().amount).isEqualTo(-300)
        assertThat(statistics.magnitude).isEqualTo(300)
    }

    @Test fun zeroGroup_isRetainedAndSameNamesRemainDistinct() {
        val statistics = statistics(category(1, 0), category(2, 200))
        assertThat(statistics.categories.map { it.typeId }).containsExactly(1L, 2L)
        assertThat(statistics.magnitude).isEqualTo(200)
    }

    @Test fun minimumLongAbsoluteValue_isRejected() {
        assertThrows(ArithmeticException::class.java) { statistics(category(1, Long.MIN_VALUE)) }
    }

    @Test fun magnitudeOverflow_isRejected() {
        assertThrows(ArithmeticException::class.java) { statistics(category(1, Long.MAX_VALUE), category(2, 1)) }
    }

    @Test fun balanceOverflow_isRejected() {
        assertThrows(ArithmeticException::class.java) {
            RecordSearchStatistics(RecordFilterSummary(2, Long.MAX_VALUE, -1), emptyList())
        }
    }

    private fun statistics(vararg categories: RecordCategoryNet) =
        RecordSearchStatistics(RecordFilterSummary(categories.size.toLong(), 0, 0), categories.toList())

    private fun category(id: Long, amount: Long) = RecordCategoryNet(id, RecordTypeCategoryEnum.TRANSFER, "同名", amount)
}
