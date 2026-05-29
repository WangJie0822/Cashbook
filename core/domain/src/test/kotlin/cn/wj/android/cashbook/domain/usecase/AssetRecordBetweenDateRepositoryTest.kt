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

import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * 验证 RecordRepository 新增的「按资产 + 月份」查询能力（Task ④.2）。
 *
 * 由于 RecordRepositoryImplTest 所在的 :core:data 测试源集无法访问位于
 * :core:testing 的 FakeRecordRepository（且 :core:testing 反向依赖 :core:data，
 * 直接在 :core:data 测试中实例化 Impl 需要 Room 真机环境），此处在 :core:domain
 * 测试源集对 FakeRecordRepository 的两个新方法做行为校验，构成红→绿闭环。
 */
class AssetRecordBetweenDateRepositoryTest {

    private lateinit var recordRepository: FakeRecordRepository

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
    }

    @Test
    fun when_queryPagingRecordListByAssetIdBetweenDate_then_filters_asset_and_date() = runTest {
        // 命中资产 + 在区间内
        recordRepository.addRecord(createRecordModel(id = 1L, assetId = 5L, recordTime = 1500L))
        // 命中资产但区间外
        recordRepository.addRecord(createRecordModel(id = 2L, assetId = 5L, recordTime = 3000L))
        // 区间内但别的资产
        recordRepository.addRecord(createRecordModel(id = 3L, assetId = 9L, recordTime = 1500L))
        // 通过转入资产命中
        recordRepository.addRecord(createRecordModel(id = 4L, assetId = 9L, relatedAssetId = 5L, recordTime = 1500L))

        val results = recordRepository.queryPagingRecordListByAssetIdBetweenDate(
            assetId = 5L,
            startDate = 1000L,
            endDate = 2000L,
            page = 0,
            pageSize = 10,
        )

        assertThat(results.map { it.id }).containsExactly(1L, 4L)
    }

    @Test
    fun when_queryPagingRecordListByAssetIdBetweenDate_then_paginates() = runTest {
        for (i in 1L..5L) {
            recordRepository.addRecord(createRecordModel(id = i, assetId = 5L, recordTime = i * 100L))
        }

        val page0 = recordRepository.queryPagingRecordListByAssetIdBetweenDate(
            assetId = 5L,
            startDate = 0L,
            endDate = 10000L,
            page = 0,
            pageSize = 2,
        )
        val page1 = recordRepository.queryPagingRecordListByAssetIdBetweenDate(
            assetId = 5L,
            startDate = 0L,
            endDate = 10000L,
            page = 1,
            pageSize = 2,
        )

        assertThat(page0).hasSize(2)
        assertThat(page1).hasSize(2)
        // 两页不重叠
        assertThat(page0.map { it.id }.intersect(page1.map { it.id }.toSet())).isEmpty()
    }

    @Test
    fun when_queryAssetRecordsBetweenDateFlow_then_emits_filtered_records() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, assetId = 5L, recordTime = 1500L))
        recordRepository.addRecord(createRecordModel(id = 2L, assetId = 5L, recordTime = 3000L)) // 区间外
        recordRepository.addRecord(createRecordModel(id = 3L, assetId = 9L, recordTime = 1500L)) // 别的资产
        recordRepository.addRecord(createRecordModel(id = 4L, assetId = 9L, relatedAssetId = 5L, recordTime = 1500L)) // 转入

        val results = recordRepository.queryAssetRecordsBetweenDateFlow(
            assetId = 5L,
            startDate = 1000L,
            endDate = 2000L,
        ).first()

        assertThat(results.map { it.id }).containsExactly(1L, 4L)
    }
}
