package cn.wj.android.cashbook.core.model.model

/**
 * 远程仓库数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/12
 */
data class GitDataModel(
    val latestVersionName: String,
    val latestVersionInfo: String,
    val latestApkName: String,
    val latestApkDownloadUrl: String,
    val changelogData: String,
    val privacyPolicyData: String,
)
