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

/**
 * Class for application setting.
 *
 * @author Murat Artim
 * @date Feb 10, 2015
 * @time 5:37:10 PM
 */
public class Setting implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** True if application restart is required when setting value changes. */
	private final boolean restartOnChange_;

	/** Value of setting. */
	private Object value_;

	/**
	 * Creates application setting.
	 *
	 * @param value
	 *            Initial value of setting.
	 * @param restartOnChange
	 *            True if application restart is required when setting value changes.
	 */
	public Setting(Object value, boolean restartOnChange) {
		value_ = value;
		restartOnChange_ = restartOnChange;
	}

	/**
	 * Returns value of setting.
	 *
	 * @return Value of setting.
	 */
	public Object getValue() {
		return value_;
	}

	/**
	 * Sets new value for setting.
	 *
	 * @param value
	 *            New value.
	 * @return True if application restart is required.
	 */
	public boolean setValue(Object value) {

		// both values are null
		if (value_ == null && value == null)
			return false;

		// check for restart
		boolean restartRequired = false;
		if (restartOnChange_) {
			if (value_ == null) {
				restartRequired = true;
			}
			else {
				restartRequired = !value_.equals(value);
			}
		}

		// set value
		value_ = value;

		// return restart
		return restartRequired;
	}

	/**
	 * Returns true if application restart is required when setting value changes.
	 * 
	 * @return True if application restart is required when setting value changes.
	 */
	public boolean requiresRestartOnChange() {
		return restartOnChange_;
	}
}
