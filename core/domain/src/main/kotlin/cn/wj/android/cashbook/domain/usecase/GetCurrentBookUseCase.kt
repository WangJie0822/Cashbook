package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.entity.BooksEntity
import cn.wj.android.cashbook.core.model.transfer.asEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 获取当前账本数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetCurrentBookUseCase @Inject constructor(
    private val booksRepository: BooksRepository,
    private val appPreferencesDataSource: AppPreferencesDataSource,
) {

    operator fun invoke(): Flow<BooksEntity> =
        combine(booksRepository.booksListData, appPreferencesDataSource.appData) { list, appData ->
            var selected = list.firstOrNull { it.id == appData.currentBookId }
            if (null == selected) {
                // 没有找到当前账本，默认选择第一个
                selected = list.first()
                appPreferencesDataSource.updateCurrentBookId(selected.id)
            }
            selected.asEntity()
        }
}