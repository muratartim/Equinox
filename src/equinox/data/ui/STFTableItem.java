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
package equinox.data.ui;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Class for STF table item.
 *
 * @author Murat Artim
 * @date Dec 19, 2014
 * @time 11:56:28 AM
 */
public class STFTableItem {

	/** Index of stress column. */
	public static final int SX = 0, SY = 1, SXY = 2;

	/** Load case type. */
	public static final String DELTA_P = "Delta-P", STEADY = "Steady", INCREMENT = "Increment";

	/** Load case. */
	private final SimpleIntegerProperty loadcase = new SimpleIntegerProperty();

	/** Stresses. */
	private final SimpleDoubleProperty sx = new SimpleDoubleProperty(), sy = new SimpleDoubleProperty(), sxy = new SimpleDoubleProperty();

	/** Event name and comment. */
	private final SimpleStringProperty type = new SimpleStringProperty(), eventname = new SimpleStringProperty(),
			eventcomment = new SimpleStringProperty();

	/**
	 * Returns the load case number.
	 *
	 * @return Load case number.
	 */
	public int getLoadcase() {
		return this.loadcase.get();
	}

	/**
	 * Returns the stress-x value.
	 *
	 * @return Stress-x value.
	 */
	public double getSx() {
		return this.sx.get();
	}

	/**
	 * Returns the stress-y value.
	 *
	 * @return Stress-y value.
	 */
	public double getSy() {
		return this.sy.get();
	}

	/**
	 * Returns the stress-xy value.
	 *
	 * @return Stress-xy value.
	 */
	public double getSxy() {
		return this.sxy.get();
	}

	/**
	 * Returns load case type.
	 *
	 * @return Load case type.
	 */
	public String getType() {
		return this.type.get();
	}

	/**
	 * Returns event name.
	 *
	 * @return Event name.
	 */
	public String getEventname() {
		return this.eventname.get();
	}

	/**
	 * Returns event comment.
	 *
	 * @return Event comment.
	 */
	public String getEventcomment() {
		return this.eventcomment.get();
	}

	/**
	 * Sets the load case number.
	 *
	 * @param loadcase
	 *            Load case number.
	 */
	public void setLoadcase(int loadcase) {
		this.loadcase.set(loadcase);
	}

	/**
	 * Sets the stress-x value.
	 *
	 * @param sx
	 *            Stress-x value.
	 */
	public void setSx(double sx) {
		this.sx.set(sx);
	}

	/**
	 * Sets the stress-y value.
	 *
	 * @param sy
	 *            Stress-y value.
	 */
	public void setSy(double sy) {
		this.sy.set(sy);
	}

	/**
	 * Sets the stress-xy value.
	 *
	 * @param sxy
	 *            Stress-xy value.
	 */
	public void setSxy(double sxy) {
		this.sxy.set(sxy);
	}

	/**
	 * Sets load case type.
	 *
	 * @param type
	 *            Load case type.
	 */
	public void setType(String type) {
		this.type.set(type);
	}

	/**
	 * Sets event name.
	 *
	 * @param eventname
	 *            Event name.
	 */
	public void setEventname(String eventname) {
		this.eventname.set(eventname);
	}

	/**
	 * Sets event comment.
	 *
	 * @param eventcomment
	 *            Event comment.
	 */
	public void setEventcomment(String eventcomment) {
		this.eventcomment.set(eventcomment);
	}

	/**
	 * Returns stress at the demanded index.
	 *
	 * @param index
	 *            Index of stress column.
	 * @return The demanded stress.
	 */
	public double getStress(int index) {
		if (index == SX)
			return getSx();
		else if (index == SY)
			return getSy();
		else if (index == SXY)
			return getSxy();
		return Double.NaN;
	}
}
