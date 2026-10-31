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

package cn.wj.android.cashbook.core.model.model

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/** 关键词分页与全量统计共享的不可变查询上下文。 */
data class RecordKeywordQuery(val bookId: Long, val keyword: String, val amountCent: Long)

/** 分类合并后的有符号净额，单位为分，名称不作为分组键。 */
data class RecordCategoryNet(
    val typeId: Long,
    val typeCategory: RecordTypeCategoryEnum,
    val typeName: String,
    val amount: Long,
)

/** 同一次聚合产生的汇总与分类分布，极值错误不能静默回绕。 */
data class RecordSearchStatistics(val summary: RecordFilterSummary, val categories: List<RecordCategoryNet>) {
    val magnitude: Long = categories.fold(0L) { total, category ->
        if (category.amount == Long.MIN_VALUE) throw ArithmeticException("Category absolute amount overflow")
        Math.addExact(total, kotlin.math.abs(category.amount))
    }

    init {
        Math.subtractExact(summary.income, summary.expenditure)
    }
}
