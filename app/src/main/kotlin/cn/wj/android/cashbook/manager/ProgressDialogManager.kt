package cn.wj.android.cashbook.manager

import android.annotation.SuppressLint
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cn.wj.android.cashbook.ui.general.dialog.ProgressDialog
import com.orhanobut.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 加载进度帮助类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/17
 */
object ProgressDialogManager {

    /** 弹窗对象  */
    @SuppressLint("StaticFieldLeak")
    private var mDialog: ProgressDialog? = null

    /** 使用 [activity] 显示弹窗，传递参数 能否取消[cancelable] & 提示文本[hint] */
    private fun showDialog(activity: FragmentActivity, cancelable: Boolean, hint: String) {
        // 显示前先隐藏
        dismissDialog()
        // 创建并显示 Dialog
        mDialog = ProgressDialog.actionShow(activity, cancelable, hint)
    }

    /** 隐藏弹窗 */
    private fun dismissDialog() {
        if (null == mDialog) {
            return
        }
        try {
            // 隐藏弹窗
            mDialog!!.dismiss()
            // 移除引用
            mDialog = null
        } catch (e: Exception) {
            Logger.t("ProgressDialogHelper").e(e, "dismissDialog")
        }
    }

    /**
     * 使用 [activity] 显示弹窗，传递参数 能否取消 [cancelable] & 提示文本 [hint]
     * > [cancelable] 默认 `true` & [hint] 默认 `""`
     */
    fun show(activity: FragmentActivity, cancelable: Boolean = true, hint: String = "") {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                showDialog(activity, cancelable, hint)
            }
        }
    }

    /** 隐藏弹窗 */
    fun dismiss() {
        mDialog?.activity?.lifecycleScope?.launch {
            withContext(Dispatchers.Main) {
                dismissDialog()
            }
        }
    }
}