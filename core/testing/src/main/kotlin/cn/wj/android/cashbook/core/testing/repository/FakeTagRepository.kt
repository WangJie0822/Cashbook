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

package cn.wj.android.cashbook.core.testing.repository

import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.model.model.TagModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTagRepository : TagRepository {

    private val tags = mutableListOf<TagModel>()
    private val recordTagMap = mutableMapOf<Long, List<TagModel>>()
    private val _tagListData = MutableStateFlow<List<TagModel>>(emptyList())

    override val tagListData: Flow<List<TagModel>> = _tagListData

    fun addTag(tag: TagModel) {
        tags.add(tag)
        _tagListData.value = tags.toList()
    }

    fun setRelatedTags(recordId: Long, relatedTags: List<TagModel>) {
        recordTagMap[recordId] = relatedTags
    }

    override suspend fun updateTag(tag: TagModel) {
        val index = tags.indexOfFirst { it.id == tag.id }
        if (index >= 0) {
            tags[index] = tag
        } else {
            tags.add(tag)
        }
        _tagListData.value = tags.toList()
    }

    override suspend fun deleteTag(tag: TagModel) {
        tags.removeAll { it.id == tag.id }
        _tagListData.value = tags.toList()
    }

    override suspend fun getRelatedTag(recordId: Long): List<TagModel> {
        return recordTagMap[recordId] ?: emptyList()
    }

    override suspend fun getTagById(tagId: Long): TagModel? {
        return tags.find { it.id == tagId }
    }

    override suspend fun deleteRelatedWithAsset(assetId: Long) {
        // no-op
    }

    override suspend fun countTagByName(name: String): Int {
        return tags.count { it.name == name }
    }
}
