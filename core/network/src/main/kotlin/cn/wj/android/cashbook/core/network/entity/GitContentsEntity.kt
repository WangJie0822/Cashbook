package cn.wj.android.cashbook.core.network.entity

import kotlinx.serialization.Serializable

/**
 * 远程仓库文件数据实体类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/12
 */
@Serializable
data class GitContentsEntity(
    val type: String,
    val encoding: String,
    val name: String,
    val content: String,
)