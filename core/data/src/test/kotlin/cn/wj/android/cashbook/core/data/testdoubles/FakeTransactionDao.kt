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

package cn.wj.android.cashbook.core.data.testdoubles

import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.BooksTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable

/**
 * TransactionDao 的测试替身，使用内存列表存储数据
 *
 * 事务操作的简化实现，不含完整的资产金额计算逻辑
 */
class FakeTransactionDao : TransactionDao {

    /** 记录数据列表 */
    val records = mutableListOf<RecordTable>()

    /** 关联标签列表 */
    val tagWithRecords = mutableListOf<TagWithRecordTable>()

    /** 关联图片列表 */
    val imageWithRecords = mutableListOf<ImageWithRelatedTable>()

    /** 关联记录列表 */
    val relatedRecords = mutableListOf<RecordWithRelatedTable>()

    /** 资产列表 */
    val assets = mutableListOf<AssetTable>()

    /** 类型列表 */
    val types = mutableListOf<TypeTable>()

    /** 账本列表 */
    val books = mutableListOf<BooksTable>()

    /** 标签列表 */
    val tags = mutableListOf<Long>()

    /** 自增记录主键 */
    private var nextRecordId = 1L

    /** 自增关联主键 */
    private var nextRelatedId = 1L

    /** 自增标签关联主键 */
    private var nextTagRelatedId = 1L

    /** 自增图片关联主键 */
    private var nextImageRelatedId = 1L

    /** 记录删除记录操作是否被调用 */
    var deleteRecordCalled = false
        private set

    /** 记录删除账本操作是否被调用 */
    var deleteBookCalled = false
        private set

    /** 记录删除标签操作是否被调用 */
    var deleteTagCalled = false
        private set

    override suspend fun insertRecord(recordTable: RecordTable): Long {
        val id = recordTable.id ?: nextRecordId++
        records.add(recordTable.copy(id = id))
        return id
    }

    override suspend fun insertRelatedTags(tagWithRecordTable: List<TagWithRecordTable>) {
        tagWithRecords.addAll(
            tagWithRecordTable.map {
                if (it.id == null) it.copy(id = nextTagRelatedId++) else it
            },
        )
    }

    override suspend fun insertRelatedImages(images: List<ImageWithRelatedTable>) {
        imageWithRecords.addAll(
            images.map {
                if (it.id == null) it.copy(id = nextImageRelatedId++) else it
            },
        )
    }

    override suspend fun deleteRecord(recordTable: RecordTable): Int {
        val removed = records.removeAll { it.id == recordTable.id }
        return if (removed) 1 else 0
    }

    override suspend fun deleteOldRelatedTags(recordId: Long) {
        tagWithRecords.removeAll { it.recordId == recordId }
    }

    override suspend fun deleteOldRelatedImages(recordId: Long) {
        imageWithRecords.removeAll { it.recordId == recordId }
    }

    override suspend fun updateRecord(recordTable: RecordTable) {
        val index = records.indexOfFirst { it.id == recordTable.id }
        if (index >= 0) {
            records[index] = recordTable
        }
    }

    override suspend fun updateAsset(assetTable: AssetTable) {
        val index = assets.indexOfFirst { it.id == assetTable.id }
        if (index >= 0) {
            assets[index] = assetTable
        }
    }

    override suspend fun queryAssetById(assetId: Long): AssetTable? {
        return assets.firstOrNull { it.id == assetId }
    }

    override suspend fun queryTypeById(typeId: Long): TypeTable? {
        return types.firstOrNull { it.id == typeId }
    }

    override suspend fun queryRecordById(recordId: Long): RecordTable? {
        return records.firstOrNull { it.id == recordId }
    }

    override suspend fun clearRelatedRecordById(id: Long) {
        relatedRecords.removeAll { it.recordId == id || it.relatedRecordId == id }
    }

    override suspend fun queryRelatedByRelatedRecordId(id: Long): List<RecordWithRelatedTable> {
        return relatedRecords.filter { it.relatedRecordId == id }
    }

    override suspend fun queryRelatedByRecordId(id: Long): List<RecordWithRelatedTable> {
        return relatedRecords.filter { it.recordId == id }
    }

    override suspend fun insertRelatedRecord(related: List<RecordWithRelatedTable>) {
        relatedRecords.addAll(
            related.map {
                if (it.id == null) it.copy(id = nextRelatedId++) else it
            },
        )
    }

    override suspend fun updateRecordFinalAmountById(id: Long, finalAmount: Long) {
        val index = records.indexOfFirst { it.id == id }
        if (index >= 0) {
            records[index] = records[index].copy(finalAmount = finalAmount)
        }
    }

    override suspend fun queryRecordByIds(ids: List<Long>): List<RecordTable> {
        return records.filter { it.id in ids }
    }

    override suspend fun queryRecordListByBookId(bookId: Long): List<RecordTable> {
        return records.filter { it.booksId == bookId }
    }

    override suspend fun deleteRecordRelationByRecordId(recordId: Long) {
        relatedRecords.removeAll { it.recordId == recordId || it.relatedRecordId == recordId }
    }

    override suspend fun deleteTagRelationByRecordId(recordId: Long) {
        tagWithRecords.removeAll { it.recordId == recordId }
    }

    override suspend fun deleteTagRelationByTagId(tagId: Long) {
        tagWithRecords.removeAll { it.tagId == tagId }
    }

    override suspend fun deleteTagById(tagId: Long) {
        tags.remove(tagId)
        deleteTagCalled = true
    }

    override suspend fun deleteBookById(bookId: Long) {
        books.removeAll { it.id == bookId }
        deleteBookCalled = true
    }

    override suspend fun deleteTagRelationsByBookId(bookId: Long) {
        val recordIds = records.filter { it.booksId == bookId }.mapNotNull { it.id }
        tagWithRecords.removeAll { it.recordId in recordIds }
    }

    override suspend fun deleteRecordRelationsByBookId(bookId: Long) {
        val recordIds = records.filter { it.booksId == bookId }.mapNotNull { it.id }
        relatedRecords.removeAll { it.recordId in recordIds || it.relatedRecordId in recordIds }
    }

    override suspend fun deleteImageRelationsByBookId(bookId: Long) {
        val recordIds = records.filter { it.booksId == bookId }.mapNotNull { it.id }
        imageWithRecords.removeAll { it.recordId in recordIds }
    }

    override suspend fun deleteRecordsByBookId(bookId: Long) {
        records.removeAll { it.booksId == bookId }
    }

    override suspend fun queryAllAssetsByBookId(bookId: Long): List<AssetTable> {
        return assets.filter { it.booksId == bookId }
    }

    override suspend fun deleteAssetsByBookId(bookId: Long) {
        assets.removeAll { it.booksId == bookId }
    }

    override suspend fun deleteTagsByBookId(bookId: Long) {
        // 简化实现：Fake 中标签不追踪 booksId
    }

    override suspend fun queryRecordsByAssetId(assetId: Long): List<RecordTable> {
        return records.filter { it.assetId == assetId || it.intoAssetId == assetId }
    }

    override suspend fun deleteBookTransaction(bookId: Long) {
        // 简化实现：批量删除账本下的关联数据、记录、资产
        deleteTagRelationsByBookId(bookId)
        deleteRecordRelationsByBookId(bookId)
        deleteImageRelationsByBookId(bookId)
        deleteRecordsByBookId(bookId)
        deleteAssetsByBookId(bookId)
        deleteTagsByBookId(bookId)
        deleteBookById(bookId)
        deleteBookCalled = true
    }

    override suspend fun deleteAssetRelatedData(assetId: Long) {
        // 简化实现：批量删除资产关联数据
        deleteTagRelationsByAssetId(assetId)
        deleteRecordRelationsByAssetId(assetId)
        deleteImageRelationsByAssetId(assetId)
        deleteRecordsByAssetId(assetId)
    }

    override suspend fun deleteTag(id: Long) {
        deleteTagRelationByTagId(id)
        deleteTagById(id)
        deleteTagCalled = true
    }

    override suspend fun deleteTagRelationsByAssetId(assetId: Long) {
        val recordIds = records.filter { it.assetId == assetId || it.intoAssetId == assetId }
            .mapNotNull { it.id }
        tagWithRecords.removeAll { it.recordId in recordIds }
    }

    override suspend fun deleteRecordRelationsByAssetId(assetId: Long) {
        val recordIds = records.filter { it.assetId == assetId || it.intoAssetId == assetId }
            .mapNotNull { it.id }
        relatedRecords.removeAll { it.recordId in recordIds || it.relatedRecordId in recordIds }
    }

    override suspend fun deleteImageRelationsByAssetId(assetId: Long) {
        val recordIds = records.filter { it.assetId == assetId || it.intoAssetId == assetId }
            .mapNotNull { it.id }
        imageWithRecords.removeAll { it.recordId in recordIds }
    }

    override suspend fun deleteRecordsByAssetId(assetId: Long) {
        records.removeAll { it.assetId == assetId || it.intoAssetId == assetId }
    }

    override suspend fun updateRecordTypeId(oldTypeId: Long, newTypeId: Long) {
        val updated = records.mapIndexed { index, record ->
            if (record.typeId == oldTypeId) record.copy(typeId = newTypeId) else record
        }
        records.clear()
        records.addAll(updated)
    }

    override suspend fun promoteChildTypes(parentId: Long) {
        val updated = types.map { type ->
            if (type.parentId == parentId) type.copy(parentId = -1, typeLevel = 0) else type
        }
        types.clear()
        types.addAll(updated)
    }

    override suspend fun countRecordsByTypeId(typeId: Long): Int {
        return records.count { it.typeId == typeId }
    }

    override suspend fun deleteTypeById(typeId: Long) {
        types.removeAll { it.id == typeId }
    }

    // 注意：insertRecordTransaction、updateRecordTransaction、deleteRecordTransaction、deleteAssetRelatedData、migrateTypeRecords
    // 使用了默认实现（@Transaction 注解方法），会调用上面的基础方法
    // 在 Fake 中不需要重新实现事务方法，因为测试不会直接调用它们
    // Repository 层通过调用这些事务方法来间接使用基础方法
}
