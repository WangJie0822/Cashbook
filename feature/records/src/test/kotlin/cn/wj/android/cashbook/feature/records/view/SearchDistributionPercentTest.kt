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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class SearchDistributionPercentTest {
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
