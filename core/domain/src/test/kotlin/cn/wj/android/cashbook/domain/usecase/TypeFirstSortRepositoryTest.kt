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

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * 验证 TypeRepository 新增的一级分类拖动排序能力（Task ③.2）。
 *
 * 同 ④.2：:core:data 测试源集无法访问 :core:testing 的 FakeTypeRepository
 * （:core:testing 反向依赖 :core:data，且 RepositoryImpl 需要真实 CombineProtoDataSource），
 * 故在 :core:domain 对 FakeTypeRepository.updateFirstTypeSort 的「按 id 顺序写连续 sort
 * + 列表按 sort 重排」行为做校验。maxSortByLevel DAO 方法已在 :core:data
 * TypeRepositoryImplTest 覆盖；TypeRepositoryImpl.updateFirstTypeSort 委托 typeDao.updateSortById
 * （已在 ③.1 覆盖）。
 */
class TypeFirstSortRepositoryTest {

    private lateinit var typeRepository: FakeTypeRepository

    @Before
    fun setup() {
        typeRepository = FakeTypeRepository()
    }

    @Test
    fun when_updateFirstTypeSort_then_first_list_reordered_continuously() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "A",
                sort = 5,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )
        typeRepository.addType(
            createRecordTypeModel(
                id = 2L,
                name = "B",
                sort = 9,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )
        // 初始按 sort 升序：A(5), B(9)
        assertThat(
            typeRepository.firstExpenditureTypeListData.first().map { it.name },
        ).containsExactly("A", "B").inOrder()

        // 拖动重排为 B, A
        typeRepository.updateFirstTypeSort(listOf(2L, 1L))

        val list = typeRepository.firstExpenditureTypeListData.first()
        assertThat(list.map { it.name }).containsExactly("B", "A").inOrder()
        // sort 连续从 0 写入
        assertThat(list.first { it.name == "B" }.sort).isEqualTo(0)
        assertThat(list.first { it.name == "A" }.sort).isEqualTo(1)
    }
}
