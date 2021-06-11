package cn.wj.android.cashbook.ui.record.dialog

import android.view.Gravity
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.data.constants.ACTION_RECORD
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.databinding.DialogRecordInfoBinding
import cn.wj.android.cashbook.ui.record.viewmodel.RecordInfoViewModel
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 记录信息弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class RecordInfoDialog : BaseDialog<RecordInfoViewModel, DialogRecordInfoBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_record_info

    override val viewModel: RecordInfoViewModel by viewModel()

    override val gravity: Int = Gravity.BOTTOM

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }

        // 获取记录数据
        val record = requireArguments().getParcelable<RecordEntity>(ACTION_RECORD)
        if (null == record) {
            dismiss()
            return
        }
        // 绑定数据
        viewModel.record = record
        binding.item = record
    }

    override fun observe() {
    }

    companion object {
        fun actionShow(manager: FragmentManager, record: RecordEntity) {
            RecordInfoDialog().run {
                arguments = bundleOf(
                    ACTION_RECORD to record
                )
                show(manager)
            }
        }
    }
}