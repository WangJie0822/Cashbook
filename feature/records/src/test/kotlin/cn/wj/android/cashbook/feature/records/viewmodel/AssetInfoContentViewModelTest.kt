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

package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.paging.PagingSource
import androidx.paging.PagingState
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * [AssetInfoContentViewModel] 单元测试
 *
 * 由于 [AssetRecordPagingSource] 是私有类，通过本地 [TestPagingSource] 镜像其逻辑进行覆盖测试。
 */
class AssetInfoContentViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    // -------------------------------------------------------------------------
    // updateAssetId 基础行为测试
    // -------------------------------------------------------------------------

    @Test
    fun when_update_asset_id_then_no_crash() {
        // 构造真实 ViewModel 并调用 updateAssetId，验证不抛异常
        val fakeUseCase = buildFakeUseCase(emptyList())
        val viewModel = AssetInfoContentViewModel(fakeUseCase)
        viewModel.updateAssetId(1L)
        viewModel.updateAssetId(0L)
        viewModel.updateAssetId(-1L)
        // 只要走到这里说明没有崩溃
    }

    // -------------------------------------------------------------------------
    // PagingSource 分页逻辑测试（通过本地镜像类）
    // -------------------------------------------------------------------------

    @Test
    fun when_load_returns_data_then_paging_result_is_page() = runTest {
        val testData = listOf(createRecordViewsEntity(1L), createRecordViewsEntity(2L))
        val source = TestPagingSource { _, _ -> testData }

        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        )

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        val page = result as PagingSource.LoadResult.Page
        assertThat(page.data).hasSize(2)
        assertThat(page.nextKey).isEqualTo(1)
        assertThat(page.prevKey).isNull()
    }

    @Test
    fun when_load_returns_empty_then_next_key_is_null() = runTest {
        val source = TestPagingSource { _, _ -> emptyList() }

        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        )

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        val page = result as PagingSource.LoadResult.Page
        assertThat(page.data).isEmpty()
        assertThat(page.nextKey).isNull()
        assertThat(page.prevKey).isNull()
    }

    @Test
    fun when_load_second_page_then_prev_key_is_zero() = runTest {
        val testData = listOf(createRecordViewsEntity(1L))
        val source = TestPagingSource { _, _ -> testData }

        val result = source.load(
            PagingSource.LoadParams.Append(key = 1, loadSize = 20, placeholdersEnabled = false),
        )

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        val page = result as PagingSource.LoadResult.Page
        // page = 1 时，prevKey 应为 0
        assertThat(page.prevKey).isEqualTo(0)
        assertThat(page.nextKey).isEqualTo(2)
    }

    @Test
    fun when_load_throws_exception_then_result_is_error() = runTest {
        val exception = RuntimeException("模拟加载异常")
        val source = TestPagingSource { _, _ -> throw exception }

        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        )

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
        val error = result as PagingSource.LoadResult.Error
        assertThat(error.throwable).isEqualTo(exception)
    }

    @Test
    fun when_get_refresh_key_always_returns_null() {
        val source = TestPagingSource { _, _ -> emptyList() }
        val state = PagingState<Int, RecordViewsEntity>(
            pages = emptyList(),
            anchorPosition = null,
            config = androidx.paging.PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0,
        )
        assertThat(source.getRefreshKey(state)).isNull()
    }

    // -------------------------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------------------------

    /** 构造仅返回固定数据的 FakeUseCase */
    private fun buildFakeUseCase(
        data: List<RecordViewsEntity>,
    ): cn.wj.android.cashbook.domain.usecase.GetAssetRecordViewsUseCase {
        val fakeRecordRepository = cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository()
        val fakeTypeRepository = cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository()
        val fakeAssetRepository = cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository()
        val fakeTagRepository = cn.wj.android.cashbook.core.testing.repository.FakeTagRepository()
        val transUseCase = cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase(
            recordRepository = fakeRecordRepository,
            typeRepository = fakeTypeRepository,
            assetRepository = fakeAssetRepository,
            tagRepository = fakeTagRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        return cn.wj.android.cashbook.domain.usecase.GetAssetRecordViewsUseCase(
            recordRepository = fakeRecordRepository,
            recordModelTransToViewsUseCase = transUseCase,
            coroutineContext = dispatcherRule.testDispatcher,
        )
    }

    /** 创建测试用的 [RecordViewsEntity] */
    private fun createRecordViewsEntity(id: Long): RecordViewsEntity {
        return RecordViewsEntity(
            id = id,
            typeId = 1L,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            typeName = "餐饮",
            typeIconResName = "ic_type_food",
            assetId = 1L,
            assetName = "现金",
            assetIconResId = 0,
            relatedAssetId = null,
            relatedAssetName = null,
            relatedAssetIconResId = null,
            amount = 10000L,
            finalAmount = 10000L,
            charges = 0L,
            concessions = 0L,
            remark = "",
            reimbursable = false,
            relatedTags = emptyList(),
            relatedImage = emptyList(),
            relatedRecord = emptyList(),
            relatedAmount = 0L,
            recordTime = 1704110400000L,
        )
    }
}

/**
 * 镜像 AssetRecordPagingSource 私有类的分页逻辑，用于测试
 */
private class TestPagingSource(
    private val loader: suspend (page: Int, pageSize: Int) -> List<RecordViewsEntity>,
) : PagingSource<Int, RecordViewsEntity>() {

    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> {
        return runCatching {
            val page = params.key ?: 0
            val items = loader(page, params.loadSize)
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (items.isNotEmpty()) page + 1 else null
            LoadResult.Page(items, prevKey, nextKey)
        }.getOrElse { LoadResult.Error(it) }
    }
}
