package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "db_record_with_related")
class RecordWithRelatedTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    @ColumnInfo(name = "record_id") val recordId: Long,
    @ColumnInfo(name = "related_record_id") val relatedRecordId: Long,
)