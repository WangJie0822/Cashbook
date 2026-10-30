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

package cn.wj.android.cashbook.feature.records.view

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordFilterRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.domain.usecase.GetFilteredRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetSearchRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import cn.wj.android.cashbook.feature.records.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

internal class SearchFilterTestFixture {
    val records = FakeRecordRepository()
    val types = FakeTypeRepository().apply {
        addType(createRecordTypeModel(id = 10L, name = "餐饮", iconName = "vector_type_three_meals_24"))
        addType(createRecordTypeModel(id = 11L, parentId = 10L, name = "午餐", typeLevel = TypeLevelEnum.SECOND, iconName = "vector_type_three_meals_24"))
        addType(createRecordTypeModel(id = 12L, parentId = 10L, name = "咖啡", typeLevel = TypeLevelEnum.SECOND, iconName = "vector_type_three_meals_24"))
        addType(createRecordTypeModel(id = 20L, name = "工资", typeCategory = RecordTypeCategoryEnum.INCOME, iconName = "vector_type_three_meals_24"))
        addType(createRecordTypeModel(id = 30L, name = "转账", typeCategory = RecordTypeCategoryEnum.TRANSFER, iconName = "vector_type_three_meals_24"))
    }
    val tags = FakeTagRepository().apply {
        addTag(createTagModel(id = 41L, name = "旅行"))
        addTag(createTagModel(id = 42L, name = "旧项目", invisible = true))
    }
    val settings = FakeSettingRepository()
    val books = FakeBooksRepository()
    val filters = FakeRecordFilterRepository(types.allTypeListData, tags.tagListData)
    private val mapper = RecordModelTransToViewsUseCase(records, types, FakeAssetRepository(), tags, Dispatchers.Unconfined)
    val vm = SearchViewModel(records, GetSearchRecordViewsUseCase(records, mapper, Dispatchers.Unconfined), GetFilteredRecordViewsUseCase(filters), filters, types, tags, books, settings, SavedStateHandle())
    val events = mutableListOf<SearchFilterEvent>()
    fun event(event: SearchFilterEvent) { events += event; vm.onFilterEvent(event) }
    fun close() { vm.viewModelScope.cancel() }
}
