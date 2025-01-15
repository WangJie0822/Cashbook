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
 * 记录数据库表
 *
 * @param id 主键自增长
 * @param typeId 类型 id
 * @param assetId 关联资产 id
 * @param intoAssetId 转账转入资产 id
 * @param booksId 关联账本 id
 * @param amount 金额
 * @param finalAmount 最终金额
 * @param concessions 优惠
 * @param charge 手续费
 * @param remark 备注
 * @param reimbursable 能否报销
 * @param recordTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
@Entity(tableName = TABLE_RECORD)
data class RecordTable(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TABLE_RECORD_ID)
    val id: Long?,
    @ColumnInfo(name = TABLE_RECORD_TYPE_ID) val typeId: Long,
    @ColumnInfo(name = TABLE_RECORD_ASSET_ID) val assetId: Long,
    @ColumnInfo(name = TABLE_RECORD_INTO_ASSET_ID) val intoAssetId: Long,
    @ColumnInfo(name = TABLE_RECORD_BOOKS_ID) val booksId: Long,
    @ColumnInfo(name = TABLE_RECORD_AMOUNT) val amount: Double,
    @ColumnInfo(name = TABLE_RECORD_FINAL_AMOUNT) val finalAmount: Double,
    @ColumnInfo(name = TABLE_RECORD_CONCESSIONS) val concessions: Double,
    @ColumnInfo(name = TABLE_RECORD_CHARGE) val charge: Double,
    @ColumnInfo(name = TABLE_RECORD_REMARK) val remark: String,
    @ColumnInfo(name = TABLE_RECORD_REIMBURSABLE) val reimbursable: Int,
    @ColumnInfo(name = TABLE_RECORD_RECORD_TIME) val recordTime: Long,
)
