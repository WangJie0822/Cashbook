package cn.wj.android.cashbook.ui.record.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_TYPE
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.model.NoDataModel
import cn.wj.android.cashbook.databinding.ActivityTypeRecordBinding
import cn.wj.android.cashbook.databinding.LayoutNoDataBinding
import cn.wj.android.cashbook.ui.record.adapter.DateRecordRvAdapter
import cn.wj.android.cashbook.ui.record.dialog.RecordInfoDialog
import cn.wj.android.cashbook.ui.record.viewmodel.TypeRecordViewModel
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 分类下记录数据界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/13
 */
@Route(path = ROUTE_PATH_RECORD_TYPE)
class TypeRecordActivity : BaseActivity<TypeRecordViewModel, ActivityTypeRecordBinding>() {

    override val viewModel: TypeRecordViewModel by viewModel()

    /** 列表适配器对象 */
    private val adapter: DateRecordRvAdapter by lazy {
        DateRecordRvAdapter().apply {
            this.viewModel = this@TypeRecordActivity.viewModel
            setEmptyView(LayoutNoDataBinding.inflate(LayoutInflater.from(context)).apply {
                viewModel = NoDataModel(R.string.no_data_record)
            }.root)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_type_record)

        // 获取分类数据
        viewModel.typeData.value = intent.getParcelableExtra(ACTION_SELECTED)

        // 配置 RecyclerView
        binding.rv.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = this@TypeRecordActivity.adapter
        }
    }

    override fun observe() {
        // 列表数据
        viewModel.listData.observe(this) { list ->
            adapter.submitList(list)
        }
        // 显示记录详情弹窗
        viewModel.showRecordDetailsDialogEvent.observe(this) { record ->
            RecordInfoDialog.actionShow(supportFragmentManager, record)
        }
    }

    companion object {

        /** 使用 [context] 打开界面并传递类型数据 [typeEntity] */
        fun actionStart(context: Context, typeEntity: TypeEntity) {
            context.startActivity(Intent(context, TypeRecordActivity::class.java).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                putExtra(ACTION_SELECTED, typeEntity)
            })
        }
    }
}