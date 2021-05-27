package cn.wj.android.cashbook.ui.viewmodel

import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.AROUTER_PATH_MAIN
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import kotlinx.coroutines.launch

/**
 * 闪屏界面 ViewModel
 *
 * @param local 本地数据存储对象
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/16
 */
class SplashViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 初始化相关数据 */
    fun init() {
        viewModelScope.launch {
            try {
                // 获取默认账本
                val books = local.getDefaultBooks()
                CurrentBooksLiveData.value = if (null != books) {
                    books
                } else {
                    // 没有默认账本新增
                    val currentTime = System.currentTimeMillis().dateFormat()
                    val default = BooksEntity(
                        -1,
                        R.string.default_books.string,
                        "",
                        "日常账本",
                        null,
                        true,
                        currentTime,
                        currentTime
                    )
                    local.insertBooks(default)
                    default
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "init")
            } finally {
                // 跳转首页
                uiNavigationData.value = UiNavigationModel.builder {
                    jump(AROUTER_PATH_MAIN)
                    close()
                }
            }
        }
    }
}