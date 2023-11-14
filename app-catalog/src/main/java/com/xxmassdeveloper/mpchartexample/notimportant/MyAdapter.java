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

package com.xxmassdeveloper.mpchartexample.notimportant;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import cn.wj.android.cashbookcatalog.R;

import java.util.List;

/**
 * Created by Philipp Jahoda on 07/12/15.
 */
class MyAdapter extends ArrayAdapter<ContentItem> {

    private final Typeface mTypeFaceLight;
    private final Typeface mTypeFaceRegular;

    MyAdapter(Context context, List<ContentItem> objects) {
        super(context, 0, objects);

        mTypeFaceLight = Typeface.createFromAsset(context.getAssets(), "OpenSans-Light.ttf");
        mTypeFaceRegular = Typeface.createFromAsset(context.getAssets(), "OpenSans-Regular.ttf");
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        ContentItem c = getItem(position);

        ViewHolder holder;

        holder = new ViewHolder();

        if (c != null && c.isSection) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_section, null);
        } else {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, null);
        }

        holder.tvName = convertView.findViewById(R.id.tvName);
        holder.tvDesc = convertView.findViewById(R.id.tvDesc);

        convertView.setTag(holder);

        if (c != null && c.isSection)
            holder.tvName.setTypeface(mTypeFaceRegular);
        else
            holder.tvName.setTypeface(mTypeFaceLight);
        holder.tvDesc.setTypeface(mTypeFaceLight);

        holder.tvName.setText(c != null ? c.name : null);
        holder.tvDesc.setText(c != null ? c.desc : null);

        return convertView;
    }

    private class ViewHolder {

        TextView tvName, tvDesc;
    }
}
