package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "db_tag_with_record")
class TagWithRecordTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    @ColumnInfo(name = "record_id") val recordId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)