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

package com.github.mikephil.charting.components;

import android.graphics.Canvas;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public interface IMarker {

    /**
     * @return The desired (general) offset you wish the IMarker to have on the x- and y-axis.
     *         By returning x: -(width / 2) you will center the IMarker horizontally.
     *         By returning y: -(height / 2) you will center the IMarker vertically.
     */
    MPPointF getOffset();

    /**
     * @return The offset for drawing at the specific `point`. This allows conditional adjusting of the Marker position.
     *         If you have no adjustments to make, return getOffset().
     *
     * @param posX This is the X position at which the marker wants to be drawn.
     *             You can adjust the offset conditionally based on this argument.
     * @param posY This is the X position at which the marker wants to be drawn.
     *             You can adjust the offset conditionally based on this argument.
     */
    MPPointF getOffsetForDrawingAtPoint(float posX, float posY);

    /**
     * This method enables a specified custom IMarker to update it's content every time the IMarker is redrawn.
     *
     * @param e         The Entry the IMarker belongs to. This can also be any subclass of Entry, like BarEntry or
     *                  CandleEntry, simply cast it at runtime.
     * @param highlight The highlight object contains information about the highlighted value such as it's dataset-index, the
     *                  selected range or stack-index (only stacked bar entries).
     */
    void refreshContent(Entry e, Highlight highlight);

    /**
     * Draws the IMarker on the given position on the screen with the given Canvas object.
     *
     * @param canvas
     * @param posX
     * @param posY
     */
    void draw(Canvas canvas, float posX, float posY);
}
