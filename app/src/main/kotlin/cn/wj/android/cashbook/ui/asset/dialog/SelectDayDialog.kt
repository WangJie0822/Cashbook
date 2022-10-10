package cn.wj.android.cashbook.ui.asset.dialog

import android.view.Gravity
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.databinding.DialogAssetSelectDayBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.SelectDayViewModel
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 选择日期弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
class SelectDayDialog : BaseDialog<SelectDayViewModel, DialogAssetSelectDayBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_asset_select_day

    override val viewModel: SelectDayViewModel by viewModel()

    override val gravity: Int = Gravity.BOTTOM

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT

    private var onDaySelectedListener: ((String) -> Unit)? = null

    private var selectedDay = ""

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }
    }

    override fun beforeOnCreate() {
        // 获取数据
        selectedDay = requireArguments().getString(ACTION_SELECTED) ?: return
    }

    override fun doObserve() {
        // 日期数据
        viewModel.dayData.observe(this) { list ->
            binding.epv.setDataList(list)
            binding.epv.moveTo(list.indexOf(selectedDay + R.string.day.string))
        }
        // 确认事件
        viewModel.confirmClickEvent.observe(this) {
            onDaySelectedListener?.invoke(binding.epv.getCurrentData().toString().replace(R.string.day.string, ""))
            dismiss()
        }
    }

    companion object {
        fun actionShow(manager: FragmentManager, selectedDay: String, onDaySelected: (String) -> Unit) {
            SelectDayDialog().run {
                arguments = bundleOf(
                    ACTION_SELECTED to selectedDay
                )
                onDaySelectedListener = onDaySelected
                show(manager)
            }
        }
    }
}