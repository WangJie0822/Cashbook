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

package cn.wj.android.cashbook.core.data.uitl

import android.content.Context
import android.os.StatFs
import cn.wj.android.cashbook.core.common.DB_FILE_NAME
import cn.wj.android.cashbook.core.database.CashbookDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 数据库压实接缝（C-robust live VACUUM 可注入抽象，便于 JVM 单测 [runDbCompactIfNeeded] 编排）。
 */
interface DatabaseCompactor {
    /** 数据库主文件当前字节大小（不存在返 0） */
    suspend fun databaseSizeBytes(): Long

    /** 数据库所在分区可用字节 */
    suspend fun freeSpaceBytes(): Long

    /** 执行 VACUUM，真成功返 true，异常/锁/ENOSPC 返 false（best-effort） */
    suspend fun vacuum(): Boolean
}

class DatabaseCompactorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: CashbookDatabase,
) : DatabaseCompactor {

    override suspend fun databaseSizeBytes(): Long =
        context.getDatabasePath(DB_FILE_NAME).let { if (it.exists()) it.length() else 0L }

    override suspend fun freeSpaceBytes(): Long {
        val parent = context.getDatabasePath(DB_FILE_NAME).parentFile ?: return 0L
        return StatFs(parent.absolutePath).availableBytes
    }

    override suspend fun vacuum(): Boolean = try {
        // VACUUM 不能在事务中执行：走 SupportSQLiteDatabase.execSQL
        database.openHelper.writableDatabase.execSQL("VACUUM")
        true
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        false
    }
}
