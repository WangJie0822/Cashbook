package cn.wj.android.cashbook.core.data.helper

import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.database.dao.TypeDao
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel

/**
 * 数据库管理类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/9
 */
internal object DatabaseHelper {

    private var initialized = false

    /** 初始化消费类型信息 */
    internal suspend fun initTypeData(typeDao: TypeDao) {
        if (initialized) {
            return
        }
        initialized = true
        initExpenditureTypeData(typeDao)
        initIncomeTypeData(typeDao)
        initTransferTypeData(typeDao)
    }

    /** 初始化支出类型 */
    private suspend fun initExpenditureTypeData(typeDao: TypeDao) {
        // 餐饮数据
        val diningFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_dining.string,
            R.string.type_icon_name_dining.string,
            0
        )
        val diningId = typeDao.insertType(diningFirst.asTable())
        val diningParent = diningFirst.copy(id = diningId)
        typeDao.insertTypes(
            // 三餐
            newSecondTypeTable(
                parentId = diningParent,
                name = R.string.type_three_meals.string,
                iconName = R.string.type_icon_name_three_meals.string,
                sort = 0
            ),
            // 夜宵
            newSecondTypeTable(
                parentId = diningParent,
                name = R.string.type_supper.string,
                iconName = R.string.type_icon_name_supper.string,
                sort = 1
            ),
            // 柴米油盐
            newSecondTypeTable(
                parentId = diningParent,
                name = R.string.type_kitchen_daily_necessities.string,
                iconName = R.string.type_icon_name_kitchen_daily_necessities.string,
                sort = 2
            ),
            // 食材
            newSecondTypeTable(
                parentId = diningParent,
                name = R.string.type_food_ingredient.string,
                iconName = R.string.type_icon_name_food_ingredient.string,
                sort = 3
            )
        )
        // 烟酒零食
        val atsFirst = newFirstTypeModel(
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_ats.string,
            R.string.type_icon_name_ats.string,
            1
        )
        val atsId = typeDao.insertType(atsFirst.asTable())
        val atsParent = atsFirst.copy(id = atsId)
        typeDao.insertTypes(
            // 水果
            newSecondTypeTable(
                parentId = atsParent,
                name = R.string.type_fruit.string,
                iconName = R.string.type_icon_name_fruit.string,
                sort = 0
            ),
            // 甜点
            newSecondTypeTable(
                parentId = atsParent,
                name = R.string.type_dessert.string,
                iconName = R.string.type_icon_name_dessert.string,
                sort = 1
            ),
            // 零食
            newSecondTypeTable(
                parentId = atsParent,
                name = R.string.type_snacks.string,
                iconName = R.string.type_icon_name_snacks.string,
                sort = 2
            ),
            // 饮料
            newSecondTypeTable(
                parentId = atsParent,
                name = R.string.type_drinks.string,
                iconName = R.string.type_icon_name_drinks.string,
                sort = 3
            ),
            // 烟酒
            newSecondTypeTable(
                parentId = atsParent,
                name = R.string.type_at.string,
                iconName = R.string.type_icon_name_at.string,
                sort = 4
            )
        )
        // 购物
        val shoppingFirst = newFirstTypeModel(
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_shopping.string,
            R.string.type_icon_name_shopping.string,
            2
        )
        val shoppingId = typeDao.insertType(shoppingFirst.asTable())
        val shoppingParent = shoppingFirst.copy(id = shoppingId)
        typeDao.insertTypes(
            // 数码
            newSecondTypeTable(
                parentId = shoppingParent,
                name = R.string.type_digital_products.string,
                iconName = R.string.type_icon_name_digital_products.string,
                sort = 0
            ),
            // 日用
            newSecondTypeTable(
                parentId = shoppingParent,
                name = R.string.type_daily_necessities.string,
                iconName = R.string.type_icon_name_daily_necessities.string,
                sort = 1
            ),
            // 电器
            newSecondTypeTable(
                parentId = shoppingParent,
                name = R.string.type_electrical_appliances.string,
                iconName = R.string.type_icon_name_electrical_appliances.string,
                sort = 2
            ),
            // 美妆
            newSecondTypeTable(
                parentId = shoppingParent,
                name = R.string.type_beauty.string,
                iconName = R.string.type_icon_name_beauty.string,
                sort = 3
            ),
            // 鞋服
            newSecondTypeTable(
                parentId = shoppingParent,
                name = R.string.type_clothes.string,
                iconName = R.string.type_icon_name_clothes.string,
                sort = 4
            ),
            // 饰品
            newSecondTypeTable(
                parentId = shoppingParent,
                name = R.string.type_accessories.string,
                iconName = R.string.type_icon_name_accessories.string,
                sort = 5
            )
        )
        // 住房
        val housingFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_housing.string,
            R.string.type_icon_name_housing.string,
            3
        )
        val housingId = typeDao.insertType(housingFirst.asTable())
        val housingParent = housingFirst.copy(id = housingId)
        typeDao.insertTypes(
            // 房租
            newSecondTypeTable(
                parentId = housingParent,
                name = R.string.type_house_rent.string,
                iconName = R.string.type_icon_name_house_rent.string,
                sort = 0
            ),
            // 房贷
            newSecondTypeTable(
                parentId = housingParent,
                name = R.string.type_house_loan.string,
                iconName = R.string.type_icon_name_house_loan.string,
                sort = 1
            )
        )
        // 交通
        val trafficFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_traffic.string,
            R.string.type_icon_name_traffic.string,
            4
        )
        val trafficId = typeDao.insertType(trafficFirst.asTable())
        val trafficParent = trafficFirst.copy(id = trafficId)
        typeDao.insertTypes(
            // 公交
            newSecondTypeTable(
                parentId = trafficParent,
                name = R.string.type_bus.string,
                iconName = R.string.type_icon_name_bus.string,
                sort = 0
            ),
            // 地铁
            newSecondTypeTable(
                parentId = trafficParent,
                name = R.string.type_subway.string,
                iconName = R.string.type_icon_name_subway.string,
                sort = 1
            ),
            // 打车
            newSecondTypeTable(
                parentId = trafficParent,
                name = R.string.type_taxi.string,
                iconName = R.string.type_icon_name_taxi.string,
                sort = 2
            ),
            // 火车
            newSecondTypeTable(
                parentId = trafficParent,
                name = R.string.type_train.string,
                iconName = R.string.type_icon_name_train.string,
                sort = 3
            ),
            // 飞机
            newSecondTypeTable(
                parentId = trafficParent,
                name = R.string.type_plane.string,
                iconName = R.string.type_icon_name_plane.string,
                sort = 4
            )
        )
        // 娱乐
        val amusementFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_amusement.string,
            R.string.type_icon_name_amusement.string,
            5
        )
        val amusementId = typeDao.insertType(amusementFirst.asTable())
        val amusementParent = amusementFirst.copy(id = amusementId)
        typeDao.insertTypes(
            // 游戏
            newSecondTypeTable(
                parentId = amusementParent,
                name = R.string.type_game.string,
                iconName = R.string.type_icon_name_game.string,
                sort = 0
            ),
            // 聚会
            newSecondTypeTable(
                parentId = amusementParent,
                name = R.string.type_party.string,
                iconName = R.string.type_icon_name_party.string,
                sort = 1
            ),
            // 宠物
            newSecondTypeTable(
                parentId = amusementParent,
                name = R.string.type_pet.string,
                iconName = R.string.type_icon_name_pet.string,
                sort = 2
            ),
            // K歌
            newSecondTypeTable(
                parentId = amusementParent,
                name = R.string.type_sing.string,
                iconName = R.string.type_icon_name_sing.string,
                sort = 3
            ),
            // 电影
            newSecondTypeTable(
                parentId = amusementParent,
                name = R.string.type_movie.string,
                iconName = R.string.type_icon_name_movie.string,
                sort = 4
            ),
            // 演出
            newSecondTypeTable(
                parentId = amusementParent,
                name = R.string.type_show.string,
                iconName = R.string.type_icon_name_show.string,
                sort = 5
            )
        )
        // 生活
        val lifeFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_life.string,
            R.string.type_icon_name_life.string,
            6
        )
        val lifeId = typeDao.insertType(lifeFirst.asTable())
        val lifeParent = lifeFirst.copy(id = lifeId)
        typeDao.insertTypes(
            // 水费
            newSecondTypeTable(
                parentId = lifeParent,
                name = R.string.type_water_rate.string,
                iconName = R.string.type_icon_name_water_rate.string,
                sort = 0
            ),
            // 电费
            newSecondTypeTable(
                parentId = lifeParent,
                name = R.string.type_electricity.string,
                iconName = R.string.type_icon_name_electricity.string,
                sort = 1
            ),
            // 燃气费
            newSecondTypeTable(
                parentId = lifeParent,
                name = R.string.type_gas_charge.string,
                iconName = R.string.type_icon_name_gas_charge.string,
                sort = 2
            ),
            // 垃圾费
            newSecondTypeTable(
                parentId = lifeParent,
                name = R.string.type_garbage_fee.string,
                iconName = R.string.type_icon_name_garbage_fee.string,
                sort = 3
            ),
            // 物业费
            newSecondTypeTable(
                parentId = lifeParent,
                name = R.string.type_property_costs.string,
                iconName = R.string.type_icon_name_property_costs.string,
                sort = 4
            ),
            // 暖气费
            newSecondTypeTable(
                parentId = lifeParent,
                name = R.string.type_heating_fee.string,
                iconName = R.string.type_icon_name_heating_fee.string,
                sort = 5
            )
        )
        // 文教
        val booksEducationFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_book_education.string,
            R.string.type_icon_name_book_education.string,
            7
        )
        val booksEducationId = typeDao.insertType(booksEducationFirst.asTable())
        val booksEducationParent = booksEducationFirst.copy(id = booksEducationId)
        typeDao.insertTypes(
            // 学费
            newSecondTypeTable(
                parentId = booksEducationParent,
                name = R.string.type_tuition.string,
                iconName = R.string.type_icon_name_tuition.string,
                sort = 0
            ),
            // 文具
            newSecondTypeTable(
                parentId = booksEducationParent,
                name = R.string.type_stationery.string,
                iconName = R.string.type_icon_name_stationery.string,
                sort = 1
            ),
            // 考试
            newSecondTypeTable(
                parentId = booksEducationParent,
                name = R.string.type_exam.string,
                iconName = R.string.type_icon_name_exam.string,
                sort = 2
            ),
            // 培训
            newSecondTypeTable(
                parentId = booksEducationParent,
                name = R.string.type_training.string,
                iconName = R.string.type_icon_name_training.string,
                sort = 3
            ),
            // 书籍
            newSecondTypeTable(
                parentId = booksEducationParent,
                name = R.string.type_books.string,
                iconName = R.string.type_icon_name_books.string,
                sort = 4
            )
        )
        // 汽车
        val carFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_car.string,
            R.string.type_icon_name_car.string,
            8
        )
        val carId = typeDao.insertType(carFirst.asTable())
        val carParent = carFirst.copy(id = carId)
        typeDao.insertTypes(
            // 停车
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_parking.string,
                iconName = R.string.type_icon_name_parking.string,
                sort = 0
            ),
            // 加油
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_refuel.string,
                iconName = R.string.type_icon_name_refuel.string,
                sort = 1
            ),
            // 充电
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_car_charge.string,
                iconName = R.string.type_icon_name_car_charge.string,
                sort = 2
            ),
            // 保养
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_car_maintenance.string,
                iconName = R.string.type_icon_name_car_maintenance.string,
                sort = 3
            ),
            // 洗车
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_car_wash.string,
                iconName = R.string.type_icon_name_car_wash.string,
                sort = 4
            ),
            // 维修
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_repair.string,
                iconName = R.string.type_icon_name_repair.string,
                sort = 5
            ),
            // 违章
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_illegal.string,
                iconName = R.string.type_icon_name_illegal.string,
                sort = 6
            ),
            // 车险
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_car_insurance.string,
                iconName = R.string.type_icon_name_car_insurance.string,
                sort = 7
            ),
            // 车检
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_car_inspection.string,
                iconName = R.string.type_icon_name_car_inspection.string,
                sort = 8
            ),
            // 车贷
            newSecondTypeTable(
                parentId = carParent,
                name = R.string.type_car_loan.string,
                iconName = R.string.type_icon_name_car_loan.string,
                sort = 9
            )
        )
        // 通讯
        val communicationFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_communication.string,
            R.string.type_icon_name_communication.string,
            9
        )
        val communicationId = typeDao.insertType(communicationFirst.asTable())
        val communicationParent = communicationFirst.copy(id = communicationId)
        typeDao.insertTypes(
            // 话费
            newSecondTypeTable(
                parentId = communicationParent,
                name = R.string.type_call_charge.string,
                iconName = R.string.type_icon_name_call_charge.string,
                sort = 0
            ),
            // 网费
            newSecondTypeTable(
                parentId = communicationParent,
                name = R.string.type_net_fee.string,
                iconName = R.string.type_icon_name_net_fee.string,
                sort = 1
            )
        )
        // 育儿
        val parentingFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_parenting.string,
            R.string.type_icon_name_parenting.string,
            2
        )
        val parentingId = typeDao.insertType(parentingFirst.asTable())
        val parentingParent = parentingFirst.copy(id = parentingId)
        typeDao.insertTypes(
            // 奶粉
            newSecondTypeTable(
                parentId = parentingParent,
                name = R.string.type_milk_powder.string,
                iconName = R.string.type_icon_name_milk_powder.string,
                sort = 3
            ),
            // 辅食
            newSecondTypeTable(
                parentId = parentingParent,
                name = R.string.type_supplementary_food.string,
                iconName = R.string.type_icon_name_supplementary_food.string,
                sort = 4
            ),
            // 洗护
            newSecondTypeTable(
                parentId = parentingParent,
                name = R.string.type_body_hair.string,
                iconName = R.string.type_icon_name_body_hair.string,
                sort = 5
            ),
            // 玩具
            newSecondTypeTable(
                parentId = parentingParent,
                name = R.string.type_toy.string,
                iconName = R.string.type_icon_name_toy.string,
                sort = 6
            ),
            // 童装
            newSecondTypeTable(
                parentId = parentingParent,
                name = R.string.type_kids.string,
                iconName = R.string.type_icon_name_kids.string,
                sort = 7
            )
        )
        // 人际交往
        val interpersonalFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_interpersonal.string,
            R.string.type_icon_name_interpersonal.string,
            10
        )
        val interpersonalId = typeDao.insertType(interpersonalFirst.asTable())
        val interpersonalParent = interpersonalFirst.copy(id = interpersonalId)
        typeDao.insertTypes(
            // 礼金
            newSecondTypeTable(
                parentId = interpersonalParent,
                name = R.string.type_cash_gift.string,
                iconName = R.string.type_icon_name_cash_gift.string,
                sort = 0
            ),
            // 礼品
            newSecondTypeTable(
                parentId = interpersonalParent,
                name = R.string.type_gift.string,
                iconName = R.string.type_icon_name_gift.string,
                sort = 1
            ),
            // 请客
            newSecondTypeTable(
                parentId = interpersonalParent,
                name = R.string.type_treat.string,
                iconName = R.string.type_icon_name_treat.string,
                sort = 2
            )
        )
        // 医疗
        val medicalFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_medical.string,
            R.string.type_icon_name_medical.string,
            11
        )
        val medicalId = typeDao.insertType(medicalFirst.asTable())
        val medicalParent = medicalFirst.copy(id = medicalId)
        typeDao.insertTypes(
            // 挂号
            newSecondTypeTable(
                parentId = medicalParent,
                name = R.string.type_registration.string,
                iconName = R.string.type_icon_name_registration.string,
                sort = 0
            ),
            // 看诊
            newSecondTypeTable(
                parentId = medicalParent,
                name = R.string.type_diagnose.string,
                iconName = R.string.type_icon_name_diagnose.string,
                sort = 1
            ),
            // 药品
            newSecondTypeTable(
                parentId = medicalParent,
                name = R.string.type_drug.string,
                iconName = R.string.type_icon_name_drug.string,
                sort = 2
            ),
            // 住院
            newSecondTypeTable(
                parentId = medicalParent,
                name = R.string.type_hospitalization.string,
                iconName = R.string.type_icon_name_hospitalization.string,
                sort = 3
            ),
            // 保健
            newSecondTypeTable(
                parentId = medicalParent,
                name = R.string.type_hygiene.string,
                iconName = R.string.type_icon_name_hygiene.string,
                sort = 4
            )
        )
        // 旅行
        val travelFirst = newFirstTypeModel(
            RecordTypeCategoryEnum.EXPENDITURE,
            R.string.type_travel.string,
            R.string.type_icon_name_travel.string,
            12
        )
        val travelId = typeDao.insertType(travelFirst.asTable())
        val travelParent = travelFirst.copy(id = travelId)
        typeDao.insertTypes(
            // 团费
            newSecondTypeTable(
                parentId = travelParent,
                name = R.string.type_excursion_fare.string,
                iconName = R.string.type_icon_name_excursion_fare.string,
                sort = 0
            ),
            // 门票
            newSecondTypeTable(
                parentId = travelParent,
                name = R.string.type_tickets.string,
                iconName = R.string.type_icon_name_tickets.string,
                sort = 1
            ),
            // 纪念品
            newSecondTypeTable(
                parentId = travelParent,
                name = R.string.type_souvenir.string,
                iconName = R.string.type_icon_name_souvenir.string,
                sort = 2
            ),
            // 酒店
            newSecondTypeTable(
                parentId = travelParent,
                name = R.string.type_hotel.string,
                iconName = R.string.type_icon_name_hotel.string,
                sort = 3
            )
        )
        // 其它
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.EXPENDITURE,
                R.string.type_other.string,
                R.string.type_icon_name_other.string,
                13
            ).asTable()
        )
    }

    /** 初始化收入类型 */
    private suspend fun initIncomeTypeData(typeDao: TypeDao) {
        // 薪资
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.INCOME,
                R.string.type_salary.string,
                R.string.type_icon_name_salary.string,
                0,
                true
            ).asTable()
        )
        // 奖金
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.INCOME,
                R.string.type_bonus.string,
                R.string.type_icon_name_bonus.string,
                1,
                true
            ).asTable()
        )
        // 退款
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.INCOME,
                R.string.type_refund.string,
                R.string.type_icon_name_refund.string,
                2,
                protected = true
            ).asTable()
        )
        // 报销
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.INCOME,
                R.string.type_reimburse.string,
                R.string.type_icon_name_reimburse.string,
                3,
                protected = true
            ).asTable()
        )
        // 投资
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.INCOME,
                R.string.type_investment.string,
                R.string.type_icon_name_investment.string,
                4,
            ).asTable()
        )
        // 外快
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.INCOME,
                R.string.type_windfall.string,
                R.string.type_icon_name_windfall.string,
                5,
            ).asTable()
        )
        // 其它
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.INCOME,
                R.string.type_other.string,
                R.string.type_icon_name_other.string,
                6,
            ).asTable()
        )
    }

    /** 初始化转账类型 */
    private suspend fun initTransferTypeData(typeDao: TypeDao) {
        // 账户互转
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.TRANSFER,
                R.string.type_account_transfer.string,
                R.string.type_icon_name_account_transfer.string,
                0,
                true
            ).asTable()
        )
        // 还信用卡
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.TRANSFER,
                R.string.type_credit_card_payment.string,
                R.string.type_icon_name_credit_card_payment.string,
                1,
                true
            ).asTable()
        )
        // 取款
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.TRANSFER,
                R.string.type_withdrawals.string,
                R.string.type_icon_name_withdrawals.string,
                2,
                true
            ).asTable()
        )
        // 存款
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.TRANSFER,
                R.string.type_deposit.string,
                R.string.type_icon_name_deposit.string,
                3,
                true
            ).asTable()
        )
        // 借入
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.TRANSFER,
                R.string.type_borrow.string,
                R.string.type_icon_name_borrow.string,
                4,
                true
            ).asTable()
        )
        // 借出
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.TRANSFER,
                R.string.type_lend.string,
                R.string.type_icon_name_lend.string,
                5,
                true
            ).asTable()
        )
        // 还款
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.TRANSFER,
                R.string.type_repayment.string,
                R.string.type_icon_name_repayment.string,
                6,
                true
            ).asTable()
        )
        // 收款
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.TRANSFER,
                R.string.type_proceeds.string,
                R.string.type_icon_name_proceeds.string,
                7,
                true
            ).asTable()
        )
        // 其它
        typeDao.insertType(
            newFirstTypeModel(
                RecordTypeCategoryEnum.TRANSFER,
                R.string.type_other.string,
                R.string.type_icon_name_other.string,
                8,
            ).asTable()
        )
    }

    private fun newFirstTypeModel(
        typeCategory: RecordTypeCategoryEnum,
        name: String,
        iconName: String,
        sort: Int,
        protected: Boolean = false
    ): RecordTypeModel {
        return RecordTypeModel(
            id = -1L,
            parentId = -1L,
            name = name,
            iconName = iconName,
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = typeCategory,
            protected = protected,
            sort = sort,
            needRelated = false,
        )
    }

    private fun newSecondTypeTable(
        parentId: RecordTypeModel,
        name: String,
        iconName: String,
        sort: Int,
        protected: Boolean = false,
    ): TypeTable {
        return RecordTypeModel(
            id = -1L,
            parentId = parentId.id,
            name = name,
            iconName = iconName,
            typeLevel = TypeLevelEnum.SECOND,
            typeCategory = parentId.typeCategory,
            protected = protected,
            sort = sort,
            needRelated = false,
        ).asTable()
    }

    private fun RecordTypeModel.asTable(): TypeTable {
        return TypeTable(
            id = if (this.id == -1L) null else this.id,
            parentId = this.parentId,
            name = this.name,
            iconName = this.iconName,
            typeLevel = this.typeLevel.ordinal,
            typeCategory = this.typeCategory.ordinal,
            protected = if (this.protected) SWITCH_INT_OFF else SWITCH_INT_OFF,
            sort = this.sort
        )
    }
}