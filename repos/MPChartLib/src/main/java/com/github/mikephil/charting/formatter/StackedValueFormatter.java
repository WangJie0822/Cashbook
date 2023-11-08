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

package com.github.mikephil.charting.formatter;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

/**
 * Created by Philipp Jahoda on 28/01/16.
 * <p/>
 * A formatter specifically for stacked BarChart that allows to specify whether the all stack values
 * or just the top value should be drawn.
 */
public class StackedValueFormatter implements IValueFormatter
{

    /**
     * if true, all stack values of the stacked bar entry are drawn, else only top
     */
    private boolean mDrawWholeStack;

    /**
     * a string that should be appended behind the value
     */
    private String mAppendix;

    private DecimalFormat mFormat;

    /**
     * Constructor.
     *
     * @param drawWholeStack if true, all stack values of the stacked bar entry are drawn, else only top
     * @param appendix       a string that should be appended behind the value
     * @param decimals       the number of decimal digits to use
     */
    public StackedValueFormatter(boolean drawWholeStack, String appendix, int decimals) {
        this.mDrawWholeStack = drawWholeStack;
        this.mAppendix = appendix;

        StringBuffer b = new StringBuffer();
        for (int i = 0; i < decimals; i++) {
            if (i == 0)
                b.append(".");
            b.append("0");
        }

        this.mFormat = new DecimalFormat("###,###,###,##0" + b.toString());
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {

        if (!mDrawWholeStack && entry instanceof BarEntry) {

            BarEntry barEntry = (BarEntry) entry;
            float[] vals = barEntry.getYVals();

            if (vals != null) {

                // find out if we are on top of the stack
                if (vals[vals.length - 1] == value) {

                    // return the "sum" across all stack values
                    return mFormat.format(barEntry.getY()) + mAppendix;
                } else {
                    return ""; // return empty
                }
            }
        }

        // return the "proposed" value
        return mFormat.format(value) + mAppendix;
    }
}
