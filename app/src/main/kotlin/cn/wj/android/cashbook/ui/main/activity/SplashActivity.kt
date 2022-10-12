package cn.wj.android.cashbook.ui.main.activity

import android.os.Bundle
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.hexToBytes
import cn.wj.android.cashbook.base.ext.showSoftKeyboard
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.biometric.biometric
import cn.wj.android.cashbook.biometric.tryAuthenticate
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.ACTIVITY_ANIM_DURATION
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.ActivitySplashBinding
import cn.wj.android.cashbook.ui.general.dialog.GeneralDialog
import cn.wj.android.cashbook.ui.main.viewmodel.SplashViewModel
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.gyf.immersionbar.ImmersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 闪屏界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/16
 */
class SplashActivity : BaseActivity<SplashViewModel, ActivitySplashBinding>() {

    override val viewModel: SplashViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 初始化数据
        viewModel.init(intent.action)
    }

    override fun beforeOnCreate() {
        window.run {
            enterTransition = MaterialFadeThrough().apply {
                duration = ACTIVITY_ANIM_DURATION
            }
            exitTransition = MaterialFadeThrough().apply {
                duration = ACTIVITY_ANIM_DURATION
            }
        }
    }

    override fun initImmersionbar(immersionBar: ImmersionBar) {
        immersionBar.run {
            transparentStatusBar()
            fitsSystemWindows(false)
        }
    }

    override fun doObserve() {
        // 显示隐私政策弹窗
        viewModel.showPrivacyPolicyDialogEvent.observe(this) {
            GeneralDialog.newBuilder()
                .titleStr(R.string.user_agreement_and_privacy_policy.string)
                .contentStr(R.string.user_agreement_and_privacy_policy_hint.string) { ss ->
                    val content = ss.toString()
                    val keyword = R.string.user_agreement_and_privacy_policy_with_chevron.string
                    val start = content.indexOf(keyword)
                    val end = start + keyword.length
                    ss.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            // 加载隐私政策
                            viewModel.loadPrivacyPolicy()
                        }
                    }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                .setNegativeAction(R.string.disagree.string) {
                    // 不同意
                    finish()
                }
                .setPositiveAction(R.string.agree.string) {
                    // 同意，保存状态
                    AppConfigs.agreeUserAgreement = true
                    // 跳转首页并关闭当前界面
                    viewModel.jumpToMainAndFinish()
                }
                .show(supportFragmentManager)
        }
        // 拉起指纹认证
        viewModel.fingerprintVerifyEvent.observe(this) {
            biometric.run {
                // 已加密的数据
                val encodedData = AppConfigs.encryptedInformation.hexToBytes()
                ivBytes = AppConfigs.encryptedVector.hexToBytes()
                encrypt = false
                subTitle = R.string.verify_fingerprint_to_open.string
                tryAuthenticate({ cipher ->
                    // 验证成功，解密用户信息
                    val result = cipher.doFinal(encodedData)
                    val pwd = result.decodeToString()
                    if (pwd != AppConfigs.password) {
                        // 密码错误
                        viewModel.snackbarEvent.value =
                            R.string.verify_failed.string.toSnackbarModel()
                        // 清除指纹相关信息
                        AppConfigs.verifyByFingerprint = false
                    } else {
                        // 密码正确，保存验证状态，跳转
                        AppConfigs.verified = true
                        viewModel.doJumpToMain()
                    }
                }, { _, msg ->
                    // 验证失败
                    viewModel.snackbarEvent.value = msg.toSnackbarModel()
                })
            }
        }
        // 显示软键盘
        viewModel.showSoftKeyboardEvent.observe(this) {
            binding.tietPassword.showSoftKeyboard()
        }
    }
}