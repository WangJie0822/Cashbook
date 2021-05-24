package cn.wj.android.cashbook.data.entity

/**
 * 账本数据实体类
 *
 * @param id 账本 id 主键自增长
 * @param name 账本名
 * @param imageUrl 账本封面地址
 * @param amount 账本金额
 * @param sort 排序字段
 * @param selected 是否默认选中
 * @param createTime 创建时间
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
data class BooksEntity(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val amount: String,
    val sort: Int,
    val selected: Boolean,
    val createTime: String,
    val modifyTime: String
)