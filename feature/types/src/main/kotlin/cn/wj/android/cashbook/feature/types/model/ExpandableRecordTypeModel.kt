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

package cn.wj.android.cashbook.feature.types.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.wj.android.cashbook.core.model.model.RecordTypeModel

data class ExpandableRecordTypeModel(
    val data: RecordTypeModel,
    val reimburseType: Boolean,
    val refundType: Boolean,
    val creditCardPaymentType: Boolean,
    val list: List<ExpandableRecordTypeModel>,
) {
    var expanded by mutableStateOf(false)
}
