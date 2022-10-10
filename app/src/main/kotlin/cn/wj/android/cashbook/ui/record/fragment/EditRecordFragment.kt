package cn.wj.android.cashbook.ui.record.fragment

import android.annotation.SuppressLint
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ui.BaseFragment
import cn.wj.android.cashbook.data.constants.ACTION_TYPE
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.databinding.FragmentEditRecordBinding
import cn.wj.android.cashbook.ui.general.adapter.OneItemAdapter
import cn.wj.android.cashbook.ui.record.adapter.TypeSecondRvAdapter
import cn.wj.android.cashbook.ui.record.dialog.RecordInfoDialog
import cn.wj.android.cashbook.ui.record.viewmodel.EditRecordViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.ConsumptionTypeViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import com.jeremyliao.liveeventbus.LiveEventBus
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

    private val typeViewModel: ConsumptionTypeViewModel by viewModel()

    /** 类型列表适配器 */
    private val adapter: ConcatAdapter = ConcatAdapter()

    /** 二级类型适配器 */
    private val secondAdapter: TypeSecondRvAdapter by lazy {
        TypeSecondRvAdapter(typeViewModel)
    }

    override fun beforeOnCreate() {
        // 绑定基本事件
        observeBaseModel(typeViewModel)
    }

    override fun initView() {
        // 获取类型数据
        typeViewModel.typeData.value = requireArguments().getParcelable(ACTION_TYPE)

        // 配置 RecyclerView
        binding.rvType.run {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = this@EditRecordFragment.adapter
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun doObserve() {
        // 类型列表
        typeViewModel.typeListData.observe(this) { list ->
            if (null == list) {
                return@observe
            }
            adapter.adapters.forEach {
                adapter.removeAdapter(it)
            }
            var ls: ArrayList<TypeEntity> = arrayListOf()
            list.forEachIndexed { index, entity ->
                val mod = index % 5
                if (mod == 0) {
                    ls = arrayListOf()
                }
                ls.add(entity)
                if (mod == 4 || index == list.size - 1) {
                    adapter.addAdapter(createTypeAdapter(ls))
                }
            }
            adapter.addAdapter(OneItemAdapter(R.layout.recycler_footer_type_setting) {
                typeViewModel.onTypeSettingClick.invoke()
            })
            val typeValue = when (typeViewModel.typeData.value.orElse(RecordTypeEnum.EXPENDITURE)) {
                RecordTypeEnum.EXPENDITURE -> {
                    // 支出
                    viewModel.expenditureType
                }
                RecordTypeEnum.INCOME -> {
                    // 收入
                    viewModel.incomeType
                }
                else -> {
                    // 转账
                    viewModel.transferType
                }
            }.value
            if (null == typeValue) {
                // 默认选中第一条
                val firstItem = list.firstOrNull()
                firstItem?.let { first ->
                    typeViewModel.onTypeItemClick.invoke(first)
                }
            } else {
                typeViewModel.onTypeItemClick.invoke(typeValue)
            }
        }
        // 二级类型状态
        typeViewModel.secondTypeData.observe(this) { item ->
            if (null == item) {
                return@observe
            }
            adapter.removeAdapter(secondAdapter)
            if (item.expand.get() && item.childEnable && item.childList.isNotEmpty()) {
                // 需要展开
                secondAdapter.list.clear()
                secondAdapter.list.add(item)
                secondAdapter.notifyDataSetChanged()
                // 计算菜单位置
                val list = typeViewModel.typeListData.value.orEmpty()
                val itemIndex = list.indexOfFirst {
                    it.id == item.id
                }
                if (itemIndex >= 0) {
                    val adapterIndex = (itemIndex / 5) + 1
                    adapter.addAdapter(adapterIndex, secondAdapter)
                    secondAdapter.itemPosition = adapterIndex * 5
                    secondAdapter.visiblePosition = itemIndex % 5
                }
            }
        }
        // 选中类型
        typeViewModel.selectTypeData.observe(this) { selected ->
            when (typeViewModel.typeData.value) {
                RecordTypeEnum.EXPENDITURE -> {
                    // 支出
                    viewModel.expenditureType
                }
                RecordTypeEnum.INCOME -> {
                    // 收入
                    viewModel.incomeType
                }
                else -> {
                    // 转账
                    viewModel.transferType
                }
            }.value = selected
        }
        // 显示记录详情弹窗
        viewModel.showRecordDetailsDialogEvent.observe(this) { record ->
            RecordInfoDialog.actionShow(childFragmentManager, record)
        }
        // 记录数据变化
        LiveEventBus.get<Int>(EVENT_RECORD_CHANGE).observe(this) {
            typeViewModel.loadType()
            viewModel.refreshAssociatedRecord()
        }
    }

    private fun createTypeAdapter(ls: List<TypeEntity>): SimpleRvListAdapter<TypeEntity> {
        return SimpleRvListAdapter<TypeEntity>(R.layout.recycler_item_type).apply {
            this.viewModel = this@EditRecordFragment.typeViewModel
            submitList(ls)
        }
    }

    companion object {
        /** 新建一个 [EditRecordFragment] 对象并返回 */
        @JvmStatic
        fun newInstance(position: Int): EditRecordFragment {
            return EditRecordFragment().apply {
                arguments = bundleOf(
                    ACTION_TYPE to RecordTypeEnum.fromPosition(position)
                        .orElse(RecordTypeEnum.EXPENDITURE)
                )
            }
        }
    }
}