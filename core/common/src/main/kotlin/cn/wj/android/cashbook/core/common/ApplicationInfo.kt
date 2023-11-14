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

    const val dbVersion = 7

    private var _flavor: CashbookFlavor? = null

    private val flavor: CashbookFlavor
        get() = _flavor ?: CashbookFlavor.Dev

    var versionName = ""

    var applicationId = ""

    var debug = false

    val isDev: Boolean
        get() = flavor == CashbookFlavor.Dev

    val isOffline: Boolean
        get() = flavor == CashbookFlavor.Offline

    val infos: String
        get() = "${flavor.name},$isDev,$applicationId,$versionName,$dbVersion"

    fun setFlavor(flavorName: String) {
        _flavor = CashbookFlavor.entries.firstOrNull { it.name == flavorName }
    }
}
