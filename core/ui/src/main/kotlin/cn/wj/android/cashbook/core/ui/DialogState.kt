package cn.wj.android.cashbook.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.delay

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
 * 全局进度条弹窗管理对象
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/8
 */
object ProgressDialogManager {

    /** 弹窗状态 */
    var dialogState: DialogState by mutableStateOf(DialogState.Dismiss)
        private set

    /** 隐藏弹窗 */
    fun dismiss() {
        dialogState = DialogState.Dismiss
    }

    /**
     * 显示弹窗
     *
     * @param hint 提示文本
     * @param cancelable 是否可取消
     * @param onDismiss 隐藏回调
     */
    fun show(
        hint: String? = null,
        cancelable: Boolean = true,
        onDismiss: () -> Unit = {},
    ) {
        dialogState = DialogState.Shown(
            ProgressDialogState(
                hint = hint,
                cancelable = cancelable,
                onDismiss = onDismiss,
            )
        )
    }
}

suspend inline fun <R> runCatchWithProgress(
    hint: String? = null,
    cancelable: Boolean = true,
    noinline onDismiss: () -> Unit = {},
    minInterval: Long = 550L,
    block: () -> R,
): Result<R> {
    val result: Result<R>
    val ms = measureTimeMillis {
        ProgressDialogManager.show(hint, cancelable, onDismiss)
        result = runCatching(block)
    }
    if (ms < minInterval) {
        delay(minInterval - ms)
    }
    ProgressDialogManager.dismiss()
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
    staticCompositionLocalOf<String> { error("No Back Dispatcher provided") }

/**
 * 全局进度条弹窗
 * - 状态依赖 [ProgressDialogManager]
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/8
 */
@Composable
fun ProgressDialog() {
    ((ProgressDialogManager.dialogState as? DialogState.Shown<*>)?.data as? ProgressDialogState)?.let { state ->
        Dialog(
            onDismissRequest = {
                ProgressDialogManager.dismiss()
                state.onDismiss()
            },
            properties = DialogProperties(
                dismissOnBackPress = state.cancelable,
                dismissOnClickOutside = state.cancelable,
            ),
            content = {
                Card {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LinearProgressIndicator()
                        Text(
                            text = if (state.hint.isNullOrBlank()) {
                                LocalProgressDialogHint.current
                            } else {
                                state.hint
                            },
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    }
                }
            },
        )
    }
}