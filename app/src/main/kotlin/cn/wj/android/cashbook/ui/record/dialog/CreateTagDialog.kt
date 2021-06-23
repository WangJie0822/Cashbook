@file:Suppress("unused")

package cn.wj.android.cashbook.ui.record.dialog

import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogCreateTagBinding
import cn.wj.android.cashbook.ui.record.viewmodel.CreateTagViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 创建标签弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
class CreateTagDialog
    : BaseDialog<CreateTagViewModel, DialogCreateTagBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    override val viewModel: CreateTagViewModel by viewModel()

    override val layoutResId = R.layout.dialog_create_tag

    override fun initView() {
    }

    companion object {
        fun actionShow(fm: FragmentManager) {
            CreateTagDialog().show(fm)
        }
    }
}
