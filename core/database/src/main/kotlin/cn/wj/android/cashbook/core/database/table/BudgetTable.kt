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
 * 预算数据表
 *
 * @param id 主键自增长
 * @param booksId 所属账本 id
 * @param typeId 一级支出分类 id；-1 表示总体预算
 * @param amount 月度限额（单位：分）
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/23
 */
@Entity(
    tableName = "db_budget",
    indices = [Index(value = ["books_id", "type_id"], unique = true)],
)
data class BudgetTable(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long?,
    @ColumnInfo(name = "books_id") val booksId: Long,
    @ColumnInfo(name = "type_id") val typeId: Long,
    @ColumnInfo(name = "amount") val amount: Long,
)
