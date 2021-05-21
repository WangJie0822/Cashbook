@file:Suppress("unused")
@file:JvmName("StringTransform")

package cn.wj.android.cashbook.data.transform

import cn.wj.android.cashbook.data.model.SnackbarModel

/**
 * String 类型转换相关
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/21
 */


/** 根据 [String] 生成并返回 [SnackbarModel] */
fun String?.toSnackbarModel(): SnackbarModel {
    return SnackbarModel(this.orEmpty())
}