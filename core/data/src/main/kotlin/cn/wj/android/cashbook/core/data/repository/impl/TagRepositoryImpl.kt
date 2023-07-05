package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.model.tagDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.TagDao
import cn.wj.android.cashbook.core.model.model.TagModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    override val tagListData: Flow<List<TagModel>> = tagDataVersion.map {
        getAllTagList()
    }

    private suspend fun getAllTagList(coroutineContext: CoroutineContext = Dispatchers.IO): List<TagModel> =
        withContext(coroutineContext) {
            tagDao.queryAll()
                .map {
                    it.asModel()
                }
        }

    override suspend fun updateTag(tag: TagModel, coroutineContext: CoroutineContext) =
        withContext(coroutineContext) {
            val tagTable = tag.asTable()
            if (null == tagTable.id) {
                tagDao.insert(tagTable)
            } else {
                tagDao.update(tagTable)
            }
            tagDataVersion.updateVersion()
        }

    override suspend fun deleteTag(tag: TagModel, coroutineContext: CoroutineContext) =
        withContext(coroutineContext) {
            val tagTable = tag.asTable()
            tagDao.delete(tagTable)
            tagDataVersion.updateVersion()
        }

    override suspend fun getRelatedTag(
        recordId: Long,
        coroutineContext: CoroutineContext
    ): List<TagModel> = withContext(coroutineContext) {
        tagDao.queryByRecordId(recordId)
            .map { it.asModel() }
    }

    override suspend fun getTagById(tagId: Long, coroutineContext: CoroutineContext): TagModel? =
        withContext(coroutineContext) {
            tagListData.first().firstOrNull { it.id == tagId }
        }
}