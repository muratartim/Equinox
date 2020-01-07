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

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Class for RFORT omission.
 *
 * @author Murat Artim
 * @date Apr 15, 2016
 * @time 11:27:48 AM
 */
public abstract class RfortOmission implements Serializable {

	/** Omission name constant for initial analysis. */
	public static final String INITIAL_ANALYSIS = "Initial analysis";

	/** Omission type. */
	public static final String PERCENT = "Percent", DIRECT = "Direct", NO_OMISSION = "No omission";

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** True if this omission can be edited. */
	private boolean canBeEdited_ = true;

	/**
	 * Returns true if this omission can be edited.
	 *
	 * @return True if this omission can be edited.
	 */
	public boolean canBeEdited() {
		return canBeEdited_;
	}

	/**
	 * Sets whether this omission can be edited.
	 *
	 * @param canBeEdited
	 *            True if this omission can be edited.
	 */
	public void setCanBeEdited(boolean canBeEdited) {
		canBeEdited_ = canBeEdited;
	}

	/**
	 * Returns the omission type.
	 *
	 * @return Omission type.
	 */
	public abstract String getOmissionType();

	@Override
	public abstract String toString();

	@Override
	public int hashCode() {
		return new HashCodeBuilder(33, 51).append(toString()).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof RfortOmission) {
			RfortOmission omission = (RfortOmission) o;
			return toString().equals(omission.toString());
		}
		return false;
	}
}
