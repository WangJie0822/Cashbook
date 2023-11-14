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

package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.common.model.tagDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.TagDao
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.model.model.TagModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val transactionDao: TransactionDao,
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
            recordDataVersion.updateVersion()
        }

    override suspend fun deleteTag(tag: TagModel) =
        withContext(coroutineContext) {
            transactionDao.deleteTag(tag.id)
            tagDataVersion.updateVersion()
            recordDataVersion.updateVersion()
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

    override suspend fun deleteRelatedWithAsset(assetId: Long): Unit =
        withContext(coroutineContext) {
            tagDao.deleteRelatedWithAsset(assetId)
        }

    override suspend fun countTagByName(name: String): Int =
        withContext(coroutineContext) {
            tagDao.countByName(name)
        }
}
