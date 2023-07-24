package cn.wj.android.cashbook.core.common

import cn.wj.android.cashbook.core.common.buildlogic.CashbookFlavor

/**
 * 应用信息
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
object ApplicationInfo {

    private var _flavor: CashbookFlavor? = null

    private val flavor: CashbookFlavor
        get() = _flavor ?: CashbookFlavor.Dev

    var versionName = ""

    var applicationId = ""

    val isDev: Boolean
        get() = flavor == CashbookFlavor.Dev

    val infos: String
        get() = "${flavor.name},$isDev,$applicationId,$versionName"


    fun setFlavor(flavorName: String) {
        _flavor = CashbookFlavor.values().firstOrNull { it.name == flavorName }
    }
}