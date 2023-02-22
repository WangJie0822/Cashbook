package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.data.R
import cn.wj.android.cashbook.core.model.model.TypeIconGroupEntity
import cn.wj.android.cashbook.core.model.model.TypeIconModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

suspend fun getTypeIconGroupList(): Flow<List<TypeIconGroupEntity>> =
    withContext(Dispatchers.IO) {
        flowOf(
            arrayListOf(
                // 收入
                TypeIconGroupEntity(
                    R.string.income, arrayListOf(
                        TypeIconModel(
                            R.string.type_salary,
                            R.string.type_icon_name_salary.string
                        ),
                        TypeIconModel(
                            R.string.type_bonus,
                            R.string.type_icon_name_bonus.string
                        ),
                        TypeIconModel(
                            R.string.type_royalty,
                            R.string.type_icon_name_royalty.string
                        ),
                        TypeIconModel(
                            R.string.type_refund,
                            R.string.type_icon_name_refund.string
                        ),
                        TypeIconModel(
                            R.string.type_reimburse,
                            R.string.type_icon_name_reimburse.string
                        ),
                        TypeIconModel(
                            R.string.type_investment,
                            R.string.type_icon_name_investment.string
                        ),
                        TypeIconModel(
                            R.string.type_provident_fund,
                            R.string.type_icon_name_provident_fund.string
                        ),
                        TypeIconModel(
                            R.string.type_medicare,
                            R.string.type_icon_name_medicare.string
                        ),
                        TypeIconModel(
                            R.string.type_alimony,
                            R.string.type_icon_name_alimony.string
                        ),
                        TypeIconModel(
                            R.string.type_pocket_money,
                            R.string.type_icon_name_pocket_money.string
                        ),
                        TypeIconModel(
                            R.string.type_lucky_money,
                            R.string.type_icon_name_lucky_money.string
                        ),
                        TypeIconModel(
                            R.string.type_convert_payment,
                            R.string.type_icon_name_convert_payment.string
                        ),
                        TypeIconModel(
                            R.string.type_windfall,
                            R.string.type_icon_name_windfall.string
                        ),
                        TypeIconModel(R.string.type_rent, R.string.type_icon_name_rent.string),
                        TypeIconModel(
                            R.string.type_tax_rebate,
                            R.string.type_icon_name_tax_rebate.string
                        ),
                        TypeIconModel(
                            R.string.type_dividend,
                            R.string.type_icon_name_dividend.string
                        ),
                        TypeIconModel(
                            R.string.type_scholarship,
                            R.string.type_icon_name_scholarship.string
                        ),
                        TypeIconModel(
                            R.string.type_benefit,
                            R.string.type_icon_name_benefit.string
                        ),
                    )
                ),
                // 转账
                TypeIconGroupEntity(
                    R.string.transfer, arrayListOf(
                        TypeIconModel(
                            R.string.type_account_transfer,
                            R.string.type_icon_name_account_transfer.string
                        ),
                        TypeIconModel(
                            R.string.type_credit_card_payment,
                            R.string.type_icon_name_credit_card_payment.string
                        ),
                        TypeIconModel(
                            R.string.type_withdrawals,
                            R.string.type_icon_name_withdrawals.string
                        ),
                        TypeIconModel(
                            R.string.type_deposit,
                            R.string.type_icon_name_deposit.string
                        ),
                        TypeIconModel(
                            R.string.type_borrow,
                            R.string.type_icon_name_borrow.string
                        ),
                        TypeIconModel(R.string.type_lend, R.string.type_icon_name_lend.string),
                        TypeIconModel(
                            R.string.type_repayment,
                            R.string.type_icon_name_repayment.string
                        ),
                        TypeIconModel(
                            R.string.type_proceeds,
                            R.string.type_icon_name_proceeds.string
                        ),
                    )
                ),
                // 理财
                TypeIconGroupEntity(
                    R.string.money_management, arrayListOf(
                        TypeIconModel(
                            R.string.type_stock,
                            R.string.type_icon_name_stock.string
                        ),
                        TypeIconModel(R.string.type_fund, R.string.type_icon_name_fund.string),
                        TypeIconModel(
                            R.string.type_money_management,
                            R.string.type_icon_name_money_management.string
                        ),
                        TypeIconModel(R.string.type_gold, R.string.type_icon_name_gold.string),
                        TypeIconModel(R.string.type_bond, R.string.type_icon_name_bond.string),
                        TypeIconModel(
                            R.string.type_forex,
                            R.string.type_icon_name_forex.string
                        ),
                        TypeIconModel(
                            R.string.type_charge,
                            R.string.type_icon_name_charge.string
                        ),
                    )
                ),
                // 餐饮
                TypeIconGroupEntity(
                    R.string.dining, arrayListOf(
                        TypeIconModel(
                            R.string.type_dining,
                            R.string.type_icon_name_dining.string
                        ),
                        TypeIconModel(
                            R.string.type_three_meals,
                            R.string.type_icon_name_three_meals.string
                        ),
                        TypeIconModel(
                            R.string.type_breakfast,
                            R.string.type_icon_name_breakfast.string
                        ),
                        TypeIconModel(
                            R.string.type_lunch,
                            R.string.type_icon_name_lunch.string
                        ),
                        TypeIconModel(
                            R.string.type_dinner,
                            R.string.type_icon_name_dinner.string
                        ),
                        TypeIconModel(
                            R.string.type_supper,
                            R.string.type_icon_name_supper.string
                        ),
                        TypeIconModel(
                            R.string.type_takeaway,
                            R.string.type_icon_name_takeaway.string
                        ),
                        TypeIconModel(
                            R.string.type_food_ingredient,
                            R.string.type_icon_name_food_ingredient.string
                        ),
                        TypeIconModel(
                            R.string.type_fruit,
                            R.string.type_icon_name_fruit.string
                        ),
                        TypeIconModel(
                            R.string.type_dessert,
                            R.string.type_icon_name_dessert.string
                        ),
                        TypeIconModel(
                            R.string.type_snacks,
                            R.string.type_icon_name_snacks.string
                        ),
                        TypeIconModel(
                            R.string.type_drinks,
                            R.string.type_icon_name_drinks.string
                        ),
                        TypeIconModel(R.string.type_at, R.string.type_icon_name_at.string),
                        TypeIconModel(R.string.type_ats, R.string.type_icon_name_ats.string),
                    )
                ),
                // 日常
                TypeIconGroupEntity(
                    R.string.daily, arrayListOf(
                        TypeIconModel(
                            R.string.type_daily_necessities,
                            R.string.type_icon_name_daily_necessities.string
                        ),
                        TypeIconModel(
                            R.string.type_kitchen_daily_necessities,
                            R.string.type_icon_name_kitchen_daily_necessities.string
                        ),
                        TypeIconModel(
                            R.string.type_communication,
                            R.string.type_icon_name_communication.string
                        ),
                        TypeIconModel(
                            R.string.type_call_charge,
                            R.string.type_icon_name_call_charge.string
                        ),
                        TypeIconModel(
                            R.string.type_net_fee,
                            R.string.type_icon_name_net_fee.string
                        ),
                        TypeIconModel(R.string.type_life, R.string.type_icon_name_life.string),
                        TypeIconModel(
                            R.string.type_clean,
                            R.string.type_icon_name_clean.string
                        ),
                        TypeIconModel(
                            R.string.type_haircut,
                            R.string.type_icon_name_haircut.string
                        ),
                        TypeIconModel(R.string.type_bath, R.string.type_icon_name_bath.string),
                        TypeIconModel(
                            R.string.type_express,
                            R.string.type_icon_name_express.string
                        ),
                    )
                ),
                // 购物
                TypeIconGroupEntity(
                    R.string.shopping, arrayListOf(
                        TypeIconModel(
                            R.string.type_shopping,
                            R.string.type_icon_name_shopping.string
                        ),
                        TypeIconModel(
                            R.string.type_digital_products,
                            R.string.type_icon_name_digital_products.string
                        ),
                        TypeIconModel(
                            R.string.type_daily,
                            R.string.type_icon_name_daily.string
                        ),
                        TypeIconModel(
                            R.string.type_electrical_appliances,
                            R.string.type_icon_name_electrical_appliances.string
                        ),
                        TypeIconModel(
                            R.string.type_clothes,
                            R.string.type_icon_name_clothes.string
                        ),
                        TypeIconModel(
                            R.string.type_furniture,
                            R.string.type_icon_name_furniture.string
                        ),
                        TypeIconModel(
                            R.string.type_beauty,
                            R.string.type_icon_name_beauty.string
                        ),
                        TypeIconModel(
                            R.string.type_body_hair,
                            R.string.type_icon_name_body_hair.string
                        ),
                        TypeIconModel(
                            R.string.type_souvenir,
                            R.string.type_icon_name_souvenir.string
                        ),
                    )
                ),
                // 服饰
                TypeIconGroupEntity(
                    R.string.dress, arrayListOf(
                        TypeIconModel(R.string.type_hat, R.string.type_icon_name_hat.string),
                        TypeIconModel(R.string.type_coat, R.string.type_icon_name_coat.string),
                        TypeIconModel(
                            R.string.type_pants,
                            R.string.type_icon_name_pants.string
                        ),
                        TypeIconModel(
                            R.string.type_underwear,
                            R.string.type_icon_name_underwear.string
                        ),
                        TypeIconModel(R.string.type_bag, R.string.type_icon_name_bag.string),
                        TypeIconModel(
                            R.string.type_shoes,
                            R.string.type_icon_name_shoes.string
                        ),
                        TypeIconModel(
                            R.string.type_socks,
                            R.string.type_icon_name_socks.string
                        ),
                        TypeIconModel(
                            R.string.type_accessories,
                            R.string.type_icon_name_accessories.string
                        ),
                    )
                ),
                // 数码
                TypeIconGroupEntity(
                    R.string.digital, arrayListOf(
                        TypeIconModel(
                            R.string.type_mobile_phone,
                            R.string.type_icon_name_mobile_phone.string
                        ),
                        TypeIconModel(
                            R.string.type_mobile_phone_accessories,
                            R.string.type_icon_name_mobile_phone_accessories.string
                        ),
                        TypeIconModel(
                            R.string.type_member,
                            R.string.type_icon_name_member.string
                        ),
                        TypeIconModel(
                            R.string.type_camera,
                            R.string.type_icon_name_camera.string
                        ),
                    )
                ),
                // 娱乐
                TypeIconGroupEntity(
                    R.string.entertainment, arrayListOf(
                        TypeIconModel(
                            R.string.type_amusement,
                            R.string.type_icon_name_amusement.string
                        ),
                        TypeIconModel(R.string.type_game, R.string.type_icon_name_game.string),
                        TypeIconModel(
                            R.string.type_party,
                            R.string.type_icon_name_party.string
                        ),
                        TypeIconModel(
                            R.string.type_movie,
                            R.string.type_icon_name_movie.string
                        ),
                        TypeIconModel(R.string.type_sing, R.string.type_icon_name_sing.string),
                        TypeIconModel(R.string.type_show, R.string.type_icon_name_show.string),
                        TypeIconModel(
                            R.string.type_travel,
                            R.string.type_icon_name_travel.string
                        ),
                        TypeIconModel(
                            R.string.type_tickets,
                            R.string.type_icon_name_tickets.string
                        ),
                        TypeIconModel(
                            R.string.type_excursion_fare,
                            R.string.type_icon_name_excursion_fare.string
                        ),
                        TypeIconModel(
                            R.string.type_sport,
                            R.string.type_icon_name_sport.string
                        ),
                    )
                ),
                // 家庭
                TypeIconGroupEntity(
                    R.string.family, arrayListOf(
                        TypeIconModel(
                            R.string.type_family,
                            R.string.type_icon_name_family.string
                        ),
                        TypeIconModel(
                            R.string.type_parent,
                            R.string.type_icon_name_parent.string
                        ),
                        TypeIconModel(R.string.type_love, R.string.type_icon_name_love.string),
                        TypeIconModel(
                            R.string.type_child,
                            R.string.type_icon_name_child.string
                        ),
                        TypeIconModel(R.string.type_pet, R.string.type_icon_name_pet.string),
                    )
                ),
                // 育儿
                TypeIconGroupEntity(
                    R.string.parenting, arrayListOf(
                        TypeIconModel(
                            R.string.type_parenting,
                            R.string.type_icon_name_parenting.string
                        ),
                        TypeIconModel(
                            R.string.type_milk_powder,
                            R.string.type_icon_name_milk_powder.string
                        ),
                        TypeIconModel(
                            R.string.type_feeder,
                            R.string.type_icon_name_feeder.string
                        ),
                        TypeIconModel(
                            R.string.type_supplementary_food,
                            R.string.type_icon_name_supplementary_food.string
                        ),
                        TypeIconModel(
                            R.string.type_diapers,
                            R.string.type_icon_name_diapers.string
                        ),
                        TypeIconModel(R.string.type_kids, R.string.type_icon_name_kids.string),
                        TypeIconModel(R.string.type_toy, R.string.type_icon_name_toy.string),
                        TypeIconModel(
                            R.string.type_early_education,
                            R.string.type_icon_name_early_education.string
                        ),
                        TypeIconModel(
                            R.string.type_family_trip,
                            R.string.type_icon_name_family_trip.string
                        ),
                        TypeIconModel(
                            R.string.type_vaccination,
                            R.string.type_icon_name_vaccination.string
                        ),
                    )
                ),
                // 汽车
                TypeIconGroupEntity(
                    R.string.car, arrayListOf(
                        TypeIconModel(R.string.type_car, R.string.type_icon_name_car.string),
                        TypeIconModel(
                            R.string.type_refuel,
                            R.string.type_icon_name_refuel.string
                        ),
                        TypeIconModel(
                            R.string.type_car_charge,
                            R.string.type_icon_name_car_charge.string
                        ),
                        TypeIconModel(
                            R.string.type_parking,
                            R.string.type_icon_name_parking.string
                        ),
                        TypeIconModel(R.string.type_toll, R.string.type_icon_name_toll.string),
                        TypeIconModel(
                            R.string.type_auto_parts,
                            R.string.type_icon_name_auto_parts.string
                        ),
                        TypeIconModel(
                            R.string.type_repair,
                            R.string.type_icon_name_repair.string
                        ),
                        TypeIconModel(
                            R.string.type_car_maintenance,
                            R.string.type_icon_name_car_maintenance.string
                        ),
                        TypeIconModel(
                            R.string.type_car_wash,
                            R.string.type_icon_name_car_wash.string
                        ),
                        TypeIconModel(
                            R.string.type_illegal,
                            R.string.type_icon_name_illegal.string
                        ),
                        TypeIconModel(
                            R.string.type_car_insurance,
                            R.string.type_icon_name_car_insurance.string
                        ),
                        TypeIconModel(
                            R.string.type_car_inspection,
                            R.string.type_icon_name_car_inspection.string
                        ),
                        TypeIconModel(
                            R.string.type_car_loan,
                            R.string.type_icon_name_car_loan.string
                        ),
                    )
                ),
                // 人情
                TypeIconGroupEntity(
                    R.string.interpersonal, arrayListOf(
                        TypeIconModel(
                            R.string.type_interpersonal,
                            R.string.type_icon_name_interpersonal.string
                        ),
                        TypeIconModel(
                            R.string.type_cash_gift,
                            R.string.type_icon_name_cash_gift.string
                        ),
                        TypeIconModel(R.string.type_gift, R.string.type_icon_name_gift.string),
                        TypeIconModel(
                            R.string.type_treat,
                            R.string.type_icon_name_treat.string
                        ),
                    )
                ),
                // 交通
                TypeIconGroupEntity(
                    R.string.traffic, arrayListOf(
                        TypeIconModel(
                            R.string.type_traffic,
                            R.string.type_icon_name_traffic.string
                        ),
                        TypeIconModel(R.string.type_bus, R.string.type_icon_name_bus.string),
                        TypeIconModel(
                            R.string.type_subway,
                            R.string.type_icon_name_subway.string
                        ),
                        TypeIconModel(
                            R.string.type_train,
                            R.string.type_icon_name_train.string
                        ),
                        TypeIconModel(
                            R.string.type_plane,
                            R.string.type_icon_name_plane.string
                        ),
                        TypeIconModel(R.string.type_taxi, R.string.type_icon_name_taxi.string),
                        TypeIconModel(R.string.type_ship, R.string.type_icon_name_ship.string),
                        TypeIconModel(
                            R.string.type_bicycle,
                            R.string.type_icon_name_bicycle.string
                        ),
                    )
                ),
                // 住房
                TypeIconGroupEntity(
                    R.string.housing, arrayListOf(
                        TypeIconModel(
                            R.string.type_housing,
                            R.string.type_icon_name_housing.string
                        ),
                        TypeIconModel(
                            R.string.type_house_rent,
                            R.string.type_icon_name_house_rent.string
                        ),
                        TypeIconModel(
                            R.string.type_house_loan,
                            R.string.type_icon_name_house_loan.string
                        ),
                        TypeIconModel(
                            R.string.type_hotel,
                            R.string.type_icon_name_hotel.string
                        ),
                    )
                ),
                // 医疗
                TypeIconGroupEntity(
                    R.string.medical, arrayListOf(
                        TypeIconModel(
                            R.string.type_medical,
                            R.string.type_icon_name_medical.string
                        ),
                        TypeIconModel(
                            R.string.type_medkit,
                            R.string.type_icon_name_medkit.string
                        ),
                        TypeIconModel(
                            R.string.type_registration,
                            R.string.type_icon_name_registration.string
                        ),
                        TypeIconModel(
                            R.string.type_diagnose,
                            R.string.type_icon_name_diagnose.string
                        ),
                        TypeIconModel(R.string.type_drug, R.string.type_icon_name_drug.string),
                        TypeIconModel(
                            R.string.type_hospitalization,
                            R.string.type_icon_name_hospitalization.string
                        ),
                        TypeIconModel(
                            R.string.type_hygiene,
                            R.string.type_icon_name_hygiene.string
                        ),
                    )
                ),
                // 校园
                TypeIconGroupEntity(
                    R.string.campus, arrayListOf(
                        TypeIconModel(
                            R.string.type_book_education,
                            R.string.type_icon_name_book_education.string
                        ),
                        TypeIconModel(
                            R.string.type_tuition,
                            R.string.type_icon_name_tuition.string
                        ),
                        TypeIconModel(
                            R.string.type_stationery,
                            R.string.type_icon_name_stationery.string
                        ),
                        TypeIconModel(R.string.type_exam, R.string.type_icon_name_exam.string),
                        TypeIconModel(
                            R.string.type_books,
                            R.string.type_icon_name_books.string
                        ),
                        TypeIconModel(
                            R.string.type_training,
                            R.string.type_icon_name_training.string
                        ),
                    )
                ),
                // 物业
                TypeIconGroupEntity(
                    R.string.tenement, arrayListOf(
                        TypeIconModel(
                            R.string.type_water_rate,
                            R.string.type_icon_name_water_rate.string
                        ),
                        TypeIconModel(
                            R.string.type_electricity,
                            R.string.type_icon_name_electricity.string
                        ),
                        TypeIconModel(
                            R.string.type_gas_charge,
                            R.string.type_icon_name_gas_charge.string
                        ),
                        TypeIconModel(
                            R.string.type_garbage_fee,
                            R.string.type_icon_name_garbage_fee.string
                        ),
                        TypeIconModel(
                            R.string.type_property_costs,
                            R.string.type_icon_name_property_costs.string
                        ),
                        TypeIconModel(
                            R.string.type_heating_fee,
                            R.string.type_icon_name_heating_fee.string
                        ),
                    )
                ),
                // 其它
                TypeIconGroupEntity(
                    R.string.other, arrayListOf(
                        TypeIconModel(
                            R.string.type_other,
                            R.string.type_icon_name_other.string
                        ),
                    )
                ),
            )
        )
    }