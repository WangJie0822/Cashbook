@file:Suppress("unused")
@file:JvmName("ListExt")

package cn.wj.android.cashbook.base.ext.base

/**
 * 集合相关
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 2021/3/25
 */

/** 将 [MutableList] 转换为 [ArrayList] */
fun <E> MutableList<E>.toArrayList(): ArrayList<E> {
    return ArrayList(this)
}

/** 将当前 [ArrayList] 中的数据转移到新的 [ArrayList] 中并返回 */
fun <E> List<E>?.toNewList(): ArrayList<E> {
    return if (null == this) {
        arrayListOf()
    } else {
        ArrayList(this)
    }
}