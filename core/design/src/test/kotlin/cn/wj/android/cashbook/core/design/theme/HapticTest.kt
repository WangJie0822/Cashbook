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

package cn.wj.android.cashbook.core.design.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * [rememberHapticOnClick] 行为测试：点击时应先触发触觉，再执行回调
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class HapticTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class RecordingHapticFeedback : HapticFeedback {
        val events = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            events.add(hapticFeedbackType)
        }
    }

    @Test
    fun click_triggers_haptic_with_default_type() {
        val recorder = RecordingHapticFeedback()
        val callbackInvoked = mutableListOf<String>()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides recorder) {
                val onClick = rememberHapticOnClick(
                    onClick = { callbackInvoked.add("tap") },
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onClick() },
                ) {
                    Text("test-btn")
                }
            }
        }

        composeTestRule.onNodeWithText("test-btn").performClick()

        assertThat(recorder.events).containsExactly(HapticFeedbackType.TextHandleMove)
        assertThat(callbackInvoked).containsExactly("tap")
    }

    @Test
    fun custom_haptic_type_is_respected() {
        val recorder = RecordingHapticFeedback()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides recorder) {
                val onClick = rememberHapticOnClick(
                    type = HapticFeedbackType.LongPress,
                    onClick = {},
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onClick() },
                ) {
                    Text("long-btn")
                }
            }
        }

        composeTestRule.onNodeWithText("long-btn").performClick()
        assertThat(recorder.events).containsExactly(HapticFeedbackType.LongPress)
    }
}
