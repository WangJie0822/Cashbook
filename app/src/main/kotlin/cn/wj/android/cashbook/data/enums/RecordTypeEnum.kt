package cn.wj.android.cashbook.data.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 记录类型枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/8
 */
@Parcelize
enum class RecordTypeEnum(
    val position: Int
) : Parcelable {

    // 支出
    EXPENDITURE(0),

    // 收入
    INCOME(1),

    // 转账
    TRANSFER(2);

    companion object {

        fun fromName(name: String?): RecordTypeEnum? {
            return values().firstOrNull { type -> type.name == name }
        }

        fun fromPosition(position: Int): RecordTypeEnum? {
            return values().firstOrNull { type -> type.position == position }
        }
    }
}