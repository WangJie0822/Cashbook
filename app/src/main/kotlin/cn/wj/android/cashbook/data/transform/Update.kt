package cn.wj.android.cashbook.data.transform

import cn.wj.android.cashbook.data.entity.GitReleaseEntity
import cn.wj.android.cashbook.data.entity.UpdateInfoEntity

fun GitReleaseEntity.toUpdateInfoEntity(): UpdateInfoEntity {
    val asset = assets?.firstOrNull {
        it.name?.endsWith(".apk") ?: false
    }
    return UpdateInfoEntity(
        versionName = name.orEmpty(),
        versionInfo = body.orEmpty(),
        apkName = asset?.name.orEmpty(),
        downloadUrl = asset?.browser_download_url.orEmpty()
    )
}