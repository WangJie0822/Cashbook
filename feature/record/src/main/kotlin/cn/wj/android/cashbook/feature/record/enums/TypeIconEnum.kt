package cn.wj.android.cashbook.feature.record.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cn.wj.android.cashbook.feature.record.R

/**
 * 类型图标枚举
 *
 * @param group 所在分组枚举
 * @param nameResId 默认名称资源 id
 * @param drawableResId 图标资源 id
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
enum class TypeIconEnum(
    val group: TypeIconGroupEnum,
    @StringRes val nameResId: Int,
    @DrawableRes val drawableResId: Int
) {
    /* 收入 */

    /** 薪资 */
    SALARY(TypeIconGroupEnum.INCOME, R.string.type_salary, R.drawable.vector_type_salary_24),

    /** 奖金 */
    BONUS(TypeIconGroupEnum.INCOME, R.string.type_bonus, R.drawable.vector_type_bonus_24),

    /** 提成 */
    ROYALTY(TypeIconGroupEnum.INCOME, R.string.type_royalty, R.drawable.vector_type_royalty_24),

    /** 退款 */
    REFUND(TypeIconGroupEnum.INCOME, R.string.type_refund, R.drawable.vector_type_refund_24),

    /** 报销 */
    REIMBURSE(
        TypeIconGroupEnum.INCOME,
        R.string.type_reimburse,
        R.drawable.vector_type_reimburse_24
    ),

    /** 投资 */
    INVESTMENT(
        TypeIconGroupEnum.INCOME,
        R.string.type_investment,
        R.drawable.vector_type_investment_24
    ),

    /** 公积金 */
    PROVIDENT_FUND(
        TypeIconGroupEnum.INCOME,
        R.string.type_provident_fund,
        R.drawable.vector_type_provident_fund_24
    ),

    /** 医保 */
    MEDICARE(TypeIconGroupEnum.INCOME, R.string.type_medicare, R.drawable.vector_type_medicare_24),

    /** 生活费 */
    ALIMONY(TypeIconGroupEnum.INCOME, R.string.type_alimony, R.drawable.vector_type_alimony_24),

    /** 零花钱 */
    POCKET_MONEY(
        TypeIconGroupEnum.INCOME,
        R.string.type_pocket_money,
        R.drawable.vector_type_pocket_money_24
    ),

    /** 压岁钱 */
    LUCKY_MONEY(
        TypeIconGroupEnum.INCOME,
        R.string.type_lucky_money,
        R.drawable.vector_type_lucky_money_24
    ),

    /** 红包 */
    CONVERT_PAYMENT(
        TypeIconGroupEnum.INCOME,
        R.string.type_convert_payment,
        R.drawable.vector_type_convert_payment_24
    ),

    /** 外快 */
    WINDFALL(TypeIconGroupEnum.INCOME, R.string.type_windfall, R.drawable.vector_type_windfall_24),

    /** 租金 */
    RENT(TypeIconGroupEnum.INCOME, R.string.type_rent, R.drawable.vector_type_rent_24),

    /** 退税 */
    TAX_REBATE(
        TypeIconGroupEnum.INCOME,
        R.string.type_tax_rebate,
        R.drawable.vector_type_tax_rebate_24
    ),

    /** 分红 */
    DIVIDEND(TypeIconGroupEnum.INCOME, R.string.type_dividend, R.drawable.vector_type_dividend_24),

    /** 奖学金 */
    SCHOLARSHIP(
        TypeIconGroupEnum.INCOME,
        R.string.type_scholarship,
        R.drawable.vector_type_scholarship_24
    ),

    /** 津贴 */
    BENEFIT(TypeIconGroupEnum.INCOME, R.string.type_benefit, R.drawable.vector_type_benefit_24),

    /* 转账 */
    /** 转账 */
    TRANSFER(
        TypeIconGroupEnum.TRANSFER,
        R.string.type_account_transfer,
        R.drawable.vector_type_account_transfer_24
    ),

    /** 还信用卡 */
    CREDIT_CARD_PAYMENT(
        TypeIconGroupEnum.TRANSFER,
        R.string.type_credit_card_payment,
        R.drawable.vector_type_credit_card_payment_24
    ),

    /** 取款 */
    WITHDRAWALS(
        TypeIconGroupEnum.TRANSFER,
        R.string.type_withdrawals,
        R.drawable.vector_type_withdrawals_24
    ),

    /** 存款 */
    DEPOSIT(TypeIconGroupEnum.TRANSFER, R.string.type_deposit, R.drawable.vector_type_deposit_24),

    /** 借入 */
    BORROW(TypeIconGroupEnum.TRANSFER, R.string.type_borrow, R.drawable.vector_type_borrow_24),

    /** 借出 */
    LEND(TypeIconGroupEnum.TRANSFER, R.string.type_lend, R.drawable.vector_type_lend_24),

    /** 还款 */
    REPAYMENT(
        TypeIconGroupEnum.TRANSFER,
        R.string.type_repayment,
        R.drawable.vector_type_repayment_24
    ),

    /** 借款 */
    PROCEEDS(
        TypeIconGroupEnum.TRANSFER,
        R.string.type_proceeds,
        R.drawable.vector_type_proceeds_24
    ),

    /* 理财 */
    /** 股票 */
    STOCK(
        TypeIconGroupEnum.MONEY_MANAGEMENT,
        R.string.type_stock,
        R.drawable.vector_type_investment_24
    ),

    /** 基金 */
    FUND(TypeIconGroupEnum.MONEY_MANAGEMENT, R.string.type_fund, R.drawable.vector_type_fund_24),

    /** 理财 */
    MONEY_MANAGEMENT(
        TypeIconGroupEnum.MONEY_MANAGEMENT,
        R.string.type_money_management,
        R.drawable.vector_type_money_management_24
    ),

    /** 黄金 */
    GOLD(TypeIconGroupEnum.MONEY_MANAGEMENT, R.string.type_gold, R.drawable.vector_type_gold_24),

    /** 债券 */
    BOND(TypeIconGroupEnum.MONEY_MANAGEMENT, R.string.type_bond, R.drawable.vector_type_bond_24),

    /** 外汇 */
    FOREX(TypeIconGroupEnum.MONEY_MANAGEMENT, R.string.type_forex, R.drawable.vector_type_forex_24),

    /** 手续费 */
    CHARGE(
        TypeIconGroupEnum.MONEY_MANAGEMENT,
        R.string.type_charge,
        R.drawable.vector_type_charge_24
    ),

    /* 餐饮 */
    /** 餐饮 */
    DINING(TypeIconGroupEnum.DINING, R.string.type_dining, R.drawable.vector_type_dining_24),

    /** 三餐 */
    THREE_MEALS(
        TypeIconGroupEnum.DINING,
        R.string.type_three_meals,
        R.drawable.vector_type_three_meals_24
    ),

    /** 早餐 */
    BREAKFAST(
        TypeIconGroupEnum.DINING,
        R.string.type_breakfast,
        R.drawable.vector_type_breakfast_24
    ),

    /** 午餐 */
    LUNCH(TypeIconGroupEnum.DINING, R.string.type_lunch, R.drawable.vector_type_lunch_24),

    /** 晚餐 */
    DINNER(TypeIconGroupEnum.DINING, R.string.type_dinner, R.drawable.vector_type_dinner_24),

    /** 夜宵 */
    SUPPER(TypeIconGroupEnum.DINING, R.string.type_supper, R.drawable.vector_type_supper_24),

    /** 外卖 */
    TAKEAWAY(TypeIconGroupEnum.DINING, R.string.type_takeaway, R.drawable.vector_type_takeaway_24),

    /** 食材 */
    FOOD_INGREDIENT(
        TypeIconGroupEnum.DINING,
        R.string.type_food_ingredient,
        R.drawable.vector_type_food_ingredient_24
    ),

    /** 水果 */
    FRUIT(TypeIconGroupEnum.DINING, R.string.type_fruit, R.drawable.vector_type_fruit_24),

    /** 甜点 */
    DESSERT(TypeIconGroupEnum.DINING, R.string.type_dessert, R.drawable.vector_type_dessert_24),

    /** 零食 */
    SNACKS(TypeIconGroupEnum.DINING, R.string.type_snacks, R.drawable.vector_type_snacks_24),

    /** 饮料 */
    DRINKS(TypeIconGroupEnum.DINING, R.string.type_drinks, R.drawable.vector_type_drinks_24),

    /** 烟酒 */
    AT(TypeIconGroupEnum.DINING, R.string.type_at, R.drawable.vector_type_at_24),

    /** 烟酒零食 */
    ATS(TypeIconGroupEnum.DINING, R.string.type_ats, R.drawable.vector_type_ats_24),

    /* 日常 */
    /** 日用 */
    DAILY_NECESSITIES(
        TypeIconGroupEnum.DAILY,
        R.string.type_daily_necessities,
        R.drawable.vector_type_daily_necessities_24
    ),

    /** 柴米油盐 */
    KITCHEN_DAILY_NECESSITIES(
        TypeIconGroupEnum.DAILY,
        R.string.type_kitchen_daily_necessities,
        R.drawable.vector_type_kitchen_daily_necessities_24
    ),

    /** 通讯 */
    COMMUNICATION(
        TypeIconGroupEnum.DAILY,
        R.string.type_communication,
        R.drawable.vector_type_communication_24
    ),

    /** 话费 */
    CALL_CHARGE(
        TypeIconGroupEnum.DAILY,
        R.string.type_call_charge,
        R.drawable.vector_type_call_charge_24
    ),

    /** 网费 */
    NET_FEE(TypeIconGroupEnum.DAILY, R.string.type_net_fee, R.drawable.vector_type_net_fee_24),

    /** 生活 */
    LIFE(TypeIconGroupEnum.DAILY, R.string.type_life, R.drawable.vector_type_life_24),

    /** 清洁 */
    CLEAN(TypeIconGroupEnum.DAILY, R.string.type_clean, R.drawable.vector_type_clean_24),

    /** 理发 */
    HAIRCUT(TypeIconGroupEnum.DAILY, R.string.type_haircut, R.drawable.vector_type_haircut_24),

    /** 洗澡 */
    BATH(TypeIconGroupEnum.DAILY, R.string.type_bath, R.drawable.vector_type_bath_24),

    /** 快递 */
    EXPRESS(TypeIconGroupEnum.DAILY, R.string.type_express, R.drawable.vector_type_express_24),

    /* 购物 */
    /** 购物 */
    SHOPPING(
        TypeIconGroupEnum.SHOPPING,
        R.string.type_shopping,
        R.drawable.vector_type_shopping_24
    ),

    /** 数码 */
    DIGITAL_PRODUCTS(
        TypeIconGroupEnum.SHOPPING,
        R.string.type_digital_products,
        R.drawable.vector_type_digital_product_24
    ),

    /** 日常 */
    DAILY(TypeIconGroupEnum.SHOPPING, R.string.type_daily, R.drawable.vector_type_daily_24),

    /** 电器 */
    ELECTRICAL_APPLIANCES(
        TypeIconGroupEnum.SHOPPING,
        R.string.type_electrical_appliances,
        R.drawable.vector_type_electrical_appliance_24
    ),

    /** 鞋服 */
    CLOTHES(TypeIconGroupEnum.SHOPPING, R.string.type_clothes, R.drawable.vector_type_clothes_24),

    /** 家具 */
    FURNITURE(
        TypeIconGroupEnum.SHOPPING,
        R.string.type_furniture,
        R.drawable.vector_type_furniture_24
    ),

    /** 美妆 */
    BEAUTY(TypeIconGroupEnum.SHOPPING, R.string.type_beauty, R.drawable.vector_type_beauty_24),

    /** 洗护 */
    BODY_HAIR(
        TypeIconGroupEnum.SHOPPING,
        R.string.type_body_hair,
        R.drawable.vector_type_body_hair_24
    ),

    /** 纪念品 */
    SOUVENIR(
        TypeIconGroupEnum.SHOPPING,
        R.string.type_souvenir,
        R.drawable.vector_type_souvenir_24
    ),

    /* 服饰 */
    /** 帽子 */
    HAT(TypeIconGroupEnum.DRESS, R.string.type_hat, R.drawable.vector_type_hat_24),

    /** 外套 */
    COAT(TypeIconGroupEnum.DRESS, R.string.type_coat, R.drawable.vector_type_coat_24),

    /** 裤子 */
    PANTS(TypeIconGroupEnum.DRESS, R.string.type_pants, R.drawable.vector_type_pants_24),

    /** 内衣 */
    UNDERWEAR(
        TypeIconGroupEnum.DRESS,
        R.string.type_underwear,
        R.drawable.vector_type_underwear_24
    ),

    /** 包 */
    BAG(TypeIconGroupEnum.DRESS, R.string.type_bag, R.drawable.vector_type_bag_24),

    /** 鞋子 */
    SHOES(TypeIconGroupEnum.DRESS, R.string.type_shoes, R.drawable.vector_type_shoes_24),

    /** 袜子 */
    SOCKS(TypeIconGroupEnum.DRESS, R.string.type_socks, R.drawable.vector_type_socks_24),

    /** 饰品 */
    ACCESSORIES(
        TypeIconGroupEnum.DRESS,
        R.string.type_accessories,
        R.drawable.vector_type_accessories_24
    ),

    /* 数码 */
    /** 手机 */
    MOBILE_PHONE(
        TypeIconGroupEnum.DIGITAL,
        R.string.type_mobile_phone,
        R.drawable.vector_type_mobile_phone_24
    ),

    /** 手机配件 */
    MOBILE_PHONE_ACCESSORIES(
        TypeIconGroupEnum.DIGITAL,
        R.string.type_mobile_phone_accessories,
        R.drawable.vector_type_mobile_phone_accessories_24
    ),

    /** 付费会员 */
    MEMBER(TypeIconGroupEnum.DIGITAL, R.string.type_member, R.drawable.vector_type_member_24),

    /** 相机 */
    CAMERA(TypeIconGroupEnum.DIGITAL, R.string.type_camera, R.drawable.vector_type_camera_24),

    /* 娱乐 */
    /** 娱乐 */
    AMUSEMENT(
        TypeIconGroupEnum.ENTERTAINMENT,
        R.string.type_amusement,
        R.drawable.vector_type_amusement_24
    ),

    /** 游戏 */
    GAME(TypeIconGroupEnum.ENTERTAINMENT, R.string.type_game, R.drawable.vector_type_game_24),

    /** 聚会 */
    PARTY(TypeIconGroupEnum.ENTERTAINMENT, R.string.type_party, R.drawable.vector_type_party_24),

    /** 电影 */
    MOVIE(TypeIconGroupEnum.ENTERTAINMENT, R.string.type_movie, R.drawable.vector_type_movie_24),

    /** K歌 */
    SING(TypeIconGroupEnum.ENTERTAINMENT, R.string.type_sing, R.drawable.vector_type_sing_24),

    /** 演出 */
    SHOW(TypeIconGroupEnum.ENTERTAINMENT, R.string.type_show, R.drawable.vector_type_show_24),

    /** 旅游 */
    TRAVEL(TypeIconGroupEnum.ENTERTAINMENT, R.string.type_travel, R.drawable.vector_type_travel_24),

    /** 门票 */
    TICKETS(
        TypeIconGroupEnum.ENTERTAINMENT,
        R.string.type_tickets,
        R.drawable.vector_type_tickets_24
    ),

    /** 团费 */
    EXCURSION_FARE(
        TypeIconGroupEnum.ENTERTAINMENT,
        R.string.type_excursion_fare,
        R.drawable.vector_type_excursion_fare_24
    ),

    /** 运动 */
    SPORT(TypeIconGroupEnum.ENTERTAINMENT, R.string.type_sport, R.drawable.vector_type_sport_24),

    /* 家庭 */
    /** 家人 */
    FAMILY(TypeIconGroupEnum.FAMILY, R.string.type_family, R.drawable.vector_type_family_24),

    /** 父母 */
    PARENT(TypeIconGroupEnum.FAMILY, R.string.type_parent, R.drawable.vector_type_parent_24),

    /** 恋爱 */
    LOVE(TypeIconGroupEnum.FAMILY, R.string.type_love, R.drawable.vector_type_love_24),

    /** 孩子 */
    CHILD(TypeIconGroupEnum.FAMILY, R.string.type_child, R.drawable.vector_type_child_24),

    /** 宠物 */
    PET(TypeIconGroupEnum.FAMILY, R.string.type_pet, R.drawable.vector_type_pet_24),

    /* 育儿 */
    /** 育儿 */
    PARENTING(
        TypeIconGroupEnum.PARENTING,
        R.string.type_parenting,
        R.drawable.vector_type_parenting_24
    ),

    /** 奶粉 */
    MILK_POWDER(
        TypeIconGroupEnum.PARENTING,
        R.string.type_milk_powder,
        R.drawable.vector_type_milk_powder_24
    ),

    /** 奶瓶 */
    FEEDER(TypeIconGroupEnum.PARENTING, R.string.type_feeder, R.drawable.vector_type_feeder_24),

    /** 辅食 */
    SUPPLEMENTARY_FOOD(
        TypeIconGroupEnum.PARENTING,
        R.string.type_supplementary_food,
        R.drawable.vector_type_supplementary_food_24
    ),

    /** 纸尿裤 */
    DIAPERS(TypeIconGroupEnum.PARENTING, R.string.type_diapers, R.drawable.vector_type_diapers_24),

    /** 童装 */
    KIDS(TypeIconGroupEnum.PARENTING, R.string.type_kids, R.drawable.vector_type_kids_24),

    /** 玩具 */
    TOY(TypeIconGroupEnum.PARENTING, R.string.type_toy, R.drawable.vector_type_toy_24),

    /** 早教 */
    EARLY_EDUCATION(
        TypeIconGroupEnum.PARENTING,
        R.string.type_early_education,
        R.drawable.vector_type_early_education_24
    ),

    /** 亲子游 */
    FAMILY_TRIP(
        TypeIconGroupEnum.PARENTING,
        R.string.type_family_trip,
        R.drawable.vector_type_family_trip_24
    ),

    /** 疫苗接种 */
    VACCINATION(
        TypeIconGroupEnum.PARENTING,
        R.string.type_vaccination,
        R.drawable.vector_type_vaccination_24
    ),

    /* 汽车 */
    /** 汽车 */
    CAR(TypeIconGroupEnum.CAR, R.string.type_car, R.drawable.vector_type_car_24),

    /** 加油 */
    REFUEL(TypeIconGroupEnum.CAR, R.string.type_refuel, R.drawable.vector_type_refuel_24),

    /** 充电 */
    CAR_CHARGE(
        TypeIconGroupEnum.CAR,
        R.string.type_car_charge,
        R.drawable.vector_type_car_charge_24
    ),

    /** 停车 */
    PARKING(TypeIconGroupEnum.CAR, R.string.type_parking, R.drawable.vector_type_parking_24),

    /** 通行费 */
    TOLL(TypeIconGroupEnum.CAR, R.string.type_toll, R.drawable.vector_type_toll_24),

    /** 汽车配件 */
    AUTO_PARTS(
        TypeIconGroupEnum.CAR,
        R.string.type_auto_parts,
        R.drawable.vector_type_auto_parts_24
    ),

    /** 维修 */
    REPAIR(TypeIconGroupEnum.CAR, R.string.type_repair, R.drawable.vector_type_repair_24),

    /** 保养 */
    CAR_MAINTENANCE(
        TypeIconGroupEnum.CAR,
        R.string.type_car_maintenance,
        R.drawable.vector_type_car_maintenance_24
    ),

    /** 洗车 */
    CAR_WASH(TypeIconGroupEnum.CAR, R.string.type_car_wash, R.drawable.vector_type_car_wash_24),

    /** 违章 */
    ILLEGAL(TypeIconGroupEnum.CAR, R.string.type_illegal, R.drawable.vector_type_illegal_24),

    /** 车险 */
    CAR_INSURANCE(
        TypeIconGroupEnum.CAR,
        R.string.type_car_insurance,
        R.drawable.vector_type_car_insurance_24
    ),

    /** 车检 */
    CAR_INSPECTION(
        TypeIconGroupEnum.CAR,
        R.string.type_car_inspection,
        R.drawable.vector_type_car_inspection_24
    ),

    /** 车贷 */
    CAR_LOAN(TypeIconGroupEnum.CAR, R.string.type_car_loan, R.drawable.vector_type_car_loan_24),

    /* 人际关系 */
    /** 人际关系 */
    INTERPERSONAL(
        TypeIconGroupEnum.INTERPERSONAL,
        R.string.type_interpersonal,
        R.drawable.vector_type_interpersonal_24
    ),

    /** 礼金 */
    CASH_GIFT(
        TypeIconGroupEnum.INTERPERSONAL,
        R.string.type_cash_gift,
        R.drawable.vector_type_cash_gift_24
    ),

    /** 礼品 */
    GIFT(TypeIconGroupEnum.INTERPERSONAL, R.string.type_gift, R.drawable.vector_type_gift_24),

    /** 请客 */
    TREAT(TypeIconGroupEnum.INTERPERSONAL, R.string.type_treat, R.drawable.vector_type_treat_24),

    /* 交通 */
    /** 交通 */
    TRAFFIC(TypeIconGroupEnum.TRAFFIC, R.string.type_traffic, R.drawable.vector_type_traffic_24),

    /** 公交 */
    BUS(TypeIconGroupEnum.TRAFFIC, R.string.type_bus, R.drawable.vector_type_bus_24),

    /** 地铁 */
    SUBWAY(TypeIconGroupEnum.TRAFFIC, R.string.type_subway, R.drawable.vector_type_subway_24),

    /** 火车 */
    TRAIN(TypeIconGroupEnum.TRAFFIC, R.string.type_train, R.drawable.vector_type_train_24),

    /** 飞机 */
    PLANE(TypeIconGroupEnum.TRAFFIC, R.string.type_plane, R.drawable.vector_type_plane_24),

    /** 打车 */
    TAXI(TypeIconGroupEnum.TRAFFIC, R.string.type_taxi, R.drawable.vector_type_taxi_24),

    /** 轮渡 */
    SHIP(TypeIconGroupEnum.TRAFFIC, R.string.type_ship, R.drawable.vector_type_ship_24),

    /** 自行车 */
    BICYCLE(TypeIconGroupEnum.TRAFFIC, R.string.type_bicycle, R.drawable.vector_type_bicycle_24),

    /* 住房 */
    /** 住房 */
    HOUSING(TypeIconGroupEnum.HOUSING, R.string.type_housing, R.drawable.vector_type_housing_24),

    /** 房租 */
    HOUSE_RENT(
        TypeIconGroupEnum.HOUSING,
        R.string.type_house_rent,
        R.drawable.vector_type_house_rent_24
    ),

    /** 房贷 */
    HOUSE_LOAN(
        TypeIconGroupEnum.HOUSING,
        R.string.type_house_loan,
        R.drawable.vector_type_house_loan_24
    ),

    /** 酒店 */
    HOTEL(TypeIconGroupEnum.HOUSING, R.string.type_hotel, R.drawable.vector_type_hotel_24),

    /* 医疗 */
    /** 医疗 */
    MEDICAL(TypeIconGroupEnum.MEDICAL, R.string.type_medical, R.drawable.vector_type_medical_24),

    /** 医疗包 */
    MEDKIT(TypeIconGroupEnum.MEDICAL, R.string.type_medkit, R.drawable.vector_type_medkit_24),

    /** 挂号 */
    REGISTRATION(
        TypeIconGroupEnum.MEDICAL,
        R.string.type_registration,
        R.drawable.vector_type_registration_24
    ),

    /** 看诊 */
    DIAGNOSE(TypeIconGroupEnum.MEDICAL, R.string.type_diagnose, R.drawable.vector_type_diagnose_24),

    /** 药品 */
    DRUG(TypeIconGroupEnum.MEDICAL, R.string.type_drug, R.drawable.vector_type_drug_24),

    /** 住院 */
    HOSPITALIZATION(
        TypeIconGroupEnum.MEDICAL,
        R.string.type_hospitalization,
        R.drawable.vector_type_hospitalization_24
    ),

    /** 保健 */
    HYGIENE(TypeIconGroupEnum.MEDICAL, R.string.type_hygiene, R.drawable.vector_type_hygiene_24),

    /* 校园 */
    /** 文教 */
    BOOK_EDUCATIOn(
        TypeIconGroupEnum.CAMPUS,
        R.string.type_book_education,
        R.drawable.vector_type_books_education_24
    ),

    /** 学费 */
    TUITION(TypeIconGroupEnum.CAMPUS, R.string.type_tuition, R.drawable.vector_type_study_24),

    /** 文具 */
    STATIONERY(
        TypeIconGroupEnum.CAMPUS,
        R.string.type_stationery,
        R.drawable.vector_type_stationery_24
    ),

    /** 考试 */
    EXAM(TypeIconGroupEnum.CAMPUS, R.string.type_exam, R.drawable.vector_type_exam_24),

    /** 书籍 */
    BOOKS(TypeIconGroupEnum.CAMPUS, R.string.type_books, R.drawable.vector_type_books_24),

    /** 培训 */
    TRAINING(TypeIconGroupEnum.CAMPUS, R.string.type_training, R.drawable.vector_type_training_24),

    /* 物业 */
    /** 水费 */
    WATER_RATE(
        TypeIconGroupEnum.TENEMENT,
        R.string.type_water_rate,
        R.drawable.vector_type_water_rate_24
    ),

    /** 电费 */
    ELECTRICITY(
        TypeIconGroupEnum.TENEMENT,
        R.string.type_electricity,
        R.drawable.vector_type_electricity_24
    ),

    /** 燃气费 */
    GAS_CHARGE(
        TypeIconGroupEnum.TENEMENT,
        R.string.type_gas_charge,
        R.drawable.vector_type_gas_charge_24
    ),

    /** 垃圾费 */
    GARBAGE_FEE(
        TypeIconGroupEnum.TENEMENT,
        R.string.type_garbage_fee,
        R.drawable.vector_type_garbage_fee_24
    ),

    /** 物业费 */
    PROPERTY_COSTS(
        TypeIconGroupEnum.TENEMENT,
        R.string.type_property_costs,
        R.drawable.vector_type_property_costs_24
    ),

    /** 暖气费 */
    HEATING_FEE(
        TypeIconGroupEnum.TENEMENT,
        R.string.type_heating_fee,
        R.drawable.vector_type_heating_fee_24
    ),

    /** 其它 */
    OTHER(TypeIconGroupEnum.OTHER, R.string.type_other, R.drawable.vector_type_other_24),
}