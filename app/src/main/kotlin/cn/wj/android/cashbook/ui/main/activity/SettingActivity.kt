package cn.wj.android.cashbook.ui.main.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_SETTING
import cn.wj.android.cashbook.data.enums.DayNightEnum
import cn.wj.android.cashbook.data.live.CurrentDayNightLiveData
import cn.wj.android.cashbook.databinding.ActivitySettingBinding
import cn.wj.android.cashbook.ui.main.viewmodel.SettingViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 设置界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/22
 */
@Route(path = ROUTE_PATH_SETTING)
class SettingActivity : BaseActivity<SettingViewModel, ActivitySettingBinding>() {

    override val viewModel: SettingViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
    }

    private var dayNightIndex = DayNightEnum.indexOf(CurrentDayNightLiveData.currentDayNight)

    override fun observe() {
        // 显示选择主题弹窗
        viewModel.showSelectDayNightDialogEvent.observe(this) {
            dayNightIndex = DayNightEnum.indexOf(CurrentDayNightLiveData.currentDayNight)
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.please_select_application_day_night)
                .setSingleChoiceItems(DayNightEnum.getSelectItems(), dayNightIndex) { _, which ->
                    dayNightIndex = which
                }
                .setPositiveButton(R.string.confirm) { _, _ ->
                    // 更新白天黑夜模式
                    CurrentDayNightLiveData.value = DayNightEnum.fromIndex(dayNightIndex)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}