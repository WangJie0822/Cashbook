package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.core.math.MathUtils
import androidx.databinding.ObservableFloat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.tools.maps
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ABOUT_US
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_EDIT_RECORD
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MY_ASSET
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MY_BOOKS
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.interfaces.RecordListClickListener
import kotlinx.coroutines.launch

/**
 * 主界面 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
class MainViewModel(private val local: LocalDataStore) : BaseViewModel(), RecordListClickListener {

    /** 显示记录详情弹窗数据 */
    val showRecordDetailsDialogData: MutableLiveData<RecordEntity> = MutableLiveData()

    /** 当前月记录列表 */
    private val currentMonthRecord: MutableLiveData<List<RecordEntity>> = MutableLiveData()

    /** 首页列表数据 */
    val listData: MutableLiveData<List<DateRecordEntity>> = MutableLiveData()

    /** 账本名称 */
    val booksName: LiveData<String> = CurrentBooksLiveData.map { it.name }

    /** 标记 - 是否允许标题 */
    val titleEnable: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 顶部背景图片 */
    val topBgImage: LiveData<String> = CurrentBooksLiveData.map { it.imageUrl }

    /** 本月支出 */
    val expenditure: LiveData<String> = currentMonthRecord.map {
        var totalExpenditure = 0f
        it.forEach { record ->
            if (record.type == RecordTypeEnum.EXPENDITURE) {
                // 支出
                totalExpenditure += record.amount.toFloatOrNull().orElse(0f)
            }
            if (record.type == RecordTypeEnum.TRANSFER) {
                // 转账
                val chargeF = record.charge.toFloatOrNull().orElse(0f)
                if (chargeF > 0f) {
                    totalExpenditure += chargeF
                }
            }
        }
        CurrentBooksLiveData.currency.symbol + totalExpenditure.toString()
    }

    /** 本月支出透明度 */
    val expenditureAlpha = ObservableFloat(1f)

    /** 本月收入 */
    val income: LiveData<String> = currentMonthRecord.map {
        var totalIncome = 0f
        it.forEach { record ->
            if (record.type == RecordTypeEnum.INCOME) {
                // 支出
                totalIncome += record.amount.toFloatOrNull().orElse(0f)
            }
        }
        CurrentBooksLiveData.currency.symbol + totalIncome.toString()
    }

    /** 本月结余 */
    val balance: LiveData<String> = maps(expenditure, income) {
        val symbol = CurrentBooksLiveData.currency.symbol
        val income = income.value.orEmpty().replace(symbol, "").toFloatOrNull().orElse(0f)
        val expenditure = expenditure.value.orEmpty().replace(symbol, "").toFloatOrNull().orElse(0f)
        val balance = income - expenditure
        if (balance >= 0) {
            symbol + balance.toString()
        } else {
            "-" + symbol + (-balance).toString()
        }
    }

    /** 本月收入、结余透明度 */
    val incomeAndBalanceAlpha = ObservableFloat(1f)


    /** 刷新状态 */
    val refreshing: MutableLiveData<Boolean> = object : MutableLiveData<Boolean>(true) {
        override fun onActive() {
            // 进入自动加载数据
            loadHomepageList()
            // 获取当前月所有记录
            getCurrentMonthRecord()
        }

        override fun setValue(value: Boolean?) {
            super.setValue(value)
            if (value.condition) {
                // 刷新
                loadHomepageList()
                // 获取当前月所有记录
                getCurrentMonthRecord()
            }
        }
    }

    /** 状态栏折叠进度监听 */
    val onCollapsingChanged: (Float) -> Unit = { percent ->
        // 完全折叠时才显示标题文本
        titleEnable.value = percent <= 0.13f
        // 本月支出显示逻辑
        expenditureAlpha.set(MathUtils.clamp((1 - (0.9f - percent) / 0.3f), 0f, 1f))
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

    /** 我的资产点击 */
    val onMyAssetClick: () -> Unit = {
        // 跳转我的资产
        uiNavigationData.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_MY_ASSET)
        }
    }

    /** 设置点击 */
    val onSettingClick: () -> Unit = {
        // TODO 跳转设置
        snackbarData.value = "跳转设置".toSnackbarModel()
    }

    /** 关于我们点击 */
    val onAboutUsClick: () -> Unit = {
        // 跳转关于我们
        uiNavigationData.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_ABOUT_US)
        }
    }

    /** 添加点击 */
    val onAddClick: () -> Unit = {
        // 跳转编辑记录界面
        uiNavigationData.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_EDIT_RECORD)
        }
    }

    /** 记录数据点击 */
    override val onRecordItemClick: (RecordEntity) -> Unit = { item ->
        showRecordDetailsDialogData.value = item
    }

    /** 获取最近一周数据 */
    private fun loadHomepageList() {
        viewModelScope.launch {
            try {
                listData.value = local.getHomepageList()
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadHomepageList")
            } finally {
                refreshing.value = false
            }
        }
    }

    /** 获取当前月所有记录 */
    private fun getCurrentMonthRecord() {
        viewModelScope.launch {
            try {
                currentMonthRecord.value = local.getCurrentMonthRecord()
            } catch (throwable: Throwable) {
                logger().e(throwable, "getCurrentMonthRecord")
            }
        }
    }
}