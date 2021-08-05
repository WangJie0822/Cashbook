package cn.wj.android.cashbook.interfaces

import cn.wj.android.cashbook.data.entity.RecordEntity

/**
 * 记录列表点击接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/15
 */
interface RecordListClickListener {

    /** 点击回调 */
    val onRecordItemClick: (RecordEntity) -> Unit

    /** 长点击回调 */
    val onRecordItemLongClick: (RecordEntity) -> Unit
}