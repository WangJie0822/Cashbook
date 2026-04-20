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

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.model.enums.ScheduleFrequencyEnum
import cn.wj.android.cashbook.core.testing.data.createScheduleModel
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeScheduleRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class GenerateScheduleRecordsUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var scheduleRepository: FakeScheduleRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var saveRecordUseCase: SaveRecordUseCase
    private lateinit var useCase: GenerateScheduleRecordsUseCase

    @Before
    fun setup() {
        scheduleRepository = FakeScheduleRepository()
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        saveRecordUseCase = SaveRecordUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
        useCase = GenerateScheduleRecordsUseCase(
            scheduleRepository = scheduleRepository,
            saveRecordUseCase = saveRecordUseCase,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_disabled_schedule_then_no_records_generated() = runTest {
        val startDate = createCalendar(2024, Calendar.JANUARY, 1).timeInMillis
        val currentTime = createCalendar(2024, Calendar.JANUARY, 15).timeInMillis
        val schedule = createScheduleModel(
            id = 1L,
            enabled = false,
            startDate = startDate,
            frequency = ScheduleFrequencyEnum.DAILY,
        )
        scheduleRepository.addSchedule(schedule)

        val count = useCase(currentTimeMs = currentTime)

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun when_daily_schedule_and_three_days_due_then_three_records_generated() = runTest {
        val startDate = createCalendar(2024, Calendar.JANUARY, 1).timeInMillis
        val currentTime = createCalendar(2024, Calendar.JANUARY, 4).timeInMillis
        val schedule = createScheduleModel(
            id = 1L,
            enabled = true,
            startDate = startDate,
            frequency = ScheduleFrequencyEnum.DAILY,
            amount = 500L,
        )
        scheduleRepository.addSchedule(schedule)

        val count = useCase(currentTimeMs = currentTime)

        assertThat(count).isEqualTo(3)
        assertThat(recordRepository.lastUpdatedRecord?.amount).isEqualTo(500L)
    }

    @Test
    fun when_weekly_schedule_and_three_weeks_due_then_three_records_generated() = runTest {
        val startDate = createCalendar(2024, Calendar.JANUARY, 1).timeInMillis
        val currentTime = createCalendar(2024, Calendar.JANUARY, 22).timeInMillis
        val schedule = createScheduleModel(
            id = 1L,
            enabled = true,
            startDate = startDate,
            frequency = ScheduleFrequencyEnum.WEEKLY,
        )
        scheduleRepository.addSchedule(schedule)

        val count = useCase(currentTimeMs = currentTime)

        assertThat(count).isEqualTo(3)
    }

    @Test
    fun when_monthly_schedule_and_two_months_due_then_two_records_generated() = runTest {
        val startDate = createCalendar(2024, Calendar.JANUARY, 1).timeInMillis
        val currentTime = createCalendar(2024, Calendar.MARCH, 15).timeInMillis
        val schedule = createScheduleModel(
            id = 1L,
            enabled = true,
            startDate = startDate,
            frequency = ScheduleFrequencyEnum.MONTHLY,
        )
        scheduleRepository.addSchedule(schedule)

        val count = useCase(currentTimeMs = currentTime)

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun when_yearly_schedule_and_two_years_due_then_two_records_generated() = runTest {
        val startDate = createCalendar(2024, Calendar.JANUARY, 1).timeInMillis
        val currentTime = createCalendar(2026, Calendar.MARCH, 1).timeInMillis
        val schedule = createScheduleModel(
            id = 1L,
            enabled = true,
            startDate = startDate,
            frequency = ScheduleFrequencyEnum.YEARLY,
        )
        scheduleRepository.addSchedule(schedule)

        val count = useCase(currentTimeMs = currentTime)

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun when_last_executed_date_set_then_calculate_from_last_executed() = runTest {
        val startDate = createCalendar(2024, Calendar.JANUARY, 1).timeInMillis
        val lastExecuted = createCalendar(2024, Calendar.JANUARY, 10).timeInMillis
        val currentTime = createCalendar(2024, Calendar.JANUARY, 15).timeInMillis
        val schedule = createScheduleModel(
            id = 1L,
            enabled = true,
            startDate = startDate,
            lastExecutedDate = lastExecuted,
            frequency = ScheduleFrequencyEnum.DAILY,
        )
        scheduleRepository.addSchedule(schedule)

        val count = useCase(currentTimeMs = currentTime)

        assertThat(count).isEqualTo(5)
    }

    @Test
    fun when_end_date_reached_then_stop_generating() = runTest {
        val startDate = createCalendar(2024, Calendar.JANUARY, 1).timeInMillis
        val endDate = createCalendar(2024, Calendar.JANUARY, 5).timeInMillis
        val currentTime = createCalendar(2024, Calendar.JANUARY, 20).timeInMillis
        val schedule = createScheduleModel(
            id = 1L,
            enabled = true,
            startDate = startDate,
            endDate = endDate,
            frequency = ScheduleFrequencyEnum.DAILY,
        )
        scheduleRepository.addSchedule(schedule)

        val count = useCase(currentTimeMs = currentTime)

        assertThat(count).isEqualTo(4)
    }

    @Test
    fun when_records_generated_then_last_executed_date_updated() = runTest {
        val startDate = createCalendar(2024, Calendar.JANUARY, 1).timeInMillis
        val currentTime = createCalendar(2024, Calendar.JANUARY, 4).timeInMillis
        val schedule = createScheduleModel(
            id = 1L,
            enabled = true,
            startDate = startDate,
            frequency = ScheduleFrequencyEnum.DAILY,
        )
        scheduleRepository.addSchedule(schedule)

        useCase(currentTimeMs = currentTime)

        assertThat(scheduleRepository.lastUpdatedLastExecutedId).isEqualTo(1L)
    }

    @Test
    fun when_no_due_dates_then_zero_generated() = runTest {
        val startDate = createCalendar(2024, Calendar.JANUARY, 15).timeInMillis
        val currentTime = createCalendar(2024, Calendar.JANUARY, 10).timeInMillis
        val schedule = createScheduleModel(
            id = 1L,
            enabled = true,
            startDate = startDate,
            frequency = ScheduleFrequencyEnum.DAILY,
        )
        scheduleRepository.addSchedule(schedule)

        val count = useCase(currentTimeMs = currentTime)

        assertThat(count).isEqualTo(0)
    }

    private fun createCalendar(year: Int, month: Int, day: Int): Calendar {
        return Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
