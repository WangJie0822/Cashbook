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
 * Point encapsulating two double values.
 *
 * @author Philipp Jahoda
 */
public class MPPointD extends ObjectPool.Poolable {

    private static ObjectPool<MPPointD> pool;

    static {
        pool = ObjectPool.create(64, new MPPointD(0,0));
        pool.setReplenishPercentage(0.5f);
    }

    public static MPPointD getInstance(double x, double y){
        MPPointD result = pool.get();
        result.x = x;
        result.y = y;
        return result;
    }

    public static void recycleInstance(MPPointD instance){
        pool.recycle(instance);
    }

    public static void recycleInstances(List<MPPointD> instances){
        pool.recycle(instances);
    }

    public double x;
    public double y;

    protected ObjectPool.Poolable instantiate(){
        return new MPPointD(0,0);
    }

    private MPPointD(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * returns a string representation of the object
     */
    public String toString() {
        return "MPPointD, x: " + x + ", y: " + y;
    }
}