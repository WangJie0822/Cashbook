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

package cn.wj.android.cashbook.ui

import cn.wj.android.cashbook.core.common.REMINDER_TARGET_ASSET
import cn.wj.android.cashbook.core.common.REMINDER_TARGET_NONE
import cn.wj.android.cashbook.core.common.REMINDER_TARGET_REIMBURSEMENT
import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ADD
import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ASSET
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [parsePendingDeepLink] 纯函数单测（纯 JVM）。 */
class PendingDeepLinkTest {

    @Test
    fun reminderAsset_parsesToAssetInfo() {
        val result = parsePendingDeepLink(
            shortcutsType = -1,
            reminderTarget = REMINDER_TARGET_ASSET,
            reminderAssetId = 9L,
        )
        assertThat(result).isEqualTo(PendingDeepLink.AssetInfo(9L))
    }

    @Test
    fun reminderReimbursement_parsesToReimbursement() {
        val result = parsePendingDeepLink(-1, REMINDER_TARGET_REIMBURSEMENT, -1L)
        assertThat(result).isEqualTo(PendingDeepLink.Reimbursement)
    }

    @Test
    fun shortcutAdd_parsesToAddRecord() {
        val result = parsePendingDeepLink(SHORTCUTS_TYPE_ADD, REMINDER_TARGET_NONE, -1L)
        assertThat(result).isEqualTo(PendingDeepLink.AddRecord)
    }

    @Test
    fun shortcutAsset_parsesToMyAsset() {
        val result = parsePendingDeepLink(SHORTCUTS_TYPE_ASSET, REMINDER_TARGET_NONE, -1L)
        assertThat(result).isEqualTo(PendingDeepLink.MyAsset)
    }

    @Test
    fun nothing_parsesToNone() {
        val result = parsePendingDeepLink(-1, REMINDER_TARGET_NONE, -1L)
        assertThat(result).isEqualTo(PendingDeepLink.None)
    }

    @Test
    fun reminderTakesPriorityOverShortcut() {
        // 同时带 reminder 与 shortcut → reminder 优先（构造上互斥，防御性）
        val result = parsePendingDeepLink(SHORTCUTS_TYPE_ADD, REMINDER_TARGET_ASSET, 3L)
        assertThat(result).isEqualTo(PendingDeepLink.AssetInfo(3L))
    }
}
