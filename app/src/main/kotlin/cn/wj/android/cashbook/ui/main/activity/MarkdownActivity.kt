package cn.wj.android.cashbook.ui.main.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.md2Spanned
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_CONTENT
import cn.wj.android.cashbook.data.constants.ACTION_TITLE
import cn.wj.android.cashbook.databinding.ActivityMarkdownBinding
import cn.wj.android.cashbook.ui.main.viewmodel.MarkdownViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 显示 Markdown 信息界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/21
 */
class MarkdownActivity : BaseActivity<MarkdownViewModel, ActivityMarkdownBinding>() {

    override val viewModel: MarkdownViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown)

        // 获取显示信息
        viewModel.titleStr.value = intent.getStringExtra(ACTION_TITLE).orEmpty()
        viewModel.contentStr.value = intent.getStringExtra(ACTION_CONTENT).orEmpty().md2Spanned()
    }
}