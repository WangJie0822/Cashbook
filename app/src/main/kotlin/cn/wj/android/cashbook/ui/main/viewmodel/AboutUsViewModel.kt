package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.EMAIL_ADDRESS
import cn.wj.android.cashbook.data.constants.GITEE_HOMEPAGE
import cn.wj.android.cashbook.data.constants.GITHUB_HOMEPAGE
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.transform.toSnackbarModel

/**
 * 关于我们 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/17
 */
class AboutUsViewModel : BaseViewModel() {

    /** 跳转发送邮件数据 */
    val jumpSendEmailData: MutableLiveData<String> = MutableLiveData()

    /** 跳转浏览器打开数据 */
    val jumpBrowserData: MutableLiveData<String> = MutableLiveData()

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 联系我点击 */
    val onContactMeClick: () -> Unit = {
        jumpSendEmailData.value = EMAIL_ADDRESS
    }

    /** Github 点击 */
    val onGithubClick: () -> Unit = {
        jumpBrowserData.value = GITHUB_HOMEPAGE
    }

    /** Gitee 点击 */
    val onGiteeClick: () -> Unit = {
        jumpBrowserData.value = GITEE_HOMEPAGE
    }

    /** TODO 检查更新点击 */
    val onCheckUpdateClick: () -> Unit = {
        snackbarData.value = "检查更新".toSnackbarModel()
    }

    /** TODO 用户协议和隐私协议点击 */
    val onUserAgreementAndPrivacyPolicyClick: () -> Unit = {
        snackbarData.value = "用户协议和隐私协议".toSnackbarModel()
    }

}