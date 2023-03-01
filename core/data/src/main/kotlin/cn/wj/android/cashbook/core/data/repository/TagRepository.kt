package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.model.DataVersion
import cn.wj.android.cashbook.core.database.dao.TagDao
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.model.model.TagModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TagRepository @Inject constructor(
    private val tagDao: TagDao
) {

    private val dataVersion = DataVersion()

    val tagListData: Flow<List<TagModel>> = dataVersion.map {
        getAllTagList()
    }

    private suspend fun getAllTagList(): List<TagModel> = withContext(Dispatchers.IO) {
        tagDao.queryAll()
            .map {
                it.asModel()
            }
    }

    suspend fun updateTag(tag: TagModel) = withContext(Dispatchers.IO) {
        val tagTable = tag.asTable()
        if (null == tagTable.id) {
            tagDao.insert(tagTable)
        } else {
            tagDao.update(tagTable)
        }
        dataVersion.update()
    }

    suspend fun deleteTag(tag: TagModel) = withContext(Dispatchers.IO) {
        val tagTable = tag.asTable()
        tagDao.delete(tagTable)
        dataVersion.update()
    }

    private fun TagTable.asModel(): TagModel {
        return TagModel(
            id = this.id ?: -1L,
            name = this.name,
        )
    }

    private fun TagModel.asTable(): TagTable {
        return TagTable(
            id = if (this.id == -1L) null else this.id,
            name = this.name,
            booksId = -1L,
        )
    }

}