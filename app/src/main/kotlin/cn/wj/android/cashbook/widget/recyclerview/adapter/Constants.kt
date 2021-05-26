@file:Suppress("unused")

package cn.wj.android.cashbook.widget.recyclerview.adapter

/** 没有动画 */
const val ADAPTER_ANIM_NONE = 0x00000000

/** 插入动画 */
const val ADAPTER_ANIM_INSERTED = 0x00000001

/** 删除动画 */
const val ADAPTER_ANIM_REMOVED = 0x00000002

/** 移动动画 */
const val ADAPTER_ANIM_MOVED = 0x00000004

/** 变化动画 */
const val ADAPTER_ANIM_CHANGED = 0x00000008

/** 全部动画 */
const val ADAPTER_ANIM_ALL = ADAPTER_ANIM_INSERTED or ADAPTER_ANIM_REMOVED or ADAPTER_ANIM_MOVED or ADAPTER_ANIM_CHANGED
