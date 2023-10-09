package cn.wj.android.cashbook.core.model.enums

/**
 * Markdown 文档类型
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/13
 */
enum class MarkdownTypeEnum {

    /** 更新日志 */
    CHANGELOG,

    /** 隐私协议 */
    PRIVACY_POLICY,
    ;

    companion object {

        fun ordinalOf(ordinal: Int): MarkdownTypeEnum? {
            return entries.firstOrNull { it.ordinal == ordinal }
        }
    }
}