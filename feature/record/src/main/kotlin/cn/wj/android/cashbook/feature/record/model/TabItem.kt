package cn.wj.android.cashbook.feature.record.model

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/**
 * 编辑记录界面标题标签数据
 *
 * @param title 标签文本
 * @param type 标签类型 [RecordTypeCategoryEnum]
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/17
 */
internal data class TabItem(
    val title: String,
    val type: RecordTypeCategoryEnum,
)