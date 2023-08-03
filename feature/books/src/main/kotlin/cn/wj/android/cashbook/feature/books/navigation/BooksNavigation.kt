package cn.wj.android.cashbook.feature.books.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wj.android.cashbook.feature.books.screen.EditBookRoute
import cn.wj.android.cashbook.feature.books.screen.MyBooksRoute

private const val ROUTE_KEY_BOOK_ID = "bookId"

/** 路由 - 我的账本 */
private const val ROUTE_BOOKS_MY_BOOKS = "/books/my"

/** 路由 - 编辑账本 */
private const val ROUTE_BOOKS_EDIT_BOOK = "/books/edit?$ROUTE_KEY_BOOK_ID={$ROUTE_KEY_BOOK_ID}"

/** 跳转到我的账本 */
fun NavController.naviToMyBooks() {
    this.navigate(ROUTE_BOOKS_MY_BOOKS)
}

/** 跳转到编辑账本 */
fun NavController.naviToEditBook(bookId: Long = -1L) {
    this.navigate(ROUTE_BOOKS_EDIT_BOOK.replace("{$ROUTE_KEY_BOOK_ID}", bookId.toString()))
}

/**
 * 我的账本
 */
fun NavGraphBuilder.myBooksScreen(
    onEditBookClick: (Long) -> Unit,
    onBackClick: () -> Unit,
) {
    composable(route = ROUTE_BOOKS_MY_BOOKS) {
        MyBooksRoute(
            onEditBookClick = onEditBookClick,
            onBackClick = onBackClick,
        )
    }
}

/**
 * 编辑账本
 */
fun NavGraphBuilder.editBookScreen(
    onBackClick: () -> Unit,
) {
    composable(
        route = ROUTE_BOOKS_EDIT_BOOK,
        arguments = listOf(navArgument(ROUTE_KEY_BOOK_ID) {
            type = NavType.LongType
            defaultValue = -1L
        })
    ) {
        val bookId = it.arguments?.getLong(ROUTE_KEY_BOOK_ID) ?: -1L
        EditBookRoute(
            bookId = bookId,
            onBackClick = onBackClick,
        )
    }
}