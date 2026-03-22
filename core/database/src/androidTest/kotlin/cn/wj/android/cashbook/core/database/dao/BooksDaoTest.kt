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

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.table.BooksTable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BooksDao 数据库操作测试
 */
@RunWith(AndroidJUnit4::class)
class BooksDaoTest {

    private lateinit var database: CashbookDatabase
    private lateinit var booksDao: BooksDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CashbookDatabase::class.java,
        ).allowMainThreadQueries().build()
        booksDao = database.booksDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun when_insertBook_then_queryReturnsIt() = runTest {
        // 插入一条账本记录
        val book = BooksTable(
            id = null,
            name = "测试账本",
            description = "测试描述",
            bgUri = "",
            modifyTime = System.currentTimeMillis(),
        )
        val insertedId = booksDao.insert(book)

        // 查询所有账本，验证包含插入的记录
        val allBooks = booksDao.queryAll()
        assertThat(allBooks).hasSize(1)
        assertThat(allBooks[0].id).isEqualTo(insertedId)
        assertThat(allBooks[0].name).isEqualTo("测试账本")
        assertThat(allBooks[0].description).isEqualTo("测试描述")
    }

    @Test
    fun when_insertMultipleBooks_then_queryAllReturnsAll() = runTest {
        // 插入多条账本记录
        val book1 = BooksTable(
            id = null,
            name = "账本一",
            description = "描述一",
            bgUri = "",
            modifyTime = System.currentTimeMillis(),
        )
        val book2 = BooksTable(
            id = null,
            name = "账本二",
            description = "描述二",
            bgUri = "content://bg",
            modifyTime = System.currentTimeMillis(),
        )
        val book3 = BooksTable(
            id = null,
            name = "账本三",
            description = "描述三",
            bgUri = "",
            modifyTime = System.currentTimeMillis(),
        )
        booksDao.insert(book1)
        booksDao.insert(book2)
        booksDao.insert(book3)

        // 查询所有账本，验证数量正确
        val allBooks = booksDao.queryAll()
        assertThat(allBooks).hasSize(3)

        val names = allBooks.map { it.name }
        assertThat(names).containsExactly("账本一", "账本二", "账本三")
    }

    @Test
    fun when_insertOrReplaceWithSameId_then_updatesExisting() = runTest {
        // 先插入一条账本记录
        val book = BooksTable(
            id = null,
            name = "原始账本",
            description = "原始描述",
            bgUri = "",
            modifyTime = 1000L,
        )
        val insertedId = booksDao.insert(book)

        // 使用相同 id 调用 insertOrReplace 更新数据
        val updatedBook = BooksTable(
            id = insertedId,
            name = "更新后账本",
            description = "更新后描述",
            bgUri = "content://new_bg",
            modifyTime = 2000L,
        )
        booksDao.insertOrReplace(updatedBook)

        // 验证记录已更新且总数不变
        val allBooks = booksDao.queryAll()
        assertThat(allBooks).hasSize(1)
        assertThat(allBooks[0].id).isEqualTo(insertedId)
        assertThat(allBooks[0].name).isEqualTo("更新后账本")
        assertThat(allBooks[0].description).isEqualTo("更新后描述")
        assertThat(allBooks[0].bgUri).isEqualTo("content://new_bg")
        assertThat(allBooks[0].modifyTime).isEqualTo(2000L)
    }

    @Test
    fun when_noBooks_then_queryAllReturnsEmpty() = runTest {
        // 空数据库查询应返回空列表
        val allBooks = booksDao.queryAll()
        assertThat(allBooks).isEmpty()
    }
}
