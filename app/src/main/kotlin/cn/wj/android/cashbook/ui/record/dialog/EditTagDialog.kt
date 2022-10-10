@file:Suppress("unused")

package cn.wj.android.cashbook.ui.record.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.databinding.DialogEditTagBinding
import cn.wj.android.cashbook.ui.record.viewmodel.EditTagViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑标签弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
class EditTagDialog
    : BaseDialog<EditTagViewModel, DialogEditTagBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    override val viewModel: EditTagViewModel by viewModel()

    override val layoutResId = R.layout.dialog_edit_tag

    private var onCallback: ((TagEntity) -> Unit)? = null

    override fun beforeOnCreate() {
        viewModel.editTag.value = requireArguments().getParcelable(ACTION_SELECTED)
    }

    override fun initView() {
    }

    override fun doObserve() {
        // 成功事件
        viewModel.successEvent.observe(this) { tag ->
            onCallback?.invoke(tag)
        }
    }

    companion object {
        fun actionShow(fm: FragmentManager, tag: TagEntity? = null, callback: (TagEntity) -> Unit) {
            EditTagDialog().run {
                arguments = bundleOf(
                    ACTION_SELECTED to tag
                )
                onCallback = callback
                show(fm)
            }
        }
    }
}
