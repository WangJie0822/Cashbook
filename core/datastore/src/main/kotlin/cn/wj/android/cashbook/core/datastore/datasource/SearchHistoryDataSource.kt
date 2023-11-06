package cn.wj.android.cashbook.core.datastore.datasource

import androidx.datastore.core.DataStore
import cn.wj.android.cashbook.core.datastore.SearchHistory
import cn.wj.android.cashbook.core.datastore.copy
import cn.wj.android.cashbook.core.model.model.SearchHistoryModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

/**
 * 搜索历史记录数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/19
 */
@Singleton
class SearchHistoryDataSource @Inject constructor(
    private val searchHistory: DataStore<SearchHistory>
) {

    val searchHistoryData = searchHistory.data
        .map {
            SearchHistoryModel(
                keywords = it.keywordsList
            )
        }

    suspend fun updateKeywords(keywords: List<String>) {
        searchHistory.updateData {
            it.copy {
                this.keywords.clear()
                this.keywords.addAll(keywords)
            }
        }
    }
}