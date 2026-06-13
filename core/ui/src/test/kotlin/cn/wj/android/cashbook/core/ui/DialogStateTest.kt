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

package cn.wj.android.cashbook.core.ui

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * DialogState / 进度弹窗逻辑单元测试
 *
 * 覆盖：
 * - DefaultProgressDialogController 的 show/dismiss 状态机
 * - runCatchWithProgress 的成功/异常/超时三分支 + minInterval 补偿，确保始终 dismiss
 */
class DialogStateTest {

    /** 记录 show/dismiss 调用序的控制器替身 */
    private class RecordingController : ProgressDialogController {
        override var dialogState: DialogState = DialogState.Dismiss
        val events = mutableListOf<String>()

        override fun dismiss() {
            dialogState = DialogState.Dismiss
            events.add("dismiss")
        }

        override fun show(hint: String?, cancelable: Boolean, onDismiss: () -> Unit) {
            dialogState = DialogState.Shown(ProgressDialogState(hint, cancelable, onDismiss))
            events.add("show")
        }
    }

    // ---------------- DefaultProgressDialogController ----------------

    @Test
    fun controller_initial_state_is_dismiss() {
        assertThat(DefaultProgressDialogController().dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun controller_show_sets_shown_state_with_params() {
        val controller = DefaultProgressDialogController()
        controller.show(hint = "loading", cancelable = false)
        val state = controller.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        val data = (state as DialogState.Shown<*>).data as ProgressDialogState
        assertThat(data.hint).isEqualTo("loading")
        assertThat(data.cancelable).isFalse()
    }

    @Test
    fun controller_dismiss_resets_to_dismiss() {
        val controller = DefaultProgressDialogController()
        controller.show(hint = "x")
        controller.dismiss()
        assertThat(controller.dialogState).isEqualTo(DialogState.Dismiss)
    }

    // ---------------- runCatchWithProgress ----------------

    @Test
    fun runCatchWithProgress_success_returns_value_and_dismisses() = runTest {
        val controller = RecordingController()
        val result = runCatchWithProgress(controller, minInterval = 0L) { 42 }
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(42)
        assertThat(controller.dialogState).isEqualTo(DialogState.Dismiss)
        // 必先 show 再 dismiss
        assertThat(controller.events).containsExactly("show", "dismiss").inOrder()
    }

    @Test
    fun runCatchWithProgress_block_throws_returns_failure_and_dismisses() = runTest {
        val controller = RecordingController()
        val result = runCatchWithProgress<Int>(controller, minInterval = 0L) {
            throw IllegalStateException("boom")
        }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        // 异常路径仍必须 dismiss
        assertThat(controller.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun runCatchWithProgress_timeout_returns_failure_and_dismisses() = runTest {
        val controller = RecordingController()
        val result = runCatchWithProgress<String>(controller, minInterval = 0L, timeout = 100L) {
            delay(1000L)
            "never"
        }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull())
            .isInstanceOf(TimeoutCancellationException::class.java)
        // 超时路径仍必须 dismiss
        assertThat(controller.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun runCatchWithProgress_fast_block_with_min_interval_still_succeeds() = runTest {
        // block 瞬间完成（真实耗时 < minInterval），走 delay 补偿分支，结果不受影响
        val controller = RecordingController()
        val result = runCatchWithProgress(controller, minInterval = 550L) { 7 }
        assertThat(result.getOrNull()).isEqualTo(7)
        assertThat(controller.dialogState).isEqualTo(DialogState.Dismiss)
    }
}
