/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 图片关联关系表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/2/24
 */
@Entity(tableName = TABLE_IMAGE_RELATED)
data class ImageWithRelatedTable(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TABLE_IMAGE_ID)
    val id: Long?,
    @ColumnInfo(name = TABLE_IMAGE_RELATED_RECORD_ID) val recordId: Long,
    @ColumnInfo(name = TABLE_IMAGE_PATH) val path: String,
    @ColumnInfo(name = TABLE_IMAGE_BYTES) val bytes: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageWithRelatedTable

        if (id != other.id) return false
        if (recordId != other.recordId) return false
        if (path != other.path) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + recordId.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
