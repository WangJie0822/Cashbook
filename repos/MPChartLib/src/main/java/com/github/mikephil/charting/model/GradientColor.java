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

package com.github.mikephil.charting.model;

import com.github.mikephil.charting.utils.Fill;

/**
 * Deprecated. Use `Fill`
 */
@Deprecated
public class GradientColor extends Fill
{
    /**
     * Deprecated. Use `Fill.getGradientColors()`
     */
    @Deprecated
    public int getStartColor()
    {
        return getGradientColors()[0];
    }

    /**
     * Deprecated. Use `Fill.setGradientColors(...)`
     */
    @Deprecated
    public void setStartColor(int startColor)
    {
        if (getGradientColors() == null || getGradientColors().length != 2)
        {
            setGradientColors(new int[]{
                    startColor,
                    getGradientColors() != null && getGradientColors().length > 1
                            ? getGradientColors()[1]
                            : 0
            });
        } else
        {
            getGradientColors()[0] = startColor;
        }
    }

    /**
     * Deprecated. Use `Fill.getGradientColors()`
     */
    @Deprecated
    public int getEndColor()
    {
        return getGradientColors()[1];
    }

    /**
     * Deprecated. Use `Fill.setGradientColors(...)`
     */
    @Deprecated
    public void setEndColor(int endColor)
    {
        if (getGradientColors() == null || getGradientColors().length != 2)
        {
            setGradientColors(new int[]{
                    getGradientColors() != null && getGradientColors().length > 0
                            ? getGradientColors()[0]
                            : 0,
                    endColor
            });
        } else
        {
            getGradientColors()[1] = endColor;
        }
    }

}
