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

import cn.wj.android.cashbook.core.model.model.ExportRecordModel
import cn.wj.android.cashbook.core.testing.helper.FakeDailyAccountExporter
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class ExportRecordUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var exporter: FakeDailyAccountExporter
    private lateinit var useCase: ExportRecordUseCase
    private lateinit var outputFile: File

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        exporter = FakeDailyAccountExporter()
        useCase = ExportRecordUseCase(
            recordRepository = recordRepository,
            exporter = exporter,
            coroutineContext = UnconfinedTestDispatcher(),
        )
        outputFile = File.createTempFile("export_test", ".csv")
    }

    @After
    fun tearDown() {
        outputFile.delete()
    }

    @Test
    fun when_no_records_then_returns_zero() = runTest {
        recordRepository.exportRecordsList = emptyList()

        val result = useCase(booksId = 1L, startDate = 0L, endDate = Long.MAX_VALUE, outputFile = outputFile)

        assertThat(result).isEqualTo(0)
        assertThat(exporter.lastExportedRecords).isEmpty()
    }

    @Test
    fun when_has_records_then_returns_count() = runTest {
        recordRepository.exportRecordsList = listOf(
            ExportRecordModel(
                recordTime = 1000L,
                typeCategory = 0,
                assetName = "现金",
                categoryName = "餐饮",
                subCategoryName = "",
                amount = 3000L,
                remark = "",
            ),
            ExportRecordModel(
                recordTime = 2000L,
                typeCategory = 0,
                assetName = "银行卡",
                categoryName = "交通",
                subCategoryName = "地铁",
                amount = 500L,
                remark = "地铁",
            ),
            ExportRecordModel(
                recordTime = 3000L,
                typeCategory = 1,
                assetName = "支付宝",
                categoryName = "工资",
                subCategoryName = "",
                amount = 1000000L,
                remark = "月薪",
            ),
        )

        val result = useCase(booksId = 1L, startDate = 0L, endDate = Long.MAX_VALUE, outputFile = outputFile)

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun when_repository_returns_records_then_passes_to_exporter() = runTest {
        val expectedRecords = listOf(
            ExportRecordModel(
                recordTime = 1000L,
                typeCategory = 0,
                assetName = "现金",
                categoryName = "餐饮",
                subCategoryName = "",
                amount = 3000L,
                remark = "午餐",
            ),
            ExportRecordModel(
                recordTime = 2000L,
                typeCategory = 1,
                assetName = "工资卡",
                categoryName = "工资",
                subCategoryName = "",
                amount = 500000L,
                remark = "",
            ),
        )
        recordRepository.exportRecordsList = expectedRecords

        useCase(booksId = 2L, startDate = 1000L, endDate = 9000L, outputFile = outputFile)

        assertThat(exporter.lastExportedRecords).isEqualTo(expectedRecords)
        assertThat(exporter.lastOutputFile).isEqualTo(outputFile)
    }
}
