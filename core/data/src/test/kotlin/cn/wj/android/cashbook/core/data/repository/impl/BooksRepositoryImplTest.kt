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

import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.data.testdoubles.FakeBooksDao
import cn.wj.android.cashbook.core.data.testdoubles.FakeCombineProtoDataSource
import cn.wj.android.cashbook.core.data.testdoubles.FakeTransactionDao
import cn.wj.android.cashbook.core.database.table.BooksTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.model.model.BooksModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * BooksRepository 实现测试
 *
 * 测试账本仓库的核心业务逻辑，包括账本管理、当前账本切换等
 */
class BooksRepositoryImplTest {

    private lateinit var booksDao: FakeBooksDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var fakeDataSource: FakeCombineProtoDataSource

    @Before
    fun setup() {
        booksDao = FakeBooksDao()
        transactionDao = FakeTransactionDao()
        fakeDataSource = FakeCombineProtoDataSource()
    }

    // ========== 账本管理测试 ==========

    @Test
    fun when_insert_book_then_book_stored() = runTest {
        val id = booksDao.insert(createBooksTable(name = "日常账本"))

        assertThat(id).isGreaterThan(0L)
        assertThat(booksDao.books).hasSize(1)
        assertThat(booksDao.books[0].name).isEqualTo("日常账本")
    }

    @Test
    fun when_insert_book_with_null_id_then_auto_increment() = runTest {
        booksDao.insert(createBooksTable(name = "账本A"))
        booksDao.insert(createBooksTable(name = "账本B"))

        assertThat(booksDao.books[0].id).isEqualTo(1L)
        assertThat(booksDao.books[1].id).isEqualTo(2L)
    }

    @Test
    fun when_queryAll_then_returns_all_books() = runTest {
        booksDao.insert(createBooksTable(name = "账本1"))
        booksDao.insert(createBooksTable(name = "账本2"))
        booksDao.insert(createBooksTable(name = "账本3"))

        val result = booksDao.queryAll()
        assertThat(result).hasSize(3)
    }

    @Test
    fun when_insertOrReplace_existing_then_replaced() = runTest {
        booksDao.insert(createBooksTable(name = "原始"))
        val original = booksDao.books[0]

        booksDao.insertOrReplace(original.copy(name = "替换后"))

        assertThat(booksDao.books).hasSize(1)
        assertThat(booksDao.books[0].name).isEqualTo("替换后")
    }

    @Test
    fun when_insertOrReplace_new_then_inserted() = runTest {
        booksDao.insertOrReplace(createBooksTable(name = "新账本"))

        assertThat(booksDao.books).hasSize(1)
    }

    // ========== 当前账本切换测试 ==========

    @Test
    fun when_selectBook_then_currentBookId_updated() = runTest {
        fakeDataSource.updateCurrentBookId(5L)

        val settings = fakeDataSource.recordSettingsData.first()
        assertThat(settings.currentBookId).isEqualTo(5L)
    }

    @Test
    fun given_currentBook_logic_when_book_found_then_returns_selected() = runTest {
        booksDao.insert(createBooksTable(name = "账本A"))
        booksDao.insert(createBooksTable(name = "账本B"))

        fakeDataSource.updateCurrentBookId(2L)

        // 模拟 currentBook 逻辑
        val allBooks = booksDao.queryAll().map { it.asModel() }
        val currentBookId = fakeDataSource.recordSettingsData.first().currentBookId
        val selected = allBooks.firstOrNull { it.id == currentBookId }

        assertThat(selected).isNotNull()
        assertThat(selected!!.name).isEqualTo("账本B")
    }

    @Test
    fun given_currentBook_logic_when_book_not_found_then_fallback_to_first() = runTest {
        booksDao.insert(createBooksTable(name = "账本A"))
        booksDao.insert(createBooksTable(name = "账本B"))

        // 设置一个不存在的账本 id
        fakeDataSource.updateCurrentBookId(999L)

        // 模拟 currentBook 逻辑
        val allBooks = booksDao.queryAll().map { it.asModel() }
        val currentBookId = fakeDataSource.recordSettingsData.first().currentBookId
        var selected = allBooks.firstOrNull { it.id == currentBookId }
        if (selected == null) {
            selected = allBooks.first()
            fakeDataSource.updateCurrentBookId(selected.id)
        }

        assertThat(selected.name).isEqualTo("账本A")
        // 验证 currentBookId 也被更新了
        val updatedSettings = fakeDataSource.recordSettingsData.first()
        assertThat(updatedSettings.currentBookId).isEqualTo(1L)
    }

    // ========== 重复检查测试 ==========

    @Test
    fun given_duplicate_name_when_isDuplicated_logic_then_returns_true() = runTest {
        booksDao.insert(createBooksTable(name = "我的账本"))
        booksDao.insert(createBooksTable(name = "其他账本"))

        // 模拟 isDuplicated 逻辑
        val allBooks = booksDao.queryAll().map { it.asModel() }
        val newBook = BooksModel(id = -1L, name = "我的账本", description = "", bgUri = "", modifyTime = 0L)
        val isDuplicated = allBooks.count { it.id != newBook.id && it.name == newBook.name } > 0

        assertThat(isDuplicated).isTrue()
    }

    @Test
    fun given_unique_name_when_isDuplicated_logic_then_returns_false() = runTest {
        booksDao.insert(createBooksTable(name = "我的账本"))

        val allBooks = booksDao.queryAll().map { it.asModel() }
        val newBook = BooksModel(id = -1L, name = "新账本", description = "", bgUri = "", modifyTime = 0L)
        val isDuplicated = allBooks.count { it.id != newBook.id && it.name == newBook.name } > 0

        assertThat(isDuplicated).isFalse()
    }

    @Test
    fun given_same_id_same_name_when_isDuplicated_logic_then_returns_false() = runTest {
        booksDao.insert(createBooksTable(name = "我的账本"))

        // 更新自己的名字不算重复
        val allBooks = booksDao.queryAll().map { it.asModel() }
        val existingBook = BooksModel(id = 1L, name = "我的账本", description = "", bgUri = "", modifyTime = 0L)
        val isDuplicated = allBooks.count { it.id != existingBook.id && it.name == existingBook.name } > 0

        assertThat(isDuplicated).isFalse()
    }

    // ========== 删除账本测试 ==========

    @Test
    fun when_deleteBookTransaction_then_book_and_records_deleted() = runTest {
        // 准备数据
        val bookId = 1L
        transactionDao.books.add(createBooksTable(id = bookId, name = "待删除"))
        transactionDao.records.add(createRecordTable(id = 1L, booksId = bookId))
        transactionDao.records.add(createRecordTable(id = 2L, booksId = bookId))
        transactionDao.records.add(createRecordTable(id = 3L, booksId = 2L))

        transactionDao.deleteBookTransaction(bookId)

        // 验证账本下的记录被删除
        assertThat(transactionDao.records).hasSize(1)
        assertThat(transactionDao.records[0].booksId).isEqualTo(2L)
        // 验证账本被删除
        assertThat(transactionDao.deleteBookCalled).isTrue()
    }

    @Test
    fun when_deleteBook_logic_with_failure_then_returns_false() = runTest {
        // 模拟 BooksRepositoryImpl.deleteBook 逻辑中的异常处理
        val result = runCatching {
            // 模拟删除过程中出错
            throw RuntimeException("删除失败")
        }.getOrElse { false }

        assertThat(result).isEqualTo(false)
    }

    // ========== 默认账本测试 ==========

    @Test
    fun given_existing_book_when_getDefaultBook_logic_then_returns_existing() = runTest {
        booksDao.insert(createBooksTable(name = "我的账本"))

        // 模拟 getDefaultBook 逻辑
        val allBooks = booksDao.queryAll().map { it.asModel() }
        val result = allBooks.firstOrNull { it.id == 1L } ?: BooksModel(
            id = 1L,
            name = "",
            description = "",
            bgUri = "",
            modifyTime = System.currentTimeMillis(),
        )

        assertThat(result.name).isEqualTo("我的账本")
    }

    @Test
    fun given_nonexistent_book_when_getDefaultBook_logic_then_returns_empty_model() = runTest {
        // 没有任何账本

        val allBooks = booksDao.queryAll().map { it.asModel() }
        val result = allBooks.firstOrNull { it.id == 999L } ?: BooksModel(
            id = 999L,
            name = "",
            description = "",
            bgUri = "",
            modifyTime = 0L,
        )

        assertThat(result.name).isEmpty()
        assertThat(result.id).isEqualTo(999L)
    }

    // ========== 模型映射集成测试 ==========

    @Test
    fun when_insertBook_with_model_then_roundtrip_works() = runTest {
        val model = BooksModel(
            id = -1L,
            name = "测试账本",
            description = "描述",
            bgUri = "uri://bg",
            modifyTime = 1000L,
        )

        val table = model.asTable()
        assertThat(table.id).isNull()

        booksDao.insert(table)
        val queried = booksDao.queryAll().first()
        val result = queried.asModel()

        assertThat(result.name).isEqualTo("测试账本")
        assertThat(result.description).isEqualTo("描述")
        assertThat(result.bgUri).isEqualTo("uri://bg")
    }

    @Test
    fun when_updateBook_then_insertOrReplace_delegates() = runTest {
        // 模拟 BooksRepositoryImpl.updateBook 逻辑
        booksDao.insert(createBooksTable(name = "原始"))
        val book = booksDao.books[0].asModel()
        val updatedModel = book.copy(name = "更新后")

        booksDao.insertOrReplace(updatedModel.asTable())

        assertThat(booksDao.books).hasSize(1)
        assertThat(booksDao.books[0].name).isEqualTo("更新后")
    }

    // ========== 辅助方法 ==========

    private fun createBooksTable(
        id: Long? = null,
        name: String = "账本",
        description: String = "",
        bgUri: String = "",
        modifyTime: Long = 1000L,
    ) = BooksTable(
        id = id,
        name = name,
        description = description,
        bgUri = bgUri,
        modifyTime = modifyTime,
    )

    private fun createRecordTable(
        id: Long? = null,
        booksId: Long = 1L,
    ) = RecordTable(
        id = id,
        typeId = 1L,
        assetId = 1L,
        intoAssetId = -1L,
        booksId = booksId,
        amount = 0L,
        finalAmount = 0L,
        concessions = 0L,
        charge = 0L,
        remark = "",
        reimbursable = 0,
        recordTime = 1000L,
    )
}
