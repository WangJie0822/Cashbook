package cn.wj.android.cashbook.manager

import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.getSharedBoolean
import cn.wj.android.cashbook.data.constants.SHARED_KEY_TYPE_INITIALIZED
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 数据库管理类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/9
 */
object DatabaseManager {

    /** 初始化数据库数据 */
    suspend fun initDatabase(local: LocalDataStore) {
        // 初始化账本数据
        initBooksData(local)
        // 初始化类型数据
        initTypeData(local)
    }

    /** 初始化账本信息 */
    private suspend fun initBooksData(local: LocalDataStore) {
        // 获取默认账本
        val books = local.getDefaultBooks()
        CurrentBooksLiveData.value = if (null != books) {
            books
        } else {
            // 没有默认账本新增
            val currentTime = System.currentTimeMillis().dateFormat()
            val default = BooksEntity(
                -1,
                R.string.default_books.string,
                "",
                "日常账本",
                null,
                true,
                currentTime,
                currentTime
            )
            val insertId = local.insertBooks(default)
            default.copy(id = insertId)
        }
    }

    /** 初始化消费类型信息 */
    private suspend fun initTypeData(local: LocalDataStore) {
        val initialized = getSharedBoolean(SHARED_KEY_TYPE_INITIALIZED).condition
        if (initialized) {
            // 已初始化
            return
        }
        // 未初始化
        if (local.hasType()) {
            // TODO 已有数据
            local.clearTypes()
//            return
        }
        initExpenditureTypeData(local)
        initIncomeTypeData(local)
        initTransferTypeData(local)
    }

    /** 初始化支出类型 */
    private suspend fun initExpenditureTypeData(local: LocalDataStore) {
        // 餐饮数据
        val diningId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_dining.string, R.string.type_icon_name_dining.drawableString))
        local.insertTypes(
            // 三餐
            TypeEntity.newSecondExpenditure(parentId = diningId, name = R.string.type_three_meals.string, R.string.type_icon_name_three_meals.drawableString),
            // 夜宵
            TypeEntity.newSecondExpenditure(parentId = diningId, name = R.string.type_supper.string, R.string.type_icon_name_supper.drawableString),
            // 柴米油盐
            TypeEntity.newSecondExpenditure(parentId = diningId, name = R.string.type_kitchen_daily_necessities.string, R.string.type_icon_name_kitchen_daily_necessities.drawableString),
            // 食材
            TypeEntity.newSecondExpenditure(parentId = diningId, name = R.string.type_food_ingredient.string, R.string.type_icon_name_food_ingredient.drawableString)
        )
        // 烟酒零食
        val atsId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_ats.string, R.string.type_icon_name_ats.drawableString))
        local.insertTypes(
            // 水果
            TypeEntity.newSecondExpenditure(parentId = atsId, name = R.string.type_fruit.string, R.string.type_icon_name_fruit.drawableString),
            // 甜点
            TypeEntity.newSecondExpenditure(parentId = atsId, name = R.string.type_dessert.string, R.string.type_icon_name_dessert.drawableString),
            // 零食
            TypeEntity.newSecondExpenditure(parentId = atsId, name = R.string.type_snacks.string, R.string.type_icon_name_snacks.drawableString),
            // 饮料
            TypeEntity.newSecondExpenditure(parentId = atsId, name = R.string.type_drinks.string, R.string.type_icon_name_drinks.drawableString),
            // 烟酒
            TypeEntity.newSecondExpenditure(parentId = atsId, name = R.string.type_at.string, R.string.type_icon_name_at.drawableString)
        )
        // 购物
        val shoppingId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_shopping.string, R.string.type_icon_name_shopping.drawableString))
        local.insertTypes(
            // 数码
            TypeEntity.newSecondExpenditure(parentId = shoppingId, name = R.string.type_digital_products.string, R.string.type_icon_name_digital_products.drawableString),
            // 日用
            TypeEntity.newSecondExpenditure(parentId = shoppingId, name = R.string.type_daily_necessities.string, R.string.type_icon_name_daily_necessities.drawableString),
            // 电器
            TypeEntity.newSecondExpenditure(parentId = shoppingId, name = R.string.type_electrical_appliances.string, R.string.type_icon_name_electrical_appliances.drawableString),
            // 美妆
            TypeEntity.newSecondExpenditure(parentId = shoppingId, name = R.string.type_beauty.string, R.string.type_icon_name_beauty.drawableString),
            // 鞋服
            TypeEntity.newSecondExpenditure(parentId = shoppingId, name = R.string.type_clothes.string, R.string.type_icon_name_clothes.drawableString),
            // 饰品
            TypeEntity.newSecondExpenditure(parentId = shoppingId, name = R.string.type_accessories.string, R.string.type_icon_name_accessories.drawableString)
        )
        // 住房
        val housingId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_housing.string, R.string.type_icon_name_housing.drawableString))
        local.insertTypes(
            // 房租
            TypeEntity.newSecondExpenditure(parentId = housingId, name = R.string.type_house_rent.string, R.string.type_icon_name_house_rent.drawableString),
            // 房贷
            TypeEntity.newSecondExpenditure(parentId = housingId, name = R.string.type_house_loan.string, R.string.type_icon_name_house_loan.drawableString)
        )
        // 交通
        val trafficId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_traffic.string, R.string.type_icon_name_traffic.drawableString))
        local.insertTypes(
            // 公交
            TypeEntity.newSecondExpenditure(parentId = trafficId, name = R.string.type_bus.string, R.string.type_icon_name_bus.drawableString),
            // 地铁
            TypeEntity.newSecondExpenditure(parentId = trafficId, name = R.string.type_subway.string, R.string.type_icon_name_subway.drawableString),
            // 打车
            TypeEntity.newSecondExpenditure(parentId = trafficId, name = R.string.type_taxi.string, R.string.type_icon_name_taxi.drawableString),
            // 火车
            TypeEntity.newSecondExpenditure(parentId = trafficId, name = R.string.type_train.string, R.string.type_icon_name_train.drawableString),
            // 飞机
            TypeEntity.newSecondExpenditure(parentId = trafficId, name = R.string.type_plane.string, R.string.type_icon_name_plane.drawableString)
        )
        // 娱乐
        val amusementId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_amusement.string, R.string.type_icon_name_amusement.drawableString))
        local.insertTypes(
            // 游戏
            TypeEntity.newSecondExpenditure(parentId = amusementId, name = R.string.type_game.string, R.string.type_icon_name_game.drawableString),
            // 聚会
            TypeEntity.newSecondExpenditure(parentId = amusementId, name = R.string.type_party.string, R.string.type_icon_name_party.drawableString),
            // 宠物
            TypeEntity.newSecondExpenditure(parentId = amusementId, name = R.string.type_pet.string, R.string.type_icon_name_pet.drawableString),
            // K歌
            TypeEntity.newSecondExpenditure(parentId = amusementId, name = R.string.type_sing.string, R.string.type_icon_name_sing.drawableString),
            // 电影
            TypeEntity.newSecondExpenditure(parentId = amusementId, name = R.string.type_movie.string, R.string.type_icon_name_movie.drawableString),
            // 演出
            TypeEntity.newSecondExpenditure(parentId = amusementId, name = R.string.type_show.string, R.string.type_icon_name_show.drawableString)
        )
        // 生活
        val lifeId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_life.string, R.string.type_icon_name_life.drawableString))
        local.insertTypes(
            // 水费
            TypeEntity.newSecondExpenditure(parentId = lifeId, name = R.string.type_water_rate.string, R.string.type_icon_name_water_rate.drawableString),
            // 电费
            TypeEntity.newSecondExpenditure(parentId = lifeId, name = R.string.type_electricity.string, R.string.type_icon_name_electricity.drawableString),
            // 燃气费
            TypeEntity.newSecondExpenditure(parentId = lifeId, name = R.string.type_gas_charge.string, R.string.type_icon_name_gas_charge.drawableString),
            // 垃圾费
            TypeEntity.newSecondExpenditure(parentId = lifeId, name = R.string.type_garbage_fee.string, R.string.type_icon_name_garbage_fee.drawableString),
            // 物业费
            TypeEntity.newSecondExpenditure(parentId = lifeId, name = R.string.type_property_costs.string, R.string.type_icon_name_property_costs.drawableString),
            // 暖气费
            TypeEntity.newSecondExpenditure(parentId = lifeId, name = R.string.type_heating_fee.string, R.string.type_icon_name_heating_fee.drawableString)
        )
        // 文教
        val booksEducationId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_book_education.string, R.string.type_icon_name_book_education.drawableString))
        local.insertTypes(
            // 学费
            TypeEntity.newSecondExpenditure(parentId = booksEducationId, name = R.string.type_tuition.string, R.string.type_icon_name_tuition.drawableString),
            // 文具
            TypeEntity.newSecondExpenditure(parentId = booksEducationId, name = R.string.type_stationery.string, R.string.type_icon_name_stationery.drawableString),
            // 考试
            TypeEntity.newSecondExpenditure(parentId = booksEducationId, name = R.string.type_exam.string, R.string.type_icon_name_exam.drawableString),
            // 培训
            TypeEntity.newSecondExpenditure(parentId = booksEducationId, name = R.string.type_training.string, R.string.type_icon_name_training.drawableString),
            // 书籍
            TypeEntity.newSecondExpenditure(parentId = booksEducationId, name = R.string.type_books.string, R.string.type_icon_name_books.drawableString)
        )
        // 汽车
        val carId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_car.string, R.string.type_icon_name_car.drawableString))
        local.insertTypes(
            // 停车
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_parking.string, R.string.type_icon_name_parking.drawableString),
            // 加油
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_refuel.string, R.string.type_icon_name_refuel.drawableString),
            // 充电
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_charge.string, R.string.type_icon_name_charge.drawableString),
            // 保养
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_car_maintenance.string, R.string.type_icon_name_car_maintenance.drawableString),
            // 洗车
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_car_wash.string, R.string.type_icon_name_car_wash.drawableString),
            // 维修
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_repair.string, R.string.type_icon_name_repair.drawableString),
            // 违章
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_illegal.string, R.string.type_icon_name_illegal.drawableString),
            // 车险
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_car_insurance.string, R.string.type_icon_name_car_insurance.drawableString),
            // 车检
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_car_inspection.string, R.string.type_icon_name_car_inspection.drawableString),
            // 车贷
            TypeEntity.newSecondExpenditure(parentId = carId, name = R.string.type_car_loan.string, R.string.type_icon_name_car_loan.drawableString)
        )
        // 通讯
        val communicationId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_communication.string, R.string.type_icon_name_communication.drawableString))
        local.insertTypes(
            // 话费
            TypeEntity.newSecondExpenditure(parentId = communicationId, name = R.string.type_call_charge.string, R.string.type_icon_name_call_charge.drawableString),
            // 网费
            TypeEntity.newSecondExpenditure(parentId = communicationId, name = R.string.type_net_fee.string, R.string.type_icon_name_net_fee.drawableString)
        )
        // 育儿
        val parentingId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_parenting.string, R.string.type_icon_name_parenting.drawableString))
        local.insertTypes(
            // 奶粉
            TypeEntity.newSecondExpenditure(parentId = parentingId, name = R.string.type_milk_powder.string, R.string.type_icon_name_milk_powder.drawableString),
            // 辅食
            TypeEntity.newSecondExpenditure(parentId = parentingId, name = R.string.type_supplementary_food.string, R.string.type_icon_name_supplementary_food.drawableString),
            // 洗护
            TypeEntity.newSecondExpenditure(parentId = parentingId, name = R.string.type_body_hair.string, R.string.type_icon_name_body_hair.drawableString),
            // 玩具
            TypeEntity.newSecondExpenditure(parentId = parentingId, name = R.string.type_toy.string, R.string.type_icon_name_toy.drawableString),
            // 童装
            TypeEntity.newSecondExpenditure(parentId = parentingId, name = R.string.type_kids.string, R.string.type_icon_name_kids.drawableString)
        )
        // 人际交往
        val interpersonalId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_interpersonal.string, R.string.type_icon_name_interpersonal.drawableString))
        local.insertTypes(
            // 礼金
            TypeEntity.newSecondExpenditure(parentId = interpersonalId, name = R.string.type_cash_gift.string, R.string.type_icon_name_cash_gift.drawableString),
            // 礼品
            TypeEntity.newSecondExpenditure(parentId = interpersonalId, name = R.string.type_gift.string, R.string.type_icon_name_gift.drawableString),
            // 请客
            TypeEntity.newSecondExpenditure(parentId = interpersonalId, name = R.string.type_treat.string, R.string.type_icon_name_treat.drawableString)
        )
        // 医疗
        val medicalId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_medical.string, R.string.type_icon_name_medical.drawableString))
        local.insertTypes(
            // 挂号
            TypeEntity.newSecondExpenditure(parentId = medicalId, name = R.string.type_registration.string, R.string.type_icon_name_registration.drawableString),
            // 看诊
            TypeEntity.newSecondExpenditure(parentId = medicalId, name = R.string.type_diagnose.string, R.string.type_icon_name_diagnose.drawableString),
            // 药品
            TypeEntity.newSecondExpenditure(parentId = medicalId, name = R.string.type_drug.string, R.string.type_icon_name_drug.drawableString),
            // 住院
            TypeEntity.newSecondExpenditure(parentId = medicalId, name = R.string.type_hospitalization.string, R.string.type_icon_name_hospitalization.drawableString),
            // 保健
            TypeEntity.newSecondExpenditure(parentId = medicalId, name = R.string.type_hygiene.string, R.string.type_icon_name_hygiene.drawableString)
        )
        // 旅行
        val travelId = local.insertType(TypeEntity.newFirstExpenditure(R.string.type_travel.string, R.string.type_icon_name_travel.drawableString))
        local.insertTypes(
            // 团费
            TypeEntity.newSecondExpenditure(parentId = travelId, name = R.string.type_excursion_fare.string, R.string.type_icon_name_excursion_fare.drawableString),
            // 门票
            TypeEntity.newSecondExpenditure(parentId = travelId, name = R.string.type_tickets.string, R.string.type_icon_name_tickets.drawableString),
            // 纪念品
            TypeEntity.newSecondExpenditure(parentId = travelId, name = R.string.type_souvenir.string, R.string.type_icon_name_souvenir.drawableString),
            // 酒店
            TypeEntity.newSecondExpenditure(parentId = travelId, name = R.string.type_hotel.string, R.string.type_icon_name_hotel.drawableString)
        )
        // 其它
        local.insertType(TypeEntity.newFirstExpenditure(R.string.type_other.string, R.string.type_icon_name_other.drawableString, false))
    }

    /** 初始化收入类型 */
    private suspend fun initIncomeTypeData(local: LocalDataStore) {
        // 薪资
        local.insertType(TypeEntity.newFirstIncome(R.string.type_salary.string, R.string.type_icon_name_salary.drawableString, false))
        // 奖金
        local.insertType(TypeEntity.newFirstIncome(R.string.type_bonus.string, R.string.type_icon_name_bonus.drawableString, false))
        // 投资
        local.insertType(TypeEntity.newFirstIncome(R.string.type_investment.string, R.string.type_icon_name_investment.drawableString, false))
        // 外快
        local.insertType(TypeEntity.newFirstIncome(R.string.type_windfall.string, R.string.type_icon_name_windfall.drawableString, false))
        // 其它
        local.insertType(TypeEntity.newFirstIncome(R.string.type_other.string, R.string.type_icon_name_other.drawableString, false))
    }

    /** 初始化转账类型 */
    private suspend fun initTransferTypeData(local: LocalDataStore) {

    }

    private val Int.drawableString: String
        get() = "@drawable/${this.string}"
}