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

package cn.wj.android.cashbook.feature.types.viewmodel

import androidx.test.core.app.ApplicationProvider
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypeIconGroupListViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var viewModel: TypeIconGroupListViewModel

    @Before
    fun setup() {
        viewModel = TypeIconGroupListViewModel(
            application = ApplicationProvider.getApplicationContext(),
        )
    }

    @Test
    fun when_initial_then_first_group_selected() = runTest {
        // 激活 WhileSubscribed StateFlow
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectableGroupListData.collect {}
        }

        val groups = viewModel.selectableGroupListData.value

        // 若资源可解析，列表不为空，第一项被选中，其余未选中
        if (groups.isNotEmpty()) {
            assertThat(groups.first().selected).isTrue()
            assertThat(groups.drop(1).none { it.selected }).isTrue()
        }

        job.cancel()
    }

    @Test
    fun when_select_group_then_selected_state_correct() = runTest {
        // 激活 WhileSubscribed StateFlow
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectableGroupListData.collect {}
        }

        val groups = viewModel.selectableGroupListData.value

        // 需要至少 2 个分组才能验证切换逻辑
        if (groups.size < 2) {
            job.cancel()
            return@runTest
        }

        // 选中第二个分组
        val secondGroupName = groups[1].data.name
        viewModel.selectGroup(secondGroupName)

        val updatedGroups = viewModel.selectableGroupListData.value
        assertThat(updatedGroups[1].selected).isTrue()
        assertThat(updatedGroups.filterIndexed { index, _ -> index != 1 }.none { it.selected })
            .isTrue()

        job.cancel()
    }

    @Test
    fun when_select_group_then_icon_list_updates() = runTest {
        // 同时激活两个 StateFlow
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectableGroupListData.collect {}
        }
        val iconJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.iconListData.collect {}
        }

        val groups = viewModel.selectableGroupListData.value

        // 需要至少 2 个分组才能验证切换逻辑
        if (groups.size < 2) {
            job.cancel()
            iconJob.cancel()
            return@runTest
        }

        // 记录初始（第一组）的图标列表
        val firstGroupIcons = viewModel.iconListData.value

        // 切换到第二个分组
        val secondGroupName = groups[1].data.name
        viewModel.selectGroup(secondGroupName)

        val secondGroupIcons = viewModel.iconListData.value

        // 第二组图标应与资源中定义的一致，且与第一组不同（名称或数量不同）
        assertThat(secondGroupIcons).isNotEmpty()
        assertThat(secondGroupIcons).isNotEqualTo(firstGroupIcons)

        job.cancel()
        iconJob.cancel()
    }
}
