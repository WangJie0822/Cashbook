package cn.wj.android.cashbook.interfaces

import cn.wj.android.cashbook.data.enums.AssetClassificationEnum

/**
 * 资产分类列表点击事件监听接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
interface AssetClassificationListClickListener {

    /** 列表 item 点击事件 */
    val onItemClick: (AssetClassificationEnum) -> Unit
}