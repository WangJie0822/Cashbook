package cn.wj.android.cashbook.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_BOOKS
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_EDIT_BOOKS
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.databinding.ActivityEditBooksBinding
import cn.wj.android.cashbook.ui.viewmodel.EditBooksViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑账本界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/27
 */
@Route(path = ROUTE_PATH_EDIT_BOOKS)
class EditBooksActivity : BaseActivity<EditBooksViewModel, ActivityEditBooksBinding>() {

    override val viewModel: EditBooksViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_books)

        // 获取传递数据
        viewModel.oldBooks = intent.getParcelableExtra(ACTION_BOOKS)
    }

    companion object {

        /** 使用 [context] 开启生成跳转编辑账本界面 [Intent]，修改时传递 [books] 对象，新增为 `null` */
        fun parseIntent(context: Context, books: BooksEntity? = null): Intent = Intent(context, EditBooksActivity::class.java).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            putExtra(ACTION_BOOKS, books)
        }
    }
}