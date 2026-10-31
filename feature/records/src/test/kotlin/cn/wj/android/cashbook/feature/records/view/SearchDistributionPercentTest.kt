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

package cn.wj.android.cashbook.feature.records.view

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordCategoryNet
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class SearchDistributionPercentTest {
    @Test fun sparseNonzeroIdsDoNotCollideOrGiveTheirSlotsToZeroGroups() {
        val categories = (1L..17L).map { id -> RecordCategoryNet(id, RecordTypeCategoryEnum.EXPENDITURE, "分类$id", if (id in listOf(1L, 9L, 17L)) 100L else 0L) }
        val slots = distributionColorOrder(categories)
        // 已知不同的旧槽位作为阳性对照，随后验证三个稀疏非零ID。
        assertThat(slots["0:2"]).isNotEqualTo(slots["0:3"])
        assertThat(listOf(1L, 9L, 17L).map { slots.getValue("0:$it") % 8 }.toSet()).hasSize(3)
        assertThat(listOf(1L, 9L, 17L).map { slots.getValue("0:$it") }).containsExactly(0, 1, 2)
        assertThat(distributionColorOrder(categories.reversed())).isEqualTo(slots)
    }

    @Test fun negativeNetUsesItsMagnitudeAndZeroRemainsZero() {
        assertThat(distributionPercent(-300, 800, Locale.US)).isEqualTo("37.50%")
        assertThat(distributionPercent(0, 800, Locale.US)).isEqualTo("0%")
        assertThat(distributionPercent(0, 0, Locale.US)).isEqualTo("0%")
    }

    @Test fun verySmallPositiveShareIsNotRoundedToZero() {
        assertThat(distributionPercent(1, 1000000, Locale.US)).isEqualTo("<0.01%")
        assertThat(distributionPercent(1, 1000000, Locale.GERMANY)).isEqualTo("<0,01%")
    }
}
