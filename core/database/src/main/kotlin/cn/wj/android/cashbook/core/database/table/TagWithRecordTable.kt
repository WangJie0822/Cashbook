package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = TABLE_TAG_RELATED)
class TagWithRecordTable(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = TABLE_TAG_RELATED_ID) val id: Long?,
    @ColumnInfo(name = TABLE_TAG_RELATED_RECORD_ID) val recordId: Long,
    @ColumnInfo(name = TABLE_TAG_RELATED_TAG_ID) val tagId: Long,
)