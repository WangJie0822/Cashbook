package cn.wj.android.cashbook.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ConcatAdapter
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_BOOKS
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MY_BOOKS
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.databinding.ActivityMyBooksBinding
import cn.wj.android.cashbook.databinding.RecyclerItemBooksBinding
import cn.wj.android.cashbook.ui.adapter.BlankFooterAdapter
import cn.wj.android.cashbook.ui.dialog.GeneralDialog
import cn.wj.android.cashbook.ui.viewmodel.MyBooksViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.ADAPTER_ANIM_ALL
import cn.wj.android.cashbook.widget.recyclerview.adapter.ADAPTER_ANIM_CHANGED
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 我的账本界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于2021/5/25
 */
@Route(path = ROUTE_PATH_MY_BOOKS)
class MyBooksActivity : BaseActivity<MyBooksViewModel, ActivityMyBooksBinding>() {

    override val viewModel: MyBooksViewModel by viewModel()

    /** 账本列表适配器 */
    private val booksListRvAdapter = SimpleRvListAdapter<BooksEntity>(
        layoutResId = R.layout.recycler_item_books,
        anim = ADAPTER_ANIM_ALL xor ADAPTER_ANIM_CHANGED,
        areItemsTheSame = { old, new -> old.id == new.id }
    )

    /** 编辑账本 launcher */
    private lateinit var editBooksResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_books)

        // 注册 launcher
        editBooksResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && null != result.data) {
                result.data?.getParcelableExtra<BooksEntity>(ACTION_BOOKS)?.let { newBooks ->
                    if (newBooks.id == -1L) {
                        // 新增
                        viewModel.insertBooks(newBooks)
                    } else {
                        // 编辑
                        viewModel.updateBooks(newBooks)
                    }
                }
            }
        }

        // 配置 RecyclerView
        binding.rv.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = ConcatAdapter(booksListRvAdapter.apply {
                this.viewModel = this@MyBooksActivity.viewModel
            }, BlankFooterAdapter())
        }

        // 加载数据
        viewModel.loadBooksList()
    }

    override fun observe() {
        // 账本列表
        viewModel.booksListData.observe(this, { list ->
            booksListRvAdapter.submitList(list)
        })
        // PopupMenu 弹窗
        viewModel.showPopupMenuData.observe(this, { item ->
            val viewHolder = binding.rv.findViewHolderForAdapterPosition(
                booksListRvAdapter.mDiffer.currentList.indexOf(item)
            )
            if (null != viewHolder && viewHolder is SimpleRvListAdapter.ViewHolder<*>) {
                (viewHolder.mBinding as? RecyclerItemBooksBinding)?.ivMore?.let { anchor ->
                    PopupMenu(context, anchor).run {
                        inflate(R.menu.my_books_more)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.modify -> {
                                    // 修改
                                    editBooksResultLauncher.launch(EditBooksActivity.parseIntent(context, item))
                                }
                                R.id.delete -> {
                                    // 删除
                                    GeneralDialog.newBuilder()
                                        .contentStr(R.string.delete_books_confirm.string)
                                        .setOnPositiveAction {
                                            // 删除账本
                                            viewModel.deleteBooks(item)
                                        }.show(this@MyBooksActivity)
                                }
                            }
                            true
                        }
                        show()
                    }
                }
            }
        })
        // 跳转新增
        viewModel.jumpToAddBooksData.observe(this, {
            editBooksResultLauncher.launch(EditBooksActivity.parseIntent(context))
        })
    }
}