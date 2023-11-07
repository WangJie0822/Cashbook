package cn.wj.android.cashbook.core.network.util

import cn.wj.android.cashbook.core.model.model.BackupModel
import java.io.File
import java.io.InputStream
import javax.inject.Inject

/**
 * 离线 WebDAV 操作
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/24
 */
class OfflineWebDAVHandler @Inject constructor() : WebDAVHandler {

    override fun setCredentials(account: String, password: String) {
        // empty block
    }

    override fun exists(url: String): Boolean = false

    override fun createDirectory(url: String): Boolean = false

    override fun put(url: String, dataStream: InputStream, contentType: String): Boolean = false

    override fun put(url: String, file: File, contentType: String): Boolean = false

    override suspend fun list(url: String, propsList: List<String>): List<BackupModel> = emptyList()

    override suspend fun get(url: String): InputStream? = null

}