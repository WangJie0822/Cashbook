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
import cn.wj.android.cashbook.core.ui.ProgressDialogController
import cn.wj.android.cashbook.core.ui.ProgressDialogState
import cn.wj.android.cashbook.feature.tags.enums.EditTagDialogBookmarkEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditTagDialogViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var tagRepository: FakeTagRepository
    private lateinit var viewModel: EditTagDialogViewModel
    private lateinit var fakeController: FakeProgressDialogController

    @Before
    fun setup() {
        tagRepository = FakeTagRepository()
        viewModel = EditTagDialogViewModel(tagRepository)
        fakeController = FakeProgressDialogController()
    }

    private class FakeProgressDialogController : ProgressDialogController {
        override var dialogState: DialogState = DialogState.Dismiss
        override fun show(hint: String?, cancelable: Boolean, onDismiss: () -> Unit) {
            dialogState = DialogState.Shown(ProgressDialogState(hint, cancelable, onDismiss))
        }
        override fun dismiss() {
            dialogState = DialogState.Dismiss
        }
    }

    @Test
    fun when_save_tag_with_existing_name_then_bookmark_name_exist() = runTest {
        // 先在仓库中添加一个已存在的标签
        val existingTag = TagModel(id = 1L, name = "已存在标签", invisible = false)
        tagRepository.addTag(existingTag)

        // 尝试保存同名标签
        val newTag = TagModel(id = -1L, name = "已存在标签", invisible = false)
        var dismissed = false
        viewModel.saveTag(fakeController, newTag) { dismissed = true }

        // 验证 bookmark 状态为 NAME_EXIST，且 dismissDialog 未被调用
        assertThat(viewModel.bookmark).isEqualTo(EditTagDialogBookmarkEnum.NAME_EXIST)
        assertThat(dismissed).isFalse()
    }

    @Test
    fun when_save_tag_with_new_name_then_dismiss_dialog_called() = runTest {
        // 保存不重名的标签
        val newTag = TagModel(id = -1L, name = "新标签", invisible = false)
        var dismissed = false
        viewModel.saveTag(fakeController, newTag) { dismissed = true }

        // 验证 dismissDialog 被调用，bookmark 保持 DISMISS
        assertThat(dismissed).isTrue()
        assertThat(viewModel.bookmark).isEqualTo(EditTagDialogBookmarkEnum.DISMISS)
    }

    @Test
    fun when_dismiss_bookmark_then_bookmark_dismiss() {
        // 手动设置一个非 DISMISS 的状态后，调用 dismissBookmark
        val tag = TagModel(id = 1L, name = "已存在标签", invisible = false)
        tagRepository.addTag(tag)
        val newTag = TagModel(id = -1L, name = "已存在标签", invisible = false)
        viewModel.saveTag(fakeController, newTag) {}

        // 此时 bookmark 应该为 NAME_EXIST
        assertThat(viewModel.bookmark).isEqualTo(EditTagDialogBookmarkEnum.NAME_EXIST)

        // 调用 dismissBookmark
        viewModel.dismissBookmark()

        assertThat(viewModel.bookmark).isEqualTo(EditTagDialogBookmarkEnum.DISMISS)
    }

    @Test
    fun when_save_tag_update_existing_then_tag_updated() = runTest {
        // 添加已有标签并修改名称后保存
        val existingTag = TagModel(id = 1L, name = "原标签", invisible = false)
        tagRepository.addTag(existingTag)

        val updatedTag = TagModel(id = 1L, name = "新名称", invisible = false)
        var dismissed = false
        viewModel.saveTag(fakeController, updatedTag) { dismissed = true }

        // 验证保存成功且弹窗关闭
        assertThat(dismissed).isTrue()
        assertThat(viewModel.bookmark).isEqualTo(EditTagDialogBookmarkEnum.DISMISS)
    }
}
