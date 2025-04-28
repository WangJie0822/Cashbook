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

package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.model.enums.LogcatState
import cn.wj.android.cashbook.core.ui.DialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 关于我们
 *
 * @param settingRepository 设置相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
@HiltViewModel
class AboutUsViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
) : ViewModel() {

    /** 日志弹窗 */
    var logcatDialogState: DialogState by mutableStateOf(DialogState.Dismiss)

    /** 界面 UI 状态 */
    val uiState = settingRepository.appSettingsModel.mapLatest { model ->
        AboutUsUiState.Success(
            useGitee = !model.useGithub,
            canary = model.canary,
            autoCheckUpdate = model.autoCheckUpdate,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = AboutUsUiState.Loading,
        )

    /** 点击次数，连续点击10词开启日志输出 */
    private var counts = 0

    private var countDownJob: Job? = null

    /** 计算点击次数 */
    fun countNameClicks() {
        counts++
        if (counts >= 10) {
            // 连续点击 10 次，显示日志选择弹窗
            viewModelScope.launch {
                logcatDialogState = DialogState.Shown(
                    when {
                        settingRepository.appSettingsModel.first().logcatInRelease -> LogcatState.ALWAYS
                        ApplicationInfo.logcatEnable -> LogcatState.ONCE
                        else -> LogcatState.NONE
                    },
                )
            }
            // 取消倒计时
            countDownJob?.cancel()
            countDownJob = null
        } else {
            // 5s 未操作重置计数器
            countDownJob?.cancel()
            countDownJob = viewModelScope.launch {
                delay(5 * 1000L)
                counts = 0
            }
        }
    }

    /** 更新 logcat 状态 */
    fun updateLogcatState(state: LogcatState) {
        viewModelScope.launch {
            when (state) {
                LogcatState.ALWAYS -> {
                    ApplicationInfo.logcatEnable = false
                    settingRepository.updateLogcatInRelease(true)
                }

                LogcatState.ONCE -> {
                    ApplicationInfo.logcatEnable = true
                    settingRepository.updateLogcatInRelease(false)
                }

                LogcatState.NONE -> {
                    ApplicationInfo.logcatEnable = false
                    settingRepository.updateLogcatInRelease(false)
                }
            }
            dismissDialog()
        }
    }

    /** 更新是否使用 Gitee 源 */
    fun updateUseGitee(useGitee: Boolean) {
        viewModelScope.launch {
            settingRepository.updateUseGithub(!useGitee)
        }
    }

    /** 更新是否支持实验版本 */
    fun updateCanary(canary: Boolean) {
        viewModelScope.launch {
            settingRepository.updateCanary(canary)
        }
    }

    /** 更新自动检查更新开关 */
    fun updateAutoCheckUpdate(autoCheckUpdate: Boolean) {
        viewModelScope.launch {
            settingRepository.updateAutoCheckUpdate(autoCheckUpdate)
        }
    }

    fun dismissDialog() {
        logcatDialogState = DialogState.Dismiss
    }
}

/**
 * 界面 UI 状态
 *
 * @param useGitee 是否使用 gitee 源
 * @param autoCheckUpdate 是否自动检查更新
 */
sealed class AboutUsUiState(
    open val useGitee: Boolean = false,
    open val canary: Boolean = false,
    open val autoCheckUpdate: Boolean = false,
) {
    /** 加载中 */
    data object Loading : AboutUsUiState()

    /** 加载完成 */
    data class Success(
        override val useGitee: Boolean,
        override val autoCheckUpdate: Boolean,
        override val canary: Boolean,
    ) : AboutUsUiState()
}
