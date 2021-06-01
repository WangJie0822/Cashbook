@file:Suppress("unused")
@file:JvmName("FragmentManagerExt")

package cn.wj.android.cashbook.base.ext

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/** 获取第一个显示的 [Fragment]，没有返回 `null` */
fun FragmentManager.firstVisibleFragmentOrNull(): Fragment? {
    return fragments.firstOrNull {
        it.isVisible
    }
}