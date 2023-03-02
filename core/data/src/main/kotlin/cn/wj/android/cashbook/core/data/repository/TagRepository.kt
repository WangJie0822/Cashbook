package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.model.model.TagModel
import kotlinx.coroutines.flow.Flow

interface TagRepository {

    val tagListData: Flow<List<TagModel>>

    suspend fun updateTag(tag: TagModel)

    suspend fun deleteTag(tag: TagModel)
}

internal fun TagTable.asModel(): TagModel {
    return TagModel(
        id = this.id ?: -1L,
        name = this.name,
    )
}

internal fun TagModel.asTable(): TagTable {
    return TagTable(
        id = if (this.id == -1L) null else this.id,
        name = this.name,
        booksId = -1L,
    )
}