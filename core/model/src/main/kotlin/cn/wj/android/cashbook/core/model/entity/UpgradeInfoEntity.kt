package cn.wj.android.cashbook.core.model.entity

/**
 * 升级信息数据实体类
 *
 * @param versionName 版本名
 * @param versionInfo 版本更新信息
 * @param apkName 安装包名称
 * @param downloadUrl 下载地址
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/21
 */
data class UpgradeInfoEntity(
    val versionName: String,
    val versionInfo: String,
    val apkName: String,
    val downloadUrl: String
)