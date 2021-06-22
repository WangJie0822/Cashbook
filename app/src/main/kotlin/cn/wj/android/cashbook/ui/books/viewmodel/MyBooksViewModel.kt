package cn.wj.android.cashbook.ui.books.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.add
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.remove
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.toNewList
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
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

    /** 显示弹窗事件 */
    val showPopupMenuEvent: LifecycleEvent<BooksEntity> = LifecycleEvent()

    /** 跳转新增账本事件 */
    val jumpToAddBooksEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 账本列表数据 */
    val booksListData: MutableLiveData<ArrayList<BooksEntity>> = MutableLiveData()

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 添加按钮点击 */
    val onAddClick: () -> Unit = {
        // 跳转新增
        jumpToAddBooksEvent.value = 0
    }

    /** 账本 item 点击 */
    val onBooksItemClick: (BooksEntity) -> Unit = { item ->
        if (!item.selected) {
            // 未选中时才操作，获取已选中账本数据
            val ls = booksListData.value.toNewList()
            val selectedItem = ls.firstOrNull {
                it.selected
            }
            // 更新数据
            updateBooksSelected(
                ls.indexOf(item) to item.copy(selected = true),
                ls.indexOf(selectedItem) to selectedItem?.copy(selected = false)
            )
        }
    }

    /** 账本 item 更多点击 */
    val onBooksItemMoreClick: (BooksEntity) -> Unit = { item ->
        // 显示 PopupMenu 弹窗
        showPopupMenuEvent.value = item
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

    /** 插入 [books] 账本 */
    fun insertBooks(books: BooksEntity) {
        viewModelScope.launch {
            try {
                // 插入数据
                val id = local.insertBooks(books)
                // 插入完成，更新列表
                booksListData.add(books.copy(id = id))
            } catch (throwable: Throwable) {
                logger().e(throwable, "insertBooks")
            }
        }
    }

    /** 删除 [books] 账本 */
    fun deleteBooks(books: BooksEntity) {
        if (books.selected) {
            // 已选择账本不能删除
            snackbarEvent.value = R.string.cannot_delete_selected_books.string.toSnackbarModel()
            return
        }
        viewModelScope.launch {
            try {
                // 删除数据
                local.deleteBooks(books)
                // 删除成功，更新列表
                booksListData.remove(books)
            } catch (throwable: Throwable) {
                logger().e(throwable, "deleteBooks")
            }
        }
    }

    /** 更新 [books] 账本信息 */
    fun updateBooks(books: BooksEntity) {
        viewModelScope.launch {
            // 更新数据
            local.updateBooks(books)
            // 更新成功，刷新列表
            val ls = booksListData.value.toNewList()
            val index = ls.indexOfFirst {
                it.id == books.id
            }
            if (index >= 0) {
                // 有对应数据
                ls[index] = books
                booksListData.value = ls
            }
            // 更新当前账本数据
            CurrentBooksLiveData.value = books
        }
    }

    /** 更新账本选中状态信息 */
    private fun updateBooksSelected(selected: Pair<Int, BooksEntity>, unSelected: Pair<Int, BooksEntity?>) {
        viewModelScope.launch {
            try {
                // 更新数据
                val selectedItem = selected.second
                val unSelectedItem = unSelected.second
                if (null == unSelectedItem) {
                    local.updateBooks(selectedItem)
                } else {
                    local.updateBooks(selectedItem, unSelectedItem)
                }
                // 更新完成，刷新列表
                val ls = booksListData.value.toNewList()
                ls[selected.first] = selectedItem
                if (unSelected.first >= 0 && null != unSelectedItem) {
                    ls[unSelected.first] = unSelectedItem
                }
                booksListData.value = ls
                // 更新当前选中账本
                CurrentBooksLiveData.value = selectedItem
            } catch (throwable: Throwable) {
                logger().e(throwable, "updateBooks")
            }
        }
    }
}