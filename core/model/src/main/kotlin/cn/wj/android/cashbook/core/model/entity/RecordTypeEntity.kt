package cn.wj.android.cashbook.core.model.entity

data class RecordTypeEntity(
    val id: Long,
    val parentId: Long,
    val name: String,
    val iconResId: Int,
    val sort: Int,
    val child: List<RecordTypeEntity>,
    val selected: Boolean,
)

