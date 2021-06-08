package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.annotation.StringRes
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.getSharedBoolean
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MAIN
import cn.wj.android.cashbook.data.constants.SHARED_KEY_TYPE_INITIALIZED
import cn.wj.android.cashbook.data.constants.SPLASH_WAIT_MS
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import java.util.Date
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 闪屏界面 ViewModel
 *
 * @param local 本地数据存储对象
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/16
 */
class SplashViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 信息字符串 */
    val infoStr: ObservableField<String> = ObservableField(
        "${R.string.app_name.string}\n" +
                "${BuildConfig.VERSION_NAME}\n" +
                "©2021 - ${Date().dateFormat("yyyy")} By WangJie0822"
    )

    /** 初始化相关数据 */
    fun init() {
        viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            try {
                initBooksData()
                initTypeData()
            } catch (throwable: Throwable) {
                logger().e(throwable, "init")
            } finally {
                // 消耗的时间
                val spendMs = (System.currentTimeMillis() - startMs).absoluteValue
                logger().d("init spendMs: $spendMs")
                if (spendMs < SPLASH_WAIT_MS) {
                    // 耗时小于等待时间，等待凑足时长
                    delay(SPLASH_WAIT_MS - spendMs)
                }
                // 跳转首页并关闭启动页
                uiNavigationData.value = UiNavigationModel.builder {
                    jump(ROUTE_PATH_MAIN)
                    close()
                }
            }
        }
    }

    /** 初始化账本信息 */
    private suspend fun initBooksData() {
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
    private suspend fun initTypeData() {
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
        // 添加数据
        fun generateIconRes(@StringRes nameResId: Int): String {
            return "@drawable/${nameResId.string}"
        }
        // 餐饮数据
        val diningId = local.insertType(TypeEntity.newFirst(R.string.type_dining.string, generateIconRes(R.string.type_icon_name_dining)))
        local.insertTypes(
            // 三餐
            TypeEntity.newSecond(parentId = diningId, name = R.string.type_three_meals.string, generateIconRes(R.string.type_icon_name_three_meals)),
            // 夜宵
            TypeEntity.newSecond(parentId = diningId, name = R.string.type_supper.string, generateIconRes(R.string.type_icon_name_supper)),
            // 柴米油盐
            TypeEntity.newSecond(parentId = diningId, name = R.string.type_kitchen_daily_necessities.string, generateIconRes(R.string.type_icon_name_kitchen_daily_necessities)),
            // 食材
            TypeEntity.newSecond(parentId = diningId, name = R.string.type_food_ingredient.string, generateIconRes(R.string.type_icon_name_food_ingredient))
        )
        // 烟酒零食
        val atsId = local.insertType(TypeEntity.newFirst(R.string.type_ats.string, generateIconRes(R.string.type_icon_name_ats)))
        local.insertTypes(
            // 水果
            TypeEntity.newSecond(parentId = atsId, name = R.string.type_fruit.string, generateIconRes(R.string.type_icon_name_fruit)),
            // 甜点
            TypeEntity.newSecond(parentId = atsId, name = R.string.type_dessert.string, generateIconRes(R.string.type_icon_name_dessert)),
            // 零食
            TypeEntity.newSecond(parentId = atsId, name = R.string.type_snacks.string, generateIconRes(R.string.type_icon_name_snacks)),
            // 饮料
            TypeEntity.newSecond(parentId = atsId, name = R.string.type_drinks.string, generateIconRes(R.string.type_icon_name_drinks)),
            // 烟酒
            TypeEntity.newSecond(parentId = atsId, name = R.string.type_at.string, generateIconRes(R.string.type_icon_name_at))
        )
        // 购物
        val shoppingId = local.insertType(TypeEntity.newFirst(R.string.type_shopping.string, generateIconRes(R.string.type_icon_name_shopping)))
        local.insertTypes(
            // 数码
            TypeEntity.newSecond(parentId = shoppingId, name = R.string.type_digital_products.string, generateIconRes(R.string.type_icon_name_digital_products)),
            // 日用
            TypeEntity.newSecond(parentId = shoppingId, name = R.string.type_daily_necessities.string, generateIconRes(R.string.type_icon_name_daily_necessities)),
            // 玩具
            TypeEntity.newSecond(parentId = shoppingId, name = R.string.type_toy.string, generateIconRes(R.string.type_icon_name_toy)),
            // 电器
            TypeEntity.newSecond(parentId = shoppingId, name = R.string.type_electrical_appliances.string, generateIconRes(R.string.type_icon_name_electrical_appliances)),
            // 美妆
            TypeEntity.newSecond(parentId = shoppingId, name = R.string.type_beauty.string, generateIconRes(R.string.type_icon_name_beauty)),
            // 鞋服
            TypeEntity.newSecond(parentId = shoppingId, name = R.string.type_clothes.string, generateIconRes(R.string.type_icon_name_clothes)),
            // 饰品
            TypeEntity.newSecond(parentId = shoppingId, name = R.string.type_accessories.string, generateIconRes(R.string.type_icon_name_accessories))
        )
        // 住房
        local.insertType(TypeEntity.newFirst(R.string.type_housing.string, generateIconRes(R.string.type_icon_name_housing)))
        // TODO
    }
}