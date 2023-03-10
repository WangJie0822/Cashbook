package cn.wj.android.cashbook.core.model.entity

/**
 * 账本数据表格
 *
 * @param id 账本 id 主键自增长
 * @param name 账本名
 * @param description 描述
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
data class BooksEntity(
    val id: Long,
    val name: String,
    val description: String,
    val modifyTime: Long
)