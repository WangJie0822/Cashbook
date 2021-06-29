package cn.wj.android.cashbook.ui.type.fragment

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ConcatAdapter
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ui.BaseFragment
import cn.wj.android.cashbook.data.constants.ACTION_TYPE
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.databinding.FragmentTypeListBinding
import cn.wj.android.cashbook.ui.general.adapter.OneItemAdapter
import cn.wj.android.cashbook.ui.type.adapter.EditTypeRvAdapter
import cn.wj.android.cashbook.ui.type.viewmodel.TypListViewModel
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
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
            adapter = ConcatAdapter(
                this@TypeListFragment.adapter,
                OneItemAdapter(R.layout.recycler_footer_blank)
            )
        }
    }

    override fun observe() {
        // 数据列表
        viewModel.listData.observe(this, { list ->
            adapter.submitList(list)
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