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

package cn.wj.android.cashbook.core.network.util

import androidx.annotation.WorkerThread
import cn.wj.android.cashbook.core.model.model.BackupModel
import java.io.File
import java.io.InputStream

/**
 * 使用 OkHttp 实现的 WebDAV 操作
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/24
 */
interface WebDAVHandler {

    fun setCredentials(
        account: String,
        password: String,
    )

    @WorkerThread
    fun exists(url: String): Boolean

    @WorkerThread
    fun createDirectory(url: String): Boolean

    @WorkerThread
    fun put(url: String, dataStream: InputStream, contentType: String): Boolean

    @WorkerThread
    fun put(url: String, file: File, contentType: String): Boolean

    @WorkerThread
    suspend fun list(
        url: String,
        propsList: List<String> = emptyList(),
    ): List<BackupModel>

    /**
     * 流式下载 [url] 到 [dest] 文件；成功返回 true。
     * 失败契约：非 2xx / 网络异常 / 累计写入超过 [maxBytes] 上限 → 返回 false 且 dest 无残留（实现负责清）。
     * [maxBytes] 由调用方按语义传入（如 core:data 的 MAX_RECOVERY_TOTAL_BYTES），core:network 不定义业务上限。
     */
    @WorkerThread
    suspend fun get(url: String, dest: File, maxBytes: Long): Boolean
}
