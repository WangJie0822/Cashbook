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

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordCategoryNet
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordSearchStatistics
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "zh-rCN-w411dp-h891dp-480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SearchDistributionScreenshotTests {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val mixed = RecordSearchStatistics(
        RecordFilterSummary(3, 40000, 12000),
        listOf(
            RecordCategoryNet(20, RecordTypeCategoryEnum.INCOME, "工资", 40000),
            RecordCategoryNet(10, RecordTypeCategoryEnum.EXPENDITURE, "餐饮与日常生活开销", 12300),
            RecordCategoryNet(30, RecordTypeCategoryEnum.TRANSFER, "转账优惠", -300),
        ),
    )
    private val many = RecordSearchStatistics(
        RecordFilterSummary(60, 0, 182901),
        (1L..60L).map {
            RecordCategoryNet(it, RecordTypeCategoryEnum.EXPENDITURE, if (it == 1L) "朋友聚餐与家庭特别餐饮开支" else "分类$it", if (it == 60L) 1L else (61L - it) * 100L)
        },
    )

    @Composable private fun Content(statistics: RecordSearchStatistics) {
        val state = rememberSearchDistributionState("screenshot", statistics.categories)
        LazyColumn { searchResultDistribution(statistics, state) }
    }

    @Test fun mixed_multipleThemes() { compose.captureMultiTheme("SearchDistribution_mixed") { Content(mixed) } }

    @Test fun mixed_multipleDevices() { compose.captureMultiDevice("SearchDistribution_mixed") { CashbookTheme { Content(mixed) } } }

    @Test fun many_multipleThemes() { compose.captureMultiTheme("SearchDistribution_many") { Content(many) } }

    @Test fun many_multipleDevices() { compose.captureMultiDevice("SearchDistribution_many") { CashbookTheme { Content(many) } } }

    @Test fun negativeOnly_multipleThemes() {
        val data = RecordSearchStatistics(RecordFilterSummary(1, 0, -300), listOf(RecordCategoryNet(30, RecordTypeCategoryEnum.TRANSFER, "转账优惠", -300)))
        compose.captureMultiTheme("SearchDistribution_negative") { Content(data) }
    }

    @Test fun zero_multipleThemes() {
        val data = RecordSearchStatistics(RecordFilterSummary(2, 0, 0), listOf(RecordCategoryNet(30, RecordTypeCategoryEnum.TRANSFER, "免费转账", 0)))
        compose.captureMultiTheme("SearchDistribution_zero") { Content(data) }
    }
}
