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
        propsList: List<String> = emptyList()
    ): List<BackupModel>

    @WorkerThread
    suspend fun get(url: String): InputStream?
}