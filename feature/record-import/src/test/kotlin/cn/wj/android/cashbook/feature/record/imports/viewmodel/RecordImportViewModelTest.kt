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

package cn.wj.android.cashbook.feature.record.imports.viewmodel

import androidx.lifecycle.SavedStateHandle
import cn.wj.android.cashbook.core.model.model.BillDirection
import cn.wj.android.cashbook.core.model.model.DuplicateStatus
import cn.wj.android.cashbook.core.model.model.ImportedBillItem
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RecordImportViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun createViewModel(
        filePath: String = "",
        recordRepository: FakeRecordRepository = FakeRecordRepository(),
    ): RecordImportViewModel {
        return RecordImportViewModel(
            savedStateHandle = SavedStateHandle(mapOf("fileUri" to filePath)),
            recordRepository = recordRepository,
            typeRepository = FakeTypeRepository(),
            assetRepository = FakeAssetRepository(),
            booksRepository = FakeBooksRepository(),
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_file_not_found_then_ui_state_error() = runTest {
        // 给定一个不存在的文件路径，解析后应进入 Error 状态
        val viewModel = createViewModel(filePath = "/nonexistent/path/bill.csv")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }

    @Test
    fun when_empty_file_path_then_ui_state_error() = runTest {
        // 空路径对应的 File 不存在，解析后应进入 Error 状态
        val viewModel = createViewModel(filePath = "")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }

    @Test
    fun when_toggle_on_non_ready_state_then_no_crash() = runTest {
        // 在 Error 状态下调用 toggleItemSelection 应安全退出不崩溃
        val viewModel = createViewModel(filePath = "/nonexistent/path/bill.csv")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)

        // 调用不应抛出异常
        viewModel.toggleItemSelection(0)

        // 状态保持不变
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }

    @Test
    fun when_select_all_on_non_ready_state_then_no_crash() = runTest {
        // 在 Error 状态下调用 selectAll 应安全退出不崩溃
        val viewModel = createViewModel(filePath = "/nonexistent/path/bill.csv")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)

        // 调用不应抛出异常
        viewModel.selectAll(true)

        // 状态保持不变
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }

    @Test
    fun when_confirm_import_on_non_ready_state_then_no_crash() = runTest {
        // 在 Error 状态下调用 confirmImport 应安全退出不崩溃
        val viewModel = createViewModel(filePath = "/nonexistent/path/bill.csv")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)

        // 调用不应抛出异常
        viewModel.confirmImport()

        // 状态保持不变
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }

    /**
     * 回归测试：账单去重金额单位错配修复。
     *
     * 库中已有一条 99.80 元（9980 分）、当天的记录；导入条目同天同金额且无微信单号，
     * 去重应判定为 POSSIBLE（可能重复）。
     *
     * 修复前：ViewModel 直接把 item.amount（元，Double=99.8）传给以分（Long）比对的查询，
     * 口径不一致永不命中，会错误返回 NONE，该断言失败。
     */
    @Test
    fun when_same_day_same_amount_no_transaction_id_then_possible_duplicate() = runTest {
        val booksId = 1L
        // 当天 12:00（毫秒），落在 dayStart..dayEnd 区间内
        val recordTime = 86400000L + 12 * 3600000L
        val repository = FakeRecordRepository().apply {
            addRecord(
                RecordModel(
                    id = 1L,
                    booksId = booksId,
                    typeId = 1L,
                    assetId = -1L,
                    relatedAssetId = -1L,
                    // 99.80 元 = 9980 分
                    amount = 9980L,
                    finalAmount = 9980L,
                    charges = 0L,
                    concessions = 0L,
                    remark = "已有记录",
                    reimbursable = false,
                    recordTime = recordTime,
                ),
            )
        }
        val viewModel = createViewModel(recordRepository = repository)
        advanceUntilIdle()

        val item = ImportedBillItem(
            transactionTime = recordTime,
            transactionType = "商户消费",
            counterparty = "某商户",
            description = "",
            direction = BillDirection.EXPENDITURE,
            // 元（Double），与库中 9980 分对应
            amount = 99.80,
            paymentMethod = "",
            status = "",
            // 无微信单号，走模糊去重路径
            transactionId = "",
            merchantId = "",
            remark = "",
        )

        val status = viewModel.checkDuplicate(item, booksId)

        assertThat(status).isEqualTo(DuplicateStatus.POSSIBLE)
    }

    /**
     * 对照测试：同天但金额不同（分值不相等）时不应判为重复，返回 NONE。
     * 防止修复退化为"只要同天就算重复"的过宽匹配。
     */
    @Test
    fun when_same_day_different_amount_then_not_duplicate() = runTest {
        val booksId = 1L
        val recordTime = 86400000L + 12 * 3600000L
        val repository = FakeRecordRepository().apply {
            addRecord(
                RecordModel(
                    id = 1L,
                    booksId = booksId,
                    typeId = 1L,
                    assetId = -1L,
                    relatedAssetId = -1L,
                    amount = 9980L,
                    finalAmount = 9980L,
                    charges = 0L,
                    concessions = 0L,
                    remark = "已有记录",
                    reimbursable = false,
                    recordTime = recordTime,
                ),
            )
        }
        val viewModel = createViewModel(recordRepository = repository)
        advanceUntilIdle()

        val item = ImportedBillItem(
            transactionTime = recordTime,
            transactionType = "商户消费",
            counterparty = "某商户",
            description = "",
            direction = BillDirection.EXPENDITURE,
            // 88.00 元 = 8800 分，与库中 9980 分不同
            amount = 88.00,
            paymentMethod = "",
            status = "",
            transactionId = "",
            merchantId = "",
            remark = "",
        )

        val status = viewModel.checkDuplicate(item, booksId)

        assertThat(status).isEqualTo(DuplicateStatus.NONE)
    }

    /**
     * happy-path：精确单号匹配走 EXACT 路径（优先于同天同额模糊匹配）。
     *
     * 库中记录 remark 含微信单号；导入条目带相同 transactionId 时去重应判定为 EXACT。
     * 此前 FakeRecordRepository.queryByWechatTransactionId 为 emptyList 桩，EXACT 路径从未被覆盖。
     */
    @Test
    fun when_transaction_id_matches_existing_then_exact_duplicate() = runTest {
        val booksId = 1L
        val recordTime = 86400000L + 12 * 3600000L
        val repository = FakeRecordRepository().apply {
            addRecord(
                RecordModel(
                    id = 1L,
                    booksId = booksId,
                    typeId = 1L,
                    assetId = -1L,
                    relatedAssetId = -1L,
                    amount = 9980L,
                    finalAmount = 9980L,
                    charges = 0L,
                    concessions = 0L,
                    // remark 含方括号定界的微信单号，与真实写入(RecordImportViewModel)+ DAO SQL 一致
                    remark = "某商户 [微信单号:1000050001]",
                    reimbursable = false,
                    recordTime = recordTime,
                ),
            )
        }
        val viewModel = createViewModel(recordRepository = repository)
        advanceUntilIdle()

        val item = ImportedBillItem(
            transactionTime = recordTime,
            transactionType = "商户消费",
            counterparty = "某商户",
            description = "",
            direction = BillDirection.EXPENDITURE,
            amount = 99.80,
            paymentMethod = "",
            status = "",
            transactionId = "1000050001",
            merchantId = "",
            remark = "",
        )

        val status = viewModel.checkDuplicate(item, booksId)

        assertThat(status).isEqualTo(DuplicateStatus.EXACT)
    }
}
