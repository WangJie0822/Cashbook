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

import android.annotation.SuppressLint;

/**
 * Created by philipp on 13/06/16.
 */
@SuppressLint("ParcelCreator")
public class RadarEntry extends Entry {

    public RadarEntry(float value) {
        super(0f, value);
    }

    public RadarEntry(float value, Object data) {
        super(0f, value, data);
    }

    /**
     * This is the same as getY(). Returns the value of the RadarEntry.
     *
     * @return
     */
    public float getValue() {
        return getY();
    }

    public RadarEntry copy() {
        RadarEntry e = new RadarEntry(getY(), getData());
        return e;
    }

    @Deprecated
    @Override
    public void setX(float x) {
        super.setX(x);
    }

    @Deprecated
    @Override
    public float getX() {
        return super.getX();
    }
}
