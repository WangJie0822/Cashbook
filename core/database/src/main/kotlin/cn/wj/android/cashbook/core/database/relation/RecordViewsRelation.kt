package cn.wj.android.cashbook.core.database.relation

data class RecordViewsRelation(
    val id: Long,
    val typeCategory: String,
    val typeName: String,
    val typeIconResName: String,
    val assetName: String?,
    val assetClassification: String?,
    val relatedAssetName: String?,
    val relatedAssetClassification: String?,
    val amount: Double,
    val charges: Double,
    val concessions: Double,
    val remark: String,
    val reimbursable: Int,
    val recordTime: Long,
)