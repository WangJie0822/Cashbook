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
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class MyCategoriesScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val typeList = listOf(
        ExpandableRecordTypeModel(
            data = createRecordTypeModel(id = 1L, name = "餐饮", iconName = "vector_type_three_meals_24"),
            list = listOf(
                ExpandableRecordTypeModel(
                    data = createRecordTypeModel(
                        id = 10L,
                        parentId = 1L,
                        name = "早餐",
                        iconName = "vector_type_three_meals_24",
                        typeLevel = TypeLevelEnum.SECOND,
                    ),
                    list = emptyList(),
                ),
            ),
        ),
        ExpandableRecordTypeModel(
            data = createRecordTypeModel(id = 2L, name = "交通", iconName = "vector_type_traffic_24"),
            list = emptyList(),
        ),
    )

    @Test
    fun myCategoriesScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "MyCategoriesScreen",
            overrideFileName = "MyCategoriesScreen_loading",
        ) {
            MyCategoriesScreen(
                shouldDisplayBookmark = MyCategoriesBookmarkEnum.DISMISS,
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                uiState = MyCategoriesUiState.Loading,
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

    @Test
    fun myCategoriesScreen_withTypes_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "MyCategoriesScreen") {
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

    @Test
    fun myCategoriesScreen_withTypes_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "MyCategoriesScreen") {
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
    }

    @Test
    fun myCategoriesScreen_empty_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "MyCategoriesScreen_empty") {
            CashbookTheme {
                MyCategoriesScreen(
                    shouldDisplayBookmark = MyCategoriesBookmarkEnum.DISMISS,
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    uiState = MyCategoriesUiState.Success(
                        selectedTab = RecordTypeCategoryEnum.EXPENDITURE,
                        typeList = emptyList(),
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
    }
}
