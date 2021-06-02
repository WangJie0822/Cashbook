package cn.wj.android.cashbook.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.BOTTOM_SHEET_STATE_HALF_EXPANDED
import cn.wj.android.cashbook.data.constants.BOTTOM_SHEET_STATE_HIDDEN
import cn.wj.android.cashbook.data.entity.AssetClassificationListEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.interfaces.AssetClassificationListClickListener
import kotlinx.coroutines.launch

/**
 * 选择资产账户分类 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class SelectAssetClassificationViewModel(private val local: LocalDataStore) : BaseViewModel(), AssetClassificationListClickListener {

    /** 标记 - 是否选择资产分类 */
    private val selectClassification: MutableLiveData<Boolean> = MutableLiveData(true)

    /** 资产分类状态 */
    val assetClassificationState: LiveData<Int> = selectClassification.map {
        if (it) {
            BOTTOM_SHEET_STATE_HALF_EXPANDED
        } else {
            BOTTOM_SHEET_STATE_HIDDEN
        }
    }

    /** 银行状态 */
    val bankState: LiveData<Int> = selectClassification.map {
        if (it) {
            BOTTOM_SHEET_STATE_HIDDEN
        } else {
            BOTTOM_SHEET_STATE_HALF_EXPANDED
        }
    }

    /** 资产分类列表 */
    val assetClassificationListData: LiveData<ArrayList<AssetClassificationListEntity>> = getClassificationData()

    /** 银行列表 */
    val bankListData: LiveData<ArrayList<AssetClassificationEnum>> = getBankData()

    /** 背景点击 */
    val onBackgroundClick: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 分类列表隐藏回调 */
    val onAssetClassificationHidden: () -> Unit = {
        if (selectClassification.value.condition) {
            // 当前在选择分类
            uiNavigationData.value = UiNavigationModel.builder {
                close()
            }
        }
    }

    /** 银行列表隐藏回调 */
    val onBankHidden: () -> Unit = {
        if (!selectClassification.value.condition) {
            // 当前在选择银行
            uiNavigationData.value = UiNavigationModel.builder {
                close()
            }
        }
    }

    /** 资产列表 item 点击 */
    override val onItemClick: (AssetClassificationEnum) -> Unit = { item ->
        if (item.needSelectBank) {
            // 需要选择银行
            selectClassification.value = false
        }

    }

    /** 获取分类列表数据 */
    private fun getClassificationData(): LiveData<ArrayList<AssetClassificationListEntity>> {
        val result = MutableLiveData<ArrayList<AssetClassificationListEntity>>()
        viewModelScope.launch {
            result.value = local.getAssetClassificationList()
        }
        return result
    }

    /** 获取银行列表数据 */
    private fun getBankData(): LiveData<ArrayList<AssetClassificationEnum>> {
        val result = MutableLiveData<ArrayList<AssetClassificationEnum>>()
        viewModelScope.launch {
            result.value = local.getBankList()
        }
        return result
    }
}