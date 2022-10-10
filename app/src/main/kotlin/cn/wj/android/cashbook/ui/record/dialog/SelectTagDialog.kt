package cn.wj.android.cashbook.ui.record.dialog

import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.databinding.DialogSelectTagBinding
import cn.wj.android.cashbook.databinding.RecyclerItemTagBinding
import cn.wj.android.cashbook.ui.record.viewmodel.SelectTagViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.FlowLayoutManager
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 选择标签弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
class SelectTagDialog : BaseDialog<SelectTagViewModel, DialogSelectTagBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_select_tag

    override val viewModel: SelectTagViewModel by viewModel()

    override val gravity: Int = Gravity.BOTTOM

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.MATCH_PARENT

    private val tagAdapter: SimpleRvListAdapter<TagEntity> by lazy {
        SimpleRvListAdapter<TagEntity>(R.layout.recycler_item_tag).apply {
            this.viewModel = this@SelectTagDialog.viewModel
        }
    }

    private var onCallback: ((List<TagEntity>) -> Unit)? = null

    override fun beforeOnCreate() {
        // 获取已选中数据
        viewModel.selectedTags.value = requireArguments().getParcelableArrayList<TagEntity>(ACTION_SELECTED).orEmpty()
    }

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }

        // 配置 RecyclerView
        binding.rvTag.run {
            layoutManager = FlowLayoutManager()
            adapter = tagAdapter
        }
    }

    override fun doObserve() {
        // 标签列表
        viewModel.tagListData.observe(this) { list ->
            tagAdapter.submitList(list)
        }
        // 确认回调
        viewModel.confirmClickEvent.observe(this) { selected ->
            onCallback?.invoke(selected)
            dismiss()
        }
        // 显示新建标签弹窗
        viewModel.showCreateTagDialogEvent.observe(this) {
            EditTagDialog.actionShow(requireActivity().supportFragmentManager, callback = { tag ->
                viewModel.insertTag(tag)
            })
        }
        // 显示菜单
        viewModel.showMenuEvent.observe(this) { tag ->
            val viewHolder = binding.rvTag.findViewHolderForAdapterPosition(
                tagAdapter.mDiffer.currentList.indexOf(tag)
            )
            if (null != viewHolder && viewHolder is SimpleRvListAdapter.ViewHolder<*>) {
                (viewHolder.mBinding as? RecyclerItemTagBinding)?.root?.let { anchor ->
                    PopupMenu(requireContext(), anchor).run {
                        inflate(R.menu.menu_select_tag_more)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.modify -> {
                                    // 修改
                                    EditTagDialog.actionShow(requireActivity().supportFragmentManager, tag) { modified ->
                                        viewModel.updateTag(modified)
                                    }
                                    this@SelectTagDialog.dismiss()
                                }
                                R.id.delete -> {
                                    // 删除
                                    viewModel.deleteTag(tag)
                                }
                            }
                            true
                        }
                        show()
                    }
                }
            }
        }
    }

    companion object {
        fun actionShow(manager: FragmentManager, selected: List<TagEntity>, callback: (List<TagEntity>) -> Unit) {
            SelectTagDialog().run {
                arguments = bundleOf(
                    ACTION_SELECTED to selected
                )
                onCallback = callback
                show(manager)
            }
        }
    }
}