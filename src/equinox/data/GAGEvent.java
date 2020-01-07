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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Class for GAG event.
 *
 * @author Murat Artim
 * @date Jun 2, 2015
 * @time 10:29:22 AM
 */
public class GAGEvent {

	/** Event name and load case. */
	private final String event_, issyCode_;

	/** Comment. */
	private String comment_ = null;

	/** Rating. */
	private double rating_ = 0.0;

	/** GAG contribution type. */
	private boolean isMax_ = true;

	/** Segment. */
	private Segment segment_;

	/**
	 * Creates GAG event.
	 *
	 * @param event
	 *            Event name.
	 * @param issyCode
	 *            Load case.
	 */
	public GAGEvent(String event, String issyCode) {
		event_ = event;
		issyCode_ = issyCode;
	}

	/**
	 * Sets comment.
	 *
	 * @param comment
	 *            Comment.
	 */
	public void setComment(String comment) {
		comment_ = comment;
	}

	/**
	 * Sets rating.
	 *
	 * @param rating
	 *            Rating.
	 */
	public void setRating(double rating) {
		rating_ = rating;
	}

	/**
	 * Contribution type.
	 *
	 * @param isMax
	 *            True if max.
	 */
	public void setType(boolean isMax) {
		isMax_ = isMax;
	}

	/**
	 * Sets segment.
	 *
	 * @param segment
	 *            Segment.
	 */
	public void setSegment(Segment segment) {
		segment_ = segment;
	}

	/**
	 * Returns event name.
	 *
	 * @return Event name.
	 */
	public String getEvent() {
		return event_;
	}

	/**
	 * Returns load case.
	 *
	 * @return Load case.
	 */
	public String getIssyCode() {
		return issyCode_;
	}

	/**
	 * Returns comment.
	 *
	 * @return Comment.
	 */
	public String getComment() {
		return comment_;
	}

	/**
	 * Returns rating.
	 *
	 * @return Rating.
	 */
	public double getRating() {
		return rating_;
	}

	/**
	 * Returns segment.
	 *
	 * @return Segment.
	 */
	public Segment getSegment() {
		return segment_;
	}

	/**
	 * Returns true if max.
	 *
	 * @return True if max.
	 */
	public boolean getType() {
		return isMax_;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(11, 29).append(event_).append(issyCode_).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GAGEvent))
			return false;
		if (o == this)
			return true;
		GAGEvent g = (GAGEvent) o;
		return new EqualsBuilder().append(event_, g.event_).append(issyCode_, g.issyCode_).isEquals();
	}
}
