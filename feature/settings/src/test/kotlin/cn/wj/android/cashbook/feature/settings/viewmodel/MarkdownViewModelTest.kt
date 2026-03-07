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

package cn.wj.android.cashbook.feature.settings.viewmodel

import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MarkdownViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var viewModel: MarkdownViewModel

    @Before
    fun setup() {
        settingRepository = FakeSettingRepository()
        viewModel = MarkdownViewModel(settingRepository)
    }

    @Test
    fun when_initial_state_then_markdown_data_is_empty() {
        assertThat(viewModel.markdownData.value).isEmpty()
    }

    @Test
    fun when_update_markdown_type_to_changelog_then_data_updated() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.markdownData.collect()
        }

        viewModel.updateMarkdownType(MarkdownTypeEnum.CHANGELOG)

        // FakeSettingRepository.getContentByMarkdownType 返回空字符串
        assertThat(viewModel.markdownData.value).isEqualTo("")

        collectJob.cancel()
    }

    @Test
    fun when_update_markdown_type_to_privacy_policy_then_data_updated() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.markdownData.collect()
        }

        viewModel.updateMarkdownType(MarkdownTypeEnum.PRIVACY_POLICY)

        assertThat(viewModel.markdownData.value).isEqualTo("")

        collectJob.cancel()
    }

    @Test
    fun when_update_markdown_type_to_null_then_data_is_empty() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.markdownData.collect()
        }

        // 先设置有值的类型
        viewModel.updateMarkdownType(MarkdownTypeEnum.CHANGELOG)
        // 再设置为 null
        viewModel.updateMarkdownType(null)

        assertThat(viewModel.markdownData.value).isEqualTo("")

        collectJob.cancel()
    }

    @Test
    fun when_update_markdown_type_multiple_times_then_last_value_used() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.markdownData.collect()
        }

        viewModel.updateMarkdownType(MarkdownTypeEnum.CHANGELOG)
        viewModel.updateMarkdownType(MarkdownTypeEnum.PRIVACY_POLICY)

        // 最终结果应该是最后一次 emit 的类型
        assertThat(viewModel.markdownData.value).isEqualTo("")

        collectJob.cancel()
    }
}
