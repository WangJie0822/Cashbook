package cn.wj.android.cashbook.ui.asset.activity

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ASSET_MY
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.model.NoDataModel
import cn.wj.android.cashbook.databinding.ActivityMyAssetBinding
import cn.wj.android.cashbook.ui.asset.dialog.AssetLongClickMenuDialog
import cn.wj.android.cashbook.ui.asset.dialog.AssetMoreMenuDialog
import cn.wj.android.cashbook.ui.asset.viewmodel.MyAssetViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.callback.DragItemTouchCallback
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.gyf.immersionbar.ImmersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 我的资产界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
@Route(path = ROUTE_PATH_ASSET_MY)
class MyAssetActivity : BaseActivity<MyAssetViewModel, ActivityMyAssetBinding>() {

    override val viewModel: MyAssetViewModel by viewModel()

    /** 资金账户列表适配器对象 */
    private val capitalAdapter: SimpleRvListAdapter<AssetEntity> by lazy {
        createAssetAdapter()
    }

    private val capitalTouchHelper: ItemTouchHelper by lazy {
        ItemTouchHelper(DragItemTouchCallback(capitalAdapter))
    }

    /** 信用卡账户列表适配器对象 */
    private val creditCardAdapter: SimpleRvListAdapter<AssetEntity> by lazy {
        createAssetAdapter()
    }

    private val creditCardTouchHelper: ItemTouchHelper by lazy {
        ItemTouchHelper(DragItemTouchCallback(creditCardAdapter))
    }

    /** 充值账户列表适配器对象 */
    private val topUpAdapter: SimpleRvListAdapter<AssetEntity> by lazy {
        createAssetAdapter()
    }

    private val topUpTouchHelper: ItemTouchHelper by lazy {
        ItemTouchHelper(DragItemTouchCallback(topUpAdapter))
    }

    /** 理财账户列表适配器对象 */
    private val investmentFinancialAdapter: SimpleRvListAdapter<AssetEntity> by lazy {
        createAssetAdapter()
    }

    private val investmentFinancialTouchHelper: ItemTouchHelper by lazy {
        ItemTouchHelper(DragItemTouchCallback(investmentFinancialAdapter))
    }

    /** 债务账户列表适配器对象 */
    private val debtAdapter: SimpleRvListAdapter<AssetEntity> by lazy {
        createAssetAdapter()
    }

    private val debtTouchHelper: ItemTouchHelper by lazy {
        ItemTouchHelper(DragItemTouchCallback(debtAdapter))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_asset)

        // 配置无数据界面
        binding.includeNoData.viewModel = NoDataModel(R.string.no_asset_hint)

        // 配置 RecyclerView
        binding.rvCapital.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = capitalAdapter
        }
        binding.rvCreditCard.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = creditCardAdapter
        }
        binding.rvTopUp.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = topUpAdapter
        }
        binding.rvInvestmentFinancial.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = investmentFinancialAdapter
        }
        binding.rvDebt.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = debtAdapter
        }
    }

    override fun initImmersionbar(immersionBar: ImmersionBar) {
        immersionBar.run {
            transparentStatusBar()
            statusBarDarkFont(true)
            fitsSystemWindows(false)
        }
    }

    override fun doObserve() {
        // 无数据界面
        viewModel.showNoData.observe(this, {
            binding.includeNoData.root.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        })
        // 绑定列表数据
        viewModel.capitalListData.observe(this, { list ->
            capitalAdapter.submitList(list)
        })
        viewModel.creditCardListData.observe(this, { list ->
            creditCardAdapter.submitList(list)
        })
        viewModel.topUpListData.observe(this, { list ->
            topUpAdapter.submitList(list)
        })
        viewModel.investmentFinancialListData.observe(this, { list ->
            investmentFinancialAdapter.submitList(list)
        })
        viewModel.debtListData.observe(this, { list ->
            debtAdapter.submitList(list)
        })
        // 隐藏显示资产
        viewModel.hideCapitalAccountList.observe(this, { hide ->
            capitalAdapter.submitList(
                if (hide.condition) {
                    arrayListOf()
                } else {
                    viewModel.capitalListData.value
                }
            )
        })
        viewModel.hideCreditCardAccountList.observe(this, { hide ->
            creditCardAdapter.submitList(
                if (hide.condition) {
                    arrayListOf()
                } else {
                    viewModel.creditCardListData.value
                }
            )
        })
        viewModel.hideTopUpAccountList.observe(this, { hide ->
            topUpAdapter.submitList(
                if (hide.condition) {
                    arrayListOf()
                } else {
                    viewModel.topUpListData.value
                }
            )
        })
        viewModel.hideInvestmentFinancialAccountList.observe(this, { hide ->
            investmentFinancialAdapter.submitList(
                if (hide.condition) {
                    arrayListOf()
                } else {
                    viewModel.investmentFinancialListData.value
                }
            )
        })
        viewModel.hideDebtAccountList.observe(this, { hide ->
            debtAdapter.submitList(
                if (hide.condition) {
                    arrayListOf()
                } else {
                    viewModel.debtListData.value
                }
            )
        })
        // 编辑状态
        viewModel.edit.observe(this, {
            // 编辑状态
            capitalTouchHelper.attachToRecyclerView(if (it) binding.rvCapital else null)
            creditCardTouchHelper.attachToRecyclerView(if (it) binding.rvCreditCard else null)
            topUpTouchHelper.attachToRecyclerView(if (it) binding.rvTopUp else null)
            investmentFinancialTouchHelper.attachToRecyclerView(if (it) binding.rvInvestmentFinancial else null)
            debtTouchHelper.attachToRecyclerView(if (it) binding.rvDebt else null)
        })
        // 显示资产长按菜单
        viewModel.showLongClickMenuEvent.observe(this, { asset ->
            AssetLongClickMenuDialog.actionShow(
                manager = supportFragmentManager,
                onEditClick = {
                    // 跳转编辑资产
                    EditAssetActivity.actionStart(context, asset)
                },
                onSortClick = {
                    // 排序点击
                    viewModel.edit.value = true
                },
                onHiddenClick = {
                    // 隐藏资产
                    viewModel.hideAsset(asset)
                })
        })
        // 显示更多菜单
        viewModel.showMoreMenuEvent.observe(this, {
            AssetMoreMenuDialog().show(supportFragmentManager)
        })
        // 保存点击
        viewModel.saveClickEvent.observe(this, {
            // 获取需要更新的数据列表
            val ls = arrayListOf<AssetEntity>()
            capitalAdapter.mDiffer.currentList.forEachIndexed { index, entity ->
                ls.add(entity.copy(sort = index))
            }
            creditCardAdapter.mDiffer.currentList.forEachIndexed { index, entity ->
                ls.add(entity.copy(sort = index))
            }
            topUpAdapter.mDiffer.currentList.forEachIndexed { index, entity ->
                ls.add(entity.copy(sort = index))
            }
            investmentFinancialAdapter.mDiffer.currentList.forEachIndexed { index, entity ->
                ls.add(entity.copy(sort = index))
            }
            debtAdapter.mDiffer.currentList.forEachIndexed { index, entity ->
                ls.add(entity.copy(sort = index))
            }
            viewModel.updateAsset(ls)
        })
    }

    /** 创建资产列表适配器对象 */
    private fun createAssetAdapter() = SimpleRvListAdapter<AssetEntity>(
        layoutResId = R.layout.recycler_item_asset_list,
        areItemsTheSame = { old, new -> old.id == new.id }
    ).apply {
        viewModel = this@MyAssetActivity.viewModel
    }
}