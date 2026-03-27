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
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import cn.wj.android.cashbook.feature.books.screen.EditBookRoute
import cn.wj.android.cashbook.feature.books.screen.MyBooksRoute
import kotlinx.serialization.Serializable

/** 路由 - 我的账本 */
@Serializable
object MyBooks

/** 路由 - 编辑账本 */
@Serializable
data class EditBook(val bookId: Long = -1L)

/** 跳转到我的账本 */
fun NavController.naviToMyBooks() {
    this.navigate(MyBooks)
}

/** 跳转到编辑账本 */
fun NavController.naviToEditBook(bookId: Long = -1L) {
    this.navigate(EditBook(bookId = bookId))
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
    composable<MyBooks> {
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
    composable<EditBook> { backStackEntry ->
        val route = backStackEntry.toRoute<EditBook>()
        EditBookRoute(
            bookId = route.bookId,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}
