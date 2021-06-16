package cn.wj.android.cashbook.ui.record.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_CURRENT_AMOUNT
import cn.wj.android.cashbook.data.constants.ACTION_DATE
import cn.wj.android.cashbook.data.constants.ACTION_REFUND
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_SELECT_ASSOCIATED_RECORD
import cn.wj.android.cashbook.databinding.ActivitySelectAccosiatedRecordBinding
import cn.wj.android.cashbook.ui.record.viewmodel.SelectAssociatedRecordViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 选择关联记录界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/16
 */
@Route(path = ROUTE_PATH_SELECT_ASSOCIATED_RECORD)
class SelectAssociatedRecordActivity : BaseActivity<SelectAssociatedRecordViewModel, ActivitySelectAccosiatedRecordBinding>() {

    override val viewModel: SelectAssociatedRecordViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_accosiated_record)

        // 获取传递的数据
        viewModel.refund.value = intent.getBooleanExtra(ACTION_REFUND, true)
        viewModel.dateStr.value = intent.getStringExtra(ACTION_DATE)
        viewModel.amount.value = intent.getStringExtra(ACTION_CURRENT_AMOUNT)
    }

    companion object {

        /** 使用 [context] 开启生成跳转选择关联记录界面 [Intent]，修改时传递 日期 [date]、金额 [amount]、是否是退款 */
        fun parseIntent(context: Context, date: String, amount: String, refund: Boolean): Intent = Intent(context, SelectAssociatedRecordActivity::class.java).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            putExtra(ACTION_DATE, date)
            putExtra(ACTION_CURRENT_AMOUNT, amount)
            putExtra(ACTION_REFUND, refund)
        }
    }
}