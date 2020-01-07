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

/**
 * Class for flight segment.
 *
 * @author Murat Artim
 * @date Sep 4, 2014
 * @time 4:32:55 PM
 */
public class Segment implements Comparable<Segment>, Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Segment name. */
	private final String name_;

	/** Segment number. */
	private final Integer segmentNumber_;

	/** Start and end points. */
	private double startPeak_, endPeak_;

	/**
	 * Creates flight segment.
	 *
	 * @param name
	 *            Segment name.
	 * @param segmentNumber
	 *            Segment number.
	 */
	public Segment(String name, int segmentNumber) {
		name_ = name;
		segmentNumber_ = segmentNumber;
	}

	/**
	 * Sets start peak.
	 *
	 * @param startPeak
	 *            Start peak.
	 */
	public void setStartPeak(double startPeak) {
		startPeak_ = startPeak;
	}

	/**
	 * Sets end peak.
	 *
	 * @param endPeak
	 *            End peak.
	 */
	public void setEndPeak(double endPeak) {
		endPeak_ = endPeak;
	}

	/**
	 * Returns segment name.
	 *
	 * @return Segment name.
	 */
	public String getName() {
		return name_;
	}

	/**
	 * Returns segment number.
	 *
	 * @return Segment number.
	 */
	public int getSegmentNumber() {
		return segmentNumber_;
	}

	/**
	 * Returns start peak.
	 *
	 * @return Start peak.
	 */
	public double getStartPeak() {
		return startPeak_;
	}

	/**
	 * Returns end peak.
	 *
	 * @return End peak.
	 */
	public double getEndPeak() {
		return endPeak_;
	}

	@Override
	public String toString() {
		return name_ + " (" + segmentNumber_.intValue() + ")";
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(name_).append(segmentNumber_.intValue()).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Segment))
			return false;
		if (o == this)
			return true;
		Segment segment = (Segment) o;
		return new EqualsBuilder().append(name_, segment.name_).append(segmentNumber_.intValue(), segment.segmentNumber_.intValue()).isEquals();
	}

	@Override
	public int compareTo(Segment o) {
		return segmentNumber_.compareTo(o.segmentNumber_);
	}
}
