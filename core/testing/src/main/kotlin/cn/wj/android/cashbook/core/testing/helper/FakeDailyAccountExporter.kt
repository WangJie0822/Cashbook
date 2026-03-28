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

package cn.wj.android.cashbook.core.testing.helper

import cn.wj.android.cashbook.core.data.helper.DailyAccountExporter
import cn.wj.android.cashbook.core.model.model.ExportRecordModel
import java.io.File

/**
 * 测试用 DailyAccountExporter 替身
 *
 * 记录传入的参数，返回可配置的导出数量，不实际写入文件。
 */
class FakeDailyAccountExporter : DailyAccountExporter() {

    /** 最近一次 export 调用传入的记录列表 */
    var lastExportedRecords: List<ExportRecordModel>? = null
        private set

    /** 最近一次 export 调用传入的输出文件 */
    var lastOutputFile: File? = null
        private set

    override fun export(records: List<ExportRecordModel>, outputFile: File): Int {
        lastExportedRecords = records
        lastOutputFile = outputFile
        return records.size
    }
}
