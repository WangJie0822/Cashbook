package cn.wj.android.cashbook.ui.type.fragment

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ItemTouchHelper
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ui.BaseFragment
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.constants.ACTION_TYPE
import cn.wj.android.cashbook.data.constants.EVENT_TYPE_CHANGE
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_TYPE_EDIT
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.FragmentTypeListBinding
import cn.wj.android.cashbook.ui.type.adapter.EditTypeRvAdapter
import cn.wj.android.cashbook.ui.type.dialog.EditTypeMenuDialog
import cn.wj.android.cashbook.ui.type.viewmodel.TypListViewModel
import cn.wj.android.cashbook.widget.recyclerview.callback.DragItemTouchCallback
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.jeremyliao.liveeventbus.LiveEventBus
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 类型列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/29
 */
class TypeListFragment : BaseFragment<TypListViewModel, FragmentTypeListBinding>() {

    override val layoutResId: Int = R.layout.fragment_type_list

    override val viewModel: TypListViewModel by viewModel()

    /** 适配器对象 */
    private val adapter: EditTypeRvAdapter by lazy {
        EditTypeRvAdapter().apply {
            this.viewModel = this@TypeListFragment.viewModel
        }
    }

    override fun beforeOnCreate() {
        // 获取分类大类
        viewModel.typeData.value = requireArguments().getParcelable<RecordTypeEnum>(ACTION_TYPE).orElse(RecordTypeEnum.EXPENDITURE)
    }

    override fun initView() {
        // 配置 RecyclerView
        binding.rvType.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = this@TypeListFragment.adapter
            val helper = ItemTouchHelper(DragItemTouchCallback(this@TypeListFragment.adapter) { viewModel.edit.value = true })
            helper.attachToRecyclerView(this)
        }
    }

    override fun observe() {
        // 数据列表
        viewModel.listData.observe(this, { list ->
            adapter.submitList(list)
        })
        // 编辑保存点击
        viewModel.saveEvent.observe(this, {
            viewModel.updateType(adapter.getChangeList())
        })
        // 显示编辑弹窗
        viewModel.showEditTypeMenuEvent.observe(this, { type ->
            EditTypeMenuDialog.actionShow(childFragmentManager,
                onEditClick = {
                    // 编辑点击
                    viewModel.uiNavigationEvent.value = UiNavigationModel.builder {
                        jump(
                            ROUTE_PATH_TYPE_EDIT, bundleOf(
                                ACTION_SELECTED to type
                            )
                        )
                    }
                },
                onDeleteClick = {
                    // 删除点击
                    viewModel.deleteType(type)
                },
                onStatisticsClick = {
                    // TODO 统计数据点击
                    viewModel.snackbarEvent.value = "跳转统计数据-${type.name}".toSnackbarModel()
                })
        })
        // 分类数据变化
        LiveEventBus.get(EVENT_TYPE_CHANGE).observe(this, {
            viewModel.loadTypeList()
        })
    }

    companion object {
        /** 新建一个 [TypeListFragment] 对象并返回 */
        @JvmStatic
        fun newInstance(position: Int): TypeListFragment {
            return TypeListFragment().apply {
                arguments = bundleOf(
                    ACTION_TYPE to RecordTypeEnum.fromPosition(position).orElse(RecordTypeEnum.EXPENDITURE)
                )
            }
        }
    }
}