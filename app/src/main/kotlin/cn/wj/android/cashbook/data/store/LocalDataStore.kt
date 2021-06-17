package cn.wj.android.cashbook.data.store

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.liveData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.toMoneyFloat
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_MONTH_DAY
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_YEAR_MONTH
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.toLongTime
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.dao.AssetDao
import cn.wj.android.cashbook.data.database.dao.BooksDao
import cn.wj.android.cashbook.data.database.dao.RecordDao
import cn.wj.android.cashbook.data.database.dao.TypeDao
import cn.wj.android.cashbook.data.database.table.BooksTable
import cn.wj.android.cashbook.data.database.table.RecordTable
import cn.wj.android.cashbook.data.database.table.TypeTable
import cn.wj.android.cashbook.data.entity.AssetClassificationListEntity
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.source.RecordPagingSource
import cn.wj.android.cashbook.data.transform.toAssetEntity
import cn.wj.android.cashbook.data.transform.toAssetTable
import cn.wj.android.cashbook.data.transform.toBooksEntity
import cn.wj.android.cashbook.data.transform.toRecordTable
import cn.wj.android.cashbook.data.transform.toTypeEntity
import cn.wj.android.cashbook.data.transform.toTypeTable
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地数据存储
 *
 * @param database 数据库对象
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/19
 */
class LocalDataStore(private val database: CashbookDatabase) {

    /** 账本数据库操作对象 */
    private val booksDao: BooksDao by lazy {
        database.booksDao()
    }

    /** 资产数据库操作对象 */
    private val assetDao: AssetDao by lazy {
        database.assetDao()
    }

    /** 类型数据库操作对象 */
    private val typeDao: TypeDao by lazy {
        database.typeDao()
    }

    /** 记录数据库操作对象 */
    private val recordDao: RecordDao by lazy {
        database.recordDao()
    }

    /** 将账本数据 [books] 插入到数据库中 */
    suspend fun insertBooks(books: BooksEntity) = withContext(Dispatchers.IO) {
        booksDao.insert(books.toAssetTable())
    }

    /** 从数据库中删除 [books] */
    suspend fun deleteBooks(books: BooksEntity) = withContext(Dispatchers.IO) {
        booksDao.delete(books.toAssetTable())
    }

    /** 从数据库中删除 [books] */
    suspend fun updateBooks(vararg books: BooksEntity) = withContext(Dispatchers.IO) {
        val ls = arrayListOf<BooksTable>()
        books.forEach {
            ls.add(it.toAssetTable())
        }
        booksDao.update(*ls.toTypedArray())
    }

    /** 获取名称为 [name] 的账本数量 */
    suspend fun getBooksCountByName(name: String) = withContext(Dispatchers.IO) {
        booksDao.getCountByName(name)
    }

    /** 获取所有账本数据并返回 */
    suspend fun getBooksList(): List<BooksEntity> = withContext(Dispatchers.IO) {
        booksDao.queryAll().map {
            it.toBooksEntity()
        }
    }

    /** 获取并返回默认选中的账本，没有返回 `null` */
    suspend fun getDefaultBooks(): BooksEntity? = withContext(Dispatchers.IO) {
        booksDao.queryDefault().firstOrNull()?.toBooksEntity()
    }

    /** 获取资产分类列表 */
    suspend fun getAssetClassificationList() = withContext(Dispatchers.IO) {
        arrayListOf(
            // 资金账户
            AssetClassificationListEntity(
                classificationType = ClassificationTypeEnum.CAPITAL_ACCOUNT,
                classifications = arrayListOf(*AssetClassificationEnum.CAPITAL_ACCOUNT)
            ),
            // 信用卡账户
            AssetClassificationListEntity(
                classificationType = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
                classifications = arrayListOf(*AssetClassificationEnum.CREDIT_CARD_ACCOUNT)
            ),
            // 充值账户
            AssetClassificationListEntity(
                classificationType = ClassificationTypeEnum.TOP_UP_ACCOUNT,
                classifications = arrayListOf(*AssetClassificationEnum.TOP_UP_ACCOUNT)
            ),
            // 投资理财账户
            AssetClassificationListEntity(
                classificationType = ClassificationTypeEnum.INVESTMENT_FINANCIAL_ACCOUNT,
                classifications = arrayListOf(*AssetClassificationEnum.INVESTMENT_FINANCIAL_ACCOUNT)
            ),
            // 债务
            AssetClassificationListEntity(
                classificationType = ClassificationTypeEnum.DEBT_ACCOUNT,
                classifications = arrayListOf(*AssetClassificationEnum.DEBT_ACCOUNT)
            )
        )
    }

    /** 获取银行列表 */
    suspend fun getBankList() = withContext(Dispatchers.IO) {
        arrayListOf(*AssetClassificationEnum.BANK_LIST)
    }

    /** 插入新的资产 [asset] 到数据库 */
    suspend fun insertAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        // 插入资产
        val assetId = assetDao.insert(asset.toAssetTable())
        // 插入余额记录
        recordDao.insert(RecordTable.newModifyBalance(assetId, asset.balance, R.string.new_asset.string, true))
        assetId
    }

    /** 更新资产 [asset] 到数据库 */
    suspend fun updateAsset(asset: AssetEntity, balanceChanged: Boolean = false) = withContext(Dispatchers.IO) {
        assetDao.update(asset.toAssetTable())
        if (balanceChanged) {
            // 插入余额记录
            recordDao.insert(RecordTable.newModifyBalance(asset.id, asset.balance))
        }
    }

    /** 获取资产数据并返回 */
    suspend fun getCurrentAssetList(): List<AssetEntity> = withContext(Dispatchers.IO) {
        assetDao.queryByBooksId(CurrentBooksLiveData.booksId).map {
            // 获取余额
            val balance = getAssetBalanceById(it.id)
            it.toAssetEntity(balance)
        }
    }

    /** 根据资产 id [assetId] 获取资产数据 */
    suspend fun findAssetById(assetId: Long?): AssetEntity? = withContext(Dispatchers.IO) {
        if (null == assetId || assetId < 0) {
            return@withContext null
        }
        val queryById = assetDao.queryById(assetId)
        queryById?.toAssetEntity(getAssetBalanceById(assetId))
    }

    /** 获取资产数据最大排序数字并返回 */
    suspend fun queryMaxSort(): Int? = withContext(Dispatchers.IO) {
        assetDao.queryMaxSortByBooksId(CurrentBooksLiveData.booksId)
    }

    /** 获取隐藏资产数据并返回 */
    suspend fun getInvisibleAssetList(): List<AssetEntity> = withContext(Dispatchers.IO) {
        assetDao.queryInvisibleByBooksId(CurrentBooksLiveData.booksId).map {
            //  获取余额
            val balance = getAssetBalanceById(it.id)
            it.toAssetEntity(balance)
        }
    }

    /** 获取未隐藏资产数据并返回 */
    suspend fun getVisibleAssetList(): List<AssetEntity> = withContext(Dispatchers.IO) {
        assetDao.queryVisibleByBooksId(CurrentBooksLiveData.booksId).map {
            //  获取余额
            val balance = getAssetBalanceById(it.id)
            it.toAssetEntity(balance)
        }
    }

    /** 返回数据库是否存在类型数据 */
    suspend fun hasType(): Boolean = withContext(Dispatchers.IO) {
        typeDao.getCount() > 0
    }

    /** 将 [type] 插入数据库并返回 id */
    suspend fun insertType(type: TypeEntity): Long = withContext(Dispatchers.IO) {
        typeDao.insert(type.toTypeTable())
    }

    /** 将 [types] 插入数据库 */
    suspend fun insertTypes(vararg types: TypeEntity) = withContext(Dispatchers.IO) {
        val ls = arrayListOf<TypeTable>()
        types.forEach {
            ls.add(it.toTypeTable())
        }
        typeDao.insert(*ls.toTypedArray())
    }

    /** 清空类型数据 */
    suspend fun clearTypes() = withContext(Dispatchers.IO) {
        typeDao.deleteAll()
    }

    /** 查询并返回记录类型为 [type] 的类型数据列表 */
    suspend fun getTypeListByType(type: RecordTypeEnum): List<TypeEntity> = withContext(Dispatchers.IO) {
        typeDao.queryByPosition(TypeEnum.FIRST.name, type.position).map { first ->
            first.toTypeEntity().copy(
                childList = typeDao.queryByParentId(TypeEnum.SECOND.name, first.id.orElse(-1L)).map { second ->
                    second.toTypeEntity()
                }.sortedBy {
                    it.sort
                }
            )
        }.sortedBy {
            it.sort
        }
    }

    /** 获取 id 为 [assetId] 的资产余额 */
    suspend fun getAssetBalanceById(assetId: Long?): String = withContext(Dispatchers.IO) {
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
        var result = lastModify.amount
        val recordList = recordDao.queryAfterRecordTime(assetId, lastModify.recordTime)
        recordList.forEach {
            when (it.type) {
                RecordTypeEnum.INCOME.name -> {
                    // 收入
                    result += it.amount
                }
                RecordTypeEnum.EXPENDITURE.name -> {
                    // 支出
                    result -= it.amount
                }
                RecordTypeEnum.TRANSFER.name -> {
                    // 转账
                    result -= it.charge
                    result -= it.charge
                }
            }
        }
        // 查询转账转入数据
        val transferRecordList = recordDao.queryByIntoAssetIdAfterRecordTime(assetId, lastModify.recordTime)
        transferRecordList.forEach {
            when (it.type) {
                RecordTypeEnum.TRANSFER.name -> {
                    // 转账
                    result += it.amount
                    result += it.charge
                }
            }
        }
        result.toString()
    }

    /** 将记录 [record] 插入到数据库并返回生成的主键 id */
    suspend fun insertRecord(record: RecordEntity): Long = withContext(Dispatchers.IO) {
        recordDao.insert(record.toRecordTable())
    }

    /** 更新 [record] 记录 */
    suspend fun updateRecord(record: RecordEntity) = withContext(Dispatchers.IO) {
        recordDao.update(record.toRecordTable())
    }

    /** 获取首页数据 */
    suspend fun getHomepageList(): List<DateRecordEntity> = withContext(Dispatchers.IO) {
        // 首页显示一周内数据
        val result = arrayListOf<DateRecordEntity>()
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val recordTime = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime(DATE_FORMAT_NO_SECONDS) ?: return@withContext result
        val list = recordDao.queryAfterRecordTimeByBooksId(CurrentBooksLiveData.booksId, recordTime).filter {
            it.system != SWITCH_INT_ON
        }
        val map = hashMapOf<String, MutableList<RecordEntity>>()
        for (item in list) {
            val dateKey = item.recordTime.dateFormat(DATE_FORMAT_MONTH_DAY)
            val dayInt = dateKey.split(".").lastOrNull()?.toIntOrNull().orElse(-1)
            val monthInt = dateKey.split(".").firstOrNull()?.toIntOrNull().orElse(-1)
            val key = dateKey + if (monthInt == month) {
                when (dayInt) {
                    today -> {
                        // 今天
                        " ${R.string.today.string}"
                    }
                    today - 1 -> {
                        // 昨天
                        " ${R.string.yesterday.string}"
                    }
                    today - 2 -> {
                        // 前天
                        " ${R.string.the_day_before_yesterday.string}"
                    }
                    else -> {
                        ""
                    }
                }
            } else {
                ""
            }
            val value = loadRecordEntityFromTable(item, false) ?: continue
            if (key.isNotBlank()) {
                if (map.containsKey(key)) {
                    map[key]!!.add(value)
                } else {
                    map[key] = arrayListOf(value)
                }
            }
        }
        map.keys.forEach { key ->
            result.add(
                DateRecordEntity(
                    date = key,
                    list = map[key].orEmpty().sortedBy { it.recordTime }.reversed()
                )
            )
        }
        result.sortedBy { it.date.toLongTime(DATE_FORMAT_MONTH_DAY) }.reversed()
    }

    /** 根据 [record] 数据获取 [RecordEntity] 数据并返回 */
    private suspend fun loadRecordEntityFromTable(record: RecordTable?, showDate: Boolean): RecordEntity? = withContext(Dispatchers.IO) {
        if (null == record) {
            null
        } else {
            val assetTable = assetDao.queryById(record.assetId)
            val asset = assetTable?.toAssetEntity(getAssetBalanceById(record.assetId))
            val intoAssetTable = assetDao.queryById(record.intoAssetId)
            val intoAsset = intoAssetTable?.toAssetEntity(getAssetBalanceById(record.intoAssetId))
            RecordEntity(
                id = record.id.orElse(-1L),
                type = RecordTypeEnum.fromName(record.type).orElse(RecordTypeEnum.EXPENDITURE),
                firstType = if (record.firstTypeId < 0) null else typeDao.queryById(record.firstTypeId)?.toTypeEntity(),
                secondType = if (record.secondTypeId < 0) null else typeDao.queryById(record.secondTypeId)?.toTypeEntity(),
                asset = asset,
                intoAsset = intoAsset,
                booksId = record.booksId,
                record = loadRecordEntityFromTable(recordDao.queryById(record.recordId), false),
                beAssociated = loadBeAssociatedRecord(record.id.orElse(-1L)),
                amount = record.amount.toString(),
                charge = record.charge.toString(),
                remark = record.remark,
                // TODO
                tags = arrayListOf(),
                reimbursable = record.reimbursable == SWITCH_INT_ON,
                system = record.system == SWITCH_INT_ON,
                recordTime = record.recordTime,
                createTime = record.createTime.dateFormat(),
                modifyTime = record.modifyTime.dateFormat(),
                showDate = showDate
            )
        }
    }

    private suspend fun loadBeAssociatedRecord(id: Long): RecordEntity? = withContext(Dispatchers.IO) {
        if (id < 0) {
            return@withContext null
        }
        val record = recordDao.queryAssociatedById(id) ?: return@withContext null
        val assetTable = assetDao.queryById(record.assetId)
        val asset = assetTable?.toAssetEntity(getAssetBalanceById(record.assetId))
        val intoAssetTable = assetDao.queryById(record.intoAssetId)
        val intoAsset = intoAssetTable?.toAssetEntity(getAssetBalanceById(record.intoAssetId))
        RecordEntity(
            id = record.id.orElse(-1L),
            type = RecordTypeEnum.fromName(record.type).orElse(RecordTypeEnum.EXPENDITURE),
            firstType = if (record.firstTypeId < 0) null else typeDao.queryById(record.firstTypeId)?.toTypeEntity(),
            secondType = if (record.secondTypeId < 0) null else typeDao.queryById(record.secondTypeId)?.toTypeEntity(),
            asset = asset,
            intoAsset = intoAsset,
            booksId = record.booksId,
            record = null,
            beAssociated = null,
            amount = record.amount.toString(),
            charge = record.charge.toString(),
            remark = record.remark,
            // TODO
            tags = arrayListOf(),
            reimbursable = record.reimbursable == SWITCH_INT_ON,
            system = record.system == SWITCH_INT_ON,
            recordTime = record.recordTime,
            createTime = record.createTime.dateFormat(),
            modifyTime = record.modifyTime.dateFormat(),
            showDate = true
        )
    }

    /** 获取当前月所有记录 */
    suspend fun getCurrentMonthRecord(): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        // 获取当前月开始时间
        val calendar = Calendar.getInstance()
        val monthStartDate = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_YEAR_MONTH)}-01 00:00:00".toLongTime() ?: return@withContext result
        recordDao.queryAfterRecordTimeByBooksId(CurrentBooksLiveData.booksId, monthStartDate).forEach { item ->
            val record = loadRecordEntityFromTable(item, false)
            if (null != record) {
                result.add(record)
            }
        }
        result
    }

    /** 获取最近三个月金额小于等于 [amount] 的所有支出记录 */
    suspend fun getLastThreeMonthExpenditureRecordLargerThanAmount(amount: String): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        // 获取最近三个月开始时间
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, -90)
        val startDate = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime() ?: return@withContext result
        recordDao.queryExpenditureRecordAfterDateLargerThanAmount(CurrentBooksLiveData.booksId, amount.toMoneyFloat(), startDate).forEach { item ->
            val record = loadRecordEntityFromTable(item, true)
            if (null != record) {
                result.add(record)
            }
        }
        result.filter {
            // 排除已关联的以及标记为可报销的
            null == it.beAssociated && !it.reimbursable
        }
    }

    /** 获取最近三个月标记为可报销的支出记录 */
    suspend fun getLastThreeMonthReimburseExpenditureRecord(): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        // 获取最近三个月开始时间
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, -90)
        val startDate = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime() ?: return@withContext result
        recordDao.queryReimburseExpenditureRecordAfterDate(CurrentBooksLiveData.booksId, startDate).forEach { item ->
            val record = loadRecordEntityFromTable(item, true)
            if (null != record) {
                result.add(record)
            }
        }
        result.filter {
            // 排除已关联的
            null == it.beAssociated
        }
    }

    /** 删除记录数据 [record] */
    suspend fun deleteRecord(record: RecordEntity) = withContext(Dispatchers.IO) {
        recordDao.delete(record.toRecordTable())
    }

    /** 根据资产id [assetId] 获取记录 Pager 数据 */
    fun getRecordListByAssetIdPagerData(assetId: Long) = Pager(
        config = PagingConfig(20),
        pagingSourceFactory = { RecordPagingSource(assetId, this) }
    ).liveData

    /** 根据资产 id [assetId] 页码 [pageNum] 每页数据 [pageSize] 获取记录数据 */
    suspend fun getRecordByAssetId(assetId: Long, pageNum: Int, pageSize: Int): List<DateRecordEntity> = withContext(Dispatchers.IO) {
        val list = recordDao.queryRecordByAssetId(assetId, pageNum * pageSize, pageSize)
        val map = hashMapOf<String, MutableList<RecordEntity>>()
        for (item in list) {
            val key = item.recordTime.dateFormat(DATE_FORMAT_YEAR_MONTH)
            val value = loadRecordEntityFromTable(item, true) ?: continue
            if (key.isNotBlank()) {
                if (map.containsKey(key)) {
                    map[key]!!.add(value)
                } else {
                    map[key] = arrayListOf(value)
                }
            }
        }
        val result = arrayListOf<DateRecordEntity>()
        map.keys.forEach { key ->
            result.add(
                DateRecordEntity(
                    date = key,
                    list = map[key].orEmpty().sortedBy { it.recordTime }.reversed()
                )
            )
        }
        result.sortedBy { it.date.toLongTime(DATE_FORMAT_YEAR_MONTH) }.reversed()
    }

    /** 从数据库中删除资产 [asset] 以及关联数据 */
    suspend fun deleteAsset(asset: AssetEntity) = withContext(Dispatchers.IO) {
        if (asset.id == -1L) {
            return@withContext
        }
        // 删除相关的转账、修改余额记录
        recordDao.deleteModifyAndTransferByAssetId(asset.id)
        // 删除资产信息
        assetDao.delete(asset.toAssetTable())
    }
}