package cn.wj.android.cashbook.data.entity

import android.os.Parcelable
import androidx.databinding.ObservableBoolean
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * 标签数据实体类
 *
 * @param id 主键自增长
 * @param name 标签名称
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
@Parcelize
data class TagEntity(
    val id: Long,
    val name: String,
) : Parcelable {

    /** 选中状态 */
    @IgnoredOnParcel
    val selected: ObservableBoolean by lazy {
        ObservableBoolean(false)
    }
}