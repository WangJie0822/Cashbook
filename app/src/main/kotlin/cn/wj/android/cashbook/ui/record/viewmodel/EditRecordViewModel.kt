package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.color
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.getSharedParcelable
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.SHARED_KEY_LAST_ASSET
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.CurrencyEnum
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel

/**
 * 编辑记录 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
class EditRecordViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 显示选择账号弹窗 */
    val showSelectAssetData: MutableLiveData<Int> = MutableLiveData()

    /** 选择日期弹窗 */
    val showSelectDateData: MutableLiveData<Int> = MutableLiveData()

    /** 账户信息 */
    val accountData: MutableLiveData<AssetEntity> = MutableLiveData(getSharedParcelable(SHARED_KEY_LAST_ASSET))

    /** TODO 标签数据 */
    val tagsData: MutableLiveData<String> = MutableLiveData("")

    /** 选中时间 */
    val dateData: MutableLiveData<String> = MutableLiveData(System.currentTimeMillis().dateFormat(DATE_FORMAT_NO_SECONDS))

    /** 当前界面下标 */
    val currentItem: MutableLiveData<Int> = MutableLiveData(RecordTypeEnum.INCOME.position)

    /** 账户文本 */
    val accountStr: LiveData<String> = accountData.map {
        if (null == it || it.classification == AssetClassificationEnum.NOT_SELECT) {
            R.string.account.string
        } else {
            it.showStr
        }
    }

    /** 账户选中状态 */
    val accountChecked: LiveData<Boolean> = accountData.map {
        null != it && it.classification !== AssetClassificationEnum.NOT_SELECT
    }

    /** 标签文本 */
    val tagsStr: LiveData<String> = MutableLiveData(R.string.tags.string)

    /** 标签选中状态 */
    val tagsChecked: LiveData<Boolean> = tagsData.map {
        !it.isNullOrBlank()
    }

    /** 手续费文本 */
    val chargeStr: LiveData<String> = MutableLiveData(R.string.charge.string)

    /** 手续费选中状态 */
    val chargeChecked: LiveData<Boolean> = MutableLiveData(false)

    /** 是否显示手续费 */
    val showCharge: LiveData<Boolean> = currentItem.map {
        it == RecordTypeEnum.TRANSFER.position
    }

    /** 可报销选中状态 */
    val reimbursableChecked: MutableLiveData<Boolean> = MutableLiveData()

    /** 是否显示可报销 */
    val showReimbursable: LiveData<Boolean> = currentItem.map {
        it == RecordTypeEnum.INCOME.position
    }

    /** 货币符号 */
    val currencySymbol: LiveData<String> = CurrentBooksLiveData.map {
        (it.currency ?: CurrencyEnum.CNY).symbol
    }

    /** 计算结果显示 */
    val calculatorStr: ObservableField<String> = ObservableField()

    /** 界面主色调 */
    val primaryTint: LiveData<Int> = currentItem.map {
        when (it) {
            RecordTypeEnum.INCOME.position -> {
                // 支出
                R.color.color_spending
            }
            RecordTypeEnum.EXPENDITURE.position -> {
                // 收入
                R.color.color_income
            }
            else -> {
                // 转账
                R.color.color_secondary
            }
        }.color
    }

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 账户点击 */
    val onAccountClick: () -> Unit = {
        showSelectAssetData.value = 0
    }

    /** 标签点击 */
    val onTagsClick: () -> Unit = {
        snackbarData.value = "标签点击".toSnackbarModel()
    }

    /** 日期点击 */
    val onDateClick: () -> Unit = {
        // 以当前选中时间显示弹窗
        showSelectDateData.value = 0
    }

    /** 时间点击 */
    val onTimeClick: () -> Unit = {
        snackbarData.value = "时间点击".toSnackbarModel()
    }

    /** 手续费点击 */
    val onChargeClick: () -> Unit = {
        snackbarData.value = "手续费点击".toSnackbarModel()
    }

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        snackbarData.value = "确认保存".toSnackbarModel()
    }
}