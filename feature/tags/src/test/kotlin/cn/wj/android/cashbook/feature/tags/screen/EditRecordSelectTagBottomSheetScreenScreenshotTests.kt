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

package cn.wj.android.cashbook.feature.tags.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.model.Selectable
import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.DialogState
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
class EditRecordSelectTagBottomSheetScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val tagList = listOf(
        Selectable(createTagModel(id = 1L, name = "餐饮"), selected = true),
        Selectable(createTagModel(id = 2L, name = "交通"), selected = false),
        Selectable(createTagModel(id = 3L, name = "日常"), selected = true),
    )

    @Test
    fun editRecordSelectTagBottomSheetScreen_withTags_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "EditRecordSelectTagBottomSheetScreen") {
            EditRecordSelectTagBottomSheetScreen(
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                tagList = tagList,
                onAddTagClick = {},
                onDoneClick = {},
                onTagItemClick = {},
            )
        }
    }

    @Test
    fun editRecordSelectTagBottomSheetScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "EditRecordSelectTagBottomSheetScreen",
            overrideFileName = "EditRecordSelectTagBottomSheetScreen_empty",
        ) {
            EditRecordSelectTagBottomSheetScreen(
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                tagList = emptyList(),
                onAddTagClick = {},
                onDoneClick = {},
                onTagItemClick = {},
            )
        }
    }

    @Test
    fun editRecordSelectTagBottomSheetScreen_withTags_multipleDevices() {
        composeTestRule.captureMultiDevice(
            screenshotName = "EditRecordSelectTagBottomSheetScreen",
        ) {
            CashbookTheme {
                EditRecordSelectTagBottomSheetScreen(
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    tagList = tagList,
                    onAddTagClick = {},
                    onDoneClick = {},
                    onTagItemClick = {},
                )
            }
        }
    }

    @Test
    fun editRecordSelectTagBottomSheetScreen_empty_multipleDevices() {
        composeTestRule.captureMultiDevice(
            screenshotName = "EditRecordSelectTagBottomSheetScreen_empty",
        ) {
            CashbookTheme {
                EditRecordSelectTagBottomSheetScreen(
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    tagList = emptyList(),
                    onAddTagClick = {},
                    onDoneClick = {},
                    onTagItemClick = {},
                )
            }
        }
    }
}
