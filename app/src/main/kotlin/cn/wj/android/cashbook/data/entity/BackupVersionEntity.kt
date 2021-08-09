package cn.wj.android.cashbook.data.entity

import kotlinx.serialization.Serializable

/**
 * 备份信息
 *
 * @param version 备份版本号
 * @param createTime 备份创建时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/9
 */
@Serializable
class BackupVersionEntity(
    val version: Int,
    val createTime: String
)