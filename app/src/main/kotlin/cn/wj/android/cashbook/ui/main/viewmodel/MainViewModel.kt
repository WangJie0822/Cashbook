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
import cn.wj.android.cashbook.base.tools.mutableLiveDataOf
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
 * ????????? ViewModel
 *
 * > [??????](mailto:15555650921@163.com) ????????? 2021/5/11
 */
class MainViewModel(private val repository: MainRepository) : BaseViewModel(), RecordListClickListener {

    /** ???????????????????????? */
    val showUpdateDialogEvent: LifecycleEvent<UpdateInfoEntity> = LifecycleEvent()

    /** ?????????????????????????????? */
    val showRecordDetailsDialogEvent: LifecycleEvent<RecordEntity> = LifecycleEvent()

    /** ?????????????????? */
    val checkBackupPathEvent: LifecycleEvent<String> = LifecycleEvent()

    /** ????????????????????? */
    private val currentMonthRecord: MutableLiveData<List<RecordEntity>> = MutableLiveData()

    /** ?????????????????? */
    val listData: MutableLiveData<List<DateRecordEntity>> = MutableLiveData()

    /** ???????????? */
    val booksName: LiveData<String> = CurrentBooksLiveData.map { it.name }

    /** ?????? - ?????????????????? */
    val titleEnable: MutableLiveData<Boolean> = MutableLiveData(false)

    /** ?????????????????? */
    val topBgImage: LiveData<String> = CurrentBooksLiveData.map { it.imageUrl }

    /** ???????????? */
    val expenditure: LiveData<String> = currentMonthRecord.map {
        var totalExpenditure = "0".toBigDecimal()
        it.forEach { record ->
            if (record.typeEnum == RecordTypeEnum.EXPENDITURE) {
                // ??????
                totalExpenditure += record.amount.toBigDecimalOrZero()
            }
            if (record.typeEnum == RecordTypeEnum.TRANSFER) {
                // ??????
                val chargeF = record.charge.toFloatOrZero()
                if (chargeF > 0f) {
                    totalExpenditure += chargeF.toBigDecimalOrZero()
                }
            }
        }
        totalExpenditure.decimalFormat().moneyFormat()
    }

    /** ????????????????????? */
    val expenditureAlpha = ObservableFloat(1f)

    /** ???????????? */
    val income: LiveData<String> = currentMonthRecord.map {
        var totalIncome = "0".toBigDecimal()
        it.forEach { record ->
            if (record.typeEnum == RecordTypeEnum.INCOME) {
                // ??????
                totalIncome += record.amount.toBigDecimalOrZero()
            }
            if (record.typeEnum == RecordTypeEnum.TRANSFER) {
                // ??????
                val chargeF = record.charge.toFloatOrZero()
                if (chargeF < 0f) {
                    totalIncome -= chargeF.toBigDecimalOrZero()
                }
            }
        }
        totalIncome.decimalFormat().moneyFormat()
    }

    /** ???????????? */
    val balance: LiveData<String> = maps(expenditure, income) {
        val symbol = CurrentBooksLiveData.currency.symbol
        val income = income.value.orEmpty().replace(symbol, "").toBigDecimalOrZero()
        val expenditure = expenditure.value.orEmpty().replace(symbol, "").toBigDecimalOrZero()
        (income - expenditure).decimalFormat().moneyFormat()
    }

    /** ?????????????????????????????? */
    val incomeAndBalanceAlpha = ObservableFloat(1f)

    /** ???????????? */
    val refreshing: MutableLiveData<Boolean> = mutableLiveDataOf(
        default = true,
        onActive = {
            // ????????????????????????
            loadHomepageList()
            // ???????????????????????????
            getCurrentMonthRecord()
        },
        onSet = {
            if (value.condition) {
                // ??????
                loadHomepageList()
                // ???????????????????????????
                getCurrentMonthRecord()
            }
        }
    )

    /** ??????????????????????????? */
    val onCollapsingChanged: (Float) -> Unit = { percent ->
        // ????????????????????????????????????
        titleEnable.value = percent <= 0.13f
        // ????????????????????????
        expenditureAlpha.set(MathUtils.clamp((1 - (0.9f - percent) / 0.3f), 0f, 1f))
        // ?????????????????????????????????
        incomeAndBalanceAlpha.set(MathUtils.clamp((1 - (0.4f - percent) / 0.3f), 0f, 1f))
    }

    /** ????????????????????? */
    val onToolbarMenuClick: (Int) -> Unit = { menuId ->
        when (menuId) {
            R.id.search -> {
                // ????????????
                uiNavigationEvent.value = UiNavigationModel.builder {
                    jump(ROUTE_PATH_RECORD_SEARCH)
                }
            }
            R.id.calendar -> {
                // ????????????
                uiNavigationEvent.value = UiNavigationModel.builder {
                    jump(ROUTE_PATH_RECORD_CALENDAR)
                }
            }
            R.id.my_asset -> {
                // ??????????????????
                uiNavigationEvent.value = UiNavigationModel.builder {
                    jump(ROUTE_PATH_ASSET_MY)
                }
            }
        }
    }

    /** ?????????????????? */
    val onMyBooksClick: () -> Unit = {
        // ????????????????????????
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_BOOKS_MY)
        }
    }

    /** ?????????????????? */
    val onMyAssetClick: () -> Unit = {
        // ??????????????????
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_ASSET_MY)
        }
    }

    /** ?????????????????? */
    val onMyTypeClick: () -> Unit = {
        // ??????????????????
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_TYPE_LIST_EDIT)
        }
    }

    /** ???????????? */
    val onSettingClick: () -> Unit = {
        // ????????????
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_SETTING)
        }
    }

    /** ?????????????????? */
    val onAboutUsClick: () -> Unit = {
        // ??????????????????
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_ABOUT_US)
        }
    }

    /** ???????????? */
    val onAddClick: () -> Unit = {
        // ????????????????????????
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_RECORD_EDIT)
        }
    }

    /** ?????????????????? */
    override val onRecordItemClick: (RecordEntity) -> Unit = { item ->
        showRecordDetailsDialogEvent.value = item
    }

    override val onRecordItemLongClick: (RecordEntity) -> Unit = { }

    /** ???????????? */
    fun checkUpdate() {
        if (!AppConfigs.autoUpdate) {
            // ??????????????????
            return
        }
        if (UpdateManager.downloading) {
            // ?????????
            return
        }
        viewModelScope.launch {
            try {
                // ?????? Release ??????
                val info = repository.queryLatestRelease(AppConfigs.useGitee)
                if (AppConfigs.ignoreVersion == info.versionName) {
                    // ???????????????
                    return@launch
                }
                UpdateManager.checkFromInfo(info, {
                    // ???????????????
                    snackbarEvent.value = R.string.new_versions_found.string.toSnackbarModel(
                        duration = SnackbarModel.LENGTH_INDEFINITE,
                        actionText = R.string.view.string,
                        onAction = {
                            // ????????????????????????
                            showUpdateDialogEvent.value = info
                        })
                }, {
                    // ???????????????
                })
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkUpdate")
            }
        }
    }

    /** ???????????? */
    fun autoBackup() {
        // ??????????????????
        val autoBackup = AutoBackupEnum.fromValue(AppConfigs.autoBackup)
        logger().i(autoBackup.toString())
        if (autoBackup == AutoBackupEnum.CLOSED) {
            // ??????????????????
            return
        }
        // ??????????????????????????????
        val duration = (System.currentTimeMillis() - AppConfigs.lastBackupMs).absoluteValue
        if ((autoBackup == AutoBackupEnum.WHEN_OPEN) || (autoBackup == AutoBackupEnum.EVERY_DAY && duration >= MS_DAY) || (autoBackup == AutoBackupEnum.EVERY_WEEK && duration >= MS_WEEK)) {
            // ???????????????????????????????????????
            checkBackupPathEvent.value = AppConfigs.backupPath
        }
    }

    /** ?????? */
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

    /** ???????????????????????? */
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

    /** ??????????????????????????? */
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