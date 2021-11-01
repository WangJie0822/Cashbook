package cn.wj.android.cashbook.data.repository.asset

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.liveData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_YEAR_MONTH
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.toLongTime
import cn.wj.android.cashbook.data.constants.DEFAULT_PAGE_SIZE
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.table.RecordTable
import cn.wj.android.cashbook.data.entity.AssetClassificationListEntity
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.repository.Repository
import cn.wj.android.cashbook.data.source.AssetRecordPagingSource
import cn.wj.android.cashbook.data.transform.toAssetEntity
import cn.wj.android.cashbook.data.transform.toAssetTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 资产相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/28
 */
class AssetRepository(database: CashbookDatabase) : Repository(database) {

    /** 根据资产id [assetId] 获取记录 Pager 数据 */
    fun getRecordListByAssetIdPagerData(assetId: Long) = Pager(
        config = PagingConfig(DEFAULT_PAGE_SIZE),
        pagingSourceFactory = { AssetRecordPagingSource(assetId, this) }
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

    /** 更新资产 [ls] 到数据库 */
    suspend fun updateAssets(ls: List<AssetEntity>) = withContext(Dispatchers.IO) {
        val tables = ls.map {
            it.toAssetTable()
        }.toTypedArray()
        assetDao.update(*tables)
    }

    /** 根据资产 id [assetId] 获取资产数据 */
    suspend fun findAssetById(assetId: Long): AssetEntity? = withContext(Dispatchers.IO) {
        if (assetId < 0) {
            return@withContext null
        }
        val queryById = assetDao.queryById(assetId)
        queryById?.toAssetEntity(getAssetBalanceById(assetId, queryById.needNegative))
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

    /** 获取资产数据最大排序数字并返回 */
    suspend fun queryMaxSort(): Int? = withContext(Dispatchers.IO) {
        assetDao.queryMaxSortByBooksId()
    }

    /** 获取隐藏资产数据并返回 */
    suspend fun getInvisibleAssetList(): List<AssetEntity> = withContext(Dispatchers.IO) {
        assetDao.queryInvisibleByBooksId().map {
            //  获取余额
            val balance = getAssetBalanceById(it.id, it.needNegative)
            it.toAssetEntity(balance)
        }
    }

    /** 获取未隐藏资产数据通过记录数量排序 */
    suspend fun getVisibleAssetListSortByRecord(): List<AssetEntity> = withContext(Dispatchers.IO) {
        assetDao.queryVisibleByBooksId().map {
            //  获取余额
            val balance = getAssetBalanceById(it.id, it.needNegative)
            // 获取记录数
            val count = getRecordCountByAssetId(it.id)
            it.toAssetEntity(balance, count)
        }.sortedBy {
            it.sort
        }.reversed()
    }

    /** 获取资产数据并返回 */
    suspend fun getCurrentAssetList(): List<AssetEntity> = withContext(Dispatchers.IO) {
        assetDao.queryByBooksId().map {
            // 获取余额
            val balance = getAssetBalanceById(it.id, it.needNegative)
            it.toAssetEntity(balance)
        }
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
}