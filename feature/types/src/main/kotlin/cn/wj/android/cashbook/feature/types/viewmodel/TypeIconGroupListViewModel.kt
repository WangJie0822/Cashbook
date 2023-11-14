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

package cn.wj.android.cashbook.feature.types.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.model.model.Selectable
import cn.wj.android.cashbook.feature.types.model.getTypeIconGroupList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 类型图标列表 ViewModel
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2023/9/18
 */
@HiltViewModel
class TypeIconGroupListViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val _selectedGroupName = MutableStateFlow("")

    private val _groupListData = channelFlow {
        channel.trySend(getTypeIconGroupList(getApplication()))
    }

    val selectableGroupListData =
        combine(_selectedGroupName, _groupListData) { selectedName, list ->
            if (selectedName.isBlank()) {
                var first = true
                list.map {
                    val data = Selectable(it, first)
                    first = false
                    data
                }
            } else {
                list.map {
                    Selectable(it, it.name == selectedName)
                }
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList(),
            )

    val iconListData = selectableGroupListData.mapLatest { list ->
        list.firstOrNull { it.selected }?.data?.icons ?: emptyList()
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    fun selectGroup(name: String) {
        _selectedGroupName.tryEmit(name)
    }
}
