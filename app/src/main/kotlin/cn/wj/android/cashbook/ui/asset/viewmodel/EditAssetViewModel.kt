package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.decimalFormat
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.asset.AssetRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.text.orEmpty

/**
 * 编辑资产 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
class EditAssetViewModel(private val repository: AssetRepository) : BaseViewModel() {

    /** 资产 id，新建为 -1L */
    var id = -1L

    /** 编辑资产创建时间 */
    var createTime = ""

    /** 排序数据 */
    var sort = -1

    /** 标记资产余额 */
    var oldBalance = ""
        set(value) {
            field = value
            balance.value = value
        }

    /** 显示选择日期弹窗事件 */
    val showSelectDayEvent: LifecycleEvent<Boolean> = LifecycleEvent()

    /** 显示选择资产类型弹窗事件 */
    val showSelectAssetClassificationEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 资产分类大类 */
    val classificationType: MutableLiveData<ClassificationTypeEnum> =
        MutableLiveData(ClassificationTypeEnum.CAPITAL_ACCOUNT)

    /** 资产分类 */
    val assetClassification: MutableLiveData<AssetClassificationEnum> =
        MutableLiveData(AssetClassificationEnum.CASH)

    /** 标记 - 是否是信用卡 */
    val creditCardType: LiveData<Boolean> = classificationType.map {
        it == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT
    }

    /** 标记 - 是否是债务类型 */
    val debtType: LiveData<Boolean> = classificationType.map {
        it == ClassificationTypeEnum.DEBT_ACCOUNT
    }

    /** 余额提示文本 */
    val balanceHint: LiveData<String> = creditCardType.map {
        if (it) {
            // 信用卡类型，提示欠款
            R.string.current_arrears
        } else {
            // 其他类型，提示余额
            R.string.asset_balance
        }.string
    }

    /** 资产名称 */
    val assetName: MutableLiveData<String> = MutableLiveData()

    /** 名称错误提示 */
    val nameError: ObservableField<String> = ObservableField()

    /** 信用卡总额度 */
    val totalAmount: MutableLiveData<String> = MutableLiveData()

    /** 总额度错误提示 */
    val totalAmountError: ObservableField<String> = ObservableField()

    /** 余额、信用卡欠款 */
    val balance: MutableLiveData<String> = MutableLiveData()

    /** 开户行 */
    val openBank: MutableLiveData<String> = MutableLiveData()

    /** 卡号 */
    val cardNo: MutableLiveData<String> = MutableLiveData()

    /** 备注 */
    val remark: MutableLiveData<String> = MutableLiveData()

    /** 账单日 */
    val billingDate: MutableLiveData<String> = MutableLiveData()

    /** 账单日显示文本 */
    val billingDateStr: LiveData<String> = billingDate.map {
        if (it.isNullOrBlank()) {
            ""
        } else {
            "$it${R.string.day.string}"
        }
    }

    /** 还款日 */
    val repaymentDate: MutableLiveData<String> = MutableLiveData()

    /** 还款日显示文本 */
    val repaymentDateStr: LiveData<String> = repaymentDate.map {
        if (it.isNullOrBlank()) {
            ""
        } else {
            "$it${R.string.day.string}"
        }
    }

    /** 标记 - 是否隐藏资产 */
    val invisibleAsset: MutableLiveData<Boolean> = MutableLiveData()

    /** 标题文本 */
    val titleStr: ObservableField<String> = ObservableField(R.string.new_asset.string)

    /** 保持按钮是否允许点击 */
    val saveEnable: ObservableBoolean = ObservableBoolean(true)

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 资产类型点击 */
    val onAssetClassificationClick: () -> Unit = {
        showSelectAssetClassificationEvent.value = 0
    }

    /** 账单日点击 */
    val onBillingDateClick: () -> Unit = {
        // 显示选择日期弹窗
        showSelectDayEvent.value = true
    }

    /** 还款日点击 */
    val onRepaymentDateClick: () -> Unit = {
        // 显示选择日期弹窗
        showSelectDayEvent.value = false
    }

    /** 保存点击 */
    val onSaveClick: () -> Unit = {
        saveAsset()
    }

    /** 保存资产 */
    private fun saveAsset() {
        val type = classificationType.value ?: return
        val classification = assetClassification.value ?: return
        val name = assetName.value
        if (name.isNullOrBlank()) {
            // 资产名称不能为空
            nameError.set(R.string.asset_name_cannot_be_empty.string)
            return
        }
        val totalAmount = totalAmount.value.decimalFormat()
        if (type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
            // 信用卡，判断总额度
            if (totalAmount.toFloatOrNull().orElse(0f) <= 0f) {
                // 总额度必填且大于0
                totalAmountError.set(R.string.total_amount_must_be_greater_than_zero.string)
                return
            }
        }
        val balance = balance.value.decimalFormat()
        val openBank = openBank.value.orEmpty()
        val cardNo = cardNo.value.orEmpty()
        val remark = remark.value.orEmpty()
        val billingDate = billingDate.value.orEmpty()
        val repaymentDate = repaymentDate.value.orEmpty()
        val invisible = invisibleAsset.value.condition
        val currentTime = System.currentTimeMillis().dateFormat()
        viewModelScope.launch {
            try {
                saveEnable.set(false)
                if (id >= 0) {
                    // 编辑
                    repository.updateAsset(
                        AssetEntity(
                            id = id,
                            booksId = CurrentBooksLiveData.booksId,
                            name = name,
                            totalAmount = totalAmount,
                            billingDate = billingDate,
                            repaymentDate = repaymentDate,
                            type = type,
                            classification = classification,
                            invisible = invisible,
                            openBank = openBank,
                            cardNo = cardNo,
                            remark = remark,
                            sort = sort,
                            createTime = createTime,
                            modifyTime = currentTime,
                            balance = balance
                        ), balance != oldBalance
                    )
                } else {
                    // 新建
                    repository.insertAsset(
                        AssetEntity(
                            id = -1,
                            booksId = CurrentBooksLiveData.booksId,
                            name = name,
                            totalAmount = totalAmount,
                            billingDate = billingDate,
                            repaymentDate = repaymentDate,
                            type = type,
                            classification = classification,
                            invisible = invisible,
                            openBank = openBank,
                            cardNo = cardNo,
                            remark = remark,
                            sort = repository.queryMaxSort().orElse(-1) + 1,
                            createTime = currentTime,
                            modifyTime = currentTime,
                            balance = balance
                        )
                    )
                }
                callOnSaveSuccess()
            } catch (throwable: Throwable) {
                saveEnable.set(true)
                logger().e(throwable, "saveAsset")
            }
        }
    }

    /** 保存成功 */
    private fun callOnSaveSuccess() {
        // 保存成功，提示并关闭界面
        snackbarEvent.value =
            R.string.save_success.string.toSnackbarModel(onCallback = object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    uiNavigationEvent.value = UiNavigationModel.builder {
                        close()
                    }
                }
            })
    }
}