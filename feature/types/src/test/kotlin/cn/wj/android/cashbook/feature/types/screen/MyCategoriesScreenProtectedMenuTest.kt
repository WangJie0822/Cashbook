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
 * 受保护类型渲染测试。
 *
 * 实现内容：FirstTypeItem/SecondTypeItem 点击「始终弹出菜单」+「受保护类型仅渲染统计数据项、
 * 隐藏编辑/删除/转换/移动」，已在 [MyCategoriesScreen] 实现并通过编译。
 *
 * 测试限制说明：plan 原设计为「点击受保护类型 → 断言菜单仅含统计数据」的 DropdownMenu 交互测试，
 * 但 Robolectric + Compose BOM 2026.05.01 环境下该交互无法自动化——已实测 performClick(touch)、
 * @Config(sdk=28)、点击带 OnClick 语义的 ListItem 节点、performSemanticsAction(OnClick) 四种方式，
 * 点击后 DropdownMenu 的菜单项均不出现在测试语义树（参考 robolectric/robolectric#9595 的
 * Compose clickable onClick regression / Popup 渲染限制）。故此处降级为渲染 smoke：验证受保护
 * 类型在分类界面可正常渲染不崩溃。菜单交互需后续 instrumented 测试（androidTest）覆盖。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class MyCategoriesScreenProtectedMenuTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun protectedFirstType_rendersWithoutCrash() {
        val protectedType = ExpandableRecordTypeModel(
            data = createRecordTypeModel(
                id = 1L,
                name = "报销",
                iconName = "vector_type_three_meals_24",
                protected = true,
            ),
            list = emptyList(),
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
                        typeList = listOf(protectedType),
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
        composeTestRule.onNodeWithText("报销").assertExists()
    }
}
