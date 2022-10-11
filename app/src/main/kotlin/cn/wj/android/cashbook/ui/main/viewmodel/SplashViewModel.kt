package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.core.os.bundleOf
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.shaEncode
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.*
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.main.MainRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.manager.DatabaseManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.absoluteValue

/**
 * 闪屏界面 ViewModel
 *
 * @param repository 本地数据存储对象
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/16
 */
class SplashViewModel(private val repository: MainRepository) : BaseViewModel() {

    /** 信息字符串 */
    val infoStr: ObservableField<String> = ObservableField(
        "${R.string.app_name.string}\n" +
                "${BuildConfig.VERSION_NAME}\n" +
                "©2021 - ${Date().dateFormat("yyyy")} By WangJie0822"
    )

    /** 显示隐私政策弹窗事件 */
    val showPrivacyPolicyDialogEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 显示软键盘事件 */
    val showSoftKeyboardEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 界面启动 action */
    private var action: String? = null

    /** 是否需要验证 */
    val needVerify: MutableLiveData<Boolean> = MutableLiveData()

    /** 是否支持指纹登录 */
    val supportFingerprint: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 密码 */
    val password: MutableLiveData<String> = MutableLiveData()

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        val pwd = password.value.orEmpty()
        if (pwd.shaEncode() != AppConfigs.password) {
            // 密码错误
            snackbarEvent.value = R.string.please_enter_right_password.string.toSnackbarModel()
        } else {
            // 密码正确，保存验证状态，跳转
            AppConfigs.verified = true
            doJumpToMain()
        }
    }

    /** 指纹点击 */
    val onFingerprintClick: () -> Unit = {

    }

    /** 初始化相关数据 */
    fun init(action: String?) {
        logger().i("init(action = [$action])")
        this.action = action
        viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            try {
                // 初始化数据数据
                DatabaseManager.initDatabase(repository.database)
            } catch (throwable: Throwable) {
                logger().e(throwable, "init")
            } finally {
                if (AppConfigs.agreeUserAgreement) {
                    // 已同意隐私政策
                    val isShortcuts = action in arrayOf(SHORTCUTS_ASSET, SHORTCUTS_RECORD)
                    // 消耗的时间
                    val spendMs = (System.currentTimeMillis() - startMs).absoluteValue
                    logger().d("init isShortcuts: $isShortcuts spendMs: $spendMs")
                    if (!isShortcuts && spendMs < SPLASH_WAIT_MS) {
                        // 耗时小于等待时间，等待凑足时长
                        delay(SPLASH_WAIT_MS - spendMs)
                    }
                    jumpToMainAndFinish()
                } else {
                    // 显示隐私政策弹窗
                    showPrivacyPolicyDialogEvent.value = 0
                }
            }
        }
    }

    /** 加载用户协议和隐私政策数据 */
    fun loadPrivacyPolicy() {
        viewModelScope.launch {
            try {
                val privacyPolicy = repository.getPrivacyPolicy(AppConfigs.useGitee)
                logger().d("loadPrivacyPolicy: $privacyPolicy")
                // 跳转 Markdown 界面打开
                uiNavigationEvent.value = UiNavigationModel.builder {
                    jump(
                        ROUTE_PATH_MARKDOWN, bundleOf(
                            ACTION_TITLE to R.string.user_agreement_and_privacy_policy.string,
                            ACTION_CONTENT to privacyPolicy
                        )
                    )
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadPrivacyPolicy")
            }
        }
    }

    /** 跳转首页并关闭启动页 */
    fun jumpToMainAndFinish() {
        if (AppConfigs.needVerifyWhenOpen && AppConfigs.password.isNotBlank() && !AppConfigs.verified) {
            // 需要认证、密码不为空且未认证
            needVerify.value = true
            // 显示软键盘
            showSoftKeyboardEvent.value = 0
        } else {
            doJumpToMain()
        }
    }

    private fun doJumpToMain() {
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(
                ROUTE_PATH_MAIN,
                bundleOf(
                    ACTION_CONTENT to action
                )
            )
            close()
        }
    }
}