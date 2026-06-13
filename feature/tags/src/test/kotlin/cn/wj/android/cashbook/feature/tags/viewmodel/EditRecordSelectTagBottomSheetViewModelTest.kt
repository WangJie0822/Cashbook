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

package cn.wj.android.cashbook.feature.tags.viewmodel

import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetSelectableVisibleTagListUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditRecordSelectTagBottomSheetViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var tagRepository: FakeTagRepository
    private lateinit var viewModel: EditRecordSelectTagBottomSheetViewModel

    @Before
    fun setup() {
        tagRepository = FakeTagRepository()
        viewModel = EditRecordSelectTagBottomSheetViewModel(
            tagRepository = tagRepository,
            getSelectableVisibleTagListUseCase = GetSelectableVisibleTagListUseCase(),
        )
    }

    @Test
    fun when_display_add_tag_dialog_then_dialog_state_shown() {
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)

        viewModel.displayAddTagDialog()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
    }

    @Test
    fun when_dismiss_dialog_then_dialog_state_dismiss() {
        viewModel.displayAddTagDialog()
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        viewModel.dismissDialog()

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_initial_tag_list_then_empty() {
        assertThat(viewModel.tagListData.value).isEmpty()
    }

    @Test
    fun when_update_selected_tags_called_twice_then_only_first_seeds() = runTest {
        tagRepository.addTag(createTagModel(id = 1L, name = "餐饮"))
        tagRepository.addTag(createTagModel(id = 2L, name = "交通"))
        tagRepository.addTag(createTagModel(id = 3L, name = "日常"))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.tagListData.collect {}
        }

        viewModel.updateSelectedTags(listOf(1L))
        // 二次调用应被 firstSet 守卫忽略
        viewModel.updateSelectedTags(listOf(2L, 3L))

        val list = viewModel.tagListData.value
        assertThat(list.first { it.data.id == 1L }.selected).isTrue()
        assertThat(list.first { it.data.id == 2L }.selected).isFalse()
        assertThat(list.first { it.data.id == 3L }.selected).isFalse()
    }

    @Test
    fun when_toggle_tag_then_selection_added_and_removed() = runTest {
        tagRepository.addTag(createTagModel(id = 1L, name = "餐饮"))
        tagRepository.addTag(createTagModel(id = 2L, name = "交通"))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.tagListData.collect {}
        }
        viewModel.updateSelectedTags(listOf(1L))

        // 切换：选中 2
        viewModel.updateSelectedTagList(2L)
        var list = viewModel.tagListData.value
        assertThat(list.first { it.data.id == 1L }.selected).isTrue()
        assertThat(list.first { it.data.id == 2L }.selected).isTrue()

        // 再切换：取消 1
        viewModel.updateSelectedTagList(1L)
        list = viewModel.tagListData.value
        assertThat(list.first { it.data.id == 1L }.selected).isFalse()
        assertThat(list.first { it.data.id == 2L }.selected).isTrue()
    }

    @Test
    fun when_tag_invisible_and_unselected_then_filtered_out() = runTest {
        tagRepository.addTag(createTagModel(id = 1L, name = "可见"))
        tagRepository.addTag(createTagModel(id = 2L, name = "隐藏", invisible = true))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.tagListData.collect {}
        }

        // 未选中的隐藏标签应被过滤
        val list = viewModel.tagListData.value
        assertThat(list.map { it.data.id }).containsExactly(1L)
    }

    @Test
    fun when_tag_invisible_but_initially_selected_then_kept_visible() = runTest {
        tagRepository.addTag(createTagModel(id = 1L, name = "可见"))
        tagRepository.addTag(createTagModel(id = 2L, name = "隐藏", invisible = true))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.tagListData.collect {}
        }
        // 编辑场景：已选中的隐藏标签应保持可见
        viewModel.updateSelectedTags(listOf(2L))

        val list = viewModel.tagListData.value
        assertThat(list.map { it.data.id }).containsExactly(1L, 2L)
        assertThat(list.first { it.data.id == 2L }.selected).isTrue()
        assertThat(list.first { it.data.id == 1L }.selected).isFalse()
    }
}
