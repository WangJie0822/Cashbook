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

package cn.wj.android.cashbook.feature.types.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.types.enums.MyCategoriesBookmarkEnum
import cn.wj.android.cashbook.feature.types.model.ExpandableRecordTypeModel
import cn.wj.android.cashbook.feature.types.viewmodel.MyCategoriesUiState
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/**
 * BUG-1 回归测试：分类界面在 Success + 非空 typeList 时，一级分类必须**真实可见**。
 *
 * 根因背景：`MyCategoriesTopBar` 把 `CbTabRow(Modifier.fillMaxSize())` 放进 `CbTopAppBar` 的 title 槽，
 * `fillMaxSize` 含 `fillMaxHeight`，使 Material3 `TopAppBar` 按 title 撑满全屏高度，`CbScaffold` 的
 * body 被挤压成 ~0 高 → 分类列表/空态都不渲染、tab 浮于屏幕垂直中部。修复：title 内 TabRow 改用
 * `Modifier.fillMaxWidth()`。
 *
 * 断言用 `assertIsDisplayed()`（而非 `assertExists()`）——body 塌陷时分类节点可能仍存在于语义树
 * 但被裁剪不可见，只有 `assertIsDisplayed` 能区分「真实渲染可见」与「存在但 0 高被裁剪」。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class MyCategoriesScreenRenderRegressionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun categoryList_isDisplayed_whenSuccessWithTypes() {
        val typeList = listOf(
            ExpandableRecordTypeModel(
                data = createRecordTypeModel(id = 1L, name = "餐饮", iconName = "vector_type_three_meals_24"),
                list = emptyList(),
            ),
            ExpandableRecordTypeModel(
                data = createRecordTypeModel(id = 2L, name = "交通", iconName = "vector_type_traffic_24"),
                list = emptyList(),
            ),
        )
        composeTestRule.setContent {
            CashbookTheme {
                MyCategoriesScreen(
                    shouldDisplayBookmark = MyCategoriesBookmarkEnum.DISMISS,
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    uiState = MyCategoriesUiState.Success(
                        selectedTab = RecordTypeCategoryEnum.EXPENDITURE,
                        typeList = typeList,
                    ),
                    onRequestSelectTypeCategory = {},
                    onRequestEditType = {},
                    onRequestChangeFirstTypeToSecond = {},
                    onRequestAddFirstType = {},
                    onRequestAddSecondType = {},
                    changeFirstTypeToSecond = { _, _ -> },
                    onRequestChangeSecondTypeToFirst = {},
                    onRequestMoveSecondTypeToAnother = { _, _ -> },
                    onRequestNaviToTypeStatistics = {},
                    onRequestDeleteType = {},
                    changeRecordTypeBeforeDelete = { _, _ -> },
                    onRequestSaveRecordType = { _, _, _, _ -> },
                    onRequestPopBackStack = {},
                )
            }
        }
        // body 未塌陷时，一级分类应真实可见
        composeTestRule.onNodeWithText("餐饮").assertIsDisplayed()
    }
}
