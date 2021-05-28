package cn.wj.android.cashbook.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import cn.wj.android.cashbook.ui.fragment.ConsumptionTypeFragment

/**
 * 编辑记录界面 ViewPager 适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
class EditRecordVpAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return ConsumptionTypeFragment.newInstance()
    }

}