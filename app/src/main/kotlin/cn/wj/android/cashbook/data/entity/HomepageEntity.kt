package cn.wj.android.cashbook.data.entity

import cn.wj.android.cashbook.base.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_MONTH_DAY
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.toLongTime

/**
 * 首页数据实体类
 *
 * @param date 时间
 * @param list 记录列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/11
 */
data class HomepageEntity(
    val date: String,
    val list: List<RecordEntity>
) {

}