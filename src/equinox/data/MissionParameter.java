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

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Class for mission parameter.
 *
 * @author Murat Artim
 * @date Nov 26, 2014
 * @time 10:28:18 AM
 */
public class MissionParameter {

	/** Parameter name. */
	private final SimpleStringProperty name = new SimpleStringProperty();

	/** Parameter value. */
	private final SimpleDoubleProperty value = new SimpleDoubleProperty();

	/**
	 * Creates mission parameter.
	 *
	 * @param name
	 *            Parameter name.
	 * @param value
	 *            Parameter value.
	 */
	public MissionParameter(String name, double value) {
		setName(name);
		setValue(value);
	}

	/**
	 * Returns the name of the parameter.
	 *
	 * @return The name of the parameter.
	 */
	public String getName() {
		return this.name.get();
	}

	/**
	 * Returns the parameter value.
	 *
	 * @return The parameter value.
	 */
	public double getValue() {
		return this.value.get();
	}

	/**
	 * Sets parameter name.
	 *
	 * @param name
	 *            Parameter name.
	 */
	public void setName(String name) {
		this.name.set(name);
	}

	/**
	 * Sets parameter value.
	 *
	 * @param value
	 *            Parameter value.
	 */
	public void setValue(double value) {
		this.value.set(value);
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(53, 91).append(name.get()).append(value.get()).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MissionParameter))
			return false;
		if (o == this)
			return true;
		MissionParameter param = (MissionParameter) o;
		return new EqualsBuilder().append(name.get(), param.name.get()).append(value.get(), param.value.get()).isEquals();
	}
}
