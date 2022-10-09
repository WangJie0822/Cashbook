package cn.wj.android.cashbook.data.entity

import kotlinx.serialization.Serializable

/**
 * 备份信息
 *
 * @param channel 备份渠道
 * @param createTime 备份创建时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/9
 */
@Serializable
data class BackupVersionEntity(
    val channel: String,
    val createTime: String
)