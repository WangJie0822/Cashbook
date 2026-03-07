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

import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.tags.model.TagDialogState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MyTagsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var tagRepository: FakeTagRepository
    private lateinit var viewModel: MyTagsViewModel

    @Before
    fun setup() {
        tagRepository = FakeTagRepository()
        viewModel = MyTagsViewModel(tagRepository)
    }

    @Test
    fun when_initial_state_then_empty_list() {
        assertThat(viewModel.tagListData.value).isEmpty()
    }

    @Test
    fun when_switch_invisible_state_then_repository_updated() = runTest {
        val tag = TagModel(id = 1L, name = "标签1", invisible = false)
        tagRepository.addTag(tag)

        // 切换隐藏状态
        viewModel.switchInvisibleState(tag)

        // 验证 repository 中标签隐藏状态已取反
        val updatedTag = tagRepository.getTagById(1L)
        assertThat(updatedTag).isNotNull()
        assertThat(updatedTag!!.invisible).isTrue()
    }

    @Test
    fun when_show_edit_tag_dialog_then_dialog_state_shown() {
        val tag = TagModel(id = 1L, name = "标签1", invisible = false)

        viewModel.showEditTagDialog(tag)

        val state = viewModel.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        val shownData = (state as DialogState.Shown<*>).data
        assertThat(shownData).isInstanceOf(TagDialogState.Edit::class.java)
        assertThat((shownData as TagDialogState.Edit).tag).isEqualTo(tag)
    }

    @Test
    fun when_show_edit_tag_dialog_with_null_then_dialog_state_shown() {
        viewModel.showEditTagDialog(null)

        val state = viewModel.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        val shownData = (state as DialogState.Shown<*>).data
        assertThat(shownData).isInstanceOf(TagDialogState.Edit::class.java)
        assertThat((shownData as TagDialogState.Edit).tag).isNull()
    }

    @Test
    fun when_show_delete_tag_dialog_then_dialog_state_shown() {
        val tag = TagModel(id = 1L, name = "标签1", invisible = false)

        viewModel.showDeleteTagDialog(tag)

        val state = viewModel.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        val shownData = (state as DialogState.Shown<*>).data
        assertThat(shownData).isInstanceOf(TagDialogState.Delete::class.java)
        assertThat((shownData as TagDialogState.Delete).tag).isEqualTo(tag)
    }

    @Test
    fun when_dismiss_dialog_then_dialog_state_dismiss() {
        val tag = TagModel(id = 1L, name = "标签1", invisible = false)
        viewModel.showEditTagDialog(tag)
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        viewModel.dismissDialog()

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }
}
