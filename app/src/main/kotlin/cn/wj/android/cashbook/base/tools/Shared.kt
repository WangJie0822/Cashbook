@file:Suppress("unused")
@file:JvmName("SharedTools")

package cn.wj.android.cashbook.base.tools

import cn.wj.android.cashbook.manager.AppManager
import com.tencent.mmkv.MMKV

/**
 * 共享数据相关
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/27
 */

/** 默认 [MMKV] 对象 */
private val mmkv: MMKV? by lazy {
    MMKV.initialize(AppManager.getContext())
    MMKV.defaultMMKV()
}

/** 根据 [key] 获取对应 [String] 值，没有返回默认 [defaultValue] */
@JvmOverloads
fun getSharedString(key: String, defaultValue: String? = null): String? {
    return mmkv?.getString(key, defaultValue)
}

/** 使用 [key] 保存对应值 [value] */
fun setSharedString(key: String, value: String?) {
    mmkv?.encode(key, value)
}