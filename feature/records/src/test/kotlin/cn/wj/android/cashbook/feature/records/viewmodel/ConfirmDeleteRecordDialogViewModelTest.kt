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

import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.DeleteRecordUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * ConfirmDeleteRecordDialogViewModel 的单元测试
 *
 * 由于 runCatchWithProgress 使用了 ProgressDialogManager（依赖 Compose mutableStateOf），
 * 需要使用 Robolectric 提供 Android 环境。
 */
@RunWith(RobolectricTestRunner::class)
class ConfirmDeleteRecordDialogViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var viewModel: ConfirmDeleteRecordDialogViewModel

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        val deleteRecordUseCase = DeleteRecordUseCase(
            recordRepository = recordRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        viewModel = ConfirmDeleteRecordDialogViewModel(
            deleteRecordUseCase = deleteRecordUseCase,
        )
    }

    @Test
    fun when_delete_record_confirm_then_success_result() = runTest {
        val recordId = 1L
        // 先添加一条记录
        recordRepository.addRecord(
            RecordModel(
                id = recordId,
                booksId = 1L,
                typeId = 1L,
                assetId = -1L,
                relatedAssetId = -1L,
                amount = 10000L,
                finalAmount = 10000L,
                charges = 0L,
                concessions = 0L,
                remark = "测试记录",
                reimbursable = false,
                recordTime = 1704067200000L,
            ),
        )

        var result: ResultModel? = null

        // 执行删除
        viewModel.onDeleteRecordConfirm(
            hintText = "删除中...",
            recordId = recordId,
            onResult = { result = it },
        )

        // 验证删除成功
        assertThat(result).isNotNull()
        assertThat(result!!.isSuccess).isTrue()
        // 验证 repository 中的记录已被删除
        assertThat(recordRepository.lastDeletedRecordId).isEqualTo(recordId)
    }

    @Test
    fun when_delete_nonexistent_record_then_still_success() = runTest {
        // 删除不存在的记录也应成功（repository 不会抛异常）
        var result: ResultModel? = null

        viewModel.onDeleteRecordConfirm(
            hintText = "删除中...",
            recordId = 999L,
            onResult = { result = it },
        )

        assertThat(result).isNotNull()
        assertThat(result!!.isSuccess).isTrue()
        assertThat(recordRepository.lastDeletedRecordId).isEqualTo(999L)
    }
}
