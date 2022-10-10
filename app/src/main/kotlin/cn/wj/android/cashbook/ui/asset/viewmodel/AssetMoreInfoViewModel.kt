package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.copyToClipboard
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.model.SnackbarModel

/**
 * 资产更多信息 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/10/9
 */
class AssetMoreInfoViewModel : BaseViewModel() {

    /** 资产信息 */
    val assetData: MutableLiveData<AssetEntity> = MutableLiveData()

    /** 开户行信息 */
    val openBankStr: LiveData<String> = assetData.map {
        it.openBank
    }

    /** 标记 - 是否有开户行信息 */
    val hasOpenBank: LiveData<Boolean> = openBankStr.map {
        it.isNotBlank()
    }

    /** 卡号 */
    val cardNoStr: LiveData<String> = assetData.map {
        it.cardNo
    }

    /** 标记 - 是否有卡号信息 */
    val hasCardNo: LiveData<Boolean> = cardNoStr.map {
        it.isNotBlank()
    }

    /** 开户行复制点击 */
    val onOpenBankClick: () -> Unit = {
        openBankStr.value?.copyToClipboard()
        snackbarEvent.value = SnackbarModel(R.string.copy_to_clipboard_success)
    }

    /** 卡号复制点击 */
    val onCardNoClick: () -> Unit = {
        cardNoStr.value?.copyToClipboard()
        snackbarEvent.value = SnackbarModel(R.string.copy_to_clipboard_success)
    }

    /** 全部复制点击 */
    val onCopyAllClick: () -> Unit = {
        val hasOpenBank = hasOpenBank.value.condition
        val hasCardNo = hasCardNo.value.condition
        if (hasOpenBank && hasCardNo) {
            R.string.open_bank_copy_to_clipboard_format.string.format(openBankStr.value) +
                    "\n" +
                    R.string.card_no_copy_to_clipboard_format.string.format(cardNoStr.value)
        } else if (hasOpenBank) {
            R.string.open_bank_copy_to_clipboard_format.string.format(openBankStr.value)
        } else {
            R.string.card_no_copy_to_clipboard_format.string.format(cardNoStr.value)
        }.copyToClipboard()
        snackbarEvent.value = SnackbarModel(R.string.copy_to_clipboard_success)
    }
}