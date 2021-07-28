package cn.wj.android.cashbook.data.repository

import cn.wj.android.cashbook.base.ext.base.formatToNumber
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.toBigDecimalOrZero
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.dao.AssetDao
import cn.wj.android.cashbook.data.database.dao.BooksDao
import cn.wj.android.cashbook.data.database.dao.RecordDao
import cn.wj.android.cashbook.data.database.dao.TagDao
import cn.wj.android.cashbook.data.database.dao.TypeDao
import cn.wj.android.cashbook.data.database.table.RecordTable
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.transform.toAssetEntity
import cn.wj.android.cashbook.data.transform.toTagEntity
import cn.wj.android.cashbook.data.transform.toTypeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据仓库基本类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/28
 */
abstract class Repository(protected val database: CashbookDatabase) {

    /** 账本数据库操作对象 */
    protected val booksDao: BooksDao by lazy {
        database.booksDao()
    }

    /** 资产数据库操作对象 */
    protected val assetDao: AssetDao by lazy {
        database.assetDao()
    }

    /** 类型数据库操作对象 */
    protected val typeDao: TypeDao by lazy {
        database.typeDao()
    }

    /** 记录数据库操作对象 */
    protected val recordDao: RecordDao by lazy {
        database.recordDao()
    }

    /** 标签数据库操作对象 */
    protected val tagDao: TagDao by lazy {
        database.tagDao()
    }


    /** 获取 id 为 [assetId] 的资产余额 */
    protected suspend fun getAssetBalanceById(assetId: Long?, creditCard: Boolean): String = withContext(Dispatchers.IO) {
        if (null == assetId) {
            return@withContext "0"
        }
        val modifyList = recordDao.queryLastModifyRecord(assetId)
        if (modifyList.isEmpty()) {
            return@withContext "0"
        }
        // 获取最后一条修改数据
        val lastModify = modifyList.first()
        // 获取在此之后的所有记录
        var result = lastModify.amount.toBigDecimalOrZero()
        val recordList = recordDao.queryAfterRecordTime(assetId, lastModify.recordTime)
        recordList.forEach {
            when (it.typeEnum) {
                RecordTypeEnum.INCOME.name -> {
                    // 收入
                    if (creditCard) {
                        // 信用卡，降低欠款
                        result -= it.amount.toBigDecimalOrZero()
                    } else {
                        result += it.amount.toBigDecimalOrZero()
                    }
                }
                RecordTypeEnum.EXPENDITURE.name -> {
                    // 支出
                    if (creditCard) {
                        // 信用卡，增加欠款
                        result += it.amount.toBigDecimalOrZero()
                    } else {
                        result -= it.amount.toBigDecimalOrZero()
                    }
                }
                RecordTypeEnum.TRANSFER.name -> {
                    // 转账转出
                    if (creditCard) {
                        // 信用卡，增加欠款
                        result += it.amount.toBigDecimalOrZero()
                        result += it.charge.toBigDecimalOrZero()
                    } else {
                        result -= it.amount.toBigDecimalOrZero()
                        result -= it.charge.toBigDecimalOrZero()
                    }
                }
            }
        }
        // 查询转账转入数据
        val transferRecordList = recordDao.queryByIntoAssetIdAfterRecordTime(assetId, lastModify.recordTime)
        transferRecordList.forEach {
            when (it.typeEnum) {
                RecordTypeEnum.TRANSFER.name -> {
                    // 转账转入
                    if (creditCard) {
                        // 信用卡，减少欠款
                        result -= it.amount.toBigDecimalOrZero()
                    } else {
                        result += it.amount.toBigDecimalOrZero()
                    }
                }
            }
        }
        result.formatToNumber()
    }

    /** 根据 [record] 数据获取 [RecordEntity] 数据并返回 */
    protected suspend fun loadRecordEntityFromTable(record: RecordTable?, showDate: Boolean): RecordEntity? = withContext(Dispatchers.IO) {
        if (null == record) {
            null
        } else {
            val assetTable = assetDao.queryById(record.assetId)
            val asset = assetTable?.toAssetEntity(getAssetBalanceById(record.assetId, assetTable.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.name))
            val intoAssetTable = assetDao.queryById(record.intoAssetId)
            val intoAsset = intoAssetTable?.toAssetEntity(getAssetBalanceById(record.intoAssetId, intoAssetTable.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.name))
            val typeTable = if (record.typeId < 0) null else typeDao.queryById(record.typeId)
            val type = if (null == typeTable) {
                null
            } else {
                if (typeTable.parentId < 0) {
                    typeTable.toTypeEntity(null)
                } else {
                    typeTable.toTypeEntity(typeDao.queryById(typeTable.parentId)?.toTypeEntity(null))
                }
            }
            RecordEntity(
                id = record.id.orElse(-1L),
                typeEnum = RecordTypeEnum.fromName(record.typeEnum).orElse(RecordTypeEnum.EXPENDITURE),
                type = type,
                asset = asset,
                intoAsset = intoAsset,
                booksId = record.booksId,
                record = loadRecordEntityFromTable(recordDao.queryById(record.recordId), false),
                beAssociated = loadBeAssociatedRecord(record.id.orElse(-1L)),
                amount = record.amount.toString(),
                charge = record.charge.toString(),
                remark = record.remark,
                tags = loadTagListFromIds(record.tagIds),
                reimbursable = record.reimbursable == SWITCH_INT_ON,
                system = record.system == SWITCH_INT_ON,
                recordTime = record.recordTime,
                createTime = record.createTime.dateFormat(),
                modifyTime = record.modifyTime.dateFormat(),
                showDate = showDate
            )
        }
    }

    protected suspend fun loadBeAssociatedRecord(id: Long): RecordEntity? = withContext(Dispatchers.IO) {
        if (id < 0) {
            return@withContext null
        }
        val record = recordDao.queryAssociatedById(id) ?: return@withContext null
        val assetTable = assetDao.queryById(record.assetId)
        val asset = assetTable?.toAssetEntity(getAssetBalanceById(record.assetId, assetTable.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.name))
        val intoAssetTable = assetDao.queryById(record.intoAssetId)
        val intoAsset = intoAssetTable?.toAssetEntity(getAssetBalanceById(record.intoAssetId, intoAssetTable.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.name))
        val typeTable = if (record.typeId < 0) null else typeDao.queryById(record.typeId)
        val type = if (null == typeTable) {
            null
        } else {
            if (typeTable.parentId < 0) {
                typeTable.toTypeEntity(null)
            } else {
                typeTable.toTypeEntity(typeDao.queryById(typeTable.parentId)?.toTypeEntity(null))
            }
        }
        RecordEntity(
            id = record.id.orElse(-1L),
            typeEnum = RecordTypeEnum.fromName(record.typeEnum).orElse(RecordTypeEnum.EXPENDITURE),
            type = type,
            asset = asset,
            intoAsset = intoAsset,
            booksId = record.booksId,
            record = null,
            beAssociated = null,
            amount = record.amount.toString(),
            charge = record.charge.toString(),
            remark = record.remark,
            tags = loadTagListFromIds(record.tagIds),
            reimbursable = record.reimbursable == SWITCH_INT_ON,
            system = record.system == SWITCH_INT_ON,
            recordTime = record.recordTime,
            createTime = record.createTime.dateFormat(),
            modifyTime = record.modifyTime.dateFormat(),
            showDate = true
        )
    }

    /** 从 id 列表获取 Tag 列表 */
    protected suspend fun loadTagListFromIds(ids: String): List<TagEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<TagEntity>()
        if (ids.isBlank()) {
            return@withContext result
        }
        val splits = ids.split(",")
        if (splits.isEmpty()) {
            return@withContext result
        }
        splits.forEach { id ->
            tagDao.queryById(id.toLongOrNull().orElse(-1L))?.let { table ->
                result.add(table.toTagEntity())
            }
        }
        return@withContext result
    }
}
