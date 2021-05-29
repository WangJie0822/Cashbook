package cn.wj.android.cashbook.databinding.adapter

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.drawerlayout.widget.DrawerLayout
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.databinding.adapter.getIdentifier

/**
 * DrawerLayout 相关适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/21
 */

/** 将 [DrawerLayout] 开启关联到对应 [toolbarId] 的 [Toolbar] */
@BindingAdapter("android:bind_dl_toolbar")
fun DrawerLayout.bindToToolbar(@IdRes toolbarId: Int?) {
    if (null == toolbarId) {
        return
    }
    val v = findViewById<View>(toolbarId)
    if (null == v) {
        logger().w("View with R.id.$toolbarId not found!")
        return
    }
    if (v !is Toolbar) {
        logger().w("View with R.id.$toolbarId is not the Toolbar!")
        return
    }
    v.setNavigationOnClickListener {
        open()
    }
}