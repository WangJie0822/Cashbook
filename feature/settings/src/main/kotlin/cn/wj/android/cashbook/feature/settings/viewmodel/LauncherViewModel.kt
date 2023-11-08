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
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 首页 ViewModel
 *
 * @param settingRepository 设置相关数据仓库
 * @param booksRepository 账本相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/26
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
    booksRepository: BooksRepository,
) : ViewModel() {

    /** 是否显示抽屉菜单 */
    var shouldDisplayDrawerSheet by mutableStateOf(false)
        private set

    /** 界面 UI 状态 */
    val uiState = booksRepository.currentBook.mapLatest {
        LauncherUiState.Success(
            currentBookName = it.name,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = LauncherUiState.Loading,
        )

    /** 显示抽屉菜单 */
    fun displayDrawerSheet() {
        shouldDisplayDrawerSheet = true
    }

    /** 隐藏抽屉菜单 */
    fun dismissDrawerSheet() {
        shouldDisplayDrawerSheet = false
    }
}

/**
 * 界面 UI 状态
 */
sealed interface LauncherUiState {
    /** 加载中 */
    data object Loading : LauncherUiState

    /**
     * 加载完成
     *
     * @param currentBookName 当前账本名称
     */
    data class Success(
        val currentBookName: String,
    ) : LauncherUiState
}
