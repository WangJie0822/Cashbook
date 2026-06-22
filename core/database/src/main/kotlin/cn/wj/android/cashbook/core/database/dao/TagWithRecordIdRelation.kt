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

package cn.wj.android.cashbook.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_RELATED_RECORD_ID
import cn.wj.android.cashbook.core.database.table.TagTable

/**
 * 批量按 recordId 查询标签的 JOIN 结果：每行 = 一条 (record_id, tag) 关联。
 * - [recordId] 来自 db_tag_with_record.record_id
 * - [tag] 为 @Embedded 的 db_tag 完整行（经 INNER JOIN 取得）
 *
 * 用于 [TagDao.queryByRecordIds] 批量查询，消除逐条 [TagDao.queryByRecordId] 的 1-per-record 调用。
 */
data class TagWithRecordIdRelation(
    @ColumnInfo(name = TABLE_TAG_RELATED_RECORD_ID) val recordId: Long,
    @Embedded val tag: TagTable,
)
