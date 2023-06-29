package cn.wj.android.cashbook.core.model.model

/**
 * 资产大类显示数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/27
 */
data class AssetTypeViewsModel(
    val nameResId: Int,
    val totalAmount: String,
    val assetList: List<AssetModel>,
)