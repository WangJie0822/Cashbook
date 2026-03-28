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

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.helper.DailyAccountExporter
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ExportRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val exporter: DailyAccountExporter,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {
    suspend operator fun invoke(
        booksId: Long,
        startDate: Long,
        endDate: Long,
        outputFile: File,
    ): Int = withContext(coroutineContext) {
        val records = recordRepository.queryExportRecords(booksId, startDate, endDate)
        exporter.export(records, outputFile)
    }
}
