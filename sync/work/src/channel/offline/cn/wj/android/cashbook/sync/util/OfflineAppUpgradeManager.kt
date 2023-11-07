package cn.wj.android.cashbook.sync.util

import cn.wj.android.cashbook.core.data.uitl.AppUpgradeManager
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class OfflineAppUpgradeManager @Inject constructor() : AppUpgradeManager {

    override val isDownloading: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun startDownload(info: UpgradeInfoEntity) {
        // empty block
    }

    override suspend fun updateDownloadProgress(progress: Int) {
        // empty block
    }

    override suspend fun downloadComplete(apkFile: File) {
        // empty block
    }

    override suspend fun downloadFailed() {
        // empty block
    }

    override suspend fun downloadStopped() {
        // empty block
    }

    override suspend fun cancelDownload() {
        // empty block
    }

    override suspend fun retry() {
        // empty block
    }
}