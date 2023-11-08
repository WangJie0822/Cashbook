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

package com.github.mikephil.charting.data;

import com.github.mikephil.charting.interfaces.datasets.IBubbleDataSet;
import com.github.mikephil.charting.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class BubbleDataSet extends BarLineScatterCandleBubbleDataSet<BubbleEntry> implements IBubbleDataSet {

    protected float mMaxSize;
    protected boolean mNormalizeSize = true;

    private float mHighlightCircleWidth = 2.5f;

    public BubbleDataSet(List<BubbleEntry> yVals, String label) {
        super(yVals, label);
    }

    @Override
    public void setHighlightCircleWidth(float width) {
        mHighlightCircleWidth = Utils.convertDpToPixel(width);
    }

    @Override
    public float getHighlightCircleWidth() {
        return mHighlightCircleWidth;
    }

    @Override
    protected void calcMinMax(BubbleEntry e) {
        super.calcMinMax(e);

        final float size = e.getSize();

        if (size > mMaxSize) {
            mMaxSize = size;
        }
    }

    @Override
    public DataSet<BubbleEntry> copy() {
        List<BubbleEntry> entries = new ArrayList<BubbleEntry>();
        for (int i = 0; i < mEntries.size(); i++) {
            entries.add(mEntries.get(i).copy());
        }
        BubbleDataSet copied = new BubbleDataSet(entries, getLabel());
        copy(copied);
        return copied;
    }

    protected void copy(BubbleDataSet bubbleDataSet) {
        bubbleDataSet.mHighlightCircleWidth = mHighlightCircleWidth;
        bubbleDataSet.mNormalizeSize = mNormalizeSize;
    }

    @Override
    public float getMaxSize() {
        return mMaxSize;
    }

    @Override
    public boolean isNormalizeSizeEnabled() {
        return mNormalizeSize;
    }

    public void setNormalizeSizeEnabled(boolean normalizeSize) {
        mNormalizeSize = normalizeSize;
    }
}
