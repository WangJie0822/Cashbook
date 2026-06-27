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

package cn.wj.android.cashbook.sync.reminder

import cn.wj.android.cashbook.core.common.REMINDER_TARGET_ASSET
import cn.wj.android.cashbook.core.common.REMINDER_TARGET_REIMBURSEMENT
import cn.wj.android.cashbook.sync.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [toNotificationSpec] 映射纯函数单测（纯 JVM，断言 R 资源 id 常量 + 深链目标）。 */
class NotificationSpecTest {

    @Test
    fun billing_mapsToBillingTextAssetTarget() {
        val spec = ReminderItem.CreditCardBilling(5L, "招行").toNotificationSpec()
        assertThat(spec).isEqualTo(
            NotificationSpec(R.string.reminder_credit_billing, listOf("招行"), REMINDER_TARGET_ASSET, 5L),
        )
    }

    @Test
    fun repayment_mapsToRepaymentTextAssetTarget() {
        val spec = ReminderItem.CreditCardRepayment(7L, "中信").toNotificationSpec()
        assertThat(spec).isEqualTo(
            NotificationSpec(R.string.reminder_credit_repayment, listOf("中信"), REMINDER_TARGET_ASSET, 7L),
        )
    }

    @Test
    fun reimbursement_mapsToReimbursementTextReimbursementTarget() {
        val spec = ReminderItem.Reimbursement(4).toNotificationSpec()
        assertThat(spec).isEqualTo(
            NotificationSpec(R.string.reminder_reimbursement, listOf(4), REMINDER_TARGET_REIMBURSEMENT, -1L),
        )
    }
}
