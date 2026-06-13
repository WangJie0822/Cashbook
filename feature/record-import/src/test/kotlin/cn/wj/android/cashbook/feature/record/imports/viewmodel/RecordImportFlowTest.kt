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
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 账单导入端到端流程测试。
 *
 * WechatBillParser 依赖 XmlPullParserFactory，纯 JVM 下抛 "not mocked"，故走 Robolectric。
 * 用程序化构造的最小微信 xlsx（ZIP）驱动 parseFile → Ready，再覆盖 confirmImport/toggle/selectAll。
 */
@RunWith(RobolectricTestRunner::class)
class RecordImportFlowTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var booksRepository: FakeBooksRepository

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        assetRepository = FakeAssetRepository()
        booksRepository = FakeBooksRepository()
        // 播种一级支出/收入类型，使 defaultType 非空
        typeRepository.addType(
            createRecordTypeModel(id = 1L, name = "其它支出", typeCategory = RecordTypeCategoryEnum.EXPENDITURE),
        )
        typeRepository.addType(
            createRecordTypeModel(id = 2L, name = "其它收入", typeCategory = RecordTypeCategoryEnum.INCOME),
        )
    }

    private fun createViewModel(filePath: String): RecordImportViewModel = RecordImportViewModel(
        savedStateHandle = SavedStateHandle(mapOf("fileUri" to filePath)),
        recordRepository = recordRepository,
        typeRepository = typeRepository,
        assetRepository = assetRepository,
        booksRepository = booksRepository,
        coroutineContext = UnconfinedTestDispatcher(),
    )

    /** 一条支出账单行：交易时间/类型/对方/商品/收支/金额/支付方式/状态/交易单号/商户单号/备注（11 列） */
    private val expenditureRow = listOf(
        "2026-03-26 11:50:04", "商户消费", "星巴克", "咖啡", "支出",
        "99.8", "零钱", "支付成功", "4200001", "/", "/",
    )

    /** 一条收入账单行 */
    private val incomeRow = listOf(
        "2026-03-25 09:00:00", "转账", "老板", "工资", "收入",
        "5000", "/", "已存入", "4200002", "/", "/",
    )

    /**
     * 构造最小微信 xlsx（ZIP）：含 xl/sharedStrings.xml（空池）+ xl/worksheets/sheet1.xml，
     * 数据行从 r=19 起，每行 11 个 raw `<v>` 单元格（无 t 属性，值即原文）。
     */
    private fun createWechatBillXlsx(rows: List<List<String>>): File {
        val file = tempFolder.newFile("wechat_bill.xlsx")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write("""<?xml version="1.0" encoding="UTF-8"?><sst></sst>""".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8"?><worksheet><sheetData>""")
            rows.forEachIndexed { index, cells ->
                sb.append("""<row r="${19 + index}">""")
                cells.forEach { value ->
                    sb.append("<c><v>").append(value).append("</v></c>")
                }
                sb.append("</row>")
            }
            sb.append("</sheetData></worksheet>")
            zip.write(sb.toString().toByteArray())
            zip.closeEntry()
        }
        return file
    }

    @Test
    fun when_valid_bill_parsed_then_ready_with_preview_items() = runTest {
        val file = createWechatBillXlsx(listOf(expenditureRow, incomeRow))
        val viewModel = createViewModel(file.absolutePath)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(RecordImportUiState.Ready::class.java)
        val ready = state as RecordImportUiState.Ready
        assertThat(ready.previewItems).hasSize(2)
        assertThat(ready.summary.totalCount).isEqualTo(2)
        assertThat(ready.summary.expenditureCount).isEqualTo(1)
        assertThat(ready.summary.incomeCount).isEqualTo(1)
        // 无重复 → 默认全选
        assertThat(ready.previewItems.all { it.selected }).isTrue()
    }

    @Test
    fun when_confirm_import_then_done_and_records_built() = runTest {
        val file = createWechatBillXlsx(listOf(expenditureRow, incomeRow))
        val viewModel = createViewModel(file.absolutePath)
        advanceUntilIdle()

        viewModel.confirmImport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(RecordImportUiState.Done::class.java)
        val done = state as RecordImportUiState.Done
        assertThat(done.imported).isEqualTo(2)
        assertThat(done.skipped).isEqualTo(0)

        // 忠实桩捕获构建的记录，校验 remark 拼接 / toCent / finalAmount
        val imported = recordRepository.lastImportedRecords
        assertThat(imported).hasSize(2)
        val expenditure = imported.first { it.remark.contains("星巴克") }
        assertThat(expenditure.remark).isEqualTo("星巴克 - 咖啡 [微信单号:4200001]")
        // 99.8 元 → 9980 分，finalAmount 同步
        assertThat(expenditure.amount).isEqualTo(9980L)
        assertThat(expenditure.finalAmount).isEqualTo(9980L)
        assertThat(expenditure.booksId).isEqualTo(1L)
    }

    @Test
    fun when_deselect_one_then_skipped_counted_and_only_selected_imported() = runTest {
        val file = createWechatBillXlsx(listOf(expenditureRow, incomeRow))
        val viewModel = createViewModel(file.absolutePath)
        advanceUntilIdle()

        // 取消第 0 条
        viewModel.toggleItemSelection(0)
        viewModel.confirmImport()
        advanceUntilIdle()

        val done = viewModel.uiState.value as RecordImportUiState.Done
        assertThat(done.imported).isEqualTo(1)
        assertThat(done.skipped).isEqualTo(1)
        assertThat(recordRepository.lastImportedRecords).hasSize(1)
    }

    @Test
    fun when_select_all_false_then_confirm_import_no_op() = runTest {
        val file = createWechatBillXlsx(listOf(expenditureRow, incomeRow))
        val viewModel = createViewModel(file.absolutePath)
        advanceUntilIdle()

        viewModel.selectAll(false)
        viewModel.confirmImport()
        advanceUntilIdle()

        // selectedItems 为空 → confirmImport 直接 return，保持 Ready
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Ready::class.java)
    }
}
