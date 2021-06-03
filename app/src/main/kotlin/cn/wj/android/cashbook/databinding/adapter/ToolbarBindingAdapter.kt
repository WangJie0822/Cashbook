@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter

/** 为 [Toolbar] 设置菜单点击监听 [itemClick] */
@BindingAdapter("android:bind_toolbar_menuItemClick")
fun Toolbar.setToolbarMenuItemClick(itemClick: ((MenuItem) -> Unit)?) {
    if (null == itemClick) {
        this.setOnMenuItemClickListener(null)
        return
    }
    this.setOnMenuItemClickListener {
        itemClick.invoke(it)
        true
    }
}

/** 为 [Toolbar] 设置菜单点击监听 [itemClick] */
@BindingAdapter("android:bind_toolbar_menuItemClickId")
fun Toolbar.setToolbarMenuItemIdClick(itemClick: ((Int) -> Unit)?) {
    if (null == itemClick) {
        this.setOnMenuItemClickListener(null)
        return
    }
    this.setOnMenuItemClickListener {
        itemClick.invoke(it.itemId)
        true
    }
}

/** [Toolbar] 设置菜单点击回调，有且仅有一个菜单时使用 */
@BindingAdapter("android:bind_toolbar_onClick")
fun Toolbar.setToolbarMenuClick(click: (() -> Unit)?) {
    if (null == click) {
        this.setOnMenuItemClickListener(null)
        return
    }
    this.setOnMenuItemClickListener {
        click.invoke()
        true
    }
}

/** 为 [Toolbar] 设置导航按钮点击监听 [click] */
@BindingAdapter("android:bind_toolbar_navigationClick")
fun Toolbar.setToolbarNavigationClick(click: (() -> Unit)?) {
    this.setNavigationOnClickListener { click?.invoke() }
}

/** 将 [Toolbar] 标题设置为 [title] */
@BindingAdapter("android:bind_toolbar_title")
fun Toolbar.setToolbarTitle(title: String?) {
    this.title = title
}