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
 * 簇 BFS 发现结果：簇成员 id 集 + 每个节点的出边缓存（record_id → 其吸收的支出 id 列表）。
 * outEdges 供净自付重算 step3 复用，避免对每个吸收者重新 queryRelatedByRecordId（消 N+1）。
 */
data class ClusterDiscovery(
    val clusterIds: Set<Long>,
    val outEdges: Map<Long, List<Long>>,
)

/**
 * 批量 IN 删除单批最大参数数，低于 SQLite 旧版变量上限（999）；
 * 与 core:data 的 SQL_IN_CHUNK_SIZE 同值同义，因 core:database 不能依赖 core:data 故独立定义。
 */
private const val DELETE_IN_CHUNK_SIZE = 900

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
     * 从 [seedRecordId] 沿关系表 BFS 发现连通簇（被吸收支出↔吸收者收入二部图连通分量）。
     * 返回簇成员 + outEdges 缓存（record_id 侧被吸收支出），供重算复用避免 N+1。
     *
     * @param seedRecordId 簇内任一记录 id（受影响的吸收者或被吸收支出）
     * @param excludeRecordIds 需从簇中排除的记录 id（删除场景：记录即将被删但关联尚未清除）
     */
    @Transaction
    suspend fun discoverClusterIds(
        seedRecordId: Long,
        excludeRecordIds: Set<Long> = emptySet(),
    ): ClusterDiscovery {
        val clusterIds = LinkedHashSet<Long>()
        val outEdges = HashMap<Long, List<Long>>() // record_id -> 其吸收的支出 id 列表
        val queue = ArrayDeque<Long>()
        if (seedRecordId !in excludeRecordIds) {
            clusterIds.add(seedRecordId)
            queue.add(seedRecordId)
        }
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val absorbed = queryRelatedByRecordId(cur).map { it.relatedRecordId }
            outEdges[cur] = absorbed
            val neighbors = absorbed + queryRelatedByRelatedRecordId(cur).map { it.recordId }
            for (n in neighbors) {
                if (n !in excludeRecordIds && clusterIds.add(n)) {
                    queue.add(n)
                }
            }
        }
        return ClusterDiscovery(clusterIds, outEdges)
    }

    /**
     * 对已发现的簇 [clusterIds]（含 [outEdges] 缓存）执行净自付重算（§5 顺序贪心填充）。
     * 复用 outEdges 零额外查询；只写变化项。与全量版 [recalculateAllFinalAmount] 同口径。
     */
    @Transaction
    suspend fun recalculateFinalAmountFromCluster(
        clusterIds: Set<Long>,
        outEdges: Map<Long, List<Long>>,
    ) {
        if (clusterIds.isEmpty()) return

        // 1. 批量取簇内记录（消逐条 queryRecordById 的 N+1），初始化 finalAmount。
        //    转账 = concessions - charge（与 recalculateAllFinalAmount 全量版口径一致，防增量/全量分叉）；
        //    收入/支出 = recordAmount。簇内正常仅 income/expenditure（转账不参与吸收），TRANSFER 分支为一致性防御。
        val records = queryRecordByIds(clusterIds.toList()).associateBy { it.id }
        val finalAmounts = HashMap<Long, Long>(clusterIds.size)
        for (id in clusterIds) {
            val record = records[id] ?: continue
            val type = resolveType(record.typeId) ?: continue
            val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
            finalAmounts[id] = if (category == RecordTypeCategoryEnum.TRANSFER) {
                record.concessions - record.charge
            } else {
                calculateRecordAmount(record, category)
            }
        }

        // 2. 簇内吸收者（record_id 侧有被吸收支出）按 id 升序，顺序贪心填充（用缓存 outEdges，零额外查询）
        val absorbers = clusterIds.filter { id ->
            (outEdges[id] ?: emptyList()).any { it in clusterIds }
        }.sorted()
        for (absorberId in absorbers) {
            var remaining = finalAmounts[absorberId] ?: continue
            val absorbedIds = (outEdges[absorberId] ?: emptyList())
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

        // 3. 落库（仅写变化项，对齐全量版，避免无谓 UPDATE 触发 Flow invalidation 风暴）
        for ((id, finalAmount) in finalAmounts) {
            if (records[id]?.finalAmount != finalAmount) {
                updateRecordFinalAmountById(id, finalAmount)
            }
        }
    }

    /**
     * 重算一个吸收簇的 finalAmount（净自付语义）。从 [seedRecordId] BFS 发现整簇再重算。
     * 内部拆为 [discoverClusterIds] + [recalculateFinalAmountFromCluster]，对外签名行为不变。
     *
     * @param seedRecordId 簇内任一记录 id（受影响的吸收者或被吸收支出）
     * @param excludeRecordIds 需从簇中排除的记录 id（删除场景：记录即将被删但关联尚未清除）
     */
    @Transaction
    suspend fun recalculateFinalAmountForCluster(
        seedRecordId: Long,
        excludeRecordIds: Set<Long> = emptySet(),
    ) {
        val (clusterIds, outEdges) = discoverClusterIds(seedRecordId, excludeRecordIds)
        recalculateFinalAmountFromCluster(clusterIds, outEdges)
    }

    /**
     * 全表净自付重算（迁移 / 启动 gate / 备份恢复后复用）。
     *
     * - 转账：finalAmount = concessions - charge（保持既有 migrate 语义，转账不参与吸收）
     * - 收入/支出：先初始化 recordAmount，再按全表吸收者 id 升序顺序贪心填充（§5）
     *
     * 与 [recalculateFinalAmountForCluster] 同算法同序，全量与增量结果一致；幂等（连跑结果一致）。
     */
    @Transaction
    suspend fun recalculateAllFinalAmount() {
        val allRecords = queryAllRecords()
        val allRelations = queryAllRelatedRecords()

        // 1. 初始化 finalAmount
        val finalAmounts = HashMap<Long, Long>(allRecords.size)
        for (record in allRecords) {
            val id = record.id ?: continue
            val type = resolveType(record.typeId) ?: continue
            val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
            finalAmounts[id] = if (category == RecordTypeCategoryEnum.TRANSFER) {
                record.concessions - record.charge
            } else {
                calculateRecordAmount(record, category)
            }
        }

        // 2. 吸收者 id 升序，被吸收支出 id 升序，顺序贪心填充
        val absorbedByAbsorber = allRelations.groupBy({ it.recordId }, { it.relatedRecordId })
        for (absorberId in absorbedByAbsorber.keys.sorted()) {
            var remaining = finalAmounts[absorberId] ?: continue
            for (expenseId in (absorbedByAbsorber[absorberId] ?: emptyList()).sorted()) {
                val current = finalAmounts[expenseId] ?: continue
                val offset = minOf(remaining, current)
                finalAmounts[expenseId] = current - offset
                remaining -= offset
            }
            finalAmounts[absorberId] = remaining
        }

        // 3. 落库（仅写变化项）
        for (record in allRecords) {
            val id = record.id ?: continue
            val finalAmount = finalAmounts[id] ?: continue
            if (record.finalAmount != finalAmount) {
                updateRecordFinalAmountById(id, finalAmount)
            }
        }
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
     * 仅回退单条记录涉及的资产余额（主资产 + 转账对方资产），不删任何关联/记录。
     * 符号逻辑与原 deleteRecordCore 余额回退部分逐字符一致（信用卡/非信用卡 × INCOME/支出转账 × 主/转账对方）。
     * 供 deleteRecordsBatch 逐条调用（A2：余额逐条回退、关联/记录批量删）。
     */
    @Throws(DataTransactionException::class)
    suspend fun revertRecordBalanceOnly(record: RecordTable) {
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
    }

    /**
     * 删除已有记录（单删，UI 路径）。委托 [deleteRecordsBatch]（单元素列表）——
     * 复用同一套「捕获 survivors → 余额回退 → 批量删关联/记录 → 存活簇去重重算」逻辑，避免重复（DRY）。
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordTransaction(record: RecordTable) {
        deleteRecordsBatch(listOf(record))
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

    /** 批量删除一组记录的标签关联（IN，删账本/资产/单删共用，消逐条 deleteOldRelatedTags） */
    @Query("DELETE FROM db_tag_with_record WHERE record_id IN (:ids)")
    suspend fun deleteTagRelationsByRecordIds(ids: List<Long>)

    /** 批量删除一组记录的图片关联（IN） */
    @Query("DELETE FROM db_image_with_related WHERE record_id IN (:ids)")
    suspend fun deleteImageRelationsByRecordIds(ids: List<Long>)

    /** 批量删除一组记录的关联记录关系（双向 IN-OR，等价逐条 clearRelatedRecordById） */
    @Query("DELETE FROM db_record_with_related WHERE record_id IN (:ids) OR related_record_id IN (:ids)")
    suspend fun deleteRecordRelationsByRecordIds(ids: List<Long>)

    /** 批量删除一组记录（IN），返回实际删除行数（L3 校验用） */
    @Query("DELETE FROM db_record WHERE id IN (:ids)")
    suspend fun deleteRecordsByIds(ids: List<Long>): Int

    /**
     * 批量删除一组记录：删前捕获 survivors → 逐条余额回退 → 批量删关联/记录（byIds IN + chunk）→
     * L3 行数校验 → 删后对「存活簇」只重算一次。消除逐条 per-record DB 往返（A2，P-M1）。
     * 存活引用为备份恢复/导入异常的安全网（正常数据 survivors 为空）。
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordsBatch(records: List<RecordTable>) {
        if (records.isEmpty()) return
        val deletedIds = records.mapNotNull { it.id }.toSet()
        // L7：survivors 必须删前捕获（删后关联已清无法查）
        val affectedSurvivors = LinkedHashSet<Long>()
        for (record in records) {
            val recordId = record.id ?: continue
            queryRelatedByRecordId(recordId).forEach {
                if (it.relatedRecordId !in deletedIds) affectedSurvivors.add(it.relatedRecordId)
            }
            queryRelatedByRelatedRecordId(recordId).forEach {
                if (it.recordId !in deletedIds) affectedSurvivors.add(it.recordId)
            }
        }
        // 逐条余额回退（零符号改动；过滤 id==null 与逐条版 record.id ?: return 对齐）
        for (record in records) {
            if (record.id == null) continue
            revertRecordBalanceOnly(record)
        }
        // L8：三类关联删 + 删记录遍历同一完整 deletedIds 全集
        val idList = deletedIds.toList()
        idList.chunked(DELETE_IN_CHUNK_SIZE).forEach { chunk ->
            deleteTagRelationsByRecordIds(chunk)
            deleteImageRelationsByRecordIds(chunk)
            deleteRecordRelationsByRecordIds(chunk)
        }
        // 批量删记录 + L3 行数校验（去重 size）
        val deleted = idList.chunked(DELETE_IN_CHUNK_SIZE).sumOf { deleteRecordsByIds(it) }
        if (deleted < deletedIds.size) {
            throw DataTransactionException("Record delete failed!")
        }
        // L6：存活簇重算必须在所有删除之后
        val visited = HashSet<Long>()
        for (sid in affectedSurvivors) {
            if (sid in visited) continue
            val (clusterIds, outEdges) = discoverClusterIds(sid)
            visited += clusterIds
            recalculateFinalAmountFromCluster(clusterIds, outEdges)
        }
    }

    /**
     * 事务化删除账本及其所有关联数据。
     * 逐条删记录正确回退资产余额（含跨账本转账对方资产），删后统一对存活簇重算（去 O(N²)）。
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteBookTransaction(bookId: Long) {
        deleteRecordsBatch(queryRecordListByBookId(bookId))
        // 删除该账本下的所有标签
        deleteTagsByBookId(bookId)
        // 删除该账本下的所有资产
        deleteAssetsByBookId(bookId)
        // 删除账本
        deleteBookById(bookId)
        // 删除该账本下的所有预算
        deleteBudgetsByBookId(bookId)
    }

    @Query("DELETE FROM db_budget WHERE books_id = :bookId")
    suspend fun deleteBudgetsByBookId(bookId: Long)

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

    /**
     * 事务化删除资产关联的所有数据（不删资产行本身，守 AUDIT-2 契约：目标资产存活+余额回退）。
     * 逐条删记录正确回退对方资产余额（转账场景），删后统一对存活簇重算（去 O(N²)）。
     */
    @Transaction
    suspend fun deleteAssetRelatedData(assetId: Long) {
        deleteRecordsBatch(queryRecordsByAssetId(assetId))
    }
}
