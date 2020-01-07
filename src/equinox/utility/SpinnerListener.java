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
package equinox.utility;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;

/**
 * Class for spinner listener.
 *
 * @author Murat Artim
 * @date Mar 30, 2015
 * @time 3:28:23 PM
 */
public class SpinnerListener implements ChangeListener<String> {

	/** Spinner. */
	private final Spinner<Integer> spinner_;

	/**
	 * Creates spinner listener.
	 *
	 * @param spinner
	 *            Spinner.
	 */
	public SpinnerListener(Spinner<Integer> spinner) {
		spinner_ = spinner;
	}

	@Override
	public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		try {
			spinner_.getValueFactory().setValue(Integer.parseInt(newValue));
		}

		// exception occurred
		catch (NumberFormatException e) {
			IntegerSpinnerValueFactory vf = (IntegerSpinnerValueFactory) spinner_.getValueFactory();
			spinner_.getValueFactory().setValue(vf.getMin());
		}
	}
}
