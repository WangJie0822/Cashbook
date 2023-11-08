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

import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IRadarDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data container for the RadarChart.
 *
 * @author Philipp Jahoda
 */
public class RadarData extends ChartData<IRadarDataSet> {

    private List<String> mLabels;

    public RadarData() {
        super();
    }

    public RadarData(List<IRadarDataSet> dataSets) {
        super(dataSets);
    }

    public RadarData(IRadarDataSet... dataSets) {
        super(dataSets);
    }

    /**
     * Sets the labels that should be drawn around the RadarChart at the end of each web line.
     *
     * @param labels
     */
    public void setLabels(List<String> labels) {
        this.mLabels = labels;
    }

    /**
     * Sets the labels that should be drawn around the RadarChart at the end of each web line.
     *
     * @param labels
     */
    public void setLabels(String... labels) {
        this.mLabels = Arrays.asList(labels);
    }

    public List<String> getLabels() {
        return mLabels;
    }

    @Override
    public Entry getEntryForHighlight(Highlight highlight) {
        return getDataSetByIndex(highlight.getDataSetIndex()).getEntryForIndex((int) highlight.getX());
    }
}
