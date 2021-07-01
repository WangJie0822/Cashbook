package cn.wj.android.cashbook.manager

import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.drawableString
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.getSharedBoolean
import cn.wj.android.cashbook.base.tools.setSharedBoolean
import cn.wj.android.cashbook.data.constants.SHARED_KEY_TYPE_INITIALIZED
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.store.LocalDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据库管理类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/9
 */
object DatabaseManager {

    /** 初始化数据库数据 */
    suspend fun initDatabase(local: LocalDataStore) {
        withContext(Dispatchers.IO) {
            // 初始化账本数据
            initBooksData(local)
            // 初始化类型数据
            initTypeData(local)
        }
    }

    /** 初始化账本信息 */
    private suspend fun initBooksData(local: LocalDataStore) {
        // 获取默认账本
        val books = local.getDefaultBooks()
        CurrentBooksLiveData.postValue(
            if (null != books) {
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
        )
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
            // 已有数据
            return
        }
        initExpenditureTypeData(local)
        initIncomeTypeData(local)
        initTransferTypeData(local)
        setSharedBoolean(SHARED_KEY_TYPE_INITIALIZED, true)
    }

    /** 初始化支出类型 */
    private suspend fun initExpenditureTypeData(local: LocalDataStore) {
        // 餐饮数据
        val diningFirst = TypeEntity.newFirstExpenditure(R.string.type_dining.string, R.string.type_icon_name_dining.drawableString, 0)
        val diningId = local.insertType(diningFirst)
        val diningParent = diningFirst.copy(id = diningId)
        local.insertTypes(
            // 三餐
            TypeEntity.newSecondExpenditure(parent = diningParent, name = R.string.type_three_meals.string, R.string.type_icon_name_three_meals.drawableString, 0),
            // 夜宵
            TypeEntity.newSecondExpenditure(parent = diningParent, name = R.string.type_supper.string, R.string.type_icon_name_supper.drawableString, 1),
            // 柴米油盐
            TypeEntity.newSecondExpenditure(parent = diningParent, name = R.string.type_kitchen_daily_necessities.string, R.string.type_icon_name_kitchen_daily_necessities.drawableString, 2),
            // 食材
            TypeEntity.newSecondExpenditure(parent = diningParent, name = R.string.type_food_ingredient.string, R.string.type_icon_name_food_ingredient.drawableString, 3)
        )
        // 烟酒零食
        val atsFirst = TypeEntity.newFirstExpenditure(R.string.type_ats.string, R.string.type_icon_name_ats.drawableString, 1)
        val atsId = local.insertType(atsFirst)
        val atsParent = atsFirst.copy(id = atsId)
        local.insertTypes(
            // 水果
            TypeEntity.newSecondExpenditure(parent = atsParent, name = R.string.type_fruit.string, R.string.type_icon_name_fruit.drawableString, 0),
            // 甜点
            TypeEntity.newSecondExpenditure(parent = atsParent, name = R.string.type_dessert.string, R.string.type_icon_name_dessert.drawableString, 1),
            // 零食
            TypeEntity.newSecondExpenditure(parent = atsParent, name = R.string.type_snacks.string, R.string.type_icon_name_snacks.drawableString, 2),
            // 饮料
            TypeEntity.newSecondExpenditure(parent = atsParent, name = R.string.type_drinks.string, R.string.type_icon_name_drinks.drawableString, 3),
            // 烟酒
            TypeEntity.newSecondExpenditure(parent = atsParent, name = R.string.type_at.string, R.string.type_icon_name_at.drawableString, 4)
        )
        // 购物
        val shoppingFirst = TypeEntity.newFirstExpenditure(R.string.type_shopping.string, R.string.type_icon_name_shopping.drawableString, 2)
        val shoppingId = local.insertType(shoppingFirst)
        val shoppingParent = shoppingFirst.copy(id = shoppingId)
        local.insertTypes(
            // 数码
            TypeEntity.newSecondExpenditure(parent = shoppingParent, name = R.string.type_digital_products.string, R.string.type_icon_name_digital_products.drawableString, 0),
            // 日用
            TypeEntity.newSecondExpenditure(parent = shoppingParent, name = R.string.type_daily_necessities.string, R.string.type_icon_name_daily_necessities.drawableString, 1),
            // 电器
            TypeEntity.newSecondExpenditure(parent = shoppingParent, name = R.string.type_electrical_appliances.string, R.string.type_icon_name_electrical_appliances.drawableString, 2),
            // 美妆
            TypeEntity.newSecondExpenditure(parent = shoppingParent, name = R.string.type_beauty.string, R.string.type_icon_name_beauty.drawableString, 3),
            // 鞋服
            TypeEntity.newSecondExpenditure(parent = shoppingParent, name = R.string.type_clothes.string, R.string.type_icon_name_clothes.drawableString, 4),
            // 饰品
            TypeEntity.newSecondExpenditure(parent = shoppingParent, name = R.string.type_accessories.string, R.string.type_icon_name_accessories.drawableString, 5)
        )
        // 住房
        val housingFirst = TypeEntity.newFirstExpenditure(R.string.type_housing.string, R.string.type_icon_name_housing.drawableString, 3)
        val housingId = local.insertType(housingFirst)
        val housingParent = housingFirst.copy(id = housingId)
        local.insertTypes(
            // 房租
            TypeEntity.newSecondExpenditure(parent = housingParent, name = R.string.type_house_rent.string, R.string.type_icon_name_house_rent.drawableString, 0),
            // 房贷
            TypeEntity.newSecondExpenditure(parent = housingParent, name = R.string.type_house_loan.string, R.string.type_icon_name_house_loan.drawableString, 1)
        )
        // 交通
        val trafficFirst = TypeEntity.newFirstExpenditure(R.string.type_traffic.string, R.string.type_icon_name_traffic.drawableString, 4)
        val trafficId = local.insertType(trafficFirst)
        val trafficParent = trafficFirst.copy(id = trafficId)
        local.insertTypes(
            // 公交
            TypeEntity.newSecondExpenditure(parent = trafficParent, name = R.string.type_bus.string, R.string.type_icon_name_bus.drawableString, 0),
            // 地铁
            TypeEntity.newSecondExpenditure(parent = trafficParent, name = R.string.type_subway.string, R.string.type_icon_name_subway.drawableString, 1),
            // 打车
            TypeEntity.newSecondExpenditure(parent = trafficParent, name = R.string.type_taxi.string, R.string.type_icon_name_taxi.drawableString, 2),
            // 火车
            TypeEntity.newSecondExpenditure(parent = trafficParent, name = R.string.type_train.string, R.string.type_icon_name_train.drawableString, 3),
            // 飞机
            TypeEntity.newSecondExpenditure(parent = trafficParent, name = R.string.type_plane.string, R.string.type_icon_name_plane.drawableString, 4)
        )
        // 娱乐
        val amusementFirst = TypeEntity.newFirstExpenditure(R.string.type_amusement.string, R.string.type_icon_name_amusement.drawableString, 5)
        val amusementId = local.insertType(amusementFirst)
        val amusementParent = amusementFirst.copy(id = amusementId)
        local.insertTypes(
            // 游戏
            TypeEntity.newSecondExpenditure(parent = amusementParent, name = R.string.type_game.string, R.string.type_icon_name_game.drawableString, 0),
            // 聚会
            TypeEntity.newSecondExpenditure(parent = amusementParent, name = R.string.type_party.string, R.string.type_icon_name_party.drawableString, 1),
            // 宠物
            TypeEntity.newSecondExpenditure(parent = amusementParent, name = R.string.type_pet.string, R.string.type_icon_name_pet.drawableString, 2),
            // K歌
            TypeEntity.newSecondExpenditure(parent = amusementParent, name = R.string.type_sing.string, R.string.type_icon_name_sing.drawableString, 3),
            // 电影
            TypeEntity.newSecondExpenditure(parent = amusementParent, name = R.string.type_movie.string, R.string.type_icon_name_movie.drawableString, 4),
            // 演出
            TypeEntity.newSecondExpenditure(parent = amusementParent, name = R.string.type_show.string, R.string.type_icon_name_show.drawableString, 5)
        )
        // 生活
        val lifeFirst = TypeEntity.newFirstExpenditure(R.string.type_life.string, R.string.type_icon_name_life.drawableString, 6)
        val lifeId = local.insertType(lifeFirst)
        val lifeParent = lifeFirst.copy(id = lifeId)
        local.insertTypes(
            // 水费
            TypeEntity.newSecondExpenditure(parent = lifeParent, name = R.string.type_water_rate.string, R.string.type_icon_name_water_rate.drawableString, 0),
            // 电费
            TypeEntity.newSecondExpenditure(parent = lifeParent, name = R.string.type_electricity.string, R.string.type_icon_name_electricity.drawableString, 1),
            // 燃气费
            TypeEntity.newSecondExpenditure(parent = lifeParent, name = R.string.type_gas_charge.string, R.string.type_icon_name_gas_charge.drawableString, 2),
            // 垃圾费
            TypeEntity.newSecondExpenditure(parent = lifeParent, name = R.string.type_garbage_fee.string, R.string.type_icon_name_garbage_fee.drawableString, 3),
            // 物业费
            TypeEntity.newSecondExpenditure(parent = lifeParent, name = R.string.type_property_costs.string, R.string.type_icon_name_property_costs.drawableString, 4),
            // 暖气费
            TypeEntity.newSecondExpenditure(parent = lifeParent, name = R.string.type_heating_fee.string, R.string.type_icon_name_heating_fee.drawableString, 5)
        )
        // 文教
        val booksEducationFirst = TypeEntity.newFirstExpenditure(R.string.type_book_education.string, R.string.type_icon_name_book_education.drawableString, 7)
        val booksEducationId = local.insertType(booksEducationFirst)
        val booksEducationParent = booksEducationFirst.copy(id = booksEducationId)
        local.insertTypes(
            // 学费
            TypeEntity.newSecondExpenditure(parent = booksEducationParent, name = R.string.type_tuition.string, R.string.type_icon_name_tuition.drawableString, 0),
            // 文具
            TypeEntity.newSecondExpenditure(parent = booksEducationParent, name = R.string.type_stationery.string, R.string.type_icon_name_stationery.drawableString, 1),
            // 考试
            TypeEntity.newSecondExpenditure(parent = booksEducationParent, name = R.string.type_exam.string, R.string.type_icon_name_exam.drawableString, 2),
            // 培训
            TypeEntity.newSecondExpenditure(parent = booksEducationParent, name = R.string.type_training.string, R.string.type_icon_name_training.drawableString, 3),
            // 书籍
            TypeEntity.newSecondExpenditure(parent = booksEducationParent, name = R.string.type_books.string, R.string.type_icon_name_books.drawableString, 4)
        )
        // 汽车
        val carFirst = TypeEntity.newFirstExpenditure(R.string.type_car.string, R.string.type_icon_name_car.drawableString, 8)
        val carId = local.insertType(carFirst)
        val carParent = carFirst.copy(id = carId)
        local.insertTypes(
            // 停车
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_parking.string, R.string.type_icon_name_parking.drawableString, 0),
            // 加油
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_refuel.string, R.string.type_icon_name_refuel.drawableString, 1),
            // 充电
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_car_charge.string, R.string.type_icon_name_car_charge.drawableString, 2),
            // 保养
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_car_maintenance.string, R.string.type_icon_name_car_maintenance.drawableString, 3),
            // 洗车
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_car_wash.string, R.string.type_icon_name_car_wash.drawableString, 4),
            // 维修
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_repair.string, R.string.type_icon_name_repair.drawableString, 5),
            // 违章
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_illegal.string, R.string.type_icon_name_illegal.drawableString, 6),
            // 车险
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_car_insurance.string, R.string.type_icon_name_car_insurance.drawableString, 7),
            // 车检
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_car_inspection.string, R.string.type_icon_name_car_inspection.drawableString, 8),
            // 车贷
            TypeEntity.newSecondExpenditure(parent = carParent, name = R.string.type_car_loan.string, R.string.type_icon_name_car_loan.drawableString, 9)
        )
        // 通讯
        val communicationFirst = TypeEntity.newFirstExpenditure(R.string.type_communication.string, R.string.type_icon_name_communication.drawableString, 9)
        val communicationId = local.insertType(communicationFirst)
        val communicationParent = communicationFirst.copy(id = communicationId)
        local.insertTypes(
            // 话费
            TypeEntity.newSecondExpenditure(parent = communicationParent, name = R.string.type_call_charge.string, R.string.type_icon_name_call_charge.drawableString, 0),
            // 网费
            TypeEntity.newSecondExpenditure(parent = communicationParent, name = R.string.type_net_fee.string, R.string.type_icon_name_net_fee.drawableString, 1)
        )
        // 育儿
        val parentingFirst = TypeEntity.newFirstExpenditure(R.string.type_parenting.string, R.string.type_icon_name_parenting.drawableString, 2)
        val parentingId = local.insertType(parentingFirst)
        val parentingParent = parentingFirst.copy(id = parentingId)
        local.insertTypes(
            // 奶粉
            TypeEntity.newSecondExpenditure(parent = parentingParent, name = R.string.type_milk_powder.string, R.string.type_icon_name_milk_powder.drawableString, 3),
            // 辅食
            TypeEntity.newSecondExpenditure(parent = parentingParent, name = R.string.type_supplementary_food.string, R.string.type_icon_name_supplementary_food.drawableString, 4),
            // 洗护
            TypeEntity.newSecondExpenditure(parent = parentingParent, name = R.string.type_body_hair.string, R.string.type_icon_name_body_hair.drawableString, 5),
            // 玩具
            TypeEntity.newSecondExpenditure(parent = parentingParent, name = R.string.type_toy.string, R.string.type_icon_name_toy.drawableString, 6),
            // 童装
            TypeEntity.newSecondExpenditure(parent = parentingParent, name = R.string.type_kids.string, R.string.type_icon_name_kids.drawableString, 7)
        )
        // 人际交往
        val interpersonalFirst = TypeEntity.newFirstExpenditure(R.string.type_interpersonal.string, R.string.type_icon_name_interpersonal.drawableString, 10)
        val interpersonalId = local.insertType(interpersonalFirst)
        val interpersonalParent = interpersonalFirst.copy(id = interpersonalId)
        local.insertTypes(
            // 礼金
            TypeEntity.newSecondExpenditure(parent = interpersonalParent, name = R.string.type_cash_gift.string, R.string.type_icon_name_cash_gift.drawableString, 0),
            // 礼品
            TypeEntity.newSecondExpenditure(parent = interpersonalParent, name = R.string.type_gift.string, R.string.type_icon_name_gift.drawableString, 1),
            // 请客
            TypeEntity.newSecondExpenditure(parent = interpersonalParent, name = R.string.type_treat.string, R.string.type_icon_name_treat.drawableString, 2)
        )
        // 医疗
        val medicalFirst = TypeEntity.newFirstExpenditure(R.string.type_medical.string, R.string.type_icon_name_medical.drawableString, 11)
        val medicalId = local.insertType(medicalFirst)
        val medicalParent = medicalFirst.copy(id = medicalId)
        local.insertTypes(
            // 挂号
            TypeEntity.newSecondExpenditure(parent = medicalParent, name = R.string.type_registration.string, R.string.type_icon_name_registration.drawableString, 0),
            // 看诊
            TypeEntity.newSecondExpenditure(parent = medicalParent, name = R.string.type_diagnose.string, R.string.type_icon_name_diagnose.drawableString, 1),
            // 药品
            TypeEntity.newSecondExpenditure(parent = medicalParent, name = R.string.type_drug.string, R.string.type_icon_name_drug.drawableString, 2),
            // 住院
            TypeEntity.newSecondExpenditure(parent = medicalParent, name = R.string.type_hospitalization.string, R.string.type_icon_name_hospitalization.drawableString, 3),
            // 保健
            TypeEntity.newSecondExpenditure(parent = medicalParent, name = R.string.type_hygiene.string, R.string.type_icon_name_hygiene.drawableString, 4)
        )
        // 旅行
        val travelFirst = TypeEntity.newFirstExpenditure(R.string.type_travel.string, R.string.type_icon_name_travel.drawableString, 12)
        val travelId = local.insertType(travelFirst)
        val travelParent = travelFirst.copy(id = travelId)
        local.insertTypes(
            // 团费
            TypeEntity.newSecondExpenditure(parent = travelParent, name = R.string.type_excursion_fare.string, R.string.type_icon_name_excursion_fare.drawableString, 0),
            // 门票
            TypeEntity.newSecondExpenditure(parent = travelParent, name = R.string.type_tickets.string, R.string.type_icon_name_tickets.drawableString, 1),
            // 纪念品
            TypeEntity.newSecondExpenditure(parent = travelParent, name = R.string.type_souvenir.string, R.string.type_icon_name_souvenir.drawableString, 2),
            // 酒店
            TypeEntity.newSecondExpenditure(parent = travelParent, name = R.string.type_hotel.string, R.string.type_icon_name_hotel.drawableString, 3)
        )
        // 其它
        local.insertType(TypeEntity.newFirstExpenditure(R.string.type_other.string, R.string.type_icon_name_other.drawableString, 13, false))
    }

    /** 初始化收入类型 */
    private suspend fun initIncomeTypeData(local: LocalDataStore) {
        // 薪资
        local.insertType(TypeEntity.newFirstIncome(R.string.type_salary.string, R.string.type_icon_name_salary.drawableString, 0, false))
        // 奖金
        local.insertType(TypeEntity.newFirstIncome(R.string.type_bonus.string, R.string.type_icon_name_bonus.drawableString, 1, false))
        // 退款
        local.insertType(TypeEntity.newFirstIncome(R.string.type_refund.string, R.string.type_icon_name_refund.drawableString, 2, childEnable = false, refund = true))
        // 报销
        local.insertType(TypeEntity.newFirstIncome(R.string.type_reimburse.string, R.string.type_icon_name_reimburse.drawableString, 3, childEnable = false, reimburse = true))
        // 投资
        local.insertType(TypeEntity.newFirstIncome(R.string.type_investment.string, R.string.type_icon_name_investment.drawableString, 4, false))
        // 外快
        local.insertType(TypeEntity.newFirstIncome(R.string.type_windfall.string, R.string.type_icon_name_windfall.drawableString, 5, false))
        // 其它
        local.insertType(TypeEntity.newFirstIncome(R.string.type_other.string, R.string.type_icon_name_other.drawableString, 6, false))
    }

    /** 初始化转账类型 */
    private suspend fun initTransferTypeData(local: LocalDataStore) {
        // 账户互转
        local.insertType(TypeEntity.newFirstTransfer(R.string.type_account_transfer.string, R.string.type_icon_name_account_transfer.drawableString, 0, false))
        // 还信用卡
        local.insertType(TypeEntity.newFirstTransfer(R.string.type_credit_card_payment.string, R.string.type_icon_name_credit_card_payment.drawableString, 1, false))
        // 取款
        local.insertType(TypeEntity.newFirstTransfer(R.string.type_withdrawals.string, R.string.type_icon_name_withdrawals.drawableString, 2, false))
        // 存款
        local.insertType(TypeEntity.newFirstTransfer(R.string.type_deposit.string, R.string.type_icon_name_deposit.drawableString, 3, false))
        // 借入
        local.insertType(TypeEntity.newFirstTransfer(R.string.type_borrow.string, R.string.type_icon_name_borrow.drawableString, 4, false))
        // 借出
        local.insertType(TypeEntity.newFirstTransfer(R.string.type_lend.string, R.string.type_icon_name_lend.drawableString, 5, false))
        // 还款
        local.insertType(TypeEntity.newFirstTransfer(R.string.type_repayment.string, R.string.type_icon_name_repayment.drawableString, 6, false))
        // 收款
        local.insertType(TypeEntity.newFirstTransfer(R.string.type_proceeds.string, R.string.type_icon_name_proceeds.drawableString, 7, false))
        // 其它
        local.insertType(TypeEntity.newFirstTransfer(R.string.type_other.string, R.string.type_icon_name_other.drawableString, 8, false))
    }
}