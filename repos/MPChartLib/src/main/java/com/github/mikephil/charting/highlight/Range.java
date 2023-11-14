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

package com.github.mikephil.charting.highlight;

/**
 * Created by Philipp Jahoda on 24/07/15. Class that represents the range of one value in a stacked bar entry. e.g.
 * stack values are -10, 5, 20 -> then ranges are (-10 - 0, 0 - 5, 5 - 25).
 */
public final class Range {

	public float from;
	public float to;

	public Range(float from, float to) {
		this.from = from;
		this.to = to;
	}

	/**
	 * Returns true if this range contains (if the value is in between) the given value, false if not.
	 * 
	 * @param value
	 * @return
	 */
	public boolean contains(float value) {

		if (value > from && value <= to)
			return true;
		else
			return false;
	}

	public boolean isLarger(float value) {
		return value > to;
	}

	public boolean isSmaller(float value) {
		return value < from;
	}
}