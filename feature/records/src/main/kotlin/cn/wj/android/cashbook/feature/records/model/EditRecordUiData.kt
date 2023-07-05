package cn.wj.android.cashbook.feature.records.model

data class EditRecordUiData(
    val amountText: String,
    val chargesText: String,
    val concessionsText: String,
    val remarkText:String,
    val assetText: String,
    val relatedAssetText: String,
    val dateTimeText:String,
    val reimbursable:Boolean,
    val selectedTypeId:Long,
)
