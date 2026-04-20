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

import cn.wj.android.cashbook.core.testing.data.createScheduleModel
import cn.wj.android.cashbook.core.testing.repository.FakeScheduleRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.DeleteScheduleUseCase
import cn.wj.android.cashbook.domain.usecase.GetScheduleListUseCase
import cn.wj.android.cashbook.domain.usecase.ToggleScheduleEnabledUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MySchedulesViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var scheduleRepository: FakeScheduleRepository
    private lateinit var viewModel: MySchedulesViewModel

    @Before
    fun setup() {
        scheduleRepository = FakeScheduleRepository()
        viewModel = MySchedulesViewModel(
            getScheduleListUseCase = GetScheduleListUseCase(scheduleRepository),
            toggleScheduleEnabledUseCase = ToggleScheduleEnabledUseCase(scheduleRepository),
            deleteScheduleUseCase = DeleteScheduleUseCase(scheduleRepository),
        )
    }

    @Test
    fun when_initial_state_then_schedule_list_is_empty() = runTest {
        assertThat(viewModel.scheduleListData.first()).isEmpty()
    }

    @Test
    fun when_schedules_added_then_list_reflects_data() = runTest {
        val schedule1 = createScheduleModel(id = 1L, remark = "房租")
        val schedule2 = createScheduleModel(id = 2L, remark = "工资")
        scheduleRepository.addSchedule(schedule1)
        scheduleRepository.addSchedule(schedule2)

        val result = viewModel.scheduleListData.first()

        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun when_toggle_enabled_then_repository_updated() = runTest {
        val schedule = createScheduleModel(id = 1L, enabled = true)
        scheduleRepository.addSchedule(schedule)

        viewModel.toggleEnabled(schedule)

        assertThat(scheduleRepository.lastUpdatedEnabledId).isEqualTo(1L)
        assertThat(scheduleRepository.lastUpdatedEnabledValue).isFalse()
    }

    @Test
    fun when_show_delete_dialog_then_dialog_state_shown() {
        val schedule = createScheduleModel(id = 1L)

        viewModel.showDeleteDialog(schedule)

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val shown = viewModel.dialogState as DialogState.Shown<*>
        assertThat(shown.data).isEqualTo(schedule)
    }

    @Test
    fun when_dismiss_dialog_then_dialog_state_dismiss() {
        val schedule = createScheduleModel(id = 1L)
        viewModel.showDeleteDialog(schedule)

        viewModel.dismissDialog()

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_confirm_delete_then_schedule_removed() = runTest {
        val schedule = createScheduleModel(id = 1L)
        scheduleRepository.addSchedule(schedule)
        viewModel.showDeleteDialog(schedule)

        viewModel.confirmDelete(1L)

        assertThat(scheduleRepository.lastDeletedScheduleId).isEqualTo(1L)
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }
}
