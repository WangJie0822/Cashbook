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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_INCOME
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.database.throwable.DataTransactionException
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.model.model.recordAmount

/**
 * 事务数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface TransactionDao {

    @Insert
    suspend fun insertRecord(recordTable: RecordTable): Long

    @Insert
    suspend fun insertRelatedTags(tagWithRecordTable: List<TagWithRecordTable>)

    @Insert
    suspend fun insertRelatedImages(images: List<ImageWithRelatedTable>)

    @Delete
    suspend fun deleteRecord(recordTable: RecordTable): Int

    @Query(
        value = """
        DELETE FROM db_tag_with_record WHERE record_id=:recordId
    """,
    )
    suspend fun deleteOldRelatedTags(recordId: Long)

    @Query(
        value = """
        DELETE FROM db_image_with_related WHERE record_id=:recordId
    """,
    )
    suspend fun deleteOldRelatedImages(recordId: Long)

    @Update
    suspend fun updateRecord(recordTable: RecordTable)

    @Update
    suspend fun updateAsset(assetTable: AssetTable)

    @Query(
        value = """
        SELECT * FROM db_asset WHERE id=:assetId
    """,
    )
    suspend fun queryAssetById(assetId: Long): AssetTable?

    @Query(
        value = """
        SELECT * FROM db_type WHERE id=:typeId
    """,
    )
    suspend fun queryTypeById(typeId: Long): TypeTable?

    @Query(
        value = """
        SELECT * FROM db_record WHERE id=:recordId
    """,
    )
    suspend fun queryRecordById(recordId: Long): RecordTable?

    @Query(
        value = """
        SELECT * FROM db_record WHERE id IN (:ids)
    """,
    )
    suspend fun queryRecordByIds(ids: List<Long>): List<RecordTable>

    @Query(value = "SELECT * FROM db_record")
    suspend fun queryAllRecords(): List<RecordTable>

    @Query(value = "SELECT * FROM db_record_with_related")
    suspend fun queryAllRelatedRecords(): List<RecordWithRelatedTable>

    @Query(
        value = """
        DELETE FROM db_record_with_related WHERE record_id=:id OR related_record_id=:id
    """,
    )
    suspend fun clearRelatedRecordById(id: Long)

    @Query(
        value = """
        SELECT * FROM db_record_with_related WHERE related_record_id=:id
    """,
    )
    suspend fun queryRelatedByRelatedRecordId(id: Long): List<RecordWithRelatedTable>

    @Query(
        value = """
        SELECT * FROM db_record_with_related WHERE record_id=:id
    """,
    )
    suspend fun queryRelatedByRecordId(id: Long): List<RecordWithRelatedTable>

    @Insert
    suspend fun insertRelatedRecord(related: List<RecordWithRelatedTable>)

    @Query("SELECT * FROM db_asset WHERE books_id=:bookId")
    suspend fun queryAllAssetsByBookId(bookId: Long): List<AssetTable>

    @Query("DELETE FROM db_asset WHERE books_id=:bookId")
    suspend fun deleteAssetsByBookId(bookId: Long)

    @Query("DELETE FROM db_tag WHERE books_id=:bookId")
    suspend fun deleteTagsByBookId(bookId: Long)

    @Query("SELECT * FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId")
    suspend fun queryRecordsByAssetId(assetId: Long): List<RecordTable>

    /** 解析记录类型，处理平账特殊类型 */
    suspend fun resolveType(typeId: Long): TypeTable? {
        return when (typeId) {
            TYPE_TABLE_BALANCE_EXPENDITURE.id -> TYPE_TABLE_BALANCE_EXPENDITURE
            TYPE_TABLE_BALANCE_INCOME.id -> TYPE_TABLE_BALANCE_INCOME
            else -> queryTypeById(typeId)
        }
    }

    /** 计算记录涉及的实际金额（单位：分） */
    fun calculateRecordAmount(
        record: RecordTable,
        category: RecordTypeCategoryEnum,
    ): Long = recordAmount(category, record.amount, record.charge, record.concessions)

    /**
     * 重算一个吸收簇的 finalAmount（净自付语义，§5 顺序贪心填充）。
     *
     * 从 [seedRecordId] 沿关系表 BFS 发现整个连通簇（被吸收支出↔吸收者收入构成二部图连通分量），
     * 将簇内所有记录 finalAmount 重置为 recordAmount，再按吸收者 id 升序、每个吸收者按被吸收支出 id 升序
     * 顺序贪心填充。结果只依赖 id 顺序、与插入历史无关，故增量与全量逐字段一致。
     *
     * @param seedRecordId 簇内任一记录 id（受影响的吸收者或被吸收支出）
     * @param excludeRecordIds 需从簇中排除的记录 id（删除场景：记录即将被删但关联尚未清除）
     */
    suspend fun recalculateFinalAmountForCluster(
        seedRecordId: Long,
        excludeRecordIds: Set<Long> = emptySet(),
    ) {
        // 1. BFS 发现连通簇（排除 excludeRecordIds，排除节点不入簇也不被遍历）
        val clusterIds = LinkedHashSet<Long>()
        val queue = ArrayDeque<Long>()
        if (seedRecordId !in excludeRecordIds) {
            clusterIds.add(seedRecordId)
            queue.add(seedRecordId)
        }
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val neighbors = queryRelatedByRecordId(cur).map { it.relatedRecordId } +
                queryRelatedByRelatedRecordId(cur).map { it.recordId }
            for (n in neighbors) {
                if (n !in excludeRecordIds && clusterIds.add(n)) {
                    queue.add(n)
                }
            }
        }
        if (clusterIds.isEmpty()) return

        // 2. 初始化簇内 finalAmount = recordAmount（簇内仅 income/expenditure，转账不参与吸收）
        val finalAmounts = HashMap<Long, Long>(clusterIds.size)
        for (id in clusterIds) {
            val record = queryRecordById(id) ?: continue
            val type = resolveType(record.typeId) ?: continue
            val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
            finalAmounts[id] = calculateRecordAmount(record, category)
        }

        // 3. 簇内吸收者（record_id 侧，有被吸收支出）按 id 升序，顺序贪心填充
        val absorbers = clusterIds.filter { id ->
            queryRelatedByRecordId(id).any { it.relatedRecordId in clusterIds }
        }.sorted()
        for (absorberId in absorbers) {
            var remaining = finalAmounts[absorberId] ?: continue
            val absorbedIds = queryRelatedByRecordId(absorberId)
                .map { it.relatedRecordId }
                .filter { it in clusterIds }
                .sorted()
            for (expenseId in absorbedIds) {
                val current = finalAmounts[expenseId] ?: continue
                val offset = minOf(remaining, current) // current/remaining 均不下穿 0，保证非负
                finalAmounts[expenseId] = current - offset
                remaining -= offset
            }
            finalAmounts[absorberId] = remaining // 溢出（通常 0）
        }

        // 4. 落库
        for ((id, finalAmount) in finalAmounts) {
            updateRecordFinalAmountById(id, finalAmount)
        }
    }

    /**
     * 重算受影响吸收簇的 finalAmount（净自付语义）。
     *
     * 兼容旧签名：从 [absorberId] 所在簇重算，可排除即将删除的被吸收记录 [excludeAbsorbedId]。
     * 实际委托 [recalculateFinalAmountForCluster]——不再是「仅算吸收者」，而是整簇净自付重算。
     */
    suspend fun recalculateAbsorberFinalAmount(
        absorberId: Long,
        excludeAbsorbedId: Long = -1L,
    ) {
        recalculateFinalAmountForCluster(
            seedRecordId = absorberId,
            excludeRecordIds = if (excludeAbsorbedId != -1L) setOf(excludeAbsorbedId) else emptySet(),
        )
    }

    /**
     * 校验资产余额的一致性
     *
     * 遍历资产的所有关联记录，计算余额应有的净变化量，与当前余额对比。
     * 返回差值：0 表示余额正确，非 0 表示存在偏差。
     *
     * 注意：新建资产时的初始余额不通过记录产生（直接写入 AssetTable），
     * 后续的余额修改会生成平账记录。因此本方法计算的是"记录产生的净变化量"，
     * 需要加上初始余额才等于期望的当前余额。
     *
     * @return 差值 = 当前余额 - (初始余额推算值 + 记录净变化)，正常应为 0
     */
    @Transaction
    suspend fun verifyAssetBalance(assetId: Long): Long {
        val asset = queryAssetById(assetId) ?: return 0L
        val isCreditCard =
            ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT
        val records = queryRecordsByAssetId(assetId)

        var balanceChange = 0L
        for (record in records) {
            val type = resolveType(record.typeId) ?: continue
            val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
            val recordAmount = calculateRecordAmount(record, category)

            // 此资产作为源资产（assetId）
            if (record.assetId == assetId) {
                balanceChange += if (isCreditCard) {
                    if (category == RecordTypeCategoryEnum.INCOME) -recordAmount else recordAmount
                } else {
                    if (category == RecordTypeCategoryEnum.INCOME) recordAmount else -recordAmount
                }
            }

            // 此资产作为转账目标（intoAssetId）
            if (record.intoAssetId == assetId && category == RecordTypeCategoryEnum.TRANSFER) {
                balanceChange += if (isCreditCard) -record.amount else record.amount
            }
        }

        // 初始余额推算 = 当前余额 - 记录净变化
        // 如果一切正确，初始余额应是资产创建时的值（无法独立验证）
        // 但可以用于跨资产交叉校验：同一批操作后重算应与原值一致
        return balanceChange
    }

    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun updateRecordTransaction(
        record: RecordTable,
        tagIdList: List<Long>,
        needRelated: Boolean,
        relatedRecordIdList: List<Long>,
        relatedImageList: List<ImageModel>,
    ) {
        // 删除旧数据
        deleteRecordTransaction(record.id)
        // 插入新数据
        insertRecordTransaction(
            record = record,
            tagIdList = tagIdList,
            needRelated = needRelated,
            relatedRecordIdList = relatedRecordIdList,
            relatedImageList = relatedImageList,
        )
    }

    /**
     * 插入记录
     *
     * - 计算最终金额
     * - 更新关联资产金额
     * - 转账类型更新目标资产金额
     * - 插入关联的标签数据
     * - 插入关联记录数据
     * - 更新关联记录的最终金额
     * - 更新记录的最终金额
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun insertRecordTransaction(
        record: RecordTable,
        tagIdList: List<Long>,
        needRelated: Boolean,
        relatedRecordIdList: List<Long>,
        relatedImageList: List<ImageModel>,
    ) {
        // 防御性校验：确保引用的类型存在
        val type = resolveType(record.typeId)
            ?: throw DataTransactionException("Type must not be null, typeId=${record.typeId}")
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        // 防御性校验：确保引用的资产存在（NO_ASSET_ID 除外）
        if (record.assetId != cn.wj.android.cashbook.core.common.NO_ASSET_ID) {
            queryAssetById(record.assetId)
                ?: throw DataTransactionException("Asset not found, assetId=${record.assetId}")
        }
        if (category == RecordTypeCategoryEnum.TRANSFER &&
            record.intoAssetId != cn.wj.android.cashbook.core.common.NO_ASSET_ID
        ) {
            queryAssetById(record.intoAssetId)
                ?: throw DataTransactionException("Transfer target asset not found, intoAssetId=${record.intoAssetId}")
        }
        val recordAmount = calculateRecordAmount(record, category)
        // 更新资产余额
        queryAssetById(record.assetId)?.let { asset ->
            // 计算已用额度 or 余额
            val balance =
                if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    // 信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，已用额度 - 记录金额
                        asset.balance - recordAmount
                    } else {
                        // 支出、转账，已用额度 + 记录金额
                        asset.balance + recordAmount
                    }
                } else {
                    // 非信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，余额 + 记录金额
                        asset.balance + recordAmount
                    } else {
                        // 支出、转账，余额 - 记录金额
                        asset.balance - recordAmount
                    }
                }
            // 更新资产
            updateAsset(asset.copy(balance = balance))
        }
        if (category == RecordTypeCategoryEnum.TRANSFER) {
            // 转账，更新关联资产余额
            queryAssetById(record.intoAssetId)?.let { asset ->
                // 计算已用额度 or 余额
                val balance =
                    if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                        // 信用卡账户，已用额度 - 记录金额
                        asset.balance - record.amount
                    } else {
                        // 非信用卡账户，余额 + 记录金额
                        asset.balance + record.amount
                    }
                // 更新资产
                updateAsset(asset.copy(balance = balance))
            }
        }

        val recordId = insertRecord(record.copy(finalAmount = recordAmount))

        // 插入新的关联标签
        insertRelatedTags(
            tagIdList.map { tagId ->
                TagWithRecordTable(id = null, recordId = recordId, tagId = tagId)
            },
        )

        // 插入新的关联图片
        insertRelatedImages(
            relatedImageList.map {
                ImageWithRelatedTable(
                    id = null,
                    recordId = recordId,
                    path = it.path,
                    bytes = it.bytes,
                )
            },
        )

        if (needRelated && relatedRecordIdList.isNotEmpty()) {
            // 更新关联记录
            insertRelatedRecord(
                relatedRecordIdList.map { relatedRecordId ->
                    RecordWithRelatedTable(
                        id = null,
                        recordId = recordId,
                        relatedRecordId = relatedRecordId,
                    )
                },
            )

            // 净自付：对新吸收者所在簇整体重算（替换旧的 recordAmount - Σ finalAmount 口径）
            recalculateFinalAmountForCluster(recordId)
        }
    }

    /**
     * 批量导入记录事务
     *
     * 在单个事务中插入多条记录并更新对应资产余额。
     *
     * @param records 要插入的记录列表
     * @return 插入后的记录 ID 列表
     */
    @Transaction
    suspend fun batchImportRecordsTransaction(
        records: List<RecordTable>,
    ): List<Long> {
        val insertedIds = mutableListOf<Long>()

        // 按资产分组汇总余额变化（使用 Long，与项目金额类型一致）
        data class BalanceChange(
            val assetId: Long,
            var incomeTotal: Long = 0L,
            var expenditureTotal: Long = 0L,
        )

        val balanceChanges = mutableMapOf<Long, BalanceChange>()

        for (record in records) {
            val type = queryTypeById(record.typeId) ?: continue
            val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)

            // 复用已有方法计算实际金额
            val recordAmount = calculateRecordAmount(record, category)

            // 插入记录
            val id = insertRecord(record.copy(finalAmount = recordAmount))
            insertedIds.add(id)

            // 累计余额变化
            if (record.assetId > 0) {
                val change = balanceChanges.getOrPut(record.assetId) {
                    BalanceChange(assetId = record.assetId)
                }
                if (category == RecordTypeCategoryEnum.INCOME) {
                    change.incomeTotal += recordAmount
                } else {
                    change.expenditureTotal += recordAmount
                }
            }
        }

        // 批量更新资产余额
        for ((assetId, change) in balanceChanges) {
            val asset = queryAssetById(assetId) ?: continue
            val isCreditCard = ClassificationTypeEnum.ordinalOf(asset.type) ==
                ClassificationTypeEnum.CREDIT_CARD_ACCOUNT

            val balance = if (isCreditCard) {
                // 信用卡：收入减少已用额度，支出增加已用额度
                asset.balance - change.incomeTotal + change.expenditureTotal
            } else {
                // 非信用卡：收入增加余额，支出减少余额
                asset.balance + change.incomeTotal - change.expenditureTotal
            }

            updateAsset(asset.copy(balance = balance))
        }

        return insertedIds
    }

    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordTransaction(recordId: Long?) {
        if (null == recordId) {
            return
        }
        // 获取记录信息
        val record =
            queryRecordById(recordId) ?: throw DataTransactionException("Record id not found")
        deleteRecordTransaction(record)
    }

    /**
     * 删除已有记录
     *
     * - 计算金额（非finalAmount）后更新资产金额
     * - 转账类型更新目标资产金额
     * - 移除和记录关联的所有标签数据
     * - 更新与记录关联的所有记录 finalAmount 字段
     * - 移除和记录关联的所有关联记录数据
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordTransaction(record: RecordTable) {
        val recordId = record.id ?: return
        val type = resolveType(record.typeId)
            ?: throw DataTransactionException("Type must not be null")
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        val oldRecordAmount = calculateRecordAmount(record, category)
        // 更新资产余额
        queryAssetById(record.assetId)?.let { asset ->
            // 计算回退已用额度 or 余额
            val balance =
                if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    // 信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，已用额度 + 记录金额
                        asset.balance + oldRecordAmount
                    } else {
                        // 支出、转账，已用额度 - 记录金额
                        asset.balance - oldRecordAmount
                    }
                } else {
                    // 非信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，余额 - 记录金额
                        asset.balance - oldRecordAmount
                    } else {
                        // 支出、转账，余额 + 记录金额
                        asset.balance + oldRecordAmount
                    }
                }
            // 更新资产
            updateAsset(asset.copy(balance = balance))
        }
        if (category == RecordTypeCategoryEnum.TRANSFER) {
            // 转账，更新关联资产余额
            queryAssetById(record.intoAssetId)?.let { asset ->
                // 计算回退已用额度 or 余额
                val balance =
                    if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                        // 信用卡账户，已用额度 + 记录金额
                        asset.balance + record.amount
                    } else {
                        // 非信用卡账户，余额 - 记录金额
                        asset.balance - record.amount
                    }
                // 更新资产
                updateAsset(asset.copy(balance = balance))
            }
        }

        // 移除关联标签
        deleteOldRelatedTags(recordId)

        // 移除关联照片
        deleteOldRelatedImages(recordId)

        // 更新关联记录的 finalAmount
        if (category == RecordTypeCategoryEnum.EXPENDITURE) {
            // 删除支出记录，重算所有吸收了该支出的收入记录的 finalAmount
            queryRelatedByRelatedRecordId(recordId).forEach { relation ->
                // 排除即将被删除的支出记录（关联尚未清除）
                recalculateAbsorberFinalAmount(relation.recordId, excludeAbsorbedId = recordId)
            }
        } else if (category == RecordTypeCategoryEnum.INCOME) {
            // 删除收入（吸收者）记录，需要更新被关联的支出记录
            val absorbedRelations = queryRelatedByRecordId(recordId)
            for (relation in absorbedRelations) {
                val expenseId = relation.relatedRecordId
                val expense = queryRecordById(expenseId) ?: continue
                // 检查该支出是否还有其他吸收者
                val remainingAbsorbers = queryRelatedByRelatedRecordId(expenseId)
                    .filter { it.recordId != recordId }
                if (remainingAbsorbers.isEmpty()) {
                    // 无其他吸收者，恢复支出的完整金额
                    val expenseType = resolveType(expense.typeId) ?: continue
                    val expenseCategory = RecordTypeCategoryEnum.ordinalOf(expenseType.typeCategory)
                    val fullAmount = calculateRecordAmount(expense, expenseCategory)
                    updateRecordFinalAmountById(expenseId, fullAmount)
                } else {
                    // 还有其他吸收者，支出保持 finalAmount = 0
                    // 重算每个剩余吸收者的 finalAmount
                    for (absorber in remainingAbsorbers) {
                        recalculateAbsorberFinalAmount(absorber.recordId)
                    }
                }
            }
        }

        // 移除关联记录
        clearRelatedRecordById(recordId)

        // 删除当前记录
        val result = deleteRecord(record)
        if (result <= 0) {
            throw DataTransactionException("Record delete failed!")
        }
    }

    @Query(
        value = """
        UPDATE db_record SET final_amount=:finalAmount WHERE id=:id
    """,
    )
    suspend fun updateRecordFinalAmountById(id: Long, finalAmount: Long)

    @Query(
        value = """
        SELECT * FROM db_record WHERE books_id=:bookId
    """,
    )
    suspend fun queryRecordListByBookId(bookId: Long): List<RecordTable>

    @Query(
        """
        DELETE FROM db_record_with_related
        WHERE record_id=:recordId OR related_record_id=:recordId
    """,
    )
    suspend fun deleteRecordRelationByRecordId(recordId: Long)

    @Query(
        value = """
        DELETE FROM db_tag_with_record
        WHERE record_id=:recordId
    """,
    )
    suspend fun deleteTagRelationByRecordId(recordId: Long)

    @Query(
        value = """
        DELETE FROM db_tag_with_record
        WHERE tag_id=:tagId
    """,
    )
    suspend fun deleteTagRelationByTagId(tagId: Long)

    @Query(
        value = """
        DELETE FROM db_tag
        WHERE id=:tagId
    """,
    )
    suspend fun deleteTagById(tagId: Long)

    @Query(
        value = """
       DELETE FROM db_books
        WHERE id=:bookId
    """,
    )
    suspend fun deleteBookById(bookId: Long)

    @Query("DELETE FROM db_tag_with_record WHERE record_id IN (SELECT id FROM db_record WHERE books_id = :bookId)")
    suspend fun deleteTagRelationsByBookId(bookId: Long)

    @Query("DELETE FROM db_record_with_related WHERE record_id IN (SELECT id FROM db_record WHERE books_id = :bookId) OR related_record_id IN (SELECT id FROM db_record WHERE books_id = :bookId)")
    suspend fun deleteRecordRelationsByBookId(bookId: Long)

    @Query("DELETE FROM db_image_with_related WHERE record_id IN (SELECT id FROM db_record WHERE books_id = :bookId)")
    suspend fun deleteImageRelationsByBookId(bookId: Long)

    @Query("DELETE FROM db_record WHERE books_id = :bookId")
    suspend fun deleteRecordsByBookId(bookId: Long)

    /**
     * 事务化删除账本及其所有关联数据
     *
     * 逐条删除记录以确保正确回退所有资产余额（包括跨账本转账的对方资产）
     * 并正确处理关联记录的 finalAmount 重算
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteBookTransaction(bookId: Long) {
        // 逐条删除记录，正确回退所有关联资产余额及 finalAmount
        val records = queryRecordListByBookId(bookId)
        for (record in records) {
            deleteRecordTransaction(record)
        }
        // 删除该账本下的所有标签
        deleteTagsByBookId(bookId)
        // 删除该账本下的所有资产
        deleteAssetsByBookId(bookId)
        // 删除账本
        deleteBookById(bookId)
    }

    @Query("UPDATE db_record SET type_id = :newTypeId WHERE type_id = :oldTypeId")
    suspend fun updateRecordTypeId(oldTypeId: Long, newTypeId: Long)

    @Query("UPDATE db_type SET parent_id = -1, type_level = 0 WHERE parent_id = :parentId")
    suspend fun promoteChildTypes(parentId: Long)

    @Query("SELECT COUNT(*) FROM db_record WHERE type_id = :typeId")
    suspend fun countRecordsByTypeId(typeId: Long): Int

    @Query("DELETE FROM db_type WHERE id = :typeId")
    suspend fun deleteTypeById(typeId: Long)

    /**
     * 事务化迁移类型记录：将旧类型的记录迁移到固定类型，并清理旧类型
     */
    @Transaction
    suspend fun migrateTypeRecords(oldTypeId: Long, fixedTypeId: Long) {
        // 更新记录的 type_id
        updateRecordTypeId(oldTypeId, fixedTypeId)
        // 将旧类型的子类型提升为一级类型
        promoteChildTypes(oldTypeId)
        // 如果旧类型没有剩余记录引用，则删除
        val remainingCount = countRecordsByTypeId(oldTypeId)
        if (remainingCount == 0) {
            deleteTypeById(oldTypeId)
        }
    }

    @Transaction
    suspend fun deleteTag(id: Long) {
        deleteTagRelationByTagId(id)
        deleteTagById(id)
    }

    /** 删除资产关联的标签记录关联 */
    @Query(
        value = """
        DELETE FROM db_tag_with_record
        WHERE record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
    """,
    )
    suspend fun deleteTagRelationsByAssetId(assetId: Long)

    /** 删除资产关联的记录关联数据 */
    @Query(
        value = """
        DELETE FROM db_record_with_related
        WHERE record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
        OR related_record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
    """,
    )
    suspend fun deleteRecordRelationsByAssetId(assetId: Long)

    /** 删除资产关联的图片关联数据 */
    @Query(
        value = """
        DELETE FROM db_image_with_related
        WHERE record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
    """,
    )
    suspend fun deleteImageRelationsByAssetId(assetId: Long)

    /** 删除资产关联的记录 */
    @Query(
        value = """
        DELETE FROM db_record
        WHERE asset_id=:assetId OR into_asset_id=:assetId
    """,
    )
    suspend fun deleteRecordsByAssetId(assetId: Long)

    /**
     * 事务化删除资产关联的所有数据
     *
     * 逐条删除记录以确保正确回退对方资产余额（特别是转账场景）
     * 并正确处理关联记录的 finalAmount 重算
     */
    @Transaction
    suspend fun deleteAssetRelatedData(assetId: Long) {
        // 逐条删除，正确回退对方资产余额及 finalAmount
        val records = queryRecordsByAssetId(assetId)
        for (record in records) {
            deleteRecordTransaction(record)
        }
    }
}
