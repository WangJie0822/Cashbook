package cn.wj.android.cashbook.ui.record.activity

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.widget.PopupMenu
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_TAG_MANAGER
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.data.model.NoDataModel
import cn.wj.android.cashbook.databinding.ActivityTagManagerBinding
import cn.wj.android.cashbook.databinding.LayoutNoDataBinding
import cn.wj.android.cashbook.databinding.RecyclerItemTagListBinding
import cn.wj.android.cashbook.ui.record.dialog.EditTagDialog
import cn.wj.android.cashbook.ui.record.viewmodel.TagManagerViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.FlowLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 标签管理界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/8/8
 */
@Route(path = ROUTE_PATH_RECORD_TAG_MANAGER)
class TagManagerActivity : BaseActivity<TagManagerViewModel, ActivityTagManagerBinding>() {

    override val viewModel: TagManagerViewModel by viewModel()

    private val adapter: SimpleRvListAdapter<TagEntity> by lazy {
        SimpleRvListAdapter<TagEntity>(R.layout.recycler_item_tag_list).apply {
            this.viewModel = this@TagManagerActivity.viewModel
            setEmptyView(LayoutNoDataBinding.inflate(LayoutInflater.from(context)).apply {
                viewModel = NoDataModel(R.string.no_data_record)
            }.root)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_manager)

        // 配置 RecyclerView
        binding.rvTag.run {
            layoutManager = FlowLayoutManager()
            adapter = this@TagManagerActivity.adapter
        }
    }

    override fun onStart() {
        super.onStart()

        // 刷新标签列表
        viewModel.refreshTagList()
    }

    override fun doObserve() {
        // 标签列表
        viewModel.tagListData.observe(this) { list ->
            adapter.submitList(list)
        }
        // 显示新建标签弹窗
        viewModel.showCreateTagDialogEvent.observe(this) {
            EditTagDialog.actionShow(supportFragmentManager, callback = { tag ->
                viewModel.insertTag(tag)
            })
        }
        // 显示菜单
        viewModel.showMenuEvent.observe(this) { tag ->
            val viewHolder = binding.rvTag.findViewHolderForAdapterPosition(
                adapter.mDiffer.currentList.indexOf(tag)
            )
            if (null != viewHolder && viewHolder is SimpleRvListAdapter.ViewHolder<*>) {
                (viewHolder.mBinding as? RecyclerItemTagListBinding)?.root?.let { anchor ->
                    PopupMenu(this, anchor).run {
                        inflate(R.menu.menu_tag_list_more)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.modify -> {
                                    // 修改
                                    EditTagDialog.actionShow(
                                        supportFragmentManager,
                                        tag
                                    ) { modified ->
                                        viewModel.updateTag(modified)
                                    }
                                }
                                R.id.delete -> {
                                    // 删除
                                    viewModel.deleteTag(tag)
                                }
                                R.id.statistics -> {
                                    // 统计数据
                                    TagRecordActivity.actionStart(this@TagManagerActivity, tag)
                                }
                            }
                            true
                        }
                        show()
                    }
                }
            }
        }
    }
}