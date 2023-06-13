package cn.wj.android.cashbook.core.model.model

data class AppDataModel(
    val currentBookId: Long,
    val defaultTypeId: Long,
    val lastAssetId: Long,
    val refundTypeId: Long,
    val reimburseTypeId: Long,
)
