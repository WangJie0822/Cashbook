package cn.wj.android.cashbook.data.entity

import android.os.Parcelable
import cn.wj.android.cashbook.data.model.CurrencyModel
import kotlinx.parcelize.Parcelize

/**
 * 账本数据实体类
 *
 * @param id 账本 id 主键自增长
 * @param name 账本名
 * @param imageUrl 账本封面地址
 * @param description 描述
 * @param currency [CurrencyModel] 默认货币信息
 * @param selected 是否默认选中
 * @param createTime 创建时间
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Parcelize
data class BooksEntity(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val description: String,
    val currency: CurrencyModel?,
    val selected: Boolean,
    val createTime: String,
    val modifyTime: String
) : Parcelable