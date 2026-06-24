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

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.BudgetRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.BUDGET_TYPE_ID_TOTAL
import cn.wj.android.cashbook.core.model.entity.BudgetProgressEntity
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.buildBudgetItem
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.analyticsPieNetAmount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 聚合「本周期预算进度」。
 *
 * - 各分类已花：复用 [TransRecordViewsToAnalyticsPieUseCase] 的净自付一级分类滚动汇总。
 * - 总体已花：直接对本周期 EXPENDITURE 求 Σ [analyticsPieNetAmount]（不靠 Σ pieList，规避孤儿少算）。
 * - 「本月」走可配置月周期 [DateSelectionEntity.currentMonthPeriod]。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/23
 */
class GetBudgetProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val booksRepository: BooksRepository,
    private val settingRepository: SettingRepository,
    private val typeRepository: TypeRepository,
    private val getRecordViewsBetweenDateUseCase: GetRecordViewsBetweenDateUseCase,
    private val transRecordViewsToAnalyticsPieUseCase: TransRecordViewsToAnalyticsPieUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(today: LocalDate = LocalDate.now()): BudgetProgressEntity =
        withContext(coroutineContext) {
            val monthStartDay = settingRepository.recordSettingsModel.first().monthStartDay
            val booksId = booksRepository.currentBook.first().id
            val period = DateSelectionEntity.currentMonthPeriod(today, monthStartDay)
            val records = getRecordViewsBetweenDateUseCase(period, monthStartDay)
            val pieList =
                transRecordViewsToAnalyticsPieUseCase(RecordTypeCategoryEnum.EXPENDITURE, records)
            val budgets = budgetRepository.getBudgetsByBooks(booksId)

            // 总体已花：直接 Σ 本周期 EXPENDITURE 净自付（与 Pie 内部 total 同算法）
            val totalSpent = records
                .filter { !it.isBalanceRecord && it.type.typeCategory == RecordTypeCategoryEnum.EXPENDITURE }
                .sumOf {
                    analyticsPieNetAmount(
                        RecordTypeCategoryEnum.EXPENDITURE,
                        it.finalAmount,
                        it.amount,
                        it.charges,
                        it.concessions,
                    )
                }

            val overall = budgets.firstOrNull { it.typeId == BUDGET_TYPE_ID_TOTAL }
                ?.let { buildBudgetItem(BUDGET_TYPE_ID_TOTAL, "", "", it.amount, totalSpent) }

            val categoryList = budgets
                .filter { it.typeId != BUDGET_TYPE_ID_TOTAL }
                .mapNotNull { budget ->
                    // 防御孤儿：分类已删（type 为 null）则跳过，避免渲染空名预算项
                    val type = typeRepository.getRecordTypeById(budget.typeId) ?: return@mapNotNull null
                    val spent = pieList.firstOrNull { it.typeId == budget.typeId }?.totalAmount ?: 0L
                    buildBudgetItem(budget.typeId, type.name, type.iconName, budget.amount, spent)
                }

            BudgetProgressEntity(overall = overall, categoryList = categoryList)
        }
}
