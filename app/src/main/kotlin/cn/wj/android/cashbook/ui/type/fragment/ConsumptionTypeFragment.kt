package cn.wj.android.cashbook.ui.type.fragment

import androidx.core.os.bundleOf
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseFragment
import cn.wj.android.cashbook.databinding.FragmentConsumptionTypeBinding
import cn.wj.android.cashbook.ui.type.viewmodel.ConsumptionTypeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 消费类型界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
class ConsumptionTypeFragment : BaseFragment<ConsumptionTypeViewModel, FragmentConsumptionTypeBinding>() {

    override val layoutResId: Int = R.layout.fragment_consumption_type

    override val viewModel: ConsumptionTypeViewModel by viewModel()

    override fun initView() {
        binding.tv.text = arguments?.getInt("position").toString()
    }

    companion object {
        /** 新建一个 [ConsumptionTypeFragment] 对象并返回 */
        @JvmStatic
        fun newInstance(position: Int): ConsumptionTypeFragment {
            return ConsumptionTypeFragment().apply {
                arguments = bundleOf(
                    "position" to position
                )
            }
        }
    }
}