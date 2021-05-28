package cn.wj.android.cashbook.ui.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.AROUTER_PATH_MAIN
import cn.wj.android.cashbook.data.constants.SPLASH_WAIT_MS
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import java.util.Date
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 闪屏界面 ViewModel
 *
 * @param local 本地数据存储对象
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/16
 */
class SplashViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 信息字符串 */
    val infoStr: ObservableField<String> = ObservableField(
        "${R.string.app_name.string}\n" +
                "${BuildConfig.VERSION_NAME}\n" +
                "©2021 - ${Date().dateFormat("yyyy")} By WangJie0822"
    )

    /** 初始化相关数据 */
    fun init() {
        viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            try {
                // 获取默认账本
                val books = local.getDefaultBooks()
                CurrentBooksLiveData.value = if (null != books) {
                    books
                } else {
                    // 没有默认账本新增
                    val currentTime = startMs.dateFormat()
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
                    val insertId = local.insertBooks(default)
                    default.copy(id = insertId)
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "init")
            } finally {
                // 消耗的时间
                val spendMs = (System.currentTimeMillis() - startMs).absoluteValue
                logger().d("init spendMs: $spendMs")
                if (spendMs < SPLASH_WAIT_MS) {
                    // 耗时小于等待时间，等待凑足时长
                    delay(SPLASH_WAIT_MS - spendMs)
                }
                // 跳转首页并关闭启动页
                uiNavigationData.value = UiNavigationModel.builder {
                    jump(AROUTER_PATH_MAIN)
                    close()
                }
            }
        }
    }
}