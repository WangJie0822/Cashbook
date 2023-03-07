package cn.wj.android.cashbook.feature.records.enums

/**
 * 底部菜单类型
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/27
 */
enum class EditRecordBottomSheetEnum {

    /** 不显示 */
    NONE,

    /** 金额 */
    AMOUNT,

    /** 手续费 */
    CHARGES,

    /** 优惠 */
    CONCESSIONS,

    /** 资产列表 */
    ASSETS,

    /** 关联资产列表 */
    RELATED_ASSETS,

    /** 标签 */
    TAGS,
}