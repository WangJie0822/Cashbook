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

package com.github.mikephil.charting.interfaces.datasets;

import android.graphics.Paint;

import com.github.mikephil.charting.data.CandleEntry;

/**
 * Created by philipp on 21/10/15.
 */
public interface ICandleDataSet extends ILineScatterCandleRadarDataSet<CandleEntry> {

    /**
     * Returns the space that is left out on the left and right side of each
     * candle.
     *
     * @return
     */
    float getBarSpace();

    /**
     * Returns whether the candle bars should show?
     * When false, only "ticks" will show
     *
     * - default: true
     *
     * @return
     */
    boolean getShowCandleBar();

    /**
     * Returns the width of the candle-shadow-line in pixels.
     *
     * @return
     */
    float getShadowWidth();

    /**
     * Returns shadow color for all entries
     *
     * @return
     */
    int getShadowColor();

    /**
     * Returns the neutral color (for open == close)
     *
     * @return
     */
    int getNeutralColor();

    /**
     * Returns the increasing color (for open < close).
     *
     * @return
     */
    int getIncreasingColor();

    /**
     * Returns the decreasing color (for open > close).
     *
     * @return
     */
    int getDecreasingColor();

    /**
     * Returns paint style when open < close
     *
     * @return
     */
    Paint.Style getIncreasingPaintStyle();

    /**
     * Returns paint style when open > close
     *
     * @return
     */
    Paint.Style getDecreasingPaintStyle();

    /**
     * Is the shadow color same as the candle color?
     *
     * @return
     */
    boolean getShadowColorSameAsCandle();
}
