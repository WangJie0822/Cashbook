package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.core.math.MathUtils
import androidx.databinding.ObservableFloat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_EDIT_RECORD
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MY_ASSET
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MY_BOOKS
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.observable.ObservableMoney
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 主界面 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
class MainViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 账本名称 */
    val booksName: LiveData<String> = CurrentBooksLiveData.map { it.name }

    /** 标记 - 是否允许标题 */
    val titleEnable: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 顶部背景图片 */
    val topBgImage: LiveData<String> = CurrentBooksLiveData.map { it.imageUrl }

    /** 本月支出 */
    val spending: ObservableMoney = ObservableMoney()

    /** 本月支出透明度 */
    val spendingAlpha = ObservableFloat(1f)

    /** 本月收入 */
    val income: ObservableMoney = ObservableMoney()

    /** 本月结余 */
    val balance: ObservableMoney = object : ObservableMoney(spending, income) {
        override fun get(): String {
            return (income.bigDecimalVal - spending.bigDecimalVal).toPlainString()
        }
    }

    /** 本月收入、结余透明度 */
    val incomeAndBalanceAlpha = ObservableFloat(1f)

    /** 状态栏折叠进度监听 */
    val onCollapsingChanged: (Float) -> Unit = { percent ->
        // 完全折叠时才显示标题文本
        titleEnable.value = percent <= 0.13f
        // 本月支出显示逻辑
        spendingAlpha.set(MathUtils.clamp((1 - (0.9f - percent) / 0.3f), 0f, 1f))
        // 本月收入、结余显示逻辑
        incomeAndBalanceAlpha.set(MathUtils.clamp((1 - (0.4f - percent) / 0.3f), 0f, 1f))
    }

    /** 标题栏菜单点击 */
    val onToolbarMenuClick: (Int) -> Unit = { menuId ->
        when (menuId) {
            R.id.search -> {
                // TODO 跳转搜索
            }
            R.id.my_asset -> {
                // 跳转我的资产
                uiNavigationData.value = UiNavigationModel.builder {
                    jump(ROUTE_PATH_MY_ASSET)
                }
            }
        }
    }

    /** 我的账本点击 */
    val onMyBooksClick: () -> Unit = {
        // 跳转我的账本界面
        uiNavigationData.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_MY_BOOKS)
        }
    }

    /** 添加点击 */
    val onAddClick: () -> Unit = {
        // 跳转编辑记录界面
        uiNavigationData.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_EDIT_RECORD)
        }
    }
}