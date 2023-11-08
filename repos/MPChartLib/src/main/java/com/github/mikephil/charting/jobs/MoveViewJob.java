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

package com.github.mikephil.charting.jobs;

import android.view.View;

import com.github.mikephil.charting.utils.ObjectPool;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Created by Philipp Jahoda on 19/02/16.
 */
public class MoveViewJob extends ViewPortJob {

    private static ObjectPool<MoveViewJob> pool;

    static {
        pool = ObjectPool.create(2, new MoveViewJob(null,0,0,null,null));
        pool.setReplenishPercentage(0.5f);
    }

    public static MoveViewJob getInstance(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v){
        MoveViewJob result = pool.get();
        result.mViewPortHandler = viewPortHandler;
        result.xValue = xValue;
        result.yValue = yValue;
        result.mTrans = trans;
        result.view = v;
        return result;
    }

    public static void recycleInstance(MoveViewJob instance){
        pool.recycle(instance);
    }

    public MoveViewJob(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v) {
        super(viewPortHandler, xValue, yValue, trans, v);
    }

    @Override
    public void run() {

        pts[0] = xValue;
        pts[1] = yValue;

        mTrans.pointValuesToPixel(pts);
        mViewPortHandler.centerViewPort(pts, view);

        this.recycleInstance(this);
    }

    @Override
    protected ObjectPool.Poolable instantiate() {
        return new MoveViewJob(mViewPortHandler, xValue, yValue, mTrans, view);
    }
}
