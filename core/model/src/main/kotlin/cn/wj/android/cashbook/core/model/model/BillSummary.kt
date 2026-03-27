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

/**
 * 账单汇总信息
 *
 * @param totalCount 总记录数
 * @param incomeCount 收入笔数
 * @param incomeAmount 收入总金额
 * @param expenditureCount 支出笔数
 * @param expenditureAmount 支出总金额
 */
data class BillSummary(
    val totalCount: Int,
    val incomeCount: Int,
    val incomeAmount: Double,
    val expenditureCount: Int,
    val expenditureAmount: Double,
)
