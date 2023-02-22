package cn.wj.android.cashbook.core.model.entity

/**
 * 标签数据实体类
 *
 * @param id 主键自增长
 * @param name 标签名称
 * @param booksId 所属账本主键
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
data class TagEntity(
    val id: Long,
    val name: String,
    val booksId: Long,
)