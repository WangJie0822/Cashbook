package cn.wj.android.cashbook.manager

import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.data.entity.GiteeReleaseEntity

/**
 * 更新管理类
 *
 * - 创建时间：2021/6/20
 *
 * @author 王杰
 */
object UpdateManager {

    fun checkFromInfo(info: GiteeReleaseEntity, need: () -> Unit, noNeed: () -> Unit) {
        if (!needUpdate(info.name)) {
            // 已是最新版本
            noNeed.invoke()
            return
        }
        // 不是最新版本，获取升级地址
        val asset = info.assets?.firstOrNull {
            it.name.orEmpty().endsWith(".apk")
        }
        if (null == asset || asset.browser_download_url.isNullOrBlank()) {
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
}