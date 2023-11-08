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

package cn.wj.android.cashbook.core.design.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * 文本输入框状态
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/30
 */
open class TextFieldState(
    defaultText: String = "",
    private val validator: (String) -> Boolean = { true },
    private val filter: (String) -> Boolean = { true },
    private val errorFor: (String) -> String = { "" },
    private val delayAfterTextChange: Long = 200L,
    private val afterTextChange: (String) -> Unit = {},
) {
    var text: String by mutableStateOf(defaultText)

    // was the TextField ever focused
    var isFocusedDirty: Boolean by mutableStateOf(false)
    var isFocused: Boolean by mutableStateOf(false)
    private var displayErrors: Boolean by mutableStateOf(false)

    open val isValid: Boolean
        get() = validator(text)

    fun requestErrors() {
        displayErrors = true
    }

    fun onTextChange(text: String) {
        if (filter(text)) {
            this.text = text
            onTextChanged(text)
        }
    }

    private var delayJob: Job? = null
    private val coroutineScope: CoroutineScope by lazy {
        object : CoroutineScope {
            override val coroutineContext: CoroutineContext
                get() = SupervisorJob() + Dispatchers.Main.immediate
        }
    }

    private fun onTextChanged(text: String) {
        delayJob?.cancel()
        delayJob = coroutineScope.launch {
            delay(delayAfterTextChange)
            afterTextChange(text)
        }
    }

    fun onFocusChange(focused: Boolean) {
        isFocused = focused
        if (focused) isFocusedDirty = true
    }

    fun enableShowErrors() {
        // only show errors if the text was at least once focused
        if (isFocusedDirty) {
            displayErrors = true
        }
    }

    fun showErrors() = !isValid && displayErrors

    open fun getError(): String? {
        return if (showErrors()) {
            errorFor(text)
        } else {
            null
        }
    }
}

fun textFieldStateSaver(state: TextFieldState) = listSaver<TextFieldState, Any>(
    save = { listOf(it.text, it.isFocusedDirty) },
    restore = {
        state.apply {
            text = it[0] as String
            isFocusedDirty = it[1] as Boolean
        }
    },
)
