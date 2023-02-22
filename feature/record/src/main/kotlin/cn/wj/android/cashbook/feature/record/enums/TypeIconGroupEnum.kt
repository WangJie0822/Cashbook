package cn.wj.android.cashbook.feature.record.enums

import androidx.annotation.StringRes
import cn.wj.android.cashbook.core.ui.R as UiR

/**
 * 类型图标分组枚举
 *
 * @param nameResId 分组名称资源 id
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
enum class TypeIconGroupEnum(
    @StringRes val nameResId: Int
) {
    /** 收入 */
    INCOME(UiR.string.income),

    /** 转账 */
    TRANSFER(UiR.string.transfer),

    /** 理财 */
    MONEY_MANAGEMENT(UiR.string.money_management),

    /** 餐饮 */
    DINING(UiR.string.dining),

    /** 日常 */
    DAILY(UiR.string.daily),

    /** 购物 */
    SHOPPING(UiR.string.shopping),

    /** 服饰 */
    DRESS(UiR.string.dress),

    /** 数码 */
    DIGITAL(UiR.string.digital),

    /** 娱乐 */
    ENTERTAINMENT(UiR.string.entertainment),

    /** 家庭 */
    FAMILY(UiR.string.family),

    /** 育儿 */
    PARENTING(UiR.string.parenting),

    /** 汽车 */
    CAR(UiR.string.car),

    /** 人际关系 */
    INTERPERSONAL(UiR.string.interpersonal),

    /** 交通 */
    TRAFFIC(UiR.string.traffic),

    /** 住房 */
    HOUSING(UiR.string.housing),

    /** 医疗 */
    MEDICAL(UiR.string.medical),

    /** 校园 */
    CAMPUS(UiR.string.campus),

    /** 物业 */
    TENEMENT(UiR.string.tenement),

    /** 其它 */
    OTHER(UiR.string.other),


}