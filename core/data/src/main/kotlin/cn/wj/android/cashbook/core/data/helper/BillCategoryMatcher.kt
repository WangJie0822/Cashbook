/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.data.helper

import cn.wj.android.cashbook.core.model.model.RecordTypeModel

/**
 * 账单分类关键词匹配器
 *
 * 根据交易对方和商品描述，通过内置关键词规则表匹配分类名称，
 * 再在用户的分类列表中查找对应的 typeId。
 */
object BillCategoryMatcher {

    /**
     * 分类关键词规则表
     *
     * key = 分类名称（需与数据库中一级或二级分类名称匹配）
     * value = 匹配关键词列表
     */
    private val CATEGORY_RULES: Map<String, List<String>> = mapOf(
        "餐饮" to listOf("餐", "饭", "小吃", "奶茶", "咖啡", "外卖", "美团", "饿了么", "肯德基", "麦当劳", "烧烤", "烤串", "火锅", "面馆", "酒楼", "食堂", "快餐", "点点", "蜜雪", "瑞幸", "星巴克", "卡旺卡", "沙县"),
        "交通" to listOf("充电", "加油", "停车", "打车", "滴滴", "高速", "地铁", "公交", "出行", "汽车", "车辆", "ETC", "城泊", "星星充电"),
        "购物" to listOf("超市", "商城", "淘宝", "京东", "拼多多", "天猫", "唯品会", "商店", "百货"),
        "通讯" to listOf("中国电信", "中国移动", "中国联通", "话费"),
        "住房" to listOf("房租", "物业", "水电", "燃气", "公寓", "公共支付平台"),
        "娱乐" to listOf("电影", "游戏", "KTV", "网吧", "音乐", "视频会员"),
        "医疗" to listOf("医院", "药店", "药房", "诊所", "体检"),
        "教育" to listOf("学校", "培训", "课程", "教育", "图书", "书店"),
    )

    /**
     * 匹配分类
     *
     * @param counterparty 交易对方
     * @param description 商品描述
     * @param typeList 用户的分类列表（一级分类）
     * @return 匹配到的分类，未匹配返回 null
     */
    fun match(
        counterparty: String,
        description: String,
        typeList: List<RecordTypeModel>,
    ): RecordTypeModel? {
        val text = "$counterparty $description"
        for ((categoryName, keywords) in CATEGORY_RULES) {
            if (keywords.any { text.contains(it) }) {
                // 在用户的分类列表中查找名称匹配的分类
                return typeList.find { it.name == categoryName }
                    ?: typeList.find { it.name.contains(categoryName) || categoryName.contains(it.name) }
            }
        }
        return null
    }
}
