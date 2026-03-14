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
 * 记录轻量汇总数据，用于收支统计计算，不含关联数据的 N+1 查询
 */
data class RecordViewSummaryModel(
    val id: Long,
    val typeCategory: Int,
    val typeName: String,
    val amount: Long,
    val finalAmount: Long,
    val charges: Long,
    val concessions: Long,
    val recordTime: Long,
)
