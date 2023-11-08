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

package com.github.mikephil.charting.listener;

import android.view.MotionEvent;

/**
 * Listener for callbacks when doing gestures on the chart.
 *
 * @author Philipp Jahoda
 */
public interface OnChartGestureListener {

    /**
     * Callbacks when a touch-gesture has started on the chart (ACTION_DOWN)
     *
     * @param me
     * @param lastPerformedGesture
     */
    void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture);

    /**
     * Callbacks when a touch-gesture has ended on the chart (ACTION_UP, ACTION_CANCEL)
     *
     * @param me
     * @param lastPerformedGesture
     */
    void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture);

    /**
     * Callbacks when the chart is longpressed.
     *
     * @param me
     */
    void onChartLongPressed(MotionEvent me);

    /**
     * Callbacks when the chart is double-tapped.
     *
     * @param me
     */
    void onChartDoubleTapped(MotionEvent me);

    /**
     * Callbacks when the chart is single-tapped.
     *
     * @param me
     */
    void onChartSingleTapped(MotionEvent me);

    /**
     * Callbacks then a fling gesture is made on the chart.
     *
     * @param me1
     * @param me2
     * @param velocityX
     * @param velocityY
     */
    void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY);

    /**
     * Callbacks when the chart is scaled / zoomed via pinch zoom / double-tap gesture.
     *
     * @param me
     * @param scaleX scalefactor on the x-axis
     * @param scaleY scalefactor on the y-axis
     */
    void onChartScale(MotionEvent me, float scaleX, float scaleY);

    /**
     * Callbacks when the chart is moved / translated via drag gesture.
     *
     * @param me
     * @param dX translation distance on the x-axis
     * @param dY translation distance on the y-axis
     */
    void onChartTranslate(MotionEvent me, float dX, float dY);
}
