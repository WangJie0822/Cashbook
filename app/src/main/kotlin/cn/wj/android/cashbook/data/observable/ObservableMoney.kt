@file:Suppress("unused")

package cn.wj.android.cashbook.data.observable

import androidx.databinding.Observable
import androidx.databinding.ObservableField
import cn.wj.android.cashbook.base.ext.base.orElse
import java.math.BigDecimal

/**
 * 金额相关数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/24
 */
open class ObservableMoney : ObservableField<String> {

    constructor() : super()

    constructor(value: String) : super(value)

    constructor(vararg dependencies: Observable) : super(*dependencies)

    override fun get(): String {
        return super.get().orElse("0")
    }

    fun set(value: BigDecimal) {
        set(value.toPlainString())
    }

    /** 对应 [BigDecimal] 对象 */
    val bigDecimalVal: BigDecimal
        get() = get().toBigDecimal()
}