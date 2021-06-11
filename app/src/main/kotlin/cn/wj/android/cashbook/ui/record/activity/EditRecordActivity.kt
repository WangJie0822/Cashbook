package cn.wj.android.cashbook.ui.record.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_EDIT_RECORD
import cn.wj.android.cashbook.databinding.ActivityEditRecordBinding
import cn.wj.android.cashbook.ui.asset.dialog.SelectAssetDialog
import cn.wj.android.cashbook.ui.record.adapter.EditRecordVpAdapter
import cn.wj.android.cashbook.ui.record.dialog.CalculatorDialog
import cn.wj.android.cashbook.ui.record.dialog.DateTimePickerDialog
import cn.wj.android.cashbook.ui.record.viewmodel.EditRecordViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑记录界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
@Route(path = ROUTE_PATH_EDIT_RECORD)
class EditRecordActivity : BaseActivity<EditRecordViewModel, ActivityEditRecordBinding>() {

    override val viewModel: EditRecordViewModel by viewModel()

    /** 消费类型 ViewPager 适配器 */
    private val typesVpAdapter: EditRecordVpAdapter by lazy {
        EditRecordVpAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_record)

        // 配置 ViewPager2
        binding.vpType.adapter = typesVpAdapter

        lifecycleScope.launchWhenResumed {
            // 自动弹出计算弹出
            viewModel.showCalculatorData.value = 0
        }
    }

    override fun observe() {
        // 选择资产弹窗
        viewModel.showSelectAssetData.observe(this, {
            SelectAssetDialog.actionShow(supportFragmentManager) { selected ->
                if (it) {
                    viewModel.accountData
                } else {
                    viewModel.transferAccountData
                }.value = selected
            }
        })
        // 选择日期弹窗
        viewModel.showSelectDateData.observe(this, {
            DateTimePickerDialog.Builder()
                .setDate(viewModel.dateData.value.orEmpty())
                .setOnDatePickerListener { date ->
                    viewModel.dateData.value = date
                }.show(supportFragmentManager)

        })
        // 计算器弹窗
        viewModel.showCalculatorData.observe(this, {
            CalculatorDialog.actionShow(supportFragmentManager)
        })
    }
}