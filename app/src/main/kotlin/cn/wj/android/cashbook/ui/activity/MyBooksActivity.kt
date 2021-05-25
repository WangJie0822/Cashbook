package cn.wj.android.cashbook.ui.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.AROUTER_PATH_MY_BOOKS
import cn.wj.android.cashbook.databinding.ActivityMyBooksBinding
import cn.wj.android.cashbook.ui.viewmodel.MyBooksViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 我的账本界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于2021/5/25
 */
@Route(path = AROUTER_PATH_MY_BOOKS)
class MyBooksActivity : BaseActivity<MyBooksViewModel, ActivityMyBooksBinding>() {

    override val viewModel: MyBooksViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_books)
    }
}