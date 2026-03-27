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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.wj.android.cashbook.core.design.component.CbCard
import cn.wj.android.cashbook.core.design.theme.LocalSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.system.measureTimeMillis

/**
 * 弹窗状态
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/19
 */
sealed interface DialogState {

    /** 弹窗隐藏 */
    data object Dismiss : DialogState

    /** 弹窗显示 */
    class Shown<T>(val data: T) : DialogState
}

/**
 * 全局进度弹窗状态
 *
 * @param hint 提示文本
 * @param cancelable 是否可取消
 * @param onDismiss 隐藏回调
 */
data class ProgressDialogState(
    val hint: String?,
    val cancelable: Boolean,
    val onDismiss: () -> Unit,
)

/**
 * 进度弹窗控制器接口
 */
interface ProgressDialogController {
    val dialogState: DialogState
    fun dismiss()
    fun show(
        hint: String? = null,
        cancelable: Boolean = true,
        onDismiss: () -> Unit = {},
    )
}

/**
 * 进度弹窗控制器默认实现
 */
class DefaultProgressDialogController : ProgressDialogController {
    override var dialogState: DialogState by mutableStateOf(DialogState.Dismiss)
        private set

    override fun dismiss() {
        dialogState = DialogState.Dismiss
    }

    override fun show(
        hint: String?,
        cancelable: Boolean,
        onDismiss: () -> Unit,
    ) {
        dialogState = DialogState.Shown(
            ProgressDialogState(
                hint = hint,
                cancelable = cancelable,
                onDismiss = onDismiss,
            ),
        )
    }
}

val LocalProgressDialogController = staticCompositionLocalOf<ProgressDialogController> {
    error("No ProgressDialogController provided")
}

/** 显示提示文本为[hint]，能否取消[cancelable]的进度弹窗，并执行[block]逻辑，最低显示[minInterval]ms，最高显示[timeout]ms，逻辑执行完成、异常或超时返回结果，并执行回调[onDismiss] */
suspend inline fun <R> runCatchWithProgress(
    controller: ProgressDialogController,
    hint: String? = null,
    cancelable: Boolean = true,
    noinline onDismiss: () -> Unit = {},
    minInterval: Long = 550L,
    timeout: Long = -1L,
    noinline block: suspend CoroutineScope.() -> R,
): Result<R> {
    val result: Result<R>
    val ms = measureTimeMillis {
        controller.show(hint, cancelable, onDismiss)
        result = runCatching {
            val timeMillis = if (timeout < 0L) {
                Long.MAX_VALUE
            } else {
                timeout
            }
            withTimeout(timeMillis, block)
        }
    }
    if (ms < minInterval) {
        delay(minInterval - ms)
    }
    controller.dismiss()
    return result
}

/**
 * 这个 [CompositionLocal] 用于提供提示文本为空时显示的默认提示
 *
 * ```
 * CompositionLocalProvider(
 *     LocalProgressDialogHint provides "加载中..."
 * ) { }
 * ```
 */
val LocalProgressDialogHint =
    staticCompositionLocalOf<String> { error("No ProgressDialogHint provided") }

/**
 * 全局进度条弹窗
 * - 状态依赖 [LocalProgressDialogController]
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/8
 */
@Composable
fun ProgressDialog() {
    val controller = LocalProgressDialogController.current
    val spacing = LocalSpacing.current
    ((controller.dialogState as? DialogState.Shown<*>)?.data as? ProgressDialogState)?.let { state ->
        Dialog(
            onDismissRequest = {
                controller.dismiss()
                state.onDismiss()
            },
            properties = DialogProperties(
                dismissOnBackPress = state.cancelable,
                dismissOnClickOutside = state.cancelable,
            ),
            content = {
                CbCard {
                    Column(
                        modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.extraLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LinearProgressIndicator()
                        Text(
                            text = if (state.hint.isNullOrBlank()) {
                                LocalProgressDialogHint.current
                            } else {
                                state.hint
                            },
                            modifier = Modifier.padding(top = spacing.extraLarge),
                        )
                    }
                }
            },
        )
    }
}
