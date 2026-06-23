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

package cn.wj.android.cashbook.core.data

import cn.wj.android.cashbook.core.model.model.RecordSettingsModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [RecordSettingsModel.monthStartDay] 字段默认值与可设置性测试。
 */
class RecordSettingsModelMonthStartDayTest {

    @Test
    fun defaultMonthStartDayIsOne() {
        val model = RecordSettingsModel(
            currentBookId = 1L,
            defaultTypeId = 1L,
            lastAssetId = -1L,
            refundTypeId = -1L,
            reimburseTypeId = -1L,
            creditCardPaymentTypeId = -1L,
            topUpInTotal = false,
        )
        assertThat(model.monthStartDay).isEqualTo(1)
    }

    @Test
    fun monthStartDayCanBeSet() {
        val model = RecordSettingsModel(
            currentBookId = 1L,
            defaultTypeId = 1L,
            lastAssetId = -1L,
            refundTypeId = -1L,
            reimburseTypeId = -1L,
            creditCardPaymentTypeId = -1L,
            topUpInTotal = false,
            monthStartDay = 15,
        )
        assertThat(model.monthStartDay).isEqualTo(15)
    }
}
