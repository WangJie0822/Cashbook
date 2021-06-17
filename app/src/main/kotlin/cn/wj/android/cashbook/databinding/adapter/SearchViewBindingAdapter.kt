@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.widget.SearchView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener

/*
 * SearchView DataBinding 适配器
 */

/** 搜索文本 */
@set:BindingAdapter("android:bind_search_text")
@get:InverseBindingAdapter(
    attribute = "android:bind_search_text",
    event = "android:bind_search_textAttrChanged"
)
var SearchView.text: String?
    get() = query.toString()
    set(value) {
        setQuery(value.orEmpty(), false)
    }

/** 搜索文本、搜索监听 */
@BindingAdapter("android:bind_search_onChange", "android:bind_search_onSubmit", "android:bind_search_textAttrChanged", requireAll = false)
fun SearchView.setOnQueryTextListener(onChanged: ((String) -> Unit)?, onSubmit: ((String) -> Unit)?, listener: InverseBindingListener?) {
    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean {
            return if (null != onSubmit) {
                onSubmit.invoke(query)
                true
            } else {
                false
            }
        }

        override fun onQueryTextChange(newText: String): Boolean {
            listener?.onChange()
            return if (null != onChanged) {
                onChanged.invoke(newText)
                true
            } else {
                false
            }
        }
    })
}