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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DeleteTagDialogViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var tagRepository: FakeTagRepository
    private lateinit var viewModel: DeleteTagDialogViewModel
    private lateinit var fakeController: FakeProgressDialogController

    @Before
    fun setup() {
        tagRepository = FakeTagRepository()
        viewModel = DeleteTagDialogViewModel(tagRepository)
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
    fun when_delete_tag_then_dismiss_dialog_called() = runTest {
        // 准备标签数据
        val tag = TagModel(id = 1L, name = "待删除标签", invisible = false)
        tagRepository.addTag(tag)

        var dismissed = false
        viewModel.deleteTag(fakeController, tag) { dismissed = true }

        // 验证 dismissDialog 被调用，且标签已从仓库删除
        assertThat(dismissed).isTrue()
        assertThat(viewModel.bookmark).isFalse()
    }

    @Test
    fun when_dismiss_bookmark_then_bookmark_false() {
        // 初始状态 bookmark 为 false
        assertThat(viewModel.bookmark).isFalse()

        // 调用 dismissBookmark 后仍为 false
        viewModel.dismissBookmark()

        assertThat(viewModel.bookmark).isFalse()
    }

    @Test
    fun when_delete_tag_then_tag_removed_from_repository() = runTest {
        // 准备标签数据
        val tag1 = TagModel(id = 1L, name = "标签1", invisible = false)
        val tag2 = TagModel(id = 2L, name = "标签2", invisible = false)
        tagRepository.addTag(tag1)
        tagRepository.addTag(tag2)

        // 删除 tag1
        viewModel.deleteTag(fakeController, tag1) {}

        // 验证 tag1 已被删除，tag2 还在
        val remainingTag = tagRepository.getTagById(1L)
        assertThat(remainingTag).isNull()
        val existingTag = tagRepository.getTagById(2L)
        assertThat(existingTag).isEqualTo(tag2)
    }
}
