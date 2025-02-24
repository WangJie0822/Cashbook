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
 * 记录标签关联关系表
 *
 * @param id 主键自增长
 * @param recordId 关联记录id
 * @param tagId 关联标签id
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/3/6
 */
@Entity(tableName = TABLE_TAG_RELATED)
data class TagWithRecordTable(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TABLE_TAG_RELATED_ID)
    val id: Long?,
    @ColumnInfo(name = TABLE_TAG_RELATED_RECORD_ID) val recordId: Long,
    @ColumnInfo(name = TABLE_TAG_RELATED_TAG_ID) val tagId: Long,
)
