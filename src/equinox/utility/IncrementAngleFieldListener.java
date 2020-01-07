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
import javafx.scene.control.TextField;

/**
 * Utility class for damage angle increment text field listener.
 *
 * @author Murat Artim
 * @date 4 Jul 2018
 * @time 23:33:44
 */
public class IncrementAngleFieldListener implements ChangeListener<String> {

	/** Default and interval values. */
	private static final int DEFAULT = 10, MIN = 1, MAX = 90;

	/** Angle range slider. */
	private final Spinner<Integer> startAngle_, endAngle_;

	/** Listened text field. */
	protected final TextField textField_;

	/**
	 * Creates increment angle field listener.
	 *
	 * @param textField
	 *            Text field to listen.
	 * @param startAngle
	 *            Start angle spinner.
	 * @param endAngle
	 *            End angle spinner.
	 */
	public IncrementAngleFieldListener(TextField textField, Spinner<Integer> startAngle, Spinner<Integer> endAngle) {
		textField_ = textField;
		startAngle_ = startAngle;
		endAngle_ = endAngle;
	}

	@Override
	public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {

		try {

			// parse integer value from text
			int val = Integer.parseInt(newValue);

			// check against lower boundary
			if (val < MIN || val > MAX || 30 % val != 0) {
				textField_.setText(Integer.toString(DEFAULT));
				((IntegerSpinnerValueFactory) startAngle_.getValueFactory()).setAmountToStepBy(DEFAULT);
				((IntegerSpinnerValueFactory) endAngle_.getValueFactory()).setAmountToStepBy(DEFAULT);
				startAngle_.getValueFactory().setValue(0);
				endAngle_.getValueFactory().setValue(180);
				return;
			}

			// setup range slider
			((IntegerSpinnerValueFactory) startAngle_.getValueFactory()).setAmountToStepBy(val);
			((IntegerSpinnerValueFactory) endAngle_.getValueFactory()).setAmountToStepBy(val);
			startAngle_.getValueFactory().setValue(0);
			endAngle_.getValueFactory().setValue(180);
		}

		// not integer
		catch (NumberFormatException e) {
			((IntegerSpinnerValueFactory) startAngle_.getValueFactory()).setAmountToStepBy(DEFAULT);
			((IntegerSpinnerValueFactory) endAngle_.getValueFactory()).setAmountToStepBy(DEFAULT);
			startAngle_.getValueFactory().setValue(0);
			endAngle_.getValueFactory().setValue(180);
		}
	}
}