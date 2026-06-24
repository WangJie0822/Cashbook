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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/**
 * 记录详情 sheet 截图测试：守护「标记已报销 / 改回待报销」按钮态的可视化回归。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class RecordDetailsSheetScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** 待报销：可报销 + 未关联 + 未标记 → 显示「标记已报销」按钮 */
    private val pendingRecord = RecordViewsEntity(
        id = 1L,
        typeId = 1L,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        typeName = "餐饮",
        typeIconResName = "vector_type_three_meals_24",
        assetId = null,
        assetName = null,
        assetIconResId = null,
        relatedAssetId = null,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = 50_00L,
        finalAmount = 50_00L,
        charges = 0L,
        concessions = 0L,
        remark = "午餐",
        reimbursable = true,
        relatedTags = emptyList(),
        relatedImage = emptyList(),
        relatedRecord = emptyList(),
        relatedAmount = 0L,
        recordTime = 1705276800000L,
        reimbursed = false,
    )

    /** 已手动标记已报销：可报销 + 未关联 + 已标记 → 显示「改回待报销」按钮 */
    private val markedRecord = pendingRecord.copy(reimbursed = true)

    @Test
    fun recordDetailsSheet_pending_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "RecordDetailsSheet",
            overrideFileName = "RecordDetailsSheet_pending",
        ) {
            RecordDetailsSheet(
                recordData = pendingRecord,
                onRequestNaviToEditRecord = {},
                onRequestNaviToAssetInfo = {},
                onMarkReimbursed = {},
                onRevertReimbursed = {},
                onRequestDismissSheet = {},
            )
        }
    }

    @Test
    fun recordDetailsSheet_marked_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "RecordDetailsSheet",
            overrideFileName = "RecordDetailsSheet_marked",
        ) {
            RecordDetailsSheet(
                recordData = markedRecord,
                onRequestNaviToEditRecord = {},
                onRequestNaviToAssetInfo = {},
                onMarkReimbursed = {},
                onRevertReimbursed = {},
                onRequestDismissSheet = {},
            )
        }
    }
}
