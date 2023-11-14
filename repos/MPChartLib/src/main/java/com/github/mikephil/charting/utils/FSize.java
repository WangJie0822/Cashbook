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

package com.github.mikephil.charting.utils;

import java.util.List;

/**
 * Class for describing width and height dimensions in some arbitrary
 * unit. Replacement for the android.Util.SizeF which is available only on API >= 21.
 */
public final class FSize extends ObjectPool.Poolable{

    //  : Encapsulate width & height

    public float width;
    public float height;

    private static ObjectPool<FSize> pool;

    static {
        pool = ObjectPool.create(256, new FSize(0,0));
        pool.setReplenishPercentage(0.5f);
    }


    protected ObjectPool.Poolable instantiate(){
        return new FSize(0,0);
    }

    public static FSize getInstance(final float width, final float height){
        FSize result = pool.get();
        result.width = width;
        result.height = height;
        return result;
    }

    public static void recycleInstance(FSize instance){
        pool.recycle(instance);
    }

    public static void recycleInstances(List<FSize> instances){
        pool.recycle(instances);
    }

    public FSize() {
    }

    public FSize(final float width, final float height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof FSize) {
            final FSize other = (FSize) obj;
            return width == other.width && height == other.height;
        }
        return false;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Float.floatToIntBits(width) ^ Float.floatToIntBits(height);
    }
}
