/*
 * Copyright 2018 Murat Artim (muratartim@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package equinox.data;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import equinox.data.input.GenerateStressSequenceInput;

/**
 * Class for segment stress factor.
 *
 * @author Murat Artim
 * @date Oct 5, 2014
 * @time 3:08:24 PM
 */
public class SegmentFactor implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Segment. */
	private final Segment segment_;

	/** Modifier methods. */
	private final String[] methods_ = new String[4];

	/** Modifier values. */
	private final double[] values_ = new double[4];

	/**
	 * Creates segment.
	 *
	 * @param segment
	 *            Segment.
	 */
	public SegmentFactor(Segment segment) {
		segment_ = segment;
	}

	/**
	 * Returns the segment.
	 *
	 * @return The segment.
	 */
	public Segment getSegment() {
		return segment_;
	}

	/**
	 * Returns modifier method.
	 *
	 * @param index
	 *            Index of modifier.
	 * @return Modifier method.
	 */
	public String getModifierMethod(int index) {
		return methods_[index];
	}

	/**
	 * Returns modifier value.
	 *
	 * @param index
	 *            Index of modifier.
	 * @return Modifier value.
	 */
	public double getModifierValue(int index) {
		return values_[index];
	}

	/**
	 * Sets modifier.
	 *
	 * @param method
	 *            Modifier method.
	 * @param value
	 *            Modifier value.
	 * @param index
	 *            Modifier index.
	 */
	public void setModifier(String method, double value, int index) {
		methods_[index] = method;
		values_[index] = value;
	}

	@Override
	public String toString() {
		String string = segment_.toString();
		string += ", 1G (" + values_[GenerateStressSequenceInput.ONEG] + ", " + methods_[GenerateStressSequenceInput.ONEG] + ")";
		string += ", Inc (" + values_[GenerateStressSequenceInput.INCREMENT] + ", " + methods_[GenerateStressSequenceInput.INCREMENT] + ")";
		string += ", DP (" + values_[GenerateStressSequenceInput.DELTAP] + ", " + methods_[GenerateStressSequenceInput.DELTAP] + ")";
		string += ", DT (" + values_[GenerateStressSequenceInput.DELTAT] + ", " + methods_[GenerateStressSequenceInput.DELTAT] + ")";
		return string;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(33, 61).append(segment_).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SegmentFactor))
			return false;
		if (o == this)
			return true;
		SegmentFactor sf = (SegmentFactor) o;
		return new EqualsBuilder().append(segment_, sf.segment_).isEquals();
	}
}
