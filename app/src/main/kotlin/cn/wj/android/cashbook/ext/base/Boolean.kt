@file:Suppress("unused")
@file:JvmName("BooleanExt")

package cn.wj.android.cashbook.ext.base

/** 仅为`true`时为`true` */
val Boolean?.condition: Boolean
    get() = this == true

/** [Boolean]为`null`则默认`false` */
fun Boolean?.orFalse() = this ?: false

/** [Boolean]为`null`则默认`true` */
fun Boolean?.orTrue() = this ?: true