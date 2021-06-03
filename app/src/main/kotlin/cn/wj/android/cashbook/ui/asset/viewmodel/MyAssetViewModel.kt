package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.core.math.MathUtils
import androidx.databinding.ObservableFloat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 我的资产 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
class MyAssetViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 标记 - 是否允许标题 */
    val titleEnable: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 刷新状态 */
    val refreshing: MutableLiveData<Boolean> = MutableLiveData(true)

    /** 缓存列表 */
    private val cacheList: LiveData<ArrayList<AssetEntity>> = refreshing.map {
        if (it) {
            arrayListOf()
        } else {
            assetListData.value.orElse(arrayListOf())
        }
    }

    /** 资产数据 */
    val assetListData: LiveData<ArrayList<AssetEntity>> = cacheList

    /** 净资产 */
    val netAssets: MutableLiveData<String> = MutableLiveData()

    /** 总资产 */
    val totalAssets: MutableLiveData<String> = MutableLiveData()

    /** 总负债 */
    val totalLiabilities: MutableLiveData<String> = MutableLiveData()

    /** 净资产透明度 */
    val netAssetsAlpha = ObservableFloat(1f)

    /** 总资产、总负债透明度 */
    val totalAlpha = ObservableFloat(1f)

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 状态栏折叠进度监听 */
    val onCollapsingChanged: (Float) -> Unit = { percent ->
        // 完全折叠时才显示标题文本
        titleEnable.value = percent <= 0.13f
        // 净资产透明度
        netAssetsAlpha.set(MathUtils.clamp((1 - (0.9f - percent) / 0.3f), 0f, 1f))
        // 总资产、总负债透明度
        totalAlpha.set(MathUtils.clamp((1 - (0.4f - percent) / 0.3f), 0f, 1f))
    }
}