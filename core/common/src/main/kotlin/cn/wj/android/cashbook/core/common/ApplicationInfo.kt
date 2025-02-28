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

package cn.wj.android.cashbook.core.common

import cn.wj.android.cashbook.core.common.buildlogic.CashbookFlavor

/**
 * 应用信息
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
object ApplicationInfo {

    /** 数据库版本 */
    const val DB_VERSION = 11

    /** 渠道信息 */
    private var _flavor: CashbookFlavor? = null

    /** 渠道信息 */
    private val flavor: CashbookFlavor
        get() = _flavor ?: CashbookFlavor.Dev

    /** 应用 id */
    var applicationId = ""

    /** 版本名 */
    var versionName = ""

    /** 是否是 debug 版本 */
    var debug = true

    /** 产线版本是否允许日志输出 */
    var logcatEnable = false

    /** 是否是开发渠道 */
    val isDev: Boolean
        get() = flavor == CashbookFlavor.Dev

    /** 是否是离线渠道 */
    val isOffline: Boolean
        get() = flavor == CashbookFlavor.Offline

    /** 是否是在线渠道 */
    val isOnline: Boolean
        get() = flavor == CashbookFlavor.Online

    /** 是否是产线渠道 */
    val isProduction: Boolean
        get() = isOnline || isOffline

    /** 应用信息 */
    val applicationInfo: String
        get() = "${flavor.name},$isDev,$applicationId,$versionName,$DB_VERSION"

    /** 通过渠道名 [flavorName] 设置渠道信息 */
    fun setFlavor(flavorName: String) {
        _flavor = CashbookFlavor.entries.firstOrNull { it.name == flavorName }
    }
}
