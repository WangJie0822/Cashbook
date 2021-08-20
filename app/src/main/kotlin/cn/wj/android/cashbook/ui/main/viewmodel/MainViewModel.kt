package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.core.math.MathUtils
import androidx.databinding.ObservableFloat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.decimalFormat
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.moneyFormat
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.toBigDecimalOrZero
import cn.wj.android.cashbook.base.ext.base.toFloatOrZero
import cn.wj.android.cashbook.base.tools.maps
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.MS_DAY
import cn.wj.android.cashbook.data.constants.MS_WEEK
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ABOUT_US
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ASSET_MY
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_BOOKS_MY
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_CALENDAR
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_EDIT
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_SEARCH
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_SETTING
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_TYPE_LIST_EDIT
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.UpdateInfoEntity
import cn.wj.android.cashbook.data.enums.AutoBackupEnum
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.SnackbarModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.main.MainRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.interfaces.RecordListClickListener
import cn.wj.android.cashbook.manager.UpdateManager
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * 主界面 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
class MainViewModel(private val repository: MainRepository) : BaseViewModel(), RecordListClickListener {

    /** 显示升级提示事件 */
    val showUpdateDialogEvent: LifecycleEvent<UpdateInfoEntity> = LifecycleEvent()

    /** 显示记录详情弹窗事件 */
    val showRecordDetailsDialogEvent: LifecycleEvent<RecordEntity> = LifecycleEvent()

    /** 检查备份路径 */
    val checkBackupPathEvent: LifecycleEvent<String> = LifecycleEvent()

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
        var totalExpenditure = "0".toBigDecimal()
        it.forEach { record ->
            if (record.typeEnum == RecordTypeEnum.EXPENDITURE) {
                // 支出
                totalExpenditure += record.amount.toBigDecimalOrZero()
            }
            if (record.typeEnum == RecordTypeEnum.TRANSFER) {
                // 转账
                val chargeF = record.charge.toFloatOrZero()
                if (chargeF > 0f) {
                    totalExpenditure += chargeF.toBigDecimalOrZero()
                }
            }
        }
        totalExpenditure.decimalFormat().moneyFormat()
    }

    /** 本月支出透明度 */
    val expenditureAlpha = ObservableFloat(1f)

    /** 本月收入 */
    val income: LiveData<String> = currentMonthRecord.map {
        var totalIncome = "0".toBigDecimal()
        it.forEach { record ->
            if (record.typeEnum == RecordTypeEnum.INCOME) {
                // 收入
                totalIncome += record.amount.toBigDecimalOrZero()
            }
            if (record.typeEnum == RecordTypeEnum.TRANSFER) {
                // 转账
                val chargeF = record.charge.toFloatOrZero()
                if (chargeF < 0f) {
                    totalIncome -= chargeF.toBigDecimalOrZero()
                }
            }
        }
        totalIncome.decimalFormat().moneyFormat()
    }

    /** 本月结余 */
    val balance: LiveData<String> = maps(expenditure, income) {
        val symbol = CurrentBooksLiveData.currency.symbol
        val income = income.value.orEmpty().replace(symbol, "").toBigDecimalOrZero()
        val expenditure = expenditure.value.orEmpty().replace(symbol, "").toBigDecimalOrZero()
        (income - expenditure).decimalFormat().moneyFormat()
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
                // 跳转搜索
                uiNavigationEvent.value = UiNavigationModel.builder {
                    jump(ROUTE_PATH_RECORD_SEARCH)
                }
            }
            R.id.calendar -> {
                // 跳转日历
                uiNavigationEvent.value = UiNavigationModel.builder {
                    jump(ROUTE_PATH_RECORD_CALENDAR)
                }
            }
            R.id.my_asset -> {
                // 跳转我的资产
                uiNavigationEvent.value = UiNavigationModel.builder {
                    jump(ROUTE_PATH_ASSET_MY)
                }
            }
        }
    }

    /** 我的账本点击 */
    val onMyBooksClick: () -> Unit = {
        // 跳转我的账本界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_BOOKS_MY)
        }
    }

    /** 我的资产点击 */
    val onMyAssetClick: () -> Unit = {
        // 跳转我的资产
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_ASSET_MY)
        }
    }

    /** 我的分类点击 */
    val onMyTypeClick: () -> Unit = {
        // 跳转我的分类
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_TYPE_LIST_EDIT)
        }
    }

    /** 设置点击 */
    val onSettingClick: () -> Unit = {
        // 跳转设置
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_SETTING)
        }
    }

    /** 关于我们点击 */
    val onAboutUsClick: () -> Unit = {
        // 跳转关于我们
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_ABOUT_US)
        }
    }

    /** 添加点击 */
    val onAddClick: () -> Unit = {
        // 跳转编辑记录界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_RECORD_EDIT)
        }
    }

    /** 记录数据点击 */
    override val onRecordItemClick: (RecordEntity) -> Unit = { item ->
        showRecordDetailsDialogEvent.value = item
    }

    override val onRecordItemLongClick: (RecordEntity) -> Unit = { }

    /** 检查更新 */
    fun checkUpdate() {
        if (!AppConfigs.autoUpdate) {
            // 关闭自动更新
            return
        }
        if (UpdateManager.downloading) {
            // 下载中
            return
        }
        viewModelScope.launch {
            try {
                // 获取 Release 信息
                val info = repository.queryLatestRelease(AppConfigs.useGitee)
                if (AppConfigs.ignoreVersion == info.versionName) {
                    // 已忽略版本
                    return@launch
                }
                UpdateManager.checkFromInfo(info, {
                    // 有可用版本
                    snackbarEvent.value = R.string.new_versions_found.string.toSnackbarModel(
                        duration = SnackbarModel.LENGTH_INDEFINITE,
                        actionText = R.string.view.string,
                        onAction = {
                            // 显示升级提示弹窗
                            showUpdateDialogEvent.value = info
                        })
                }, {
                    // 不需要升级
                })
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkUpdate")
            }
        }
    }

    /** 自动备份 */
    fun autoBackup() {
        // 获取备份策略
        val autoBackup = AutoBackupEnum.fromValue(AppConfigs.autoBackup)
        logger().i(autoBackup.toString())
        if (autoBackup == AutoBackupEnum.CLOSED) {
            // 关闭，不备份
            return
        }
        // 获取距离上次备份时间
        val duration = (System.currentTimeMillis() - AppConfigs.lastBackupMs).absoluteValue
        if ((autoBackup == AutoBackupEnum.WHEN_OPEN) || (autoBackup == AutoBackupEnum.EVERY_DAY && duration >= MS_DAY) || (autoBackup == AutoBackupEnum.EVERY_WEEK && duration >= MS_WEEK)) {
            // 满足备份条件，检查备份路径
            checkBackupPathEvent.value = AppConfigs.backupPath
        }
    }

    /** 备份 */
    fun tryBackup() {
        viewModelScope.launch {
            try {
                val result = repository.backup()
                logger().i(result.toString())
            } catch (throwable: Throwable) {
                logger().e(throwable, "tryBackup")
            }
        }
    }

    /** 获取最近一周数据 */
    private fun loadHomepageList() {
        viewModelScope.launch {
            try {
                listData.value = repository.getHomepageList()
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
                currentMonthRecord.value = repository.getCurrentMonthRecord()
            } catch (throwable: Throwable) {
                logger().e(throwable, "getCurrentMonthRecord")
            }
        }
    }
}