package cn.wj.android.cashbook.feature.types.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.wj.android.cashbook.core.model.model.RecordTypeModel

data class ExpandableRecordTypeModel(
    val data: RecordTypeModel,
    val reimburseType: Boolean,
    val refundType: Boolean,
    val list: List<ExpandableRecordTypeModel>,
) {
    var expanded by mutableStateOf(false)
}