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

package cn.wj.android.cashbook.core.design.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Design Token 约束测试：锁住 [CbMotion] 默认值，避免无感改动
 */
class MotionTest {

    private val motion = CbMotion()

    @Test
    fun durationShort_is_200() {
        assertThat(motion.durationShort).isEqualTo(200)
    }

    @Test
    fun durationMedium_is_250() {
        assertThat(motion.durationMedium).isEqualTo(250)
    }

    @Test
    fun durationLong_is_400() {
        assertThat(motion.durationLong).isEqualTo(400)
    }

    @Test
    fun standardEasing_falls_back_to_FastOutSlowInEasing() {
        assertThat(motion.standardEasing).isEqualTo(FastOutSlowInEasing)
    }

    @Test
    fun emphasizedEasing_is_material3_emphasized_curve() {
        // CubicBezierEasing(0.2f, 0f, 0f, 1f) 映射到 (0, 0) -> (1, 1)
        // 以若干关键点验证曲线形态，避免被误改
        val easing = motion.emphasizedEasing
        assertThat(easing.transform(0f)).isEqualTo(0f)
        assertThat(easing.transform(1f)).isEqualTo(1f)
        // 非线性校验：中段应该快于线性（emphasized 强调快启）
        assertThat(easing.transform(0.5f)).isGreaterThan(0.5f)
    }

    @Test
    fun effectiveDuration_returns_zero_when_reduced() {
        // Reduced Motion 开启时必须为 0，tween 将瞬时完成
        assertThat(motion.effectiveDuration(motion.durationShort, reduced = true)).isEqualTo(0)
        assertThat(motion.effectiveDuration(motion.durationMedium, reduced = true)).isEqualTo(0)
        assertThat(motion.effectiveDuration(motion.durationLong, reduced = true)).isEqualTo(0)
    }

    @Test
    fun effectiveDuration_returns_default_when_not_reduced() {
        assertThat(motion.effectiveDuration(motion.durationShort, reduced = false)).isEqualTo(200)
        assertThat(motion.effectiveDuration(motion.durationMedium, reduced = false)).isEqualTo(250)
        assertThat(motion.effectiveDuration(motion.durationLong, reduced = false)).isEqualTo(400)
    }
}
