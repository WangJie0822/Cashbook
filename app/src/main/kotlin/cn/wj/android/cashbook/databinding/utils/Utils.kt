package cn.wj.android.databinding.adapter

import android.content.Context

/** 根据资源类型[defType]、资源id字符串，获取对应的资源id */
internal fun String.getIdentifier(context: Context, defType: String): Int {
    return context.resources.getIdentifier(this, defType, context.packageName)
}