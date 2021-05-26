package cn.wj.android.cashbook.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import kotlinx.coroutines.launch

/**
 * 我的账本 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于2021/5/25
 */
class MyBooksViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 账本列表数据 */
    val booksListData: MutableLiveData<ArrayList<BooksEntity>> = MutableLiveData()

    /** 显示弹窗数据 */
    val showPopupMenuData: MutableLiveData<BooksEntity> = MutableLiveData()

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        snackbarData.value = "保存修改".toSnackbarModel()
    }

    /** 添加按钮点击 */
    val onAddClick: () -> Unit = {
        booksListData.value?.let { list ->
            val ls = ArrayList(list)
            val time = System.currentTimeMillis().dateFormat()
            ls.add(BooksEntity(-1, "默认${Math.random()}", "", "说明", null, false, time, time))
            booksListData.value = ls
        }
    }

    /** 账本 item 点击 */
    val onBooksItemClick: (BooksEntity) -> Unit = { item ->
        if (!item.selected) {
            // 未选中时才操作
            booksListData.value?.let { list ->
                val ls = ArrayList(list)
                val selectedItem = ls.firstOrNull { it.selected }
                if (null != selectedItem) {
                    val selectedIndex = ls.indexOf(selectedItem)
                    ls[selectedIndex] = selectedItem.copy(selected = false)
                }
                val index = ls.indexOf(item)
                ls[index] = item.copy(selected = true)
                booksListData.value = ls
            }
        }
    }

    /** 账本 item 更多点击 */
    val onBooksItemMoreClick: (BooksEntity) -> Unit = { item ->
        // 显示 PopupMenu 弹窗
        showPopupMenuData.value = item
    }

    /** 加载账本列表 */
    fun loadBooksList() {
        viewModelScope.launch {
            try {
                val ls = arrayListOf<BooksEntity>()
                ls.addAll(local.getBooksList())
                booksListData.value = ls
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadBooksList")
            }
        }
    }
}