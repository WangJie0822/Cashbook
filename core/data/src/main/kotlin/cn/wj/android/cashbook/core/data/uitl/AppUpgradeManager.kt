package cn.wj.android.cashbook.core.data.uitl

import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * 应用升级管理
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/20
 */
interface AppUpgradeManager {

    val isDownloading: Flow<Boolean>

    suspend fun startDownload(info: UpgradeInfoEntity)

    suspend fun updateDownloadProgress(progress: Int)

    suspend fun downloadComplete(apkFile: File)

    suspend fun downloadFailed()

    suspend fun downloadStopped()

    suspend fun cancelDownload()

    suspend fun retry()
}