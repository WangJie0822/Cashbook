package cn.wj.android.cashbook.feature.records.enums

/**
 * 编辑记录提示枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/26
 */
enum class EditRecordBookmarkEnum {

    /** 无提示 */
    NONE,

    /** 金额不能为 0 */
    AMOUNT_MUST_NOT_BE_ZERO,

    /** 类型不能为空 */
    TYPE_MUST_NOT_BE_NULL,

    /** 类型不匹配 */
    TYPE_NOT_MATCH_CATEGORY,

    /** 保存失败 */
    SAVE_FAILED,
}