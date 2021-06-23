package cn.wj.android.cashbook.manager

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.data.constants.ACTION_APK_NAME
import cn.wj.android.cashbook.data.constants.ACTION_DOWNLOAD_URL
import cn.wj.android.cashbook.data.entity.UpdateInfoEntity
import cn.wj.android.cashbook.work.ApkDownloadWorker

/**
 * 更新管理类
 *
 * - 创建时间：2021/6/20
 *
 * @author 王杰
 */
object UpdateManager {

    fun checkFromInfo(info: UpdateInfoEntity, need: () -> Unit, noNeed: () -> Unit) {
        logger().d("checkFromInfo info: $info")
        if (BuildConfig.DEBUG) {
            // Debug 环境，永远需要更新
            need.invoke()
            return
        }
        if (!needUpdate(info.versionName)) {
            // 已是最新版本
            noNeed.invoke()
            return
        }
        // 不是最新版本
        if (info.downloadUrl.isBlank()) {
            // 没有下载资源
            noNeed.invoke()
            return
        }
        need.invoke()
    }

    /** 根据网络返回的版本信息判断是否需要更新 */
    private fun needUpdate(versionName: String?): Boolean {
        if (versionName.isNullOrBlank()) {
            return false
        }
        val localSplits = BuildConfig.VERSION_NAME.split("_")
        val splits = versionName.split("_")
        val localVersions = localSplits.first().replace("v", "").split(".")
        val versions = splits.first().replace("v", "").split(".")
        if (localSplits.first() == splits.first()) {
            return splits[1].toInt() > localSplits[1].toInt()
        }
        for (i in localVersions.indices) {
            if (versions[i] > localVersions[i]) {
                return true
            }
        }
        return false
    }

    fun startDownload(info: UpdateInfoEntity) {
        val manager = WorkManager.getInstance(AppManager.getContext())
        manager.enqueue(
            OneTimeWorkRequestBuilder<ApkDownloadWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(ACTION_APK_NAME, info.apkName)
                        .putString(ACTION_DOWNLOAD_URL, info.downloadUrl)
                        .build()
                )
                .build()
        )
    }
}