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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
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
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class EditRecordTypeListScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val typeList = listOf(
        RecordTypeEntity(
            id = 1L,
            parentId = -1L,
            name = "餐饮",
            iconResName = "vector_type_three_meals_24",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            sort = 0,
            child = listOf(
                RecordTypeEntity(
                    id = 10L,
                    parentId = 1L,
                    name = "早餐",
                    iconResName = "vector_type_three_meals_24",
                    typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                    sort = 0,
                    child = emptyList(),
                    selected = false,
                    shapeType = -1,
                    needRelated = false,
                ),
                RecordTypeEntity(
                    id = 11L,
                    parentId = 1L,
                    name = "午餐",
                    iconResName = "vector_type_three_meals_24",
                    typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                    sort = 1,
                    child = emptyList(),
                    selected = false,
                    shapeType = 1,
                    needRelated = false,
                ),
            ),
            selected = true,
            shapeType = 0,
            needRelated = false,
        ),
        RecordTypeEntity(
            id = 2L,
            parentId = -1L,
            name = "交通",
            iconResName = "vector_type_traffic_24",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            sort = 1,
            child = emptyList(),
            selected = false,
            shapeType = 0,
            needRelated = false,
        ),
    )

    @Test
    fun editRecordTypeListScreen_withTypes_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "EditRecordTypeListScreen") {
            EditRecordTypeListScreen(
                currentTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                typeList = typeList,
                onTypeSelect = {},
                onTypeSettingClick = {},
            )
        }
    }

    @Test
    fun editRecordTypeListScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "EditRecordTypeListScreen",
            overrideFileName = "EditRecordTypeListScreen_empty",
        ) {
            EditRecordTypeListScreen(
                currentTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                typeList = emptyList(),
                onTypeSelect = {},
                onTypeSettingClick = {},
                modifier = Modifier.defaultMinSize(minHeight = 120.dp),
            )
        }
    }

    @Test
    fun editRecordTypeListScreen_withTypes_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "EditRecordTypeListScreen") {
            CashbookTheme {
                EditRecordTypeListScreen(
                    currentTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                    typeList = typeList,
                    onTypeSelect = {},
                    onTypeSettingClick = {},
                )
            }
        }
    }

    @Test
    fun editRecordTypeListScreen_empty_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "EditRecordTypeListScreen_empty") {
            CashbookTheme {
                EditRecordTypeListScreen(
                    currentTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                    typeList = emptyList(),
                    onTypeSelect = {},
                    onTypeSettingClick = {},
                    modifier = Modifier.defaultMinSize(minHeight = 120.dp),
                )
            }
        }
    }
}
