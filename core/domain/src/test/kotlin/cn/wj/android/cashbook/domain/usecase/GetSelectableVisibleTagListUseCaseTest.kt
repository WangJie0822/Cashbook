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

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetSelectableVisibleTagListUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var useCase: GetSelectableVisibleTagListUseCase

    @Before
    fun setup() {
        useCase = GetSelectableVisibleTagListUseCase()
    }

    @Test
    fun when_tag_invisible_then_excluded_from_result() {
        val tags = listOf(
            createTagModel(id = 1L, name = "可见", invisible = false),
            createTagModel(id = 2L, name = "隐藏", invisible = true),
        )

        val result = useCase(tags, emptyList(), emptyList())

        assertThat(result).hasSize(1)
        assertThat(result.first().data.name).isEqualTo("可见")
    }

    @Test
    fun when_invisible_tag_already_selected_then_included() {
        val tags = listOf(
            createTagModel(id = 1L, name = "可见", invisible = false),
            createTagModel(id = 2L, name = "隐藏", invisible = true),
        )

        val result = useCase(
            tagList = tags,
            selectedTagIdList = listOf(2L),
            inSelectTagIdList = listOf(2L),
        )

        assertThat(result).hasSize(2)
        assertThat(result.first { it.data.id == 2L }.selected).isTrue()
    }

    @Test
    fun when_tag_in_select_list_then_marked_as_selected() {
        val tags = listOf(
            createTagModel(id = 1L, name = "标签1"),
            createTagModel(id = 2L, name = "标签2"),
        )

        val result = useCase(
            tagList = tags,
            selectedTagIdList = emptyList(),
            inSelectTagIdList = listOf(1L),
        )

        assertThat(result.first { it.data.id == 1L }.selected).isTrue()
        assertThat(result.first { it.data.id == 2L }.selected).isFalse()
    }
}
