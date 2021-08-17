package cn.wj.android.cashbook.data.entity

/**
 * 备份信息数据实体类
 *
 * @param name 备份名称
 * @param path 备份路径
 * @param webDAV 是否是 webDAV 备份
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/17
 */
data class BackupEntity(
    val name: String,
    val path: String,
    val webDAV: Boolean
)