@file:Suppress("unused")
@file:JvmName("MutableLiveDataExt")

package cn.wj.android.cashbook.base.ext.base

import androidx.lifecycle.MutableLiveData

/**
 * [MutableLiveData] 相关
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2021/3/25
 */

/** 当 [MutableLiveData] 泛型类型为 [ArrayList] 时添加 [list] 数据并更新 */
fun <E> MutableLiveData<ArrayList<E>>.addAll(list: ArrayList<E>) {
    val ls = this.value.toNewList()
    ls.addAll(list)
    this.value = ls
}

/** 当 [MutableLiveData] 泛型类型为 [ArrayList] 时添加 [list] 数据到 [index] 位置并更新 */
fun <E> MutableLiveData<ArrayList<E>>.addAll(index: Int, list: ArrayList<E>) {
    val ls = this.value.toNewList()
    ls.addAll(index, list)
    this.value = ls
}

/** 当 [MutableLiveData] 泛型类型为 [ArrayList] 时添加 [item] 数据并更新 */
fun <E> MutableLiveData<ArrayList<E>>.add(item: E) {
    val ls = this.value.toNewList()
    ls.add(item)
    this.value = ls
}

/** 当 [MutableLiveData] 泛型类型为 [ArrayList] 时添加 [item] 数据到 [index] 位置并更新 */
fun <E> MutableLiveData<ArrayList<E>>.add(index: Int, item: E) {
    val ls = this.value.toNewList()
    ls.add(index, item)
    this.value = ls
}

/** 当 [MutableLiveData] 泛型类型为 [ArrayList] 时移除 [item] 数据并更新 */
fun <E> MutableLiveData<ArrayList<E>>.remove(item: E) {
    val ls = this.value.toNewList()
    ls.remove(item)
    this.value = ls
}