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

import java.util.ArrayList;

import equinox.data.fileType.AircraftLoadCase;
import javafx.beans.property.SimpleStringProperty;

/**
 * Class for load case factor table item.
 *
 * @author Murat Artim
 * @date Sep 22, 2015
 * @time 11:06:17 AM
 */
public class LoadCaseFactorTableItem {

	/** Properties. */
	private final SimpleStringProperty name = new SimpleStringProperty(), factor = new SimpleStringProperty();

	/** Load case. */
	private final AircraftLoadCase loadCase_;

	/**
	 * Creates load case factor table item.
	 *
	 * @param loadCase
	 *            Load case.
	 */
	public LoadCaseFactorTableItem(AircraftLoadCase loadCase) {
		loadCase_ = loadCase;
		this.name.set(loadCase_.getName());
		this.factor.set("1.0");
	}

	/**
	 * Returns load case.
	 *
	 * @return Load case.
	 */
	public AircraftLoadCase getLoadCase() {
		return loadCase_;
	}

	/**
	 * Returns load case name.
	 *
	 * @return load case name.
	 */
	public String getName() {
		return this.name.get();
	}

	/**
	 * Returns load to stress factor.
	 *
	 * @return Load to stress factor.
	 */
	public String getFactor() {
		return this.factor.get();
	}

	/**
	 * Sets load to stress factor.
	 *
	 * @param factor
	 *            Load to stress factor.
	 */
	public void setFactor(String factor) {
		this.factor.set(factor);
	}

	/**
	 * Returns the factor of load case for given load case ID. If the load case couldn't be found, returns 0.0.
	 *
	 * @param list
	 *            Load case list.
	 * @param loadCaseID
	 *            Load case ID.
	 * @return The factor of load case for given load case ID. If the load case couldn't be found, returns 0.0.
	 */
	public static double getFactorFromList(ArrayList<LoadCaseFactorTableItem> list, int loadCaseID) {
		for (LoadCaseFactorTableItem item : list) {
			if (loadCaseID == item.getLoadCase().getID())
				return Double.parseDouble(item.getFactor());
		}
		return 0.0;
	}
}
