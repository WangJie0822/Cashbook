package cn.wj.android.cashbook.ui.asset.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.databinding.ActivityEditAssetBinding
import cn.wj.android.cashbook.ui.asset.dialog.SelectAssetClassificationDialog
import cn.wj.android.cashbook.ui.asset.viewmodel.EditAssetViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑资产界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
class EditAssetActivity : BaseActivity<EditAssetViewModel, ActivityEditAssetBinding>() {

    override val viewModel: EditAssetViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_asset)
    }

    override fun observe() {
        // 显示选择资产分类弹窗
        viewModel.showSelectAssetClassificationData.observe(this, {
            SelectAssetClassificationDialog()
                .setOnClassificationSelectListener { classificationTypeEnum, assetClassificationEnum ->
                    viewModel.classificationType.value = classificationTypeEnum
                    viewModel.assetClassification.value = assetClassificationEnum
                }
                .show(supportFragmentManager)
        })
    }

    companion object {

        /** 使用 [context] 对象打开 [EditAssetActivity] 界面 */
        fun actionStart(context: Context) {
            context.startActivity(Intent(context, EditAssetActivity::class.java).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            })
        }
    }
}