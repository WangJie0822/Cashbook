package cn.wj.android.cashbook.core.model.entity

data class AnalyticsRecordBarEntity(
    val date: String,
    val expenditure: String,
    val income: String,
    val balance: String,
    val year: Boolean,
)