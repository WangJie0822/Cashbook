package cn.wj.android.cashbook.databinding.utils

import android.content.Context
import cn.wj.android.cashbook.databinding.constants.RESOURCE_MARK
import cn.wj.android.cashbook.databinding.constants.RESOURCE_SPLIT
import cn.wj.android.cashbook.manager.AppManager

/** 根据资源类型[defType]、资源id字符串，获取对应的资源id */
fun String.getIdentifier(context: Context, defType: String): Int {
    return context.resources.getIdentifier(this, defType, context.packageName)
}

/** 根据资源字符串 @drawable/xxx 获取对应资源 id */
fun String.getIdentifier(context: Context = AppManager.getContext()): Int {
    val params = this.replace(RESOURCE_MARK, "").split(RESOURCE_SPLIT)
    if (params.size != 2) {
        return 0
    }
    return context.resources.getIdentifier(params[1], params[0], context.packageName)
}