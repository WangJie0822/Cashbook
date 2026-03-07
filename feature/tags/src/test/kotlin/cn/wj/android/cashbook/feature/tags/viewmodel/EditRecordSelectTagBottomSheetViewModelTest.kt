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

import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetSelectableVisibleTagListUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditRecordSelectTagBottomSheetViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var viewModel: EditRecordSelectTagBottomSheetViewModel

    @Before
    fun setup() {
        viewModel = EditRecordSelectTagBottomSheetViewModel(
            tagRepository = FakeTagRepository(),
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
}
