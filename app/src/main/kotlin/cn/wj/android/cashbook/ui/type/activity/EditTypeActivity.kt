package cn.wj.android.cashbook.ui.type.activity

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_TYPE_EDIT
import cn.wj.android.cashbook.data.entity.TypeIconEntity
import cn.wj.android.cashbook.data.entity.TypeIconGroupEntity
import cn.wj.android.cashbook.databinding.ActivityEditTypeBinding
import cn.wj.android.cashbook.ui.type.viewmodel.EditTypeViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑分类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/30
 */
@Route(path = ROUTE_PATH_TYPE_EDIT)
class EditTypeActivity : BaseActivity<EditTypeViewModel, ActivityEditTypeBinding>() {

    override val viewModel: EditTypeViewModel by viewModel()

    /** 分组适配器 */
    private val groupAdapter: SimpleRvListAdapter<TypeIconGroupEntity> by lazy {
        SimpleRvListAdapter<TypeIconGroupEntity>(R.layout.recycler_item_type_icon_group).apply {
            this.viewModel = this@EditTypeActivity.viewModel
        }
    }

    /** 图标适配器 */
    private val iconAdapter: SimpleRvListAdapter<TypeIconEntity> by lazy {
        SimpleRvListAdapter<TypeIconEntity>(R.layout.recycler_item_type_icon).apply {
            this.viewModel = this@EditTypeActivity.viewModel
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_type)

        // 获取传递数据
        viewModel.typeData.value = intent.getParcelableExtra(ACTION_SELECTED)

        // 配置 RecyclerView
        binding.rvGroup.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = groupAdapter
        }
        binding.rvIcon.run {
            layoutManager = GridLayoutManager(context, 4)
            adapter = iconAdapter
        }
    }

    override fun doObserve() {
        // 列表数据
        viewModel.groupListData.observe(this) { list ->
            groupAdapter.submitList(list)
        }
        viewModel.iconListData.observe(this) { list ->
            iconAdapter.submitList(list)
        }
    }
}