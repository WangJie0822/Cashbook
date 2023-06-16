package cn.wj.android.cashbook.core.model.model

/**
 * 应用配置数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
data class AppDataModel(
    val currentBookId: Long,
    val defaultTypeId: Long,
    val lastAssetId: Long,
    val refundTypeId: Long,
    val reimburseTypeId: Long,
    val useGithub: Boolean,
    val autoCheckUpdate: Boolean,
    val ignoreUpdateVersion: String?,
    val mobileNetworkDownloadEnable: Boolean,
)
