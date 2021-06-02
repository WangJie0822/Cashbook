package cn.wj.android.cashbook.data.store

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.liveData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.dao.BooksDao
import cn.wj.android.cashbook.data.database.table.BooksTable
import cn.wj.android.cashbook.data.entity.AssetClassificationListEntity
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.source.BillPagingSource
import cn.wj.android.cashbook.data.transform.toBooksEntity
import cn.wj.android.cashbook.data.transform.toBooksTable
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

    /** 将账本数据 [books] 插入到数据库中 */
    suspend fun insertBooks(vararg books: BooksEntity) = withContext(Dispatchers.IO) {
        val ls = arrayListOf<BooksTable>()
        books.forEach {
            ls.add(it.toBooksTable())
        }
        booksDao.insert(*ls.toTypedArray())
    }

    /** 将账本数据 [books] 插入到数据库中 */
    suspend fun insertBooks(books: BooksEntity) = withContext(Dispatchers.IO) {
        booksDao.insert(books.toBooksTable())
    }

    /** 从数据库中删除 [books] */
    suspend fun deleteBooks(vararg books: BooksEntity) = withContext(Dispatchers.IO) {
        val ls = arrayListOf<BooksTable>()
        books.forEach {
            ls.add(it.toBooksTable())
        }
        booksDao.delete(*ls.toTypedArray())
    }

    /** 从数据库中删除 [books] */
    suspend fun updateBooks(vararg books: BooksEntity) = withContext(Dispatchers.IO) {
        val ls = arrayListOf<BooksTable>()
        books.forEach {
            ls.add(it.toBooksTable())
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

    fun getBillList() = Pager(
        config = PagingConfig(20),
        pagingSourceFactory = { BillPagingSource() }
    ).liveData

    /** 获取资产分类列表 */
    suspend fun getAssetClassificationList() = withContext(Dispatchers.IO) {
        arrayListOf(
            // 资金账户
            AssetClassificationListEntity(
                groupNameResId = R.string.asset_classifications_capital_account,
                classifications = arrayListOf(*AssetClassificationEnum.CAPITAL_ACCOUNT)
            ),
            // 信用卡账户
            AssetClassificationListEntity(
                groupNameResId = R.string.asset_classifications_credit_card_account,
                classifications = arrayListOf(*AssetClassificationEnum.CREDIT_CARD_ACCOUNT)
            ),
            // 充值账户
            AssetClassificationListEntity(
                groupNameResId = R.string.asset_classifications_top_up_account,
                classifications = arrayListOf(*AssetClassificationEnum.TOP_UP_ACCOUNT)
            ),
            // 投资理财账户
            AssetClassificationListEntity(
                groupNameResId = R.string.asset_classifications_investment_financial_account,
                classifications = arrayListOf(*AssetClassificationEnum.INVESTMENT_FINANCIAL_ACCOUNT)
            ),
            // 债务
            AssetClassificationListEntity(
                groupNameResId = R.string.asset_classifications_debt_account,
                classifications = arrayListOf(*AssetClassificationEnum.DEBT_ACCOUNT)
            )
        )
    }


    /** 获取银行列表 */
    suspend fun getBankList() = withContext(Dispatchers.IO) {
        arrayListOf(*AssetClassificationEnum.BANK_LIST)
    }
}