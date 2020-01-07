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

import javafx.beans.property.SimpleStringProperty;

/**
 * This class is for info table data item.
 *
 * @author Murat Artim
 * @date Dec 6, 2013
 * @time 11:04:39 AM
 */
public class TableItem {

	/** Table item data. */
	private final SimpleStringProperty label = new SimpleStringProperty(), value = new SimpleStringProperty();

	/**
	 * Creates info table item.
	 *
	 * @param label
	 *            Label of table item data.
	 */
	public TableItem(String label) {
		setLabel(label);
	}

	/**
	 * Creates info table item.
	 *
	 * @param label
	 *            Label of table item data.
	 * @param value
	 *            Value of table item data.
	 */
	public TableItem(String label, String value) {
		setLabel(label);
		setValue(value);
	}

	/**
	 * Returns the label of data item.
	 *
	 * @return The label of data item.
	 */
	public String getLabel() {
		return this.label.get();
	}

	/**
	 * Returns the value of data item.
	 *
	 * @return The value of data item.
	 */
	public String getValue() {
		return this.value.get();
	}

	/**
	 * Sets the label of data item.
	 *
	 * @param label
	 *            Label to set.
	 */
	public void setLabel(String label) {
		this.label.set(label);
	}

	/**
	 * Sets the value of data item.
	 *
	 * @param value
	 *            Value to set.
	 */
	public void setValue(String value) {
		this.value.set(value);
	}
}
