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

package cn.wj.android.cashbook.ui

import androidx.compose.ui.test.junit4.createComposeRule
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [shouldDisableDynamicTheming] 和 [shouldUseDarkTheme] 的单元测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class ActivityUiStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region shouldDisableDynamicTheming

    @Test
    fun when_loading_shouldDisableDynamicTheming_then_false() {
        var result = true
        composeTestRule.setContent {
            result = shouldDisableDynamicTheming(ActivityUiState.Loading)
        }
        assertThat(result).isFalse()
    }

    @Test
    fun when_success_dynamicColor_false_then_disable_true() {
        var result = false
        composeTestRule.setContent {
            result = shouldDisableDynamicTheming(
                ActivityUiState.Success(darkMode = DarkModeEnum.FOLLOW_SYSTEM, dynamicColor = false),
            )
        }
        assertThat(result).isTrue()
    }

    @Test
    fun when_success_dynamicColor_true_then_disable_false() {
        var result = true
        composeTestRule.setContent {
            result = shouldDisableDynamicTheming(
                ActivityUiState.Success(darkMode = DarkModeEnum.FOLLOW_SYSTEM, dynamicColor = true),
            )
        }
        assertThat(result).isFalse()
    }

    // endregion

    // region shouldUseDarkTheme

    @Test
    fun when_dark_mode_on_then_returns_true() {
        var result = false
        composeTestRule.setContent {
            result = shouldUseDarkTheme(
                ActivityUiState.Success(darkMode = DarkModeEnum.DARK, dynamicColor = false),
            )
        }
        assertThat(result).isTrue()
    }

    @Test
    fun when_dark_mode_off_then_returns_false() {
        var result = true
        composeTestRule.setContent {
            result = shouldUseDarkTheme(
                ActivityUiState.Success(darkMode = DarkModeEnum.LIGHT, dynamicColor = false),
            )
        }
        assertThat(result).isFalse()
    }

    @Test
    fun when_dark_mode_follow_system_then_does_not_crash() {
        // FOLLOW_SYSTEM 时调用 isSystemInDarkTheme()，不应抛出异常
        composeTestRule.setContent {
            shouldUseDarkTheme(
                ActivityUiState.Success(darkMode = DarkModeEnum.FOLLOW_SYSTEM, dynamicColor = false),
            )
        }
        // 只要 setContent 不抛异常即通过
    }

    // endregion
}
