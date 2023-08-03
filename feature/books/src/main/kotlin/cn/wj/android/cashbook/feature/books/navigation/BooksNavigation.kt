package cn.wj.android.cashbook.feature.books.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.feature.books.screen.MyBooksRoute

private const val ROUTE_BOOKS_MY_BOOKS = "/books/my"

/** 跳转到我的账本 */
fun NavController.naviToMyBooks() {
    this.navigate(ROUTE_BOOKS_MY_BOOKS)
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