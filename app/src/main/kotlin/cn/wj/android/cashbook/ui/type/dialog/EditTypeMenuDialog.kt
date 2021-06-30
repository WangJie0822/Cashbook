package cn.wj.android.cashbook.ui.type.dialog

import android.view.Gravity
import android.view.WindowManager
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogEditTypeMenuBinding
import cn.wj.android.cashbook.ui.type.viewmodel.EditTypeMenuViewModel
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑分类菜单弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/30
 */
class EditTypeMenuDialog : BaseDialog<EditTypeMenuViewModel, DialogEditTypeMenuBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_edit_type_menu

    override val viewModel: EditTypeMenuViewModel by viewModel()

    override val gravity: Int = Gravity.BOTTOM

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT

    private var onEditClickListener: (() -> Unit)? = null
    private var onDeleteClickListener: (() -> Unit)? = null
    private var onStatisticsClickListener: (() -> Unit)? = null

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }
    }

    override fun observe() {
        // 点击事件
        viewModel.editClickEvent.observe(this, {
            onEditClickListener?.invoke()
            dismiss()
        })
        viewModel.deleteClickEvent.observe(this, {
            onDeleteClickListener?.invoke()
            dismiss()
        })
        viewModel.statisticsClickEvent.observe(this, {
            onStatisticsClickListener?.invoke()
            dismiss()
        })
    }

    companion object {
        fun actionShow(manager: FragmentManager, onEditClick: () -> Unit, onDeleteClick: () -> Unit, onStatisticsClick: () -> Unit) {
            EditTypeMenuDialog().run {
                onEditClickListener = onEditClick
                onDeleteClickListener = onDeleteClick
                onStatisticsClickListener = onStatisticsClick
                show(manager)
            }
        }
    }
}