/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.model.model.TagModel
import kotlinx.coroutines.flow.Flow

interface TagRepository {

    val tagListData: Flow<List<TagModel>>

    suspend fun updateTag(tag: TagModel)

    suspend fun deleteTag(tag: TagModel)

    suspend fun getRelatedTag(recordId: Long): List<TagModel>

    suspend fun getTagById(tagId: Long): TagModel?

    suspend fun deleteRelatedWithAsset(assetId: Long)

    suspend fun countTagByName(name: String): Int
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
