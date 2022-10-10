package cn.wj.android.cashbook.ui.asset.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_ASSET
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ASSET_EDIT
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.databinding.ActivityEditAssetBinding
import cn.wj.android.cashbook.ui.asset.dialog.SelectAssetClassificationDialog
import cn.wj.android.cashbook.ui.asset.dialog.SelectDayDialog
import cn.wj.android.cashbook.ui.asset.viewmodel.EditAssetViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑资产界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
@Route(path = ROUTE_PATH_ASSET_EDIT)
class EditAssetActivity : BaseActivity<EditAssetViewModel, ActivityEditAssetBinding>() {

    override val viewModel: EditAssetViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_asset)

        // 获取数据
        intent.getParcelableExtra<AssetEntity>(ACTION_ASSET)?.let { value ->
            // 更新资产类型
            viewModel.classificationType.value = value.type
            viewModel.assetClassification.value = value.classification
            if (value.id >= 0) {
                viewModel.id = value.id
                viewModel.createTime = value.createTime
                viewModel.sort = value.sort
                // 编辑资产，设置标题
                viewModel.titleStr.set(R.string.edit_asset.string)
                // 更新资产数据
                viewModel.assetName.value = value.name
                viewModel.totalAmount.value = value.totalAmount
                viewModel.billingDate.value = value.billingDate
                viewModel.repaymentDate.value = value.repaymentDate
                viewModel.invisibleAsset.value = value.invisible
                // 刷新余额
                viewModel.oldBalance = value.balance
                viewModel.openBank.value = value.openBank
                viewModel.cardNo.value = value.cardNo
                viewModel.remark.value = value.remark
                viewModel.needMoreInfo.value = value.needMoreInfo
            } else {
                viewModel.assetName.value = value.classification.nameResId.string
            }
        }
    }

    override fun doObserve() {
        // 显示选择日期弹窗
        viewModel.showSelectDayEvent.observe(this) { billingDate ->
            val selectedDay = if (billingDate) {
                // 账单日
                viewModel.billingDate.value.orEmpty()
            } else {
                // 还款日
                viewModel.repaymentDate.value.orEmpty()
            }
            SelectDayDialog.actionShow(supportFragmentManager, selectedDay) { selected ->
                if (billingDate) {
                    // 账单日
                    viewModel.billingDate.value = selected
                } else {
                    // 还款日
                    viewModel.repaymentDate.value = selected
                }
            }
        }
        // 显示选择资产分类弹窗
        viewModel.showSelectAssetClassificationEvent.observe(this) {
            SelectAssetClassificationDialog()
                .setOnClassificationSelectListener { classificationTypeEnum, assetClassificationEnum ->
                    viewModel.classificationType.value = classificationTypeEnum
                    viewModel.assetClassification.value = assetClassificationEnum
                }
                .show(supportFragmentManager)
        }
    }

    companion object {

        /** 使用 [context] 对象打开 [EditAssetActivity] 界面 */
        fun actionStart(context: Context, asset: AssetEntity) {
            context.startActivity(Intent(context, EditAssetActivity::class.java).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                putExtra(ACTION_ASSET, asset)
            })
        }

        /** 使用 [context] 对象打开 [EditAssetActivity] 界面，新建使用 */
        fun actionStart(
            context: Context,
            type: ClassificationTypeEnum,
            classification: AssetClassificationEnum
        ) {
            actionStart(
                context, AssetEntity.newAsset(type, classification)
            )
        }
    }
}