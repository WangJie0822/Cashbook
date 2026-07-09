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

package cn.wj.android.cashbook.core.database.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable

/**
 * 首页记录列表分页的 @Relation 视图：一次分页把 type/asset/tags/images/双向 relatedRecord 批量 IN 物化，消 N+1。
 *
 * Room 为使用此 POJO 的 @Transaction 分页方法生成 LimitOffsetPagingSource（保位、不回顶）+ 自动观察全部
 * @Relation 表（自动 invalidate、不漏表）——避免手写 getRefreshKey/InvalidationTracker。
 *
 * - type/asset 一对一：Room @Relation 恒为集合，映射时取 firstOrNull；
 * - 平账 typeId 负（-1101/-1102）、db_type 无匹配 → [types] 空 List（@Relation 天然 LEFT，主记录不丢）；
 * - relatedRecord 双向：[relatedAsRecordId]（收入侧，我 record_id 关联的被吸收支出）、
 *   [relatedAsRelatedId]（支出侧，吸收我的收入），映射时按 typeCategory 选向。
 */
data class LauncherRecordViewRelation(
    @Embedded val record: RecordTable,

    @Relation(parentColumn = "type_id", entityColumn = "id", entity = TypeTable::class)
    val types: List<TypeTable>,

    @Relation(parentColumn = "asset_id", entityColumn = "id", entity = AssetTable::class)
    val assets: List<AssetTable>,

    @Relation(parentColumn = "into_asset_id", entityColumn = "id", entity = AssetTable::class)
    val intoAssets: List<AssetTable>,

    @Relation(parentColumn = "id", entityColumn = "record_id", entity = ImageWithRelatedTable::class)
    val images: List<ImageWithRelatedTable>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TagWithRecordTable::class,
            parentColumn = "record_id",
            entityColumn = "tag_id",
        ),
        entity = TagTable::class,
    )
    val tags: List<TagTable>,

    // 收入侧：我作为 record_id 关联的 related_record（被吸收支出）
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecordWithRelatedTable::class,
            parentColumn = "record_id",
            entityColumn = "related_record_id",
        ),
        entity = RecordTable::class,
    )
    val relatedAsRecordId: List<RecordTable>,

    // 支出侧：我作为 related_record_id 时吸收我的 record（收入）
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecordWithRelatedTable::class,
            parentColumn = "related_record_id",
            entityColumn = "record_id",
        ),
        entity = RecordTable::class,
    )
    val relatedAsRelatedId: List<RecordTable>,
)
