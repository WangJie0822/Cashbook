package cn.wj.android.cashbook.core.model.model

/**
 * 可选数据封装
 *
 * @param data 数据源
 * @param selected 是否选中
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/9/13
 */
data class Selectable<T>(
    val data: T,
    val selected: Boolean,
)