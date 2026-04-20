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

package cn.wj.android.cashbook.feature.schedule.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.model.ScheduleModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.DeleteScheduleUseCase
import cn.wj.android.cashbook.domain.usecase.GetScheduleListUseCase
import cn.wj.android.cashbook.domain.usecase.ToggleScheduleEnabledUseCase
import cn.wj.android.cashbook.feature.schedule.model.ScheduleViewsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 周期记账列表 ViewModel
 *
 * @param getScheduleListUseCase 获取周期规则列表用例
 * @param toggleScheduleEnabledUseCase 切换周期规则启用状态用例
 * @param deleteScheduleUseCase 删除周期规则用例
 * @param typeRepository 类型数据仓库
 * @param assetRepository 资产数据仓库
 * @param tagRepository 标签数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/4/20
 */
@HiltViewModel
class MySchedulesViewModel @Inject constructor(
    getScheduleListUseCase: GetScheduleListUseCase,
    private val toggleScheduleEnabledUseCase: ToggleScheduleEnabledUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase,
    private val typeRepository: TypeRepository,
    private val assetRepository: AssetRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 当前查看详情的周期规则 */
    var viewSchedule by mutableStateOf<ScheduleViewsEntity?>(null)
        private set

    /** 周期规则列表 */
    val scheduleListData = getScheduleListUseCase()
        .mapLatest { list ->
            list.map { schedule ->
                val type = typeRepository.getRecordTypeById(schedule.typeId)
                val asset = if (schedule.assetId > 0) {
                    assetRepository.getAssetById(schedule.assetId)
                } else {
                    null
                }
                val tagNames = schedule.tagIdList.mapNotNull {
                    tagRepository.getTagById(it)?.name
                }
                ScheduleViewsEntity(
                    schedule = schedule,
                    typeName = type?.name.orEmpty(),
                    typeIconResName = type?.iconName.orEmpty(),
                    assetName = asset?.name,
                    tagNameList = tagNames,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** 切换周期规则 [schedule] 的启用状态 */
    fun toggleEnabled(schedule: ScheduleModel) {
        viewModelScope.launch {
            toggleScheduleEnabledUseCase(schedule.id, !schedule.enabled)
        }
    }

    /** 显示周期规则详情 BottomSheet */
    fun showScheduleDetails(schedule: ScheduleViewsEntity) {
        viewSchedule = schedule
    }

    /** 隐藏周期规则详情 BottomSheet */
    fun dismissScheduleDetails() {
        viewSchedule = null
    }

    /** 显示删除周期规则确认弹窗 */
    fun showDeleteDialog(schedule: ScheduleModel) {
        dialogState = DialogState.Shown(schedule)
    }

    /** 隐藏弹窗 */
    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    /** 确认删除 */
    fun confirmDelete(scheduleId: Long, deleteRecords: Boolean) {
        viewModelScope.launch {
            deleteScheduleUseCase(scheduleId, deleteRecords)
            dismissDialog()
            dismissScheduleDetails()
        }
    }
}
