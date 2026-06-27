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

package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.data.testdoubles.FakeRecordDao
import cn.wj.android.cashbook.core.data.uitl.FakeRecordImageFileStorage
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** [runImageBackfill] 逐行幂等 BLOB→文件 backfill 单测 */
class RecordBackfillTest {

    @Test
    fun backfill_movesBlobToFile_setsRelativePath_emptiesBytes() = runTest {
        val storage = FakeRecordImageFileStorage()
        val dao = FakeRecordDao().apply {
            images.add(ImageWithRelatedTable(id = 3L, recordId = 1L, path = "content://old/3", bytes = byteArrayOf(1, 2)))
        }

        val clean = runImageBackfill(dao, storage)

        assertThat(clean).isTrue() // 无坏行
        // 文件按 id 确定性命名写入
        assertThat(storage.files["record_images/img_3.jpg"]).isEqualTo(byteArrayOf(1, 2))
        // 行已更新：path 相对化、bytes 置空
        val row = dao.queryAllImages().single()
        assertThat(row.path).isEqualTo("record_images/img_3.jpg")
        assertThat(row.bytes).isEmpty()
    }

    @Test
    fun backfill_oneFailingRow_continuesOthers_returnsFalse() = runTest {
        // 坏行（写失败）不阻塞其余行；返回 false → 调用方不置 migrated 标志、下次重试
        val storage = FakeRecordImageFileStorage().apply { failWritePaths.add("record_images/img_2.jpg") }
        val dao = FakeRecordDao().apply {
            images.add(ImageWithRelatedTable(id = 1L, recordId = 1L, path = "content://1", bytes = byteArrayOf(1)))
            images.add(ImageWithRelatedTable(id = 2L, recordId = 1L, path = "content://2", bytes = byteArrayOf(2)))
            images.add(ImageWithRelatedTable(id = 3L, recordId = 1L, path = "content://3", bytes = byteArrayOf(3)))
        }

        val clean = runImageBackfill(dao, storage)

        assertThat(clean).isFalse()
        // id=1,3 迁移成功，id=2 跳过、不阻塞 id=3
        assertThat(storage.files.keys).containsExactly("record_images/img_1.jpg", "record_images/img_3.jpg")
        assertThat(dao.queryAllImages().first { it.id == 1L }.bytes).isEmpty()
        assertThat(dao.queryAllImages().first { it.id == 3L }.bytes).isEmpty()
        // 坏行保留 bytes（数据不丢，下次重试）
        assertThat(dao.queryAllImages().first { it.id == 2L }.bytes).isEqualTo(byteArrayOf(2))
    }

    @Test
    fun backfill_isIdempotent_skipsAlreadyMigratedRows() = runTest {
        val storage = FakeRecordImageFileStorage()
        val dao = FakeRecordDao().apply {
            images.add(ImageWithRelatedTable(id = 3L, recordId = 1L, path = "record_images/img_3.jpg", bytes = byteArrayOf()))
        }

        runImageBackfill(dao, storage)

        assertThat(storage.files).isEmpty() // 已迁移行（bytes 空）跳过、不重写
        assertThat(dao.queryAllImages().single().path).isEqualTo("record_images/img_3.jpg")
    }

    @Test
    fun backfill_reentrant_afterFileWrittenButRowNotCommitted_overwritesSameFile() = runTest {
        // 模拟上次崩溃：文件已写、行仍是旧 path + 非空 bytes → 重跑按同名覆盖、不产孤儿
        val storage = FakeRecordImageFileStorage().apply { files["record_images/img_3.jpg"] = byteArrayOf(9) }
        val dao = FakeRecordDao().apply {
            images.add(ImageWithRelatedTable(id = 3L, recordId = 1L, path = "content://old/3", bytes = byteArrayOf(1, 2)))
        }

        runImageBackfill(dao, storage)

        assertThat(storage.files.keys).containsExactly("record_images/img_3.jpg") // 无新孤儿
        assertThat(storage.files["record_images/img_3.jpg"]).isEqualTo(byteArrayOf(1, 2)) // 同名覆盖为权威 bytes
        assertThat(dao.queryAllImages().single().bytes).isEmpty()
    }

    @Test
    fun backfill_skipsRowsWithNullId() = runTest {
        val storage = FakeRecordImageFileStorage()
        val dao = FakeRecordDao().apply {
            images.add(ImageWithRelatedTable(id = null, recordId = 1L, path = "content://x", bytes = byteArrayOf(1)))
        }

        runImageBackfill(dao, storage)

        assertThat(storage.files).isEmpty() // 无 id 无法派生确定文件名，跳过
    }
}
