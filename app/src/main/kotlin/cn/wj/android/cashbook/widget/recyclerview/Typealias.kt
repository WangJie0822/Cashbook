package cn.wj.android.cashbook.widget.recyclerview

/**
 * 别名
 *
 * @author 王杰
 * @date 2019/8/9
 */

/** 类型别名，RecyclerView 空布局点击 */
typealias OnEmptyClickListener = () -> Unit

typealias AreContentsTheSame<E> = (E, E) -> Boolean