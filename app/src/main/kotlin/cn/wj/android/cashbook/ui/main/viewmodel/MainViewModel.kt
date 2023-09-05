package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.core.math.MathUtils
import androidx.databinding.ObservableFloat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.decimalFormat
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.moneyFormat
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.toBigDecimalOrZero
import cn.wj.android.cashbook.base.ext.base.toFloatOrZero
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_MONTH_DAY
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.maps
import cn.wj.android.cashbook.base.tools.mutableLiveDataOf
import cn.wj.android.cashbook.base.tools.toLongTime
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
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_TAG_MANAGER
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
import java.util.Calendar
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/**
 * 主界面 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
class MainViewModel(private val repository: MainRepository) : BaseViewModel(),
    RecordListClickListener {

    /** 显示升级提示事件 */
    val showUpdateDialogEvent: LifecycleEvent<UpdateInfoEntity> = LifecycleEvent()

    /** 显示记录详情弹窗事件 */
    val showRecordDetailsDialogEvent: LifecycleEvent<RecordEntity> = LifecycleEvent()

    /** 检查备份路径 */
    val checkBackupPathEvent: LifecycleEvent<String> = LifecycleEvent()

    /** 显示选择年月弹出事件 */
    val showSelectYearMonthDialogEvent: LifecycleEvent<(String) -> Unit> = LifecycleEvent()

    /** 选中日期 */
    private val selectedDate: MutableLiveData<Calendar> = mutableLiveDataOf(onActive = {
        value = Calendar.getInstance()
    })

    /** 标题文本 */
    val titleStr: LiveData<String> = selectedDate.map {
        "${it[Calendar.YEAR]}-${it[Calendar.MONTH] + 1}"
    }

    /** 当前月记录列表 */
    private val currentMonthRecord: MutableLiveData<List<RecordEntity>> = MutableLiveData()

    /** 首页列表数据 */
    val listData = currentMonthRecord.map {
        transTo(it)
    }

    /** 账本名称 */
    val booksName: LiveData<String> = CurrentBooksLiveData.map { it.name }

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

    /** 标题点击 */
    val onTitleClick: () -> Unit = {
        // 显示弹窗
        showSelectYearMonthDialogEvent.value = { date ->
            // 选择回调，更新选中日期
            selectedDate.value = Calendar.getInstance().apply {
                val splits = date.split("-")
                set(Calendar.YEAR, splits.first().toInt())
                set(Calendar.MONTH, splits.last().toInt() - 1)
            }
            loadMonthRecord()
        }
    }

    /** 状态栏折叠进度监听 */
    val onCollapsingChanged: (Float) -> Unit = { percent ->
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

    /** 我的标签点击 */
    val onMyTagClick: () -> Unit = {
        // 跳转我的标签
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_RECORD_TAG_MANAGER)
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

    /** 获取当前月所有记录 */
    fun loadMonthRecord() {
        viewModelScope.launch {
            try {
                val date = selectedDate.value ?: Calendar.getInstance()
                currentMonthRecord.value =
                    repository.getRecordByYearMonth(date[Calendar.YEAR], date[Calendar.MONTH] + 1)
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadMonthRecord")
            }
        }
    }

    private fun transTo(list1: List<RecordEntity>): List<DateRecordEntity> {
        val result = arrayListOf<DateRecordEntity>()
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val list = list1.filter {
            !it.system
        }
        val map = hashMapOf<String, MutableList<RecordEntity>>()
        for (item in list) {
            val dateKey = item.recordTime.dateFormat(DATE_FORMAT_MONTH_DAY)
            val dayInt = dateKey.split(".").lastOrNull()?.toIntOrNull().orElse(-1)
            val key = dateKey + when (dayInt) {
                today -> {
                    // 今天
                    " ${R.string.today.string}"
                }

                today - 1 -> {
                    // 昨天
                    " ${R.string.yesterday.string}"
                }

                today - 2 -> {
                    // 前天
                    " ${R.string.the_day_before_yesterday.string}"
                }

                else -> {
                    ""
                }
            }
            if (key.isNotBlank()) {
                if (map.containsKey(key)) {
                    map[key]!!.add(item)
                } else {
                    map[key] = arrayListOf(item)
                }
            }
        }
        map.keys.forEach { key ->
            result.add(
                DateRecordEntity(
                    date = key,
                    list = map[key].orEmpty().sortedBy { it.recordTime }.reversed()
                )
            )
        }
        return result.sortedBy { it.date.toLongTime(DATE_FORMAT_MONTH_DAY) }.reversed()
    }
}