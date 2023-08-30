package cn.wj.android.cashbook.ui.main.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.toHexString
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.biometric.biometric
import cn.wj.android.cashbook.biometric.supportBiometric
import cn.wj.android.cashbook.biometric.tryAuthenticate
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_SETTING
import cn.wj.android.cashbook.data.enums.DayNightEnum
import cn.wj.android.cashbook.data.live.CurrentDayNightLiveData
import cn.wj.android.cashbook.data.live.PasswordLiveData
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.ActivitySettingBinding
import cn.wj.android.cashbook.ui.main.dialog.EditPasswordDialog
import cn.wj.android.cashbook.ui.main.dialog.VerifyPasswordDialog
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

        // 是否支持指纹
        viewModel.supportFingerprint.value = supportBiometric()
    }

    private var dayNightIndex = DayNightEnum.indexOf(CurrentDayNightLiveData.currentDayNight)

    override fun doObserve() {
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
        // 显示编辑密码弹窗
        viewModel.showEditPasswordDialogEvent.observe(this) {
            EditPasswordDialog.actionShow(supportFragmentManager)
        }
        // 显示清除密码弹窗
        viewModel.showClearPasswordDialogEvent.observe(this) {
            VerifyPasswordDialog.actionShow(
                supportFragmentManager,
                R.string.clear_password_hint.string,
                {
                    // 验证成功，清除密码
                    PasswordLiveData.value = ""
                    viewModel.enableVerifyWhenOpen.value = false
                    viewModel.verifyByFingerprint.value = false
                }
            )
        }
        // 显示验证密码弹窗
        viewModel.showVerifyPasswordDialogEvent.observe(this) {
            VerifyPasswordDialog.actionShow(
                supportFragmentManager,
                R.string.verify_password_for_fingerprint_hint.string,
                {
                    // 验证成功，开始指纹验证
                    biometric.run {
                        encrypt = true
                        subTitle = R.string.verify_fingerprint_to_open.string
                        tryAuthenticate({ cipher ->
                            // 验证成功，加密用户密码
                            val result =
                                cipher.doFinal(PasswordLiveData.value?.toByteArray()).toHexString()
                            // 保存加密信息
                            AppConfigs.encryptedInformation = result
                            AppConfigs.encryptedVector = cipher.iv.toHexString()
                            viewModel.verifyByFingerprint.value = true
                            viewModel.snackbarEvent.value =
                                R.string.open_fingerprint_verify_success.string.toSnackbarModel()
                        }, { _, msg ->
                            // 验证失败
                            viewModel.verifyByFingerprint.value = false
                            viewModel.snackbarEvent.value = msg.toSnackbarModel()
                        })
                    }
                }, {
                    viewModel.verifyByFingerprint.value = false
                }
            )
        }

        PasswordLiveData.observe(this) {
            // 密码变化，清除指纹验证信息
            if (firstChange) {
                firstChange = false
            } else {
                viewModel.verifyByFingerprint.value = false
            }
        }
    }

    private var firstChange = true

}