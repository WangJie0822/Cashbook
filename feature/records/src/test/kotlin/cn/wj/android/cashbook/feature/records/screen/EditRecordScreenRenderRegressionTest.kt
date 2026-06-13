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

package cn.wj.android.cashbook.feature.records.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordUiState
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/**
 * BUG-1 回归测试：记账编辑界面在 Success 时，类型列表区（typeListContent）必须**真实可见**。
 *
 * 根因背景：`EditRecordScreen` 把 `CbTabRow(Modifier.fillMaxSize())` 放进 `CbTopAppBar` 的 title 槽，
 * `fillMaxSize` 含 `fillMaxHeight`，使 Material3 `TopAppBar` 按 title 撑满全屏高度，`CbScaffold` 的
 * body（含分类网格 typeListContent）被挤压成 ~0 高 → 分类不渲染、tab 浮于屏幕垂直中部。修复：
 * title 内 TabRow 改用 `Modifier.fillMaxWidth()`。
 *
 * 断言用 `assertIsDisplayed()`——body 塌陷时 typeListContent 内容可能仍存在于语义树但被裁剪不可见。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class EditRecordScreenRenderRegressionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val successUiState = EditRecordUiState.Success(
        amountText = "100.00",
        chargesText = "",
        concessionsText = "",
        remarkText = "午餐",
        selectedAssetId = -1L,
        assetText = "现金",
        relatedAssetText = "",
        dateTimeText = "2024-01-01 12:00",
        reimbursable = false,
        selectedTypeId = 1L,
        needRelated = false,
        relatedCount = 0,
        relatedAmount = "",
        imageQuality = ImageQualityEnum.ORIGINAL,
    )

    @Test
    fun typeListContent_isDisplayed_whenSuccess() {
        composeTestRule.setContent {
            CashbookTheme {
                EditRecordScreen(
                    uiState = successUiState,
                    shouldDisplayBookmark = EditRecordBookmarkEnum.NONE,
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    onRecordTimeClick = {},
                    onDateSelected = {},
                    onTimeSelected = {},
                    selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                    onTypeCategorySelect = {},
                    bottomSheetType = EditRecordBottomSheetEnum.NONE,
                    onRequestDismissBottomSheet = {},
                    onAmountClick = {},
                    onAmountChange = {},
                    onChargesClick = {},
                    onChargesChange = {},
                    onConcessionsClick = {},
                    onRelatedRecordClick = {},
                    onConcessionsChange = {},
                    onImageItemClick = { _, _ -> },
                    onImageListSave = {},
                    typeListContent = { Text(text = "类型列表") },
                    onRemarkChange = {},
                    onAssetClick = {},
                    onRelatedAssetClick = {},
                    selectAssetBottomSheetContent = {},
                    selectRelatedAssetBottomSheetContent = {},
                    tagText = "",
                    imageList = emptyList(),
                    onTagClick = {},
                    onImageClick = {},
                    selectTagBottomSheetContent = {},
                    onReimbursableClick = {},
                    onSaveClick = {},
                    onBackClick = {},
                )
            }
        }
        // body 未塌陷时，类型列表区应真实可见
        composeTestRule.onNodeWithText("类型列表").assertIsDisplayed()
    }
}
