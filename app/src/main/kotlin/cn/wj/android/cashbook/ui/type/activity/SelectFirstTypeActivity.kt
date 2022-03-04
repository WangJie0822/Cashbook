package cn.wj.android.cashbook.ui.type.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_TYPE_SELECT_FIRST
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.databinding.ActivitySelectFirstTypeBinding
import cn.wj.android.cashbook.ui.type.viewmodel.SelectFirstTypeViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 选择一级分类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/3
 */
@Route(path = ROUTE_PATH_TYPE_SELECT_FIRST)
class SelectFirstTypeActivity : BaseActivity<SelectFirstTypeViewModel, ActivitySelectFirstTypeBinding>() {

    override val viewModel: SelectFirstTypeViewModel by viewModel()

    /** 列表适配器 */
    private val adapter: SimpleRvListAdapter<TypeEntity> by lazy {
        SimpleRvListAdapter<TypeEntity>(R.layout.recycler_item_type_first).apply {
            this.viewModel = this@SelectFirstTypeActivity.viewModel
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_first_type)

        // 获取目标分类数据
        viewModel.targetTypeData.value = intent.getParcelableExtra(ACTION_SELECTED)

        // 配置 RecyclerView
        binding.rv.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = this@SelectFirstTypeActivity.adapter
        }
    }

    override fun observe() {
        // 列表数据
        viewModel.listData.observe(this) { list ->
            adapter.submitList(list)
        }
    }

    companion object {

        /** 创建使用 [context] [typeEntity] 创建界面跳转 [Intent] */
        fun createIntent(context: Context, typeEntity: TypeEntity): Intent {
            return Intent(context, SelectFirstTypeActivity::class.java).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                putExtra(ACTION_SELECTED, typeEntity)
            }
        }
    }
}