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

package cn.wj.android.cashbook.core.model.transfer

import cn.wj.android.cashbook.core.model.model.RecordModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelTransferReimbursedTest {

    private fun model(reimbursed: Boolean) = RecordModel(
        id = 1L, booksId = 1L, typeId = 1L, assetId = -1L, relatedAssetId = -1L,
        amount = 0L, finalAmount = 0L, charges = 0L, concessions = 0L, remark = "",
        reimbursable = true, recordTime = 0L, reimbursed = reimbursed,
    )

    @Test
    fun recordModel_asEntity_then_asModel_preserves_reimbursed() {
        assertThat(model(true).asEntity().asModel().reimbursed).isTrue()
        assertThat(model(false).asEntity().asModel().reimbursed).isFalse()
    }
}
