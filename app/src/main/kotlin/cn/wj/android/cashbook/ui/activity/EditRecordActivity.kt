package cn.wj.android.cashbook.ui.activity

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_EDIT_RECORD
import cn.wj.android.cashbook.databinding.ActivityEditRecordBinding
import cn.wj.android.cashbook.ui.adapter.EditRecordVpAdapter
import cn.wj.android.cashbook.ui.viewmodel.EditRecordViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import com.google.android.material.tabs.TabLayout
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑记录界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
@Route(path = ROUTE_PATH_EDIT_RECORD)
class EditRecordActivity : BaseActivity<EditRecordViewModel, ActivityEditRecordBinding>() {

    override val viewModel: EditRecordViewModel by viewModel()

    /** 消费类型 ViewPager 适配器 */
    private val typesVpAdapter: EditRecordVpAdapter by lazy {
        EditRecordVpAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_record)

        binding.tabs.run {
            addTab(newTab().setText("支出"))
            addTab(newTab().setText("收入"))
            addTab(newTab().setText("转账"))
        }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
        binding.vpType.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {

            }
        })
    }
}