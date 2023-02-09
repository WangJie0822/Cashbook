package cn.wj.android.cashbook.ui.type.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.databinding.ActivityEditTypeListBinding
import cn.wj.android.cashbook.ui.type.adapter.EditTypeListVpAdapter
import cn.wj.android.cashbook.ui.type.viewmodel.EditTypeListViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑分类列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/29
 */
class EditTypeListActivity : BaseActivity<EditTypeListViewModel, ActivityEditTypeListBinding>() {

    override val viewModel: EditTypeListViewModel by viewModel()

    /** 适配器 */
    private val adapter: EditTypeListVpAdapter by lazy {
        EditTypeListVpAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_type_list)

        // 获取选中分类
        viewModel.currentItem.value =
            intent.getIntExtra(ACTION_SELECTED, RecordTypeEnum.EXPENDITURE.position)
                .orElse(RecordTypeEnum.EXPENDITURE.position)

        // 配置 ViewPager2
        binding.vpType.adapter = adapter
    }
}