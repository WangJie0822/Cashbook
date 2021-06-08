package cn.wj.android.cashbook.ui.type.fragment

import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ui.BaseFragment
import cn.wj.android.cashbook.data.constants.ACTION_TYPE
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.databinding.FragmentConsumptionTypeBinding
import cn.wj.android.cashbook.ui.record.viewmodel.EditRecordViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.ConsumptionTypeViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 消费类型界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
class ConsumptionTypeFragment : BaseFragment<ConsumptionTypeViewModel, FragmentConsumptionTypeBinding>() {

    override val layoutResId: Int = R.layout.fragment_consumption_type

    override val viewModel: ConsumptionTypeViewModel by viewModel()

    /** 编辑记录界面 ViewModel 用于绑定类型数据 */
    val activityViewModel: EditRecordViewModel by activityViewModels()

    /** 类型列表适配器 */
    private val typeAdapter: SimpleRvListAdapter<TypeEntity> by lazy {
        SimpleRvListAdapter<TypeEntity>(R.layout.recycler_item_type).apply {
            this.viewModel = this@ConsumptionTypeFragment.viewModel
        }
    }

    override fun initView() {
        // 获取类型数据
        viewModel.typeData.value = requireArguments().getParcelable(ACTION_TYPE)

        // 配置 RecyclerView
        binding.rvType.run {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = typeAdapter
        }
    }

    override fun observe() {
        // 类型列表
        viewModel.typeListData.observe(this, { list ->
            typeAdapter.submitList(list)
        })
    }

    companion object {
        /** 新建一个 [ConsumptionTypeFragment] 对象并返回 */
        @JvmStatic
        fun newInstance(position: Int): ConsumptionTypeFragment {
            return ConsumptionTypeFragment().apply {
                arguments = bundleOf(
                    ACTION_TYPE to RecordTypeEnum.fromPosition(position).orElse(RecordTypeEnum.INCOME)
                )
            }
        }
    }
}