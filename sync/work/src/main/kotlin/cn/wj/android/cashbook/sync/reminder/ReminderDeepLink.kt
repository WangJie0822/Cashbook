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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import cn.wj.android.cashbook.core.common.EXTRA_REMINDER_ASSET_ID
import cn.wj.android.cashbook.core.common.EXTRA_REMINDER_TARGET

/** MainActivity 全限定类名（显式组件 Intent，禁用隐式 action/导出 deepLink，防绕安全门 + 攻击面） */
private const val MAIN_ACTIVITY_CLASS_NAME = "cn.wj.android.cashbook.ui.MainActivity"

/**
 * 构造提醒通知点击的深链 [PendingIntent]。
 *
 * - 用显式组件 Intent（[Context.getPackageName] + [MAIN_ACTIVITY_CLASS_NAME]）而非隐式 action，避免被第三方劫持。
 * - [PendingIntent.FLAG_IMMUTABLE]（Android 12+ 强制）。
 * - extra 携带目标类型 [target] + [assetId]，由 MainActivity 解析后经受安全验证门控的逻辑导航。
 */
internal fun reminderDeepLinkIntent(context: Context, target: Int, assetId: Long): PendingIntent {
    val intent = Intent().apply {
        setClassName(context.packageName, MAIN_ACTIVITY_CLASS_NAME)
        putExtra(EXTRA_REMINDER_TARGET, target)
        putExtra(EXTRA_REMINDER_ASSET_ID, assetId)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    return PendingIntent.getActivity(
        context,
        target * 100000 + assetId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
