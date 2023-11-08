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

data class RecordViewsRelation(
    val id: Long,
    val typeCategory: Int,
    val typeName: String,
    val typeIconResName: String,
    val assetName: String?,
    val assetClassification: Int?,
    val relatedAssetName: String?,
    val relatedAssetClassification: Int?,
    val amount: Double,
    val charges: Double,
    val concessions: Double,
    val remark: String,
    val reimbursable: Int,
    val recordTime: Long,
)
