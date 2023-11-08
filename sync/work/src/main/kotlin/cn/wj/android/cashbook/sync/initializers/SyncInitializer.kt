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

package cn.wj.android.cashbook.sync.initializers

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cn.wj.android.cashbook.sync.workers.InitWorker

/**
 * 数据同步对外接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/12
 */
object Sync {

    /** 初始化 */
    fun initialize(context: Context) {
        WorkManager.getInstance(context).apply {
            // 执行初始化任务
            enqueueUniqueWork(
                InitWorkName,
                ExistingWorkPolicy.KEEP,
                InitWorker.startUpInitWork(),
            )
        }
    }
}

internal const val InitWorkName = "InitWorkName"
internal const val SyncWorkName = "SyncWorkName"
internal const val AutoBackupWorkName = "AutoBackupWorkName"
internal const val ApkDownloadWorkName = "ApkDownloadWorkName"
