package cn.wj.android.cashbook.data.config

import android.os.Parcelable
import cn.wj.android.cashbook.base.tools.getSharedBoolean
import cn.wj.android.cashbook.base.tools.getSharedInt
import cn.wj.android.cashbook.base.tools.getSharedLong
import cn.wj.android.cashbook.base.tools.getSharedParcelable
import cn.wj.android.cashbook.base.tools.getSharedString
import cn.wj.android.cashbook.base.tools.setSharedBoolean
import cn.wj.android.cashbook.base.tools.setSharedInt
import cn.wj.android.cashbook.base.tools.setSharedLong
import cn.wj.android.cashbook.base.tools.setSharedParcelable
import cn.wj.android.cashbook.base.tools.setSharedString
import cn.wj.android.cashbook.data.constants.SHARED_KEY_BACKUP_PATH
import cn.wj.android.cashbook.data.constants.SHARED_KEY_IGNORE_VERSION
import cn.wj.android.cashbook.data.constants.SHARED_KEY_LAST_BACKUP_MS
import kotlin.reflect.KProperty

/**
 * 应用配置属性
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/6
 */

/**
 * 非空配置代理类
 *
 * @param key 属性 key
 * @param default 属性默认值
 */
class NoNullProperties<T>(val key: String, val default: T)

@Suppress("IMPLICIT_CAST_TO_ANY")
inline operator fun <reified T> NoNullProperties<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
    return (when (default) {
        is String -> {
            getSharedString(key, default)
        }
        is Int -> {
            getSharedInt(key, default)
        }
        is Long -> {
            getSharedLong(key, default)
        }
        is Boolean -> {
            getSharedBoolean(key, default)
        }
        is Parcelable -> {
            getSharedParcelable(key, null) ?: default
        }
        else -> {
            default
        }
    } as? T) ?: default
}

inline operator fun <reified T> NoNullProperties<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    when (value) {
        is String -> {
            setSharedString(key, value)
        }
        is Int -> {
            setSharedInt(key, value)
        }
        is Long -> {
            setSharedLong(key, value)
        }
        is Boolean -> {
            setSharedBoolean(key, value)
        }
        is Parcelable -> {
            setSharedParcelable(key, value)
        }
    }
}

object AppConfigs {

    /** 备份路径 */
    var backupPath: String by NoNullProperties(SHARED_KEY_BACKUP_PATH, "")

    /** 上次备份时间 */
    var lastBackupMs: Long by NoNullProperties(SHARED_KEY_LAST_BACKUP_MS, 0L)

    /** 忽略版本号 */
    var ignoreVersion: String by NoNullProperties(SHARED_KEY_IGNORE_VERSION, "")
}


