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
import cn.wj.android.cashbook.core.common.REMINDER_TARGET_REIMBURSEMENT
import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ADD
import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ASSET

/**
 * 待消费的深链意图（统一应用快捷方式与提醒通知深链）。
 *
 * 一次性消费：导航后由调用方复位为 [None] 并清除 intent extra，避免 uiState 重发时弹回。
 */
sealed interface PendingDeepLink {
    /** 无待处理深链 */
    data object None : PendingDeepLink

    /** 快捷方式：记一笔 */
    data object AddRecord : PendingDeepLink

    /** 快捷方式：我的资产 */
    data object MyAsset : PendingDeepLink

    /** 提醒：信用卡资产详情 */
    data class AssetInfo(val assetId: Long) : PendingDeepLink

    /** 提醒：待报销列表 */
    data object Reimbursement : PendingDeepLink
}

/**
 * 合并解析 intent 三个 extra 为单一深链意图（纯函数）。
 * 优先级：reminder 高于 shortcuts（二者构造上互斥，此优先级为防御性）。
 */
internal fun parsePendingDeepLink(
    shortcutsType: Int,
    reminderTarget: Int,
    reminderAssetId: Long,
): PendingDeepLink = when {
    reminderTarget == REMINDER_TARGET_ASSET -> PendingDeepLink.AssetInfo(reminderAssetId)
    reminderTarget == REMINDER_TARGET_REIMBURSEMENT -> PendingDeepLink.Reimbursement
    shortcutsType == SHORTCUTS_TYPE_ADD -> PendingDeepLink.AddRecord
    shortcutsType == SHORTCUTS_TYPE_ASSET -> PendingDeepLink.MyAsset
    else -> PendingDeepLink.None
}
