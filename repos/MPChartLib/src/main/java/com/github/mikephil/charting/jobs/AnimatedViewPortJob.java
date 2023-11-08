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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.view.View;

import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Created by Philipp Jahoda on 19/02/16.
 */
@SuppressLint("NewApi")
public abstract class AnimatedViewPortJob extends ViewPortJob implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    protected ObjectAnimator animator;

    protected float phase;

    protected float xOrigin;
    protected float yOrigin;

    public AnimatedViewPortJob(ViewPortHandler viewPortHandler, float xValue, float yValue, Transformer trans, View v, float xOrigin, float yOrigin, long duration) {
        super(viewPortHandler, xValue, yValue, trans, v);
        this.xOrigin = xOrigin;
        this.yOrigin = yOrigin;
        animator = ObjectAnimator.ofFloat(this, "phase", 0f, 1f);
        animator.setDuration(duration);
        animator.addUpdateListener(this);
        animator.addListener(this);
    }

    @SuppressLint("NewApi")
    @Override
    public void run() {
        animator.start();
    }

    public float getPhase() {
        return phase;
    }

    public void setPhase(float phase) {
        this.phase = phase;
    }

    public float getXOrigin() {
        return xOrigin;
    }

    public float getYOrigin() {
        return yOrigin;
    }

    public abstract void recycleSelf();

    protected void resetAnimator(){
        animator.removeAllListeners();
        animator.removeAllUpdateListeners();
        animator.reverse();
        animator.addUpdateListener(this);
        animator.addListener(this);
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        try{
            recycleSelf();
        }catch (IllegalArgumentException e){
            // don't worry about it.
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        try{
            recycleSelf();
        }catch (IllegalArgumentException e){
            // don't worry about it.
        }
    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {

    }
}
