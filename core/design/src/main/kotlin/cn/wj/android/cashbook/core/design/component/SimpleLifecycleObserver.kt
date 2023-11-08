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

@file:Suppress("unused")

package cn.wj.android.cashbook.core.design.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 简易生命周期观察者
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/14
 */
open class SimpleLifecycleObserver : DefaultLifecycleObserver {

    private var onCreate: ((LifecycleOwner) -> Unit)? = null

    fun onCreate(onCreate: (LifecycleOwner) -> Unit) {
        this.onCreate = onCreate
    }

    override fun onCreate(owner: LifecycleOwner) {
        onCreate?.invoke(owner)
    }

    private var onResume: ((LifecycleOwner) -> Unit)? = null

    fun onResume(onResume: (LifecycleOwner) -> Unit) {
        this.onResume = onResume
    }

    override fun onResume(owner: LifecycleOwner) {
        onResume?.invoke(owner)
    }

    private var onStart: ((LifecycleOwner) -> Unit)? = null

    fun onStart(onStart: (LifecycleOwner) -> Unit) {
        this.onStart = onStart
    }

    override fun onStart(owner: LifecycleOwner) {
        onStart?.invoke(owner)
    }

    private var onPause: ((LifecycleOwner) -> Unit)? = null

    fun onPause(onPause: (LifecycleOwner) -> Unit) {
        this.onPause = onPause
    }

    override fun onPause(owner: LifecycleOwner) {
        onPause?.invoke(owner)
    }

    private var onStop: ((LifecycleOwner) -> Unit)? = null

    fun onStop(onStop: (LifecycleOwner) -> Unit) {
        this.onStop = onStop
    }

    override fun onStop(owner: LifecycleOwner) {
        onStop?.invoke(owner)
    }

    private var onDestroy: ((LifecycleOwner) -> Unit)? = null

    fun onDestroy(onDestroy: (LifecycleOwner) -> Unit) {
        this.onDestroy = onDestroy
    }

    override fun onDestroy(owner: LifecycleOwner) {
        onDestroy?.invoke(owner)
    }
}

/**
 * 记录一个生命周期观察者
 * - 在生命周期对象变化时移除上一个观察者并添加新的观察者
 */
@Composable
fun rememberLifecycleObserver(block: SimpleLifecycleObserver.() -> Unit = {}): SimpleLifecycleObserver {
    val owner = LocalLifecycleOwner.current
    val observer = SimpleLifecycleObserver().apply(block)
    DisposableEffect(owner) {
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
        }
    }
    return remember(owner) {
        observer
    }
}
