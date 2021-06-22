@file:Suppress("unused")
@file:JvmName("SharedTools")

package cn.wj.android.cashbook.base.tools

import android.os.Parcelable
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.manager.AppManager
import com.tencent.mmkv.MMKV

/**
 * 共享数据相关
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/27
 */

/** 默认 [MMKV] 对象 */
val mmkv: MMKV? by lazy {
    MMKV.initialize(AppManager.getContext())
    MMKV.defaultMMKV()
}

/** 根据 [key] 获取对应 [String] 值，没有返回默认 [defaultValue] */
@JvmOverloads
fun getSharedString(key: String, defaultValue: String? = null): String? {
    return mmkv?.decodeString(key, defaultValue)
}

/** 使用 [key] 保存对应值 [value] */
fun setSharedString(key: String, value: String?) {
    mmkv?.encode(key, value)
}

/** 根据 [key] 获取对应 [Boolean] 值，没有返回默认 [defaultValue] */
@JvmOverloads
fun getSharedBoolean(key: String, defaultValue: Boolean = false): Boolean? {
    return mmkv?.decodeBool(key, defaultValue)
}

/** 使用 [key] 保存对应值 [value] */
fun setSharedBoolean(key: String, value: Boolean) {
    mmkv?.encode(key, value)
}

/** 根据 [key] 获取对应 [Parcelable] 值，没有返回默认 [defaultValue] */
inline fun <reified T : Parcelable> getSharedParcelable(key: String, defaultValue: T? = null): T? {
    return mmkv?.decodeParcelable(key, T::class.java, defaultValue)
}

/** 使用 [key] 保存对应值 [value] */
fun <T : Parcelable> setSharedParcelable(key: String, value: T?) {
    mmkv?.encode(key, value)
}

/** 根据 [key] 获取对应 [Long] 值，没有返回默认 [defaultValue] */
@JvmOverloads
fun getSharedLong(key: String, defaultValue: Long? = null): Long? {
    return mmkv?.decodeLong(key, defaultValue.orElse(-1L))
}

/** 使用 [key] 保存对应值 [value] */
fun setSharedLong(key: String, value: Long?) {
    mmkv?.encode(key, value.orElse(-1L))
}

/** 根据 [key] 获取对应 [Int] 值，没有返回默认 [defaultValue] */
@JvmOverloads
fun getSharedInt(key: String, defaultValue: Int? = null): Int? {
    return mmkv?.decodeInt(key, defaultValue.orElse(-1))
}

/** 使用 [key] 保存对应值 [value] */
fun setSharedInt(key: String, value: Int?) {
    mmkv?.encode(key, value.orElse(-1))
}