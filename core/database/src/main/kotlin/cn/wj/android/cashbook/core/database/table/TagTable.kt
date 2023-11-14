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
 * 标签数据表
 *
 * @param id 主键自增长
 * @param name 标签名称
 * @param booksId 所属账本主键
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
@Entity(tableName = TABLE_TAG)
data class TagTable(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TABLE_TAG_ID)
    val id: Long?,
    @ColumnInfo(name = TABLE_TAG_NAME) val name: String,
    @ColumnInfo(name = TABLE_TAG_BOOKS_ID) val booksId: Long,
)
