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