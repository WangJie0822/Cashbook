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

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
import cn.wj.android.cashbook.core.database.relation.RecordFilterSummaryRelation
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import kotlinx.coroutines.flow.Flow

/** 筛选查询只接收绑定参数；关系表变动也必须使分页失效。 */
@Dao
interface RecordFilterDao {
    @Transaction
    @RawQuery(
        observedEntities = [
            RecordTable::class, TypeTable::class, TagTable::class, TagWithRecordTable::class,
            AssetTable::class, ImageWithRelatedTable::class, RecordWithRelatedTable::class,
        ],
    )
    fun pagingRecords(query: SupportSQLiteQuery): PagingSource<Int, LauncherRecordViewRelation>

    @RawQuery(observedEntities = [RecordTable::class, TypeTable::class, TagTable::class, TagWithRecordTable::class])
    fun observeSummary(query: SupportSQLiteQuery): Flow<RecordFilterSummaryRelation>
}
