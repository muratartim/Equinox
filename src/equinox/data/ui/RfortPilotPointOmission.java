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

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javafx.beans.property.SimpleStringProperty;

/**
 * Class for RFORT pilot point omission value.
 *
 * @author Murat Artim
 * @date Apr 15, 2016
 * @time 3:20:05 PM
 */
public class RfortPilotPointOmission implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** File name. */
	private final SimpleStringProperty name = new SimpleStringProperty(), omission = new SimpleStringProperty();

	/**
	 * Creates RFORT pilot point omission value.
	 *
	 * @param name
	 *            Pilot point name.
	 */
	public RfortPilotPointOmission(String name) {
		this.name.set(name);
		this.omission.set("1.0");
	}

	/**
	 * Returns pilot point name.
	 *
	 * @return Pilot point name.
	 */
	public String getName() {
		return this.name.get();
	}

	/**
	 * Returns omission value.
	 *
	 * @return Omission value
	 */
	public String getOmission() {
		return this.omission.get();
	}

	/**
	 * Sets omission value.
	 *
	 * @param omission
	 *            Omission value.
	 */
	public void setOmission(String omission) {
		this.omission.set(omission);
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(11, 101).append(name.get()).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof RfortPilotPointOmission))
			return false;
		if (o == this)
			return true;
		RfortPilotPointOmission item = (RfortPilotPointOmission) o;
		return new EqualsBuilder().append(name.get(), item.name.get()).isEquals();
	}
}
