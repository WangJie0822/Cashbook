package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.model.tagDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.TagDao
import cn.wj.android.cashbook.core.model.model.TagModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : TagRepository {

    override val tagListData: Flow<List<TagModel>> = tagDataVersion.map {
        getAllTagList()
    }

    private suspend fun getAllTagList(): List<TagModel> =
        withContext(coroutineContext) {
            tagDao.queryAll()
                .map {
                    it.asModel()
                }
        }

    override suspend fun updateTag(tag: TagModel) =
        withContext(coroutineContext) {
            val tagTable = tag.asTable()
            if (null == tagTable.id) {
                tagDao.insert(tagTable)
            } else {
                tagDao.update(tagTable)
            }
            tagDataVersion.updateVersion()
        }

    override suspend fun deleteTag(tag: TagModel) =
        withContext(coroutineContext) {
            val tagTable = tag.asTable()
            tagDao.delete(tagTable)
            tagDataVersion.updateVersion()
        }

    override suspend fun getRelatedTag(recordId: Long): List<TagModel> =
        withContext(coroutineContext) {
            tagDao.queryByRecordId(recordId)
                .map { it.asModel() }
        }

    override suspend fun getTagById(tagId: Long): TagModel? =
        withContext(coroutineContext) {
            tagListData.first().firstOrNull { it.id == tagId }
        }
}