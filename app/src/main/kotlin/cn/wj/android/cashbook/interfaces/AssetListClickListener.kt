package cn.wj.android.cashbook.interfaces

import cn.wj.android.cashbook.data.entity.AssetEntity

/**
 * 资产列表点击事件监听接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/4
 */
interface AssetListClickListener {

    /** 列表 item 点击事件 */
    val onAssetItemClick: (AssetEntity) -> Unit

    /** 列表 item 长点击事件 */
    val onAssetItemLongClick: (AssetEntity) -> Unit
}