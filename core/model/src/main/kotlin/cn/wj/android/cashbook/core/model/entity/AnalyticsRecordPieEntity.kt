package cn.wj.android.cashbook.core.model.entity

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

data class AnalyticsRecordPieEntity(
    val typeId: Long,
    val typeName: String,
    val typeIconResName: String,
    val typeCategory: RecordTypeCategoryEnum,
    val totalAmount: String,
    val percent: Float,
)