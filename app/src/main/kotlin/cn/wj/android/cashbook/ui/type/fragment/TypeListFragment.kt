package cn.wj.android.cashbook.ui.type.fragment

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ItemTouchHelper
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseFragment
import cn.wj.android.cashbook.data.constants.*
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.FragmentTypeListBinding
import cn.wj.android.cashbook.third.result.createForActivityResultLauncher
import cn.wj.android.cashbook.ui.type.activity.SelectFirstTypeActivity
import cn.wj.android.cashbook.ui.type.adapter.EditTypeRvAdapter
import cn.wj.android.cashbook.ui.type.dialog.EditTypeMenuDialog
import cn.wj.android.cashbook.ui.type.viewmodel.TypeListViewModel
import cn.wj.android.cashbook.widget.recyclerview.callback.DragItemTouchCallback
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.jeremyliao.liveeventbus.LiveEventBus
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 类型列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/29
 */
class TypeListFragment : BaseFragment<TypeListViewModel, FragmentTypeListBinding>() {

    override val layoutResId: Int = R.layout.fragment_type_list

    override val viewModel: TypeListViewModel by viewModel()

    /** 适配器对象 */
    private val adapter: EditTypeRvAdapter by lazy {
        EditTypeRvAdapter().apply {
            this.viewModel = this@TypeListFragment.viewModel
        }
    }

    /** 选择一级分类启动器 */
    private val selectFirstTypeLauncher = createForActivityResultLauncher(ActivityResultContracts.StartActivityForResult())

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
                type.first,
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
                onChangeToSecondTypeClick = {
                    // 修改为二级分类
                    if (type.childList.isNotEmpty()) {
                        // 有二级分类，无法修改
                        viewModel.snackbarEvent.value = R.string.first_type_has_child.string.toSnackbarModel()
                    } else {
                        selectFirstTypeLauncher.launch(SelectFirstTypeActivity.createIntent(requireActivity(), type)) {
                            if (it.resultCode != ACTIVITY_RESULT_OK) {
                                return@launch
                            }
                            // 选择一级分类返回
                            viewModel.disposeForResult(type, it.data?.getParcelableExtra(ACTION_SELECTED) ?: return@launch)
                        }
                    }
                },
                onChangeToFirstTypeClick = {
                    // 修改为一级分类
                    viewModel.changeToFirstType(type)
                },
                onMoveToOtherFirstTypeClick = {
                    // 移动到其它一级分类
                    selectFirstTypeLauncher.launch(SelectFirstTypeActivity.createIntent(requireActivity(), type)) {
                        if (it.resultCode != ACTIVITY_RESULT_OK) {
                            return@launch
                        }
                        // 选择一级分类返回
                        viewModel.disposeForResult(type, it.data?.getParcelableExtra(ACTION_SELECTED) ?: return@launch)
                    }
                },
                onStatisticsClick = {
                    // TODO 统计数据点击
                    viewModel.snackbarEvent.value = "跳转统计数据-${type.name}".toSnackbarModel()
                })
        })
        // 记录数据变化
        LiveEventBus.get(EVENT_RECORD_CHANGE).observe(this, {
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