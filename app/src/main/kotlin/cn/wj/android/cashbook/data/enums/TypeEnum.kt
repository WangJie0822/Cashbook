package cn.wj.android.cashbook.data.enums

/**
 * 类型类别枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/8
 */
enum class TypeEnum {

    /** 一级大类 */
    FIRST,

    /** 二级小类 */
    SECOND;

    companion object {

        fun fromName(name: String?): TypeEnum? {
            return values().firstOrNull { type -> type.name == name }
        }
    }
}