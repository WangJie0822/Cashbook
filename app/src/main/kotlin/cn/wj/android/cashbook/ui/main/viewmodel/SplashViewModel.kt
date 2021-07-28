package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.core.os.bundleOf
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.getSharedBoolean
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_CONTENT
import cn.wj.android.cashbook.data.constants.ACTION_TITLE
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MAIN
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MARKDOWN
import cn.wj.android.cashbook.data.constants.SHARED_KEY_AGREE_USER_AGREEMENT
import cn.wj.android.cashbook.data.constants.SHARED_KEY_USE_GITEE
import cn.wj.android.cashbook.data.constants.SPLASH_WAIT_MS
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.main.MainRepository
import cn.wj.android.cashbook.manager.DatabaseManager
import java.util.Date
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    /** 初始化相关数据 */
    fun init() {
        viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            try {
                // 初始化数据数据
                DatabaseManager.initDatabase(repository.database)
            } catch (throwable: Throwable) {
                logger().e(throwable, "init")
            } finally {
                if (getSharedBoolean(SHARED_KEY_AGREE_USER_AGREEMENT)) {
                    // 已同意隐私政策
                    // 消耗的时间
                    val spendMs = (System.currentTimeMillis() - startMs).absoluteValue
                    logger().d("init spendMs: $spendMs")
                    if (spendMs < SPLASH_WAIT_MS) {
                        // 耗时小于等待时间，等待凑足时长
                        delay(SPLASH_WAIT_MS - spendMs)
                    }
                    // 跳转首页并关闭启动页
                    uiNavigationEvent.value = UiNavigationModel.builder {
                        jump(ROUTE_PATH_MAIN)
                        close()
                    }
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
                val privacyPolicy = repository.getPrivacyPolicy(getSharedBoolean(SHARED_KEY_USE_GITEE, true))
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
}