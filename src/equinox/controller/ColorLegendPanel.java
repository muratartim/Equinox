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
package equinox.controller;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

import equinox.data.EquinoxTheme;
import equinox.viewer.LegendPanel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Class for color legend panel controller.
 *
 * @author Murat Artim
 * @date Aug 21, 2015
 * @time 9:57:36 AM
 */
public class ColorLegendPanel implements Initializable {

	/** Owner panel. */
	private LegendPanel owner_;

	@FXML
	private VBox root_;

	@FXML
	private Label val1_, val2_, val3_, val4_, val5_, val6_, val7_, val8_, val9_;

	/** Decimal formats. */
	private final DecimalFormat format1_ = new DecimalFormat("0.##"), format2_ = new DecimalFormat("0.00E00");

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Returns the root container of this panel.
	 *
	 * @return The root container of this panel.
	 */
	public VBox getRoot() {
		return root_;
	}

	/**
	 * Returns the parent panel of this panel.
	 *
	 * @return The parent panel of this panel.
	 */
	public LegendPanel getOwner() {
		return owner_;
	}

	/**
	 * Sets up the color legend.
	 *
	 * @param minVal
	 *            Minimum value.
	 * @param maxVal
	 *            Maximum value.
	 */
	public void setupLegend(double minVal, double maxVal) {

		// compute values
		double val1 = maxVal;
		double val5 = 0.5 * (maxVal + minVal);
		double val9 = minVal;
		double val3 = 0.5 * (val1 + val5);
		double val7 = 0.5 * (val5 + val9);
		double val2 = 0.5 * (val1 + val3);
		double val4 = 0.5 * (val3 + val5);
		double val6 = 0.5 * (val5 + val7);
		double val8 = 0.5 * (val7 + val9);

		// set to labels
		val1_.setText(formatValue(val1));
		val2_.setText(formatValue(val2));
		val3_.setText(formatValue(val3));
		val4_.setText(formatValue(val4));
		val5_.setText(formatValue(val5));
		val6_.setText(formatValue(val6));
		val7_.setText(formatValue(val7));
		val8_.setText(formatValue(val8));
		val9_.setText(formatValue(val9));
	}

	/**
	 * Formats given value.
	 *
	 * @param value
	 *            Value to format.
	 * @return Formatted value.
	 */
	private String formatValue(double value) {
		double absVal = Math.abs(value);
		if ((absVal < 0.01) || (absVal > 1000))
			return format2_.format(value);
		return format1_.format(value);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static ColorLegendPanel load(LegendPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ColorLegendPanel.fxml"));
			fxmlLoader.load();

			// get controller
			ColorLegendPanel controller = (ColorLegendPanel) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
