package cn.wj.android.cashbook.ui.record.fragment

import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ui.BaseFragment
import cn.wj.android.cashbook.data.constants.ACTION_TYPE
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.databinding.FragmentEditRecordBinding
import cn.wj.android.cashbook.ui.record.viewmodel.EditRecordViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.ConsumptionTypeViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑记录界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
class EditRecordFragment : BaseFragment<EditRecordViewModel, FragmentEditRecordBinding>() {

    override val layoutResId: Int = R.layout.fragment_edit_record

    override val viewModel: EditRecordViewModel by activityViewModels()

    override val activityViewModel: Boolean = true

    val typeViewModel: ConsumptionTypeViewModel by viewModel()

    /** 类型列表适配器 */
    private val typeAdapter: SimpleRvListAdapter<TypeEntity> by lazy {
        SimpleRvListAdapter<TypeEntity>(R.layout.recycler_item_type).apply {
            this.viewModel = this@EditRecordFragment.viewModel
        }
    }

    override fun initView() {
        // 获取类型数据
        typeViewModel.typeData.value = requireArguments().getParcelable(ACTION_TYPE)

        // 配置 RecyclerView
        binding.rvType.run {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = typeAdapter
        }
    }

    override fun observe() {
        // 类型列表
        typeViewModel.typeListData.observe(this, { list ->
            typeAdapter.submitList(list)
        })
    }

    companion object {
        /** 新建一个 [EditRecordFragment] 对象并返回 */
        @JvmStatic
        fun newInstance(position: Int): EditRecordFragment {
            return EditRecordFragment().apply {
                arguments = bundleOf(
                    ACTION_TYPE to RecordTypeEnum.fromPosition(position).orElse(RecordTypeEnum.INCOME)
                )
            }
        }
    }
}