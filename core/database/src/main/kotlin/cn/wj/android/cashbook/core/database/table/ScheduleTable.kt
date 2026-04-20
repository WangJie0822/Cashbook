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
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 周期记账规则数据表
 *
 * @param id 主键自增长
 * @param booksId 所属账本主键
 * @param typeId 类型主键
 * @param assetId 资产主键
 * @param amount 金额（单位：分）
 * @param charge 手续费（单位：分）
 * @param concessions 优惠（单位：分）
 * @param remark 备注
 * @param typeCategory 类型分类（支出/收入）
 * @param frequency 周期频率
 * @param startDate 开始日期（时间戳）
 * @param endDate 结束日期（时间戳，可选）
 * @param recordTime 记账时间（一天中的具体时刻，时间戳）
 * @param lastExecutedDate 上次执行日期（时间戳）
 * @param enabled 是否启用
 * @param reimbursable 是否可报销
 * @param tagIds 标签 id 列表，逗号分隔
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/4/20
 */
@Entity(
    tableName = TABLE_SCHEDULE,
    indices = [
        Index("books_id"),
        Index("enabled"),
    ],
)
data class ScheduleTable(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TABLE_SCHEDULE_ID)
    val id: Long?,
    @ColumnInfo(name = TABLE_SCHEDULE_BOOKS_ID) val booksId: Long,
    @ColumnInfo(name = TABLE_SCHEDULE_TYPE_ID) val typeId: Long,
    @ColumnInfo(name = TABLE_SCHEDULE_ASSET_ID) val assetId: Long,
    @ColumnInfo(name = TABLE_SCHEDULE_AMOUNT) val amount: Long,
    @ColumnInfo(name = TABLE_SCHEDULE_CHARGE) val charge: Long,
    @ColumnInfo(name = TABLE_SCHEDULE_CONCESSIONS) val concessions: Long,
    @ColumnInfo(name = TABLE_SCHEDULE_REMARK) val remark: String,
    @ColumnInfo(name = TABLE_SCHEDULE_TYPE_CATEGORY) val typeCategory: Int,
    @ColumnInfo(name = TABLE_SCHEDULE_FREQUENCY) val frequency: Int,
    @ColumnInfo(name = TABLE_SCHEDULE_START_DATE) val startDate: Long,
    @ColumnInfo(name = TABLE_SCHEDULE_END_DATE) val endDate: Long?,
    @ColumnInfo(name = TABLE_SCHEDULE_RECORD_TIME) val recordTime: Long,
    @ColumnInfo(name = TABLE_SCHEDULE_LAST_EXECUTED_DATE) val lastExecutedDate: Long?,
    @ColumnInfo(name = TABLE_SCHEDULE_ENABLED) val enabled: Int,
    @ColumnInfo(name = TABLE_SCHEDULE_REIMBURSABLE) val reimbursable: Int,
    @ColumnInfo(name = TABLE_SCHEDULE_TAG_IDS) val tagIds: String,
)
