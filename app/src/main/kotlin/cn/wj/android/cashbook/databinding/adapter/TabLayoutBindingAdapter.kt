@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.viewpager2.widget.ViewPager2
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.setupWithViewPager2
import com.google.android.material.tabs.TabLayout

/**
 * TabLayout 相关适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/21
 */

/** 将 [TabLayout] 与 id 为 [viewPager2Id] 的 [ViewPager2] 相关联 */
@BindingAdapter("android:bind_tl_viewpager2")
fun TabLayout.bindViewPager2(viewPager2Id: Int?) {
    if (null == viewPager2Id) {
        return
    }

    val window = (context as? Activity)?.window
    val v = if (null != window) {
        logger().w("window")
        window.decorView.findViewById<View>(viewPager2Id)
    } else {
        logger().w("view")
        var parentView = parent as? ViewGroup
        var vp2View = parentView?.findViewById<View>(viewPager2Id)
        while (null != parentView && null == vp2View) {
            parentView = parentView.parent as? ViewGroup
            vp2View = parentView?.findViewById(viewPager2Id)
        }
        vp2View
    }
    if (null == v) {
        logger().w("View with R.id.$viewPager2Id not found!")
        return
    }
    if (v !is ViewPager2) {
        logger().w("View with R.id.$viewPager2Id is not the ViewPager2!")
        return
    }
    setupWithViewPager2(v)
}