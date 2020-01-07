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

import javafx.beans.property.SimpleStringProperty;

/**
 * Class for loadcase table item.
 *
 * @author Murat Artim
 * @date Apr 21, 2014
 * @time 11:34:59 PM
 */
public class LoadcaseItem {

	/** Event name and comment, and loadcase number. */
	private final SimpleStringProperty eventName = new SimpleStringProperty(), comments = new SimpleStringProperty(),
			loadcaseNumber = new SimpleStringProperty();

	/** Loadcase type. */
	private boolean isOneg_;

	/**
	 * Returns event name.
	 *
	 * @return The event name.
	 */
	public String getEventName() {
		return this.eventName.get();
	}

	/**
	 * Returns comments.
	 *
	 * @return The comments.
	 */
	public String getComments() {
		return this.comments.get();
	}

	/**
	 * Returns the loadcase number.
	 *
	 * @return The loadcase number.
	 */
	public String getLoadcaseNumber() {
		return this.loadcaseNumber.get();
	}

	/**
	 * Returns search string for the loadcase.
	 *
	 * @return Search string for the loadcase.
	 */
	public String getSearchString() {
		String string = getLoadcaseNumber() + "-";
		string += getEventName() + "-";
		string += getComments();
		return string;
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
	 * Sets event name.
	 *
	 * @param eventName
	 *            Event name.
	 */
	public void setEventName(String eventName) {
		this.eventName.set(eventName);
	}

	/**
	 * Sets comments.
	 *
	 * @param comments
	 *            Comments.
	 */
	public void setComments(String comments) {
		this.comments.set(comments);
	}

	/**
	 * Sets loadcase number.
	 *
	 * @param loadcaseNumber
	 *            Loadcase number.
	 */
	public void setLoadcaseNumber(String loadcaseNumber) {
		this.loadcaseNumber.set(loadcaseNumber);
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

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(loadcaseNumber.get()).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LoadcaseItem))
			return false;
		if (o == this)
			return true;
		LoadcaseItem item = (LoadcaseItem) o;
		return new EqualsBuilder().append(loadcaseNumber.get(), item.loadcaseNumber.get()).isEquals();
	}

	@Override
	public String toString() {
		return getLoadcaseNumber() + " - " + getEventName();
	}
}
