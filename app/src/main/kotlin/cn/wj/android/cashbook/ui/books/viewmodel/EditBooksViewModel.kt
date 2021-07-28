package cn.wj.android.cashbook.ui.books.viewmodel

import androidx.core.os.bundleOf
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_BOOKS
import cn.wj.android.cashbook.data.constants.ACTIVITY_RESULT_OK
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.enums.CurrencyEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.books.BooksRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import kotlinx.coroutines.launch

/**
 * 编辑账本 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/27
 */
class EditBooksViewModel(private val repository: BooksRepository) : BaseViewModel() {

    /** 旧数据 */
    var oldBooks: BooksEntity? = null
        set(value) {
            field = value
            value?.let { books ->
                imgPreviewUrl.set(books.imageUrl)
                booksName.set(books.name)
                booksDescription.set(books.description)
                currencyData.set(books.currency)
            }
        }

    /** 设置顶部图片预览 */
    val imgPreviewUrl: ObservableField<String> = ObservableField()

    /** 账本名称 */
    val booksName: ObservableField<String> = ObservableField()

    /** 账本名称错误提示 */
    val booksNameError: ObservableField<String> = ObservableField()

    /** 账本描述 */
    val booksDescription: ObservableField<String> = ObservableField()

    /** 账本描述错误提示 */
    val booksDescriptionError: ObservableField<String> = ObservableField()

    /** 货币信息 */
    val currencyData: ObservableField<CurrencyEnum> = ObservableField()

    /** 货币聚合信息 */
    val currencyStr: ObservableField<String> = object : ObservableField<String>(currencyData) {
        override fun get(): String {
            val currency = currencyData.get()
            return currency?.summary ?: CurrencyEnum.CNY.summary
        }
    }

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        // 检查数据并保存
        checkToSave()
    }

    /** TODO 货币点击 */
    val onCurrencyClick: () -> Unit = {
        snackbarEvent.value = "修改货币".toSnackbarModel()
    }

    /** 检查数据并保存 */
    private fun checkToSave() {
        viewModelScope.launch {
            try {
                val name = booksName.get()
                if (name.isNullOrBlank()) {
                    // 账本名为空
                    booksNameError.set(R.string.books_name_cannot_be_empty.string)
                    return@launch
                }
                if (null == oldBooks) {
                    // 新增数据，判断名称是否已存在
                    if (repository.hasBooksCountByName(name)) {
                        booksNameError.set(R.string.books_name_already_exists.string)
                        return@launch
                    }
                }
                val imgUrl = imgPreviewUrl.get().orEmpty()
                val description = booksDescription.get().orEmpty()
                val currency = currencyData.get()
                val currentTime = System.currentTimeMillis().dateFormat()
                val resultData = if (null == oldBooks) {
                    // 新增
                    BooksEntity(
                        id = -1,
                        name = name,
                        imageUrl = imgUrl,
                        description = description,
                        currency = currency,
                        selected = false,
                        createTime = currentTime,
                        modifyTime = currentTime
                    )
                } else {
                    oldBooks!!.copy(
                        name = name,
                        imageUrl = imgUrl,
                        description = description,
                        currency = currency,
                        modifyTime = currentTime
                    )
                }
                // 设置返回数据并关闭当前界面
                uiNavigationEvent.value = UiNavigationModel.builder {
                    close(
                        resultCode = ACTIVITY_RESULT_OK,
                        result = bundleOf(
                            ACTION_BOOKS to resultData
                        )
                    )
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkToSave")
            }
        }

    }
}