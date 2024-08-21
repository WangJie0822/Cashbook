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

package cn.wj.android.cashbook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.database.dao.AssetDao
import cn.wj.android.cashbook.core.database.dao.BooksDao
import cn.wj.android.cashbook.core.database.dao.RecordDao
import cn.wj.android.cashbook.core.database.dao.TagDao
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.database.dao.TypeDao
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.BooksTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable

/**
 * 记账本数据库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Database(
    entities = [
        BooksTable::class, AssetTable::class, TypeTable::class, RecordTable::class, TagTable::class,
        TagWithRecordTable::class, RecordWithRelatedTable::class,
    ],
    version = ApplicationInfo.DB_VERSION,
)
abstract class CashbookDatabase : RoomDatabase() {

    /** 获取类型数据库操作接口 */
    abstract fun typeDao(): TypeDao

    /** 获取资产数据库操作接口 */
    abstract fun assetDao(): AssetDao

    /** 获取标签数据库操作接口 */
    abstract fun tagDao(): TagDao

    /** 获取记录数据库操作接口 */
    abstract fun recordDao(): RecordDao

    /** 获取账本数据库操作接口 */
    abstract fun booksDao(): BooksDao

    /** 复杂事务数据库操作接口 */
    abstract fun transactionDao(): TransactionDao
}
