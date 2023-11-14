/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 *
 * @param onRequestNaviToEditBook 导航到编辑账本
 * @param onRequestPopBackStack 导航上上一级
 */
fun NavGraphBuilder.myBooksScreen(
    onRequestNaviToEditBook: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable(route = ROUTE_BOOKS_MY_BOOKS) {
        MyBooksRoute(
            onRequestNaviToEditBook = onRequestNaviToEditBook,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

/**
 * 编辑账本
 *
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.editBookScreen(
    onRequestPopBackStack: () -> Unit,
) {
    composable(
        route = ROUTE_BOOKS_EDIT_BOOK,
        arguments = listOf(
            navArgument(ROUTE_KEY_BOOK_ID) {
                type = NavType.LongType
                defaultValue = -1L
            },
        ),
    ) {
        val bookId = it.arguments?.getLong(ROUTE_KEY_BOOK_ID) ?: -1L
        EditBookRoute(
            bookId = bookId,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}
