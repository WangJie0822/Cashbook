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

package cn.wj.android.cashbook.core.database.relation

/** 一条分组 SQL 的结果；未知分类及平账仍携带笔数，但不进入饼图。 */
data class RecordSearchStatisticsRelation(
    val typeId: Long,
    val typeName: String?,
    val typeCategory: Int?,
    val excluded: Boolean,
    val totalCount: Long,
    val income: Long,
    val expenditure: Long,
    val invalidAmounts: Long,
)
