package cn.wj.android.cashbook.core.model.model

/**
 * 可展开数据封装
 *
 * @param data 数据源
 * @param list 折叠数据列表
 * @param expanded 是否展开
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/9/13
 */
data class Expandable<T>(
    val data: T,
    val list: List<T>,
    val expanded: Boolean,
)