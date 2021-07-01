package cn.wj.android.cashbook.data.entity

import androidx.databinding.ObservableBoolean
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.drawableString
import cn.wj.android.cashbook.base.ext.base.string

/**
 * 分类图标分组数据实体类
 *
 * @param name 分组名称
 * @param icons 图标数据列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/30
 */
data class TypeIconGroupEntity(
    val name: String,
    val icons: List<TypeIconEntity>
) {

    /** 标记 - 是否选中 */
    val selected: ObservableBoolean by lazy {
        ObservableBoolean(false)
    }
}

/**
 * 图标数据实体类
 *
 * @param name 名称
 * @param iconResIdStr 图标资源id
 */
data class TypeIconEntity(
    val name: String,
    val iconResIdStr: String
)

fun getTypeIconGroupList(): List<TypeIconGroupEntity> {
    return arrayListOf(
        // 收入
        TypeIconGroupEntity(
            R.string.income.string, arrayListOf(
                TypeIconEntity(R.string.type_salary.string, R.string.type_icon_name_salary.drawableString),
                TypeIconEntity(R.string.type_bonus.string, R.string.type_icon_name_bonus.drawableString),
                TypeIconEntity(R.string.type_royalty.string, R.string.type_icon_name_royalty.drawableString),
                TypeIconEntity(R.string.type_refund.string, R.string.type_icon_name_refund.drawableString),
                TypeIconEntity(R.string.type_reimburse.string, R.string.type_icon_name_reimburse.drawableString),
                TypeIconEntity(R.string.type_investment.string, R.string.type_icon_name_investment.drawableString),
                TypeIconEntity(R.string.type_provident_fund.string, R.string.type_icon_name_provident_fund.drawableString),
                TypeIconEntity(R.string.type_medicare.string, R.string.type_icon_name_medicare.drawableString),
                TypeIconEntity(R.string.type_alimony.string, R.string.type_icon_name_alimony.drawableString),
                TypeIconEntity(R.string.type_pocket_money.string, R.string.type_icon_name_pocket_money.drawableString),
                TypeIconEntity(R.string.type_lucky_money.string, R.string.type_icon_name_lucky_money.drawableString),
                TypeIconEntity(R.string.type_convert_payment.string, R.string.type_icon_name_convert_payment.drawableString),
                TypeIconEntity(R.string.type_windfall.string, R.string.type_icon_name_windfall.drawableString),
                TypeIconEntity(R.string.type_rent.string, R.string.type_icon_name_rent.drawableString),
                TypeIconEntity(R.string.type_tax_rebate.string, R.string.type_icon_name_tax_rebate.drawableString),
                TypeIconEntity(R.string.type_dividend.string, R.string.type_icon_name_dividend.drawableString),
                TypeIconEntity(R.string.type_scholarship.string, R.string.type_icon_name_scholarship.drawableString),
                TypeIconEntity(R.string.type_benefit.string, R.string.type_icon_name_benefit.drawableString),
            )
        ),
        // 转账
        TypeIconGroupEntity(
            R.string.transfer.string, arrayListOf(
                TypeIconEntity(R.string.type_account_transfer.string, R.string.type_icon_name_account_transfer.drawableString),
                TypeIconEntity(R.string.type_credit_card_payment.string, R.string.type_icon_name_credit_card_payment.drawableString),
                TypeIconEntity(R.string.type_withdrawals.string, R.string.type_icon_name_withdrawals.drawableString),
                TypeIconEntity(R.string.type_deposit.string, R.string.type_icon_name_deposit.drawableString),
                TypeIconEntity(R.string.type_borrow.string, R.string.type_icon_name_borrow.drawableString),
                TypeIconEntity(R.string.type_lend.string, R.string.type_icon_name_lend.drawableString),
                TypeIconEntity(R.string.type_repayment.string, R.string.type_icon_name_repayment.drawableString),
                TypeIconEntity(R.string.type_proceeds.string, R.string.type_icon_name_proceeds.drawableString),
            )
        ),
        // 理财
        TypeIconGroupEntity(
            R.string.money_management.string, arrayListOf(
                TypeIconEntity(R.string.type_stock.string, R.string.type_icon_name_stock.drawableString),
                TypeIconEntity(R.string.type_fund.string, R.string.type_icon_name_fund.drawableString),
                TypeIconEntity(R.string.type_money_management.string, R.string.type_icon_name_money_management.drawableString),
                TypeIconEntity(R.string.type_gold.string, R.string.type_icon_name_gold.drawableString),
                TypeIconEntity(R.string.type_bond.string, R.string.type_icon_name_bond.drawableString),
                TypeIconEntity(R.string.type_forex.string, R.string.type_icon_name_forex.drawableString),
                TypeIconEntity(R.string.type_charge.string, R.string.type_icon_name_charge.drawableString),
            )
        ),
        // 餐饮
        TypeIconGroupEntity(
            R.string.dining.string, arrayListOf(
                TypeIconEntity(R.string.type_dining.string, R.string.type_icon_name_dining.drawableString),
                TypeIconEntity(R.string.type_three_meals.string, R.string.type_icon_name_three_meals.drawableString),
                TypeIconEntity(R.string.type_breakfast.string, R.string.type_icon_name_breakfast.drawableString),
                TypeIconEntity(R.string.type_lunch.string, R.string.type_icon_name_lunch.drawableString),
                TypeIconEntity(R.string.type_dinner.string, R.string.type_icon_name_dinner.drawableString),
                TypeIconEntity(R.string.type_supper.string, R.string.type_icon_name_supper.drawableString),
                TypeIconEntity(R.string.type_takeaway.string, R.string.type_icon_name_takeaway.drawableString),
                TypeIconEntity(R.string.type_food_ingredient.string, R.string.type_icon_name_food_ingredient.drawableString),
                TypeIconEntity(R.string.type_fruit.string, R.string.type_icon_name_fruit.drawableString),
                TypeIconEntity(R.string.type_dessert.string, R.string.type_icon_name_dessert.drawableString),
                TypeIconEntity(R.string.type_snacks.string, R.string.type_icon_name_snacks.drawableString),
                TypeIconEntity(R.string.type_drinks.string, R.string.type_icon_name_drinks.drawableString),
                TypeIconEntity(R.string.type_at.string, R.string.type_icon_name_at.drawableString),
                TypeIconEntity(R.string.type_ats.string, R.string.type_icon_name_ats.drawableString),
            )
        ),
        // 日常
        TypeIconGroupEntity(
            R.string.daily.string, arrayListOf(
                TypeIconEntity(R.string.type_daily_necessities.string, R.string.type_icon_name_daily_necessities.drawableString),
                TypeIconEntity(R.string.type_kitchen_daily_necessities.string, R.string.type_icon_name_kitchen_daily_necessities.drawableString),
                TypeIconEntity(R.string.type_communication.string, R.string.type_icon_name_communication.drawableString),
                TypeIconEntity(R.string.type_call_charge.string, R.string.type_icon_name_call_charge.drawableString),
                TypeIconEntity(R.string.type_net_fee.string, R.string.type_icon_name_net_fee.drawableString),
                TypeIconEntity(R.string.type_life.string, R.string.type_icon_name_life.drawableString),
                TypeIconEntity(R.string.type_clean.string, R.string.type_icon_name_clean.drawableString),
                TypeIconEntity(R.string.type_haircut.string, R.string.type_icon_name_haircut.drawableString),
                TypeIconEntity(R.string.type_bath.string, R.string.type_icon_name_bath.drawableString),
                TypeIconEntity(R.string.type_express.string, R.string.type_icon_name_express.drawableString),
            )
        ),
        // 购物
        TypeIconGroupEntity(
            R.string.shopping.string, arrayListOf(
                TypeIconEntity(R.string.type_shopping.string, R.string.type_icon_name_shopping.drawableString),
                TypeIconEntity(R.string.type_digital_products.string, R.string.type_icon_name_digital_products.drawableString),
                TypeIconEntity(R.string.type_daily.string, R.string.type_icon_name_daily.drawableString),
                TypeIconEntity(R.string.type_electrical_appliances.string, R.string.type_icon_name_electrical_appliances.drawableString),
                TypeIconEntity(R.string.type_clothes.string, R.string.type_icon_name_clothes.drawableString),
                TypeIconEntity(R.string.type_furniture.string, R.string.type_icon_name_furniture.drawableString),
                TypeIconEntity(R.string.type_beauty.string, R.string.type_icon_name_beauty.drawableString),
                TypeIconEntity(R.string.type_body_hair.string, R.string.type_icon_name_body_hair.drawableString),
                TypeIconEntity(R.string.type_souvenir.string, R.string.type_icon_name_souvenir.drawableString),
            )
        ),
        // 服饰
        TypeIconGroupEntity(
            R.string.dress.string, arrayListOf(
                TypeIconEntity(R.string.type_hat.string, R.string.type_icon_name_hat.drawableString),
                TypeIconEntity(R.string.type_coat.string, R.string.type_icon_name_coat.drawableString),
                TypeIconEntity(R.string.type_pants.string, R.string.type_icon_name_pants.drawableString),
                TypeIconEntity(R.string.type_underwear.string, R.string.type_icon_name_underwear.drawableString),
                TypeIconEntity(R.string.type_bag.string, R.string.type_icon_name_bag.drawableString),
                TypeIconEntity(R.string.type_shoes.string, R.string.type_icon_name_shoes.drawableString),
                TypeIconEntity(R.string.type_socks.string, R.string.type_icon_name_socks.drawableString),
                TypeIconEntity(R.string.type_accessories.string, R.string.type_icon_name_accessories.drawableString),
            )
        ),
        // 数码
        TypeIconGroupEntity(
            R.string.digital.string, arrayListOf(
                TypeIconEntity(R.string.type_mobile_phone.string, R.string.type_icon_name_mobile_phone.drawableString),
                TypeIconEntity(R.string.type_mobile_phone_accessories.string, R.string.type_icon_name_mobile_phone_accessories.drawableString),
                TypeIconEntity(R.string.type_member.string, R.string.type_icon_name_member.drawableString),
                TypeIconEntity(R.string.type_camera.string, R.string.type_icon_name_camera.drawableString),
            )
        ),
        // 娱乐
        TypeIconGroupEntity(
            R.string.entertainment.string, arrayListOf(
                TypeIconEntity(R.string.type_amusement.string, R.string.type_icon_name_amusement.drawableString),
                TypeIconEntity(R.string.type_game.string, R.string.type_icon_name_game.drawableString),
                TypeIconEntity(R.string.type_party.string, R.string.type_icon_name_party.drawableString),
                TypeIconEntity(R.string.type_movie.string, R.string.type_icon_name_movie.drawableString),
                TypeIconEntity(R.string.type_sing.string, R.string.type_icon_name_sing.drawableString),
                TypeIconEntity(R.string.type_show.string, R.string.type_icon_name_show.drawableString),
                TypeIconEntity(R.string.type_travel.string, R.string.type_icon_name_travel.drawableString),
                TypeIconEntity(R.string.type_tickets.string, R.string.type_icon_name_tickets.drawableString),
                TypeIconEntity(R.string.type_excursion_fare.string, R.string.type_icon_name_excursion_fare.drawableString),
                TypeIconEntity(R.string.type_sport.string, R.string.type_icon_name_sport.drawableString),
            )
        ),
        // 家庭
        TypeIconGroupEntity(
            R.string.family.string, arrayListOf(
                TypeIconEntity(R.string.type_family.string, R.string.type_icon_name_family.drawableString),
                TypeIconEntity(R.string.type_parent.string, R.string.type_icon_name_parent.drawableString),
                TypeIconEntity(R.string.type_love.string, R.string.type_icon_name_love.drawableString),
                TypeIconEntity(R.string.type_child.string, R.string.type_icon_name_child.drawableString),
                TypeIconEntity(R.string.type_pet.string, R.string.type_icon_name_pet.drawableString),
            )
        ),
        // 育儿
        TypeIconGroupEntity(
            R.string.parenting.string, arrayListOf(
                TypeIconEntity(R.string.type_parenting.string, R.string.type_icon_name_parenting.drawableString),
                TypeIconEntity(R.string.type_milk_powder.string, R.string.type_icon_name_milk_powder.drawableString),
                TypeIconEntity(R.string.type_feeder.string, R.string.type_icon_name_feeder.drawableString),
                TypeIconEntity(R.string.type_supplementary_food.string, R.string.type_icon_name_supplementary_food.drawableString),
                TypeIconEntity(R.string.type_diapers.string, R.string.type_icon_name_diapers.drawableString),
                TypeIconEntity(R.string.type_kids.string, R.string.type_icon_name_kids.drawableString),
                TypeIconEntity(R.string.type_toy.string, R.string.type_icon_name_toy.drawableString),
                TypeIconEntity(R.string.type_early_education.string, R.string.type_icon_name_early_education.drawableString),
                TypeIconEntity(R.string.type_family_trip.string, R.string.type_icon_name_family_trip.drawableString),
                TypeIconEntity(R.string.type_vaccination.string, R.string.type_icon_name_vaccination.drawableString),
            )
        ),
        // 汽车
        TypeIconGroupEntity(
            R.string.car.string, arrayListOf(
                TypeIconEntity(R.string.type_car.string, R.string.type_icon_name_car.drawableString),
                TypeIconEntity(R.string.type_refuel.string, R.string.type_icon_name_refuel.drawableString),
                TypeIconEntity(R.string.type_car_charge.string, R.string.type_icon_name_car_charge.drawableString),
                TypeIconEntity(R.string.type_parking.string, R.string.type_icon_name_parking.drawableString),
                TypeIconEntity(R.string.type_toll.string, R.string.type_icon_name_toll.drawableString),
                TypeIconEntity(R.string.type_auto_parts.string, R.string.type_icon_name_auto_parts.drawableString),
                TypeIconEntity(R.string.type_repair.string, R.string.type_icon_name_repair.drawableString),
                TypeIconEntity(R.string.type_car_maintenance.string, R.string.type_icon_name_car_maintenance.drawableString),
                TypeIconEntity(R.string.type_car_wash.string, R.string.type_icon_name_car_wash.drawableString),
                TypeIconEntity(R.string.type_illegal.string, R.string.type_icon_name_illegal.drawableString),
                TypeIconEntity(R.string.type_car_insurance.string, R.string.type_icon_name_car_insurance.drawableString),
                TypeIconEntity(R.string.type_car_inspection.string, R.string.type_icon_name_car_inspection.drawableString),
                TypeIconEntity(R.string.type_car_loan.string, R.string.type_icon_name_car_loan.drawableString),
            )
        ),
        // 人情
        TypeIconGroupEntity(
            R.string.interpersonal.string, arrayListOf(
                TypeIconEntity(R.string.type_interpersonal.string, R.string.type_icon_name_interpersonal.drawableString),
                TypeIconEntity(R.string.type_cash_gift.string, R.string.type_icon_name_cash_gift.drawableString),
                TypeIconEntity(R.string.type_gift.string, R.string.type_icon_name_gift.drawableString),
                TypeIconEntity(R.string.type_treat.string, R.string.type_icon_name_treat.drawableString),
            )
        ),
        // 交通
        TypeIconGroupEntity(
            R.string.traffic.string, arrayListOf(
                TypeIconEntity(R.string.type_traffic.string, R.string.type_icon_name_traffic.drawableString),
                TypeIconEntity(R.string.type_bus.string, R.string.type_icon_name_bus.drawableString),
                TypeIconEntity(R.string.type_subway.string, R.string.type_icon_name_subway.drawableString),
                TypeIconEntity(R.string.type_train.string, R.string.type_icon_name_train.drawableString),
                TypeIconEntity(R.string.type_plane.string, R.string.type_icon_name_plane.drawableString),
                TypeIconEntity(R.string.type_taxi.string, R.string.type_icon_name_taxi.drawableString),
                TypeIconEntity(R.string.type_ship.string, R.string.type_icon_name_ship.drawableString),
                TypeIconEntity(R.string.type_bicycle.string, R.string.type_icon_name_bicycle.drawableString),
            )
        ),
        // 住房
        TypeIconGroupEntity(
            R.string.housing.string, arrayListOf(
                TypeIconEntity(R.string.type_housing.string, R.string.type_icon_name_housing.drawableString),
                TypeIconEntity(R.string.type_house_rent.string, R.string.type_icon_name_house_rent.drawableString),
                TypeIconEntity(R.string.type_house_loan.string, R.string.type_icon_name_house_loan.drawableString),
                TypeIconEntity(R.string.type_hotel.string, R.string.type_icon_name_hotel.drawableString),
            )
        ),
        // 医疗
        TypeIconGroupEntity(
            R.string.medical.string, arrayListOf(
                TypeIconEntity(R.string.type_medical.string, R.string.type_icon_name_medical.drawableString),
                TypeIconEntity(R.string.type_medkit.string, R.string.type_icon_name_medkit.drawableString),
                TypeIconEntity(R.string.type_registration.string, R.string.type_icon_name_registration.drawableString),
                TypeIconEntity(R.string.type_diagnose.string, R.string.type_icon_name_diagnose.drawableString),
                TypeIconEntity(R.string.type_drug.string, R.string.type_icon_name_drug.drawableString),
                TypeIconEntity(R.string.type_hospitalization.string, R.string.type_icon_name_hospitalization.drawableString),
                TypeIconEntity(R.string.type_hygiene.string, R.string.type_icon_name_hygiene.drawableString),
            )
        ),
        // 校园
        TypeIconGroupEntity(
            R.string.campus.string, arrayListOf(
                TypeIconEntity(R.string.type_book_education.string, R.string.type_icon_name_book_education.drawableString),
                TypeIconEntity(R.string.type_tuition.string, R.string.type_icon_name_tuition.drawableString),
                TypeIconEntity(R.string.type_stationery.string, R.string.type_icon_name_stationery.drawableString),
                TypeIconEntity(R.string.type_exam.string, R.string.type_icon_name_exam.drawableString),
                TypeIconEntity(R.string.type_books.string, R.string.type_icon_name_books.drawableString),
                TypeIconEntity(R.string.type_training.string, R.string.type_icon_name_training.drawableString),
            )
        ),
        // 物业
        TypeIconGroupEntity(
            R.string.tenement.string, arrayListOf(
                TypeIconEntity(R.string.type_water_rate.string, R.string.type_icon_name_water_rate.drawableString),
                TypeIconEntity(R.string.type_electricity.string, R.string.type_icon_name_electricity.drawableString),
                TypeIconEntity(R.string.type_gas_charge.string, R.string.type_icon_name_gas_charge.drawableString),
                TypeIconEntity(R.string.type_garbage_fee.string, R.string.type_icon_name_garbage_fee.drawableString),
                TypeIconEntity(R.string.type_property_costs.string, R.string.type_icon_name_property_costs.drawableString),
                TypeIconEntity(R.string.type_heating_fee.string, R.string.type_icon_name_heating_fee.drawableString),
            )
        ),
        // 其它
        TypeIconGroupEntity(
            R.string.other.string, arrayListOf(
                TypeIconEntity(R.string.type_other.string, R.string.type_icon_name_other.drawableString),
            )
        ),
    )

}