package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.getSharedBoolean
import cn.wj.android.cashbook.base.tools.setSharedBoolean
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_CONTENT
import cn.wj.android.cashbook.data.constants.ACTION_TITLE
import cn.wj.android.cashbook.data.constants.EMAIL_ADDRESS
import cn.wj.android.cashbook.data.constants.GITEE_HOMEPAGE
import cn.wj.android.cashbook.data.constants.GITHUB_HOMEPAGE
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MARKDOWN
import cn.wj.android.cashbook.data.constants.SHARED_KEY_AUTO_CHECK_UPDATE
import cn.wj.android.cashbook.data.constants.SHARED_KEY_USE_GITEE
import cn.wj.android.cashbook.data.entity.UpdateInfoEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.main.MainRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.manager.UpdateManager
import kotlinx.coroutines.launch

/**
 * 关于我们 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/17
 */
class AboutUsViewModel(private val repository: MainRepository) : BaseViewModel() {

    /** 显示升级提示事件 */
    val showUpdateDialogEvent: LifecycleEvent<UpdateInfoEntity> = LifecycleEvent()

    /** 跳转发送邮件事件 */
    val jumpSendEmailEvent: LifecycleEvent<String> = LifecycleEvent()

    /** 跳转浏览器打开事件 */
    val jumpBrowserEvent: LifecycleEvent<String> = LifecycleEvent()

    /** 是否使用 Gitee */
    val useGitee: MutableLiveData<Boolean> = object : MutableLiveData<Boolean>() {

        override fun onActive() {
            if (null == value) {
                value = getSharedBoolean(SHARED_KEY_USE_GITEE, true)
            }
        }

        override fun setValue(value: Boolean?) {
            super.setValue(value)
            if (null != value) {
                setSharedBoolean(SHARED_KEY_USE_GITEE, value)
            }
        }
    }

    /** 标记 - 是否自动检查更新 */
    val autoCheckUpdate: MutableLiveData<Boolean> = object : MutableLiveData<Boolean>() {

        override fun onActive() {
            if (null == value) {
                value = getSharedBoolean(SHARED_KEY_AUTO_CHECK_UPDATE, true)
            }
        }

        override fun setValue(value: Boolean?) {
            super.setValue(value)
            if (null != value) {
                setSharedBoolean(SHARED_KEY_AUTO_CHECK_UPDATE, value)
            }
        }
    }

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 联系我点击 */
    val onContactMeClick: () -> Unit = {
        jumpSendEmailEvent.value = EMAIL_ADDRESS
    }

    /** Github 点击 */
    val onGithubClick: () -> Unit = {
        jumpBrowserEvent.value = GITHUB_HOMEPAGE
    }

    /** Gitee 点击 */
    val onGiteeClick: () -> Unit = {
        jumpBrowserEvent.value = GITEE_HOMEPAGE
    }

    /** 检查更新点击 */
    val onCheckUpdateClick: () -> Unit = {
        checkUpdate()
    }

    /** 版本信息点击 */
    val onVersionInfoClick: () -> Unit = {
        loadChangelog()
    }

    /** 用户协议和隐私协议点击 */
    val onUserAgreementAndPrivacyPolicyClick: () -> Unit = {
        loadPrivacyPolicy()
    }

    /** 检查更新 */
    private fun checkUpdate() {
        if (UpdateManager.downloading) {
            // 下载中
            snackbarEvent.value = R.string.update_downloading_hint.string.toSnackbarModel()
            return
        }
        viewModelScope.launch {
            try {
                // 获取 Release 信息
                val info = repository.queryLatestRelease(useGitee.value.condition)
                UpdateManager.checkFromInfo(info, {
                    // 显示升级提示弹窗
                    showUpdateDialogEvent.value = info
                }, {
                    // 不需要升级
                    snackbarEvent.value = R.string.it_is_the_latest_version.string.toSnackbarModel()
                })
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkUpdate")
            }
        }
    }

    /** 加载修改日志信息 */
    private fun loadChangelog() {
        viewModelScope.launch {
            try {
                val changelog = repository.getChangelog(useGitee.value.condition)
                logger().d("loadChangelog: $changelog")
                // 跳转 Markdown 界面打开
                uiNavigationEvent.value = UiNavigationModel.builder {
                    jump(
                        ROUTE_PATH_MARKDOWN, bundleOf(
                            ACTION_TITLE to R.string.changelog.string,
                            ACTION_CONTENT to changelog
                        )
                    )
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadChangelog")
            }
        }
    }

    /** 加载用户协议和隐私政策数据 */
    private fun loadPrivacyPolicy() {
        viewModelScope.launch {
            try {
                val privacyPolicy = repository.getPrivacyPolicy(useGitee.value.condition)
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