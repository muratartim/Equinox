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
 * Class for loadcase factor.
 *
 * @author Murat Artim
 * @date Apr 11, 2014
 * @time 5:20:22 PM
 */
public class LoadcaseFactor implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Loadcase number, event name, comments and modification method. */
	private String loadcaseNumber_, eventName_, comments_, method_;

	/** Modification value. */
	private double value_;

	/** Loadcase type. */
	private boolean isOneg_;

	/**
	 * Returns loadcase number.
	 *
	 * @return Loadcase number.
	 */
	public String getLoadcaseNumber() {
		return loadcaseNumber_;
	}

	/**
	 * Returns event name or null if no event name supplied.
	 *
	 * @return Event name or null if no event name supplied.
	 */
	public String getEventName() {
		return eventName_;
	}

	/**
	 * Returns loadcase comments or null if no event name supplied.
	 *
	 * @return Loadcase comments or null if no event name supplied.
	 */
	public String getComments() {
		return comments_;
	}

	/**
	 * Returns true if this is a 1g loadcase.
	 *
	 * @return True if this is a 1g loadcase.
	 */
	public boolean isOneg() {
		return isOneg_;
	}

	/**
	 * Returns modifier method.
	 *
	 * @return Modifier method.
	 */
	public String getModifierMethod() {
		return method_;
	}

	/**
	 * Returns modifier value.
	 *
	 * @return Modifier value.
	 */
	public double getModifierValue() {
		return value_;
	}

	/**
	 * Sets loadcase number.
	 *
	 * @param loadcaseNumber
	 *            Loadcase number.
	 */
	public void setLoadcaseNumber(String loadcaseNumber) {
		loadcaseNumber_ = loadcaseNumber;
	}

	/**
	 * Sets event name.
	 *
	 * @param eventName
	 *            Event name.
	 */
	public void setEventName(String eventName) {
		eventName_ = eventName;
	}

	/**
	 * Sets loadcase comments.
	 *
	 * @param comments
	 *            Loadcase comments.
	 */
	public void setComments(String comments) {
		comments_ = comments;
	}

	/**
	 * Sets if this loadcase is a 1G loadcase.
	 *
	 * @param isOneg
	 *            True if this is a 1g loadcase.
	 */
	public void setIsOneg(boolean isOneg) {
		isOneg_ = isOneg;
	}

	/**
	 * Sets modifier.
	 *
	 * @param method
	 *            Modifier method.
	 * @param value
	 *            Modifier value.
	 */
	public void setModifier(String method, double value) {
		method_ = method;
		value_ = value;
	}

	@Override
	public String toString() {
		String string = loadcaseNumber_;
		string += isOneg_ ? ", 1G" : ", Increment";
		string += ", " + eventName_;
		string += ", " + comments_;
		string += ", " + value_ + ", " + method_;
		return string;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(67, 7).append(loadcaseNumber_).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LoadcaseFactor))
			return false;
		if (o == this)
			return true;
		LoadcaseFactor lf = (LoadcaseFactor) o;
		return new EqualsBuilder().append(loadcaseNumber_, lf.loadcaseNumber_).isEquals();
	}
}
