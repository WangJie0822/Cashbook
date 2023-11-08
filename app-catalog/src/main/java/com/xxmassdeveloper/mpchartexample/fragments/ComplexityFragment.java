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

package com.xxmassdeveloper.mpchartexample.fragments;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import cn.wj.android.cashbookcatalog.R;


public class ComplexityFragment extends SimpleFragment {

    @NonNull
    public static Fragment newInstance() {
        return new ComplexityFragment();
    }

    @SuppressWarnings("FieldCanBeLocal")
    private LineChart chart;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frag_simple_line, container, false);

        chart = v.findViewById(R.id.lineChart1);

        chart.getDescription().setEnabled(false);

        chart.setDrawGridBackground(false);

        chart.setData(getComplexity());
        chart.animateX(3000);

        Typeface tf = Typeface.createFromAsset(context.getAssets(), "OpenSans-Light.ttf");

        Legend l = chart.getLegend();
        l.setTypeface(tf);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTypeface(tf);

        chart.getAxisRight().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setEnabled(false);

        return v;
    }
}
