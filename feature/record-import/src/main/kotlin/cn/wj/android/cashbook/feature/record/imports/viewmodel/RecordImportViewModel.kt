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

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.helper.BillCategoryMatcher
import cn.wj.android.cashbook.core.data.helper.BillPaymentMatcher
import cn.wj.android.cashbook.core.data.helper.WechatBillParser
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.model.model.BillDirection
import cn.wj.android.cashbook.core.model.model.BillSummary
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.model.model.DuplicateStatus
import cn.wj.android.cashbook.core.model.model.ImportPreviewItem
import cn.wj.android.cashbook.core.model.model.ImportedBillItem
import cn.wj.android.cashbook.core.model.model.PaymentMethodMapping
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 账单导入 ViewModel
 */
@HiltViewModel
class RecordImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    private val assetRepository: AssetRepository,
    private val booksRepository: BooksRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : ViewModel() {

    private val fileUri: String = URLDecoder.decode(
        savedStateHandle.get<String>("fileUri") ?: "",
        "UTF-8",
    )

    private val _uiState = MutableStateFlow<RecordImportUiState>(RecordImportUiState.Parsing)
    val uiState: StateFlow<RecordImportUiState> = _uiState

    private var parsedItems: List<ImportedBillItem> = emptyList()
    private var parsedSummary: BillSummary? = null
    private var expenditureTypes: List<RecordTypeModel> = emptyList()
    private var incomeTypes: List<RecordTypeModel> = emptyList()
    private var defaultExpenditureType: RecordTypeModel? = null
    private var defaultIncomeType: RecordTypeModel? = null

    init {
        parseFile()
    }

    private fun parseFile() {
        viewModelScope.launch {
            _uiState.value = RecordImportUiState.Parsing
            try {
                val uri = Uri.parse(fileUri)
                val inputStream = application.contentResolver.openInputStream(uri)
                    ?: run {
                        _uiState.value = RecordImportUiState.Error("")
                        return@launch
                    }

                val result = inputStream.use { WechatBillParser.parse(it) }
                if (result == null) {
                    _uiState.value = RecordImportUiState.Error("")
                    return@launch
                }

                parsedItems = result.items
                parsedSummary = result.summary

                // 加载依赖数据
                val booksList = booksRepository.booksListData.first()
                val currentBook = booksRepository.currentBook.first()
                expenditureTypes = typeRepository.firstExpenditureTypeListData.first()
                incomeTypes = typeRepository.firstIncomeTypeListData.first()
                defaultExpenditureType = expenditureTypes.lastOrNull()
                defaultIncomeType = incomeTypes.lastOrNull()

                // 匹配支付方式
                val assets = assetRepository.getVisibleAssetsByBookId(currentBook.id)
                val paymentMethods = parsedItems.map { it.paymentMethod }.filter { it.isNotBlank() }.distinct()
                val mappings = BillPaymentMatcher.matchAll(paymentMethods, assets)

                // 构建映射表：支付方式原始文本 → assetId
                val mappingMap = mappings.associate { it.originalName to it.matchedAssetId }

                // 检测重复 + 匹配分类
                val previewItems = buildPreviewItems(
                    items = parsedItems,
                    booksId = currentBook.id,
                    mappingMap = mappingMap,
                )

                _uiState.value = RecordImportUiState.Ready(
                    fileName = uri.lastPathSegment ?: "",
                    summary = result.summary,
                    selectedBooksId = currentBook.id,
                    booksList = booksList,
                    paymentMappings = mappings,
                    previewItems = previewItems,
                    hasUnmappedPayments = mappings.any { it.matchedAssetId == -1L },
                    visibleAssets = assets,
                    expenditureTypes = expenditureTypes,
                    incomeTypes = incomeTypes,
                )
            } catch (e: Exception) {
                logger().e(e, "parseFile failed")
                _uiState.value = RecordImportUiState.Error("")
            }
        }
    }

    private suspend fun buildPreviewItems(
        items: List<ImportedBillItem>,
        booksId: Long,
        mappingMap: Map<String, Long>,
    ): List<ImportPreviewItem> {
        return items.map { item ->
            // 匹配分类
            val types = if (item.direction == BillDirection.EXPENDITURE) expenditureTypes else incomeTypes
            val defaultType = if (item.direction == BillDirection.EXPENDITURE) defaultExpenditureType else defaultIncomeType
            val matchedType = BillCategoryMatcher.match(item.counterparty, item.description, types) ?: defaultType

            // 检测重复
            val duplicateStatus = checkDuplicate(item, booksId)

            // 映射资产
            val assetId = if (item.paymentMethod.isBlank()) -1L else (mappingMap[item.paymentMethod] ?: -1L)

            ImportPreviewItem(
                billItem = item,
                mappedTypeId = matchedType?.id ?: -1L,
                mappedTypeName = matchedType?.name ?: "",
                mappedAssetId = assetId,
                duplicateStatus = duplicateStatus,
                selected = duplicateStatus != DuplicateStatus.EXACT,
            )
        }
    }

    private suspend fun checkDuplicate(
        item: ImportedBillItem,
        booksId: Long,
    ): DuplicateStatus {
        // 精确匹配：交易单号
        if (item.transactionId.isNotBlank()) {
            val existing = recordRepository.queryByWechatTransactionId(booksId, item.transactionId)
            if (existing.isNotEmpty()) return DuplicateStatus.EXACT
        }

        // 模糊匹配：同天 + 同金额
        val dayStart = item.transactionTime / 86400000 * 86400000 // 当天0点
        val dayEnd = dayStart + 86400000 - 1 // 当天23:59:59
        val similar = recordRepository.queryByTimeAndAmount(booksId, dayStart, dayEnd, item.amount)
        if (similar.isNotEmpty()) return DuplicateStatus.POSSIBLE

        return DuplicateStatus.NONE
    }

    /** 切换目标账本 */
    fun selectBook(booksId: Long) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        viewModelScope.launch {
            val assets = assetRepository.getVisibleAssetsByBookId(booksId)
            val paymentMethods = parsedItems.map { it.paymentMethod }.filter { it.isNotBlank() }.distinct()
            val mappings = BillPaymentMatcher.matchAll(paymentMethods, assets)
            val mappingMap = mappings.associate { it.originalName to it.matchedAssetId }

            val previewItems = buildPreviewItems(parsedItems, booksId, mappingMap)

            _uiState.value = state.copy(
                selectedBooksId = booksId,
                paymentMappings = mappings,
                previewItems = previewItems,
                hasUnmappedPayments = mappings.any { it.matchedAssetId == -1L },
                visibleAssets = assets,
            )
        }
    }

    /** 更新支付方式映射 */
    fun updatePaymentMapping(originalName: String, assetId: Long, assetName: String) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val updatedMappings = state.paymentMappings.map { mapping ->
            if (mapping.originalName == originalName) {
                mapping.copy(matchedAssetId = assetId, matchedAssetName = assetName)
            } else {
                mapping
            }
        }
        // 同步更新预览条目中的 assetId
        val mappingMap = updatedMappings.associate { it.originalName to it.matchedAssetId }
        val updatedPreviews = state.previewItems.map { preview ->
            val newAssetId = if (preview.billItem.paymentMethod.isBlank()) {
                -1L
            } else {
                mappingMap[preview.billItem.paymentMethod] ?: -1L
            }
            preview.copy(mappedAssetId = newAssetId)
        }
        _uiState.value = state.copy(
            paymentMappings = updatedMappings,
            previewItems = updatedPreviews,
            hasUnmappedPayments = updatedMappings.any { it.matchedAssetId == -1L },
        )
    }

    /** 切换单条记录的选中状态 */
    fun toggleItemSelection(index: Int) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val updatedItems = state.previewItems.toMutableList()
        val item = updatedItems[index]
        updatedItems[index] = item.copy(selected = !item.selected)
        _uiState.value = state.copy(previewItems = updatedItems)
    }

    /** 全选/取消全选 */
    fun selectAll(selected: Boolean) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val updatedItems = state.previewItems.map { it.copy(selected = selected) }
        _uiState.value = state.copy(previewItems = updatedItems)
    }

    /** 更新单条记录的分类 */
    fun updateItemType(index: Int, typeId: Long, typeName: String) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val updatedItems = state.previewItems.toMutableList()
        updatedItems[index] = updatedItems[index].copy(mappedTypeId = typeId, mappedTypeName = typeName)
        _uiState.value = state.copy(previewItems = updatedItems)
    }

    /** 确认导入 */
    fun confirmImport() {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val selectedItems = state.previewItems.filter { it.selected }
        if (selectedItems.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = RecordImportUiState.Importing

            try {
                val records = selectedItems.map { preview ->
                    val remarkText = buildString {
                        append(preview.billItem.counterparty)
                        if (preview.billItem.description.isNotBlank()) {
                            append(" - ")
                            append(preview.billItem.description)
                        }
                        if (preview.billItem.transactionId.isNotBlank()) {
                            append(" [微信单号:")
                            append(preview.billItem.transactionId)
                            append("]")
                        }
                    }

                    RecordTable(
                        id = null,
                        typeId = preview.mappedTypeId,
                        assetId = preview.mappedAssetId,
                        intoAssetId = -1L,
                        booksId = state.selectedBooksId,
                        amount = preview.billItem.amount,
                        finalAmount = preview.billItem.amount,
                        concessions = 0.0,
                        charge = 0.0,
                        remark = remarkText,
                        reimbursable = cn.wj.android.cashbook.core.common.SWITCH_INT_OFF,
                        recordTime = preview.billItem.transactionTime,
                    )
                }

                val ids = recordRepository.batchImportRecords(records)
                val skipped = state.previewItems.size - selectedItems.size
                _uiState.value = RecordImportUiState.Done(
                    imported = ids.size,
                    skipped = skipped,
                )
            } catch (e: Exception) {
                logger().e(e, "import failed")
                _uiState.value = RecordImportUiState.Error("")
            }
        }
    }
}

/** 导入界面 UI 状态 */
sealed interface RecordImportUiState {
    /** 解析中 */
    data object Parsing : RecordImportUiState

    /** 就绪，可配置映射和预览 */
    data class Ready(
        val fileName: String,
        val summary: BillSummary,
        val selectedBooksId: Long,
        val booksList: List<BooksModel>,
        val paymentMappings: List<PaymentMethodMapping>,
        val previewItems: List<ImportPreviewItem>,
        val hasUnmappedPayments: Boolean,
        val visibleAssets: List<cn.wj.android.cashbook.core.model.model.AssetModel>,
        val expenditureTypes: List<RecordTypeModel>,
        val incomeTypes: List<RecordTypeModel>,
    ) : RecordImportUiState

    /** 导入中 */
    data object Importing : RecordImportUiState

    /** 导入完成 */
    data class Done(val imported: Int, val skipped: Int) : RecordImportUiState

    /** 错误 */
    data class Error(val message: String) : RecordImportUiState
}
