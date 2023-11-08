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

/**
 * Created by Philipp Jahoda on 07/12/15.
 */
class ContentItem {

    final String name;
    final String desc;
    boolean isSection = false;

    ContentItem(String n) {
        name = n;
        desc = "";
        isSection = true;
    }

    ContentItem(String n, String d) {
        name = n;
        desc = d;
    }
}
