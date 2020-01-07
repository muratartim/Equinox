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
package equinox.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.LevelCrossingInput;
import equinox.task.InternalEquinoxTask;

/**
 * Class for plot level crossing process.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 2:19:33 PM
 */
public class PlotLevelCrossingProcess implements EquinoxProcess<XYSeriesCollection> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** Input. */
	private final LevelCrossingInput input_;

	/** Equivalent stresses. */
	private final List<SpectrumItem> equivalentStresses_;

	/** Rainflow cycles table name. */
	private final String rainflowTableName_;

	/**
	 * Creates plot level crossing task.
	 *
	 * @param task
	 *            The owner task.
	 * @param input
	 *            Level crossing input.
	 * @param equivalentStresses
	 *            List of equivalent stresses.
	 */
	public PlotLevelCrossingProcess(InternalEquinoxTask<?> task, LevelCrossingInput input, List<SpectrumItem> equivalentStresses) {
		task_ = task;
		input_ = input;
		rainflowTableName_ = null;
		equivalentStresses_ = equivalentStresses;
	}

	/**
	 * Creates plot level crossing task.
	 *
	 * @param task
	 *            The owner task.
	 * @param input
	 *            Level crossing input.
	 * @param rainflowTableName
	 *            Rainflow cycles table name.
	 * @param equivalentStresses
	 *            List of equivalent stresses.
	 */
	public PlotLevelCrossingProcess(InternalEquinoxTask<?> task, LevelCrossingInput input, String rainflowTableName, List<SpectrumItem> equivalentStresses) {
		task_ = task;
		input_ = input;
		rainflowTableName_ = rainflowTableName;
		equivalentStresses_ = equivalentStresses;
	}

	@Override
	public XYSeriesCollection start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Plotting level crossing...");

		// create data set
		XYSeriesCollection dataset = new XYSeriesCollection();

		// set table names
		String stressTable = null, rainflowTable = null;
		SpectrumItem item = equivalentStresses_.get(0);
		if (item instanceof FatigueEquivalentStress) {
			stressTable = "fatigue_equivalent_stresses";
			rainflowTable = "fatigue_rainflow_cycles";
		}
		else if (item instanceof PreffasEquivalentStress) {
			stressTable = "preffas_equivalent_stresses";
			rainflowTable = "preffas_rainflow_cycles";
		}
		else if (item instanceof LinearEquivalentStress) {
			stressTable = "linear_equivalent_stresses";
			rainflowTable = "linear_rainflow_cycles";
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			stressTable = "fast_fatigue_equivalent_stresses";
			rainflowTable = rainflowTableName_;
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			stressTable = "fast_preffas_equivalent_stresses";
			rainflowTable = rainflowTableName_;
		}
		else if (item instanceof FastLinearEquivalentStress) {
			stressTable = "fast_linear_equivalent_stresses";
			rainflowTable = rainflowTableName_;
		}

		// prepare statement for getting fatigue equivalent stress validities
		String sql = "select validity from " + stressTable + " where id = ?";
		try (PreparedStatement getValidity = connection.prepareStatement(sql)) {

			// prepare statement for getting rainflow cycles
			sql = "select num_cycles, max_val, min_val from " + rainflowTable + " where stress_id = ? order by cycle_num asc";
			try (PreparedStatement getCycles = connection.prepareStatement(sql)) {

				// loop over equivalent stresses
				for (int i = 0; i < equivalentStresses_.size(); i++) {

					// create series
					XYSeries series = new XYSeries(getSpectrumName(equivalentStresses_.get(i), dataset), false, true);

					// get validity
					double validity = -1;
					getValidity.setInt(1, equivalentStresses_.get(i).getID());
					try (ResultSet resultSet = getValidity.executeQuery()) {
						while (resultSet.next()) {
							validity = resultSet.getDouble("validity");
						}
					}

					// create lists
					ArrayList<Double> N = new ArrayList<>();
					ArrayList<Double> Smax = new ArrayList<>();
					ArrayList<Double> Smin = new ArrayList<>();

					// get rainflow cycles
					double max = Double.NEGATIVE_INFINITY, min = Double.POSITIVE_INFINITY;
					getCycles.setInt(1, equivalentStresses_.get(i).getID());
					try (ResultSet resultSet = getCycles.executeQuery()) {
						while (resultSet.next()) {

							// get cycles
							double numCycles = resultSet.getDouble("num_cycles");
							double maxVal = resultSet.getDouble("max_val");
							double minVal = resultSet.getDouble("min_val");

							// add to lists
							N.add(numCycles);
							Smax.add(maxVal);
							Smin.add(minVal);

							// update max/min
							if (max <= maxVal) {
								max = maxVal;
							}
							if (min >= minVal) {
								min = minVal;
							}
						}
					}

					// set DSG
					double dsg = input_.isNormalize() ? validity : input_.getDsgs().get(i);

					// create plot
					createPlot(series, dsg, validity, N, Smax, Smin, max, min);

					// add series to data set
					dataset.addSeries(series);
				}
			}
		}

		// return data set
		return dataset;
	}

	private static void createPlot(XYSeries series, double dsg, double bls, ArrayList<Double> N, ArrayList<Double> Smax, ArrayList<Double> Smin, double smax, double smin) throws Exception {

		// initialize variables
		double bs = dsg / bls;
		double[] Nc = new double[65];
		double[] Ng = new double[128];
		double[] Class = new double[65];
		double[] Classg = new double[128];
		Class[0] = smax;
		Class[64] = smin;

		// calculate class values
		double stp = (Class[64] - Class[0]) / 64;
		for (int i = 1; i < 64; i++) {
			Class[i] = Class[i - 1] + stp;
		}

		// compute Nc values
		for (int i = 0; i < N.size(); i++) {
			for (int j = 0; j < 65; j++) {
				if (Smax.get(i) >= Class[j] && Smin.get(i) < Class[j]) {
					Nc[j] = N.get(i) + Nc[j];
				}
			}
		}

		// calculate classg and Ng
		Classg[0] = Class[0];
		for (int i = 1; i < 128; i += 2) {
			int j = (i + 1) / 2;
			Classg[i] = Class[j];
			if (i < 127) {
				Classg[i + 1] = Classg[i];
			}
			Ng[i - 1] = Nc[j - 1] * bs;
			Ng[i] = Ng[i - 1];
		}

		// add start point
		series.add(0.0, Classg[0]);

		// create chart data
		for (int i = 0; i < 128; i++) {
			series.add(Ng[i], Classg[i]);
		}

		// add end point
		series.add(0.0, Classg[127]);
	}

	/**
	 * Returns the name of spectrum.
	 *
	 * @param stress
	 *            Equivalent stress item to get the name.
	 * @param dataset
	 *            Dataset containing the data.
	 * @return The name of spectrum.
	 */
	private String getSpectrumName(SpectrumItem stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// fatigue equivalent stress
		if (stress instanceof FatigueEquivalentStress) {
			name = getFatigueName((FatigueEquivalentStress) stress, dataset);
		}
		else if (stress instanceof PreffasEquivalentStress) {
			name = getPreffasName((PreffasEquivalentStress) stress, dataset);
		}
		else if (stress instanceof LinearEquivalentStress) {
			name = getLinearName((LinearEquivalentStress) stress, dataset);
		}
		else if (stress instanceof FastFatigueEquivalentStress) {
			name = getFastFatigueName((FastFatigueEquivalentStress) stress, dataset);
		}
		else if (stress instanceof FastPreffasEquivalentStress) {
			name = getFastPreffasName((FastPreffasEquivalentStress) stress, dataset);
		}
		else if (stress instanceof FastLinearEquivalentStress) {
			name = getFastLinearName((FastLinearEquivalentStress) stress, dataset);
		}

		// check and return name
		return checkSeriesName(name.substring(0, name.lastIndexOf(", ")), dataset);
	}

	/**
	 * Returns fatigue equivalent stress name.
	 *
	 * @param stress
	 *            Fatigue equivalent stress.
	 * @param dataset
	 *            Dataset.
	 * @return Fatigue equivalent stress name.
	 */
	private String getFastFatigueName(FastFatigueEquivalentStress stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getName() + ", ";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getEID() + ", ";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + ", ";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + ", ";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + ", ";
		}

		// return name
		return name;
	}

	/**
	 * Returns fatigue equivalent stress name.
	 *
	 * @param stress
	 *            Fatigue equivalent stress.
	 * @param dataset
	 *            Dataset.
	 * @return Fatigue equivalent stress name.
	 */
	private String getFastPreffasName(FastPreffasEquivalentStress stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getName() + ", ";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getEID() + ", ";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + ", ";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + ", ";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + ", ";
		}

		// return name
		return name;
	}

	/**
	 * Returns fatigue equivalent stress name.
	 *
	 * @param stress
	 *            Fatigue equivalent stress.
	 * @param dataset
	 *            Dataset.
	 * @return Fatigue equivalent stress name.
	 */
	private String getFastLinearName(FastLinearEquivalentStress stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getName() + ", ";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getEID() + ", ";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + ", ";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + ", ";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + ", ";
		}

		// return name
		return name;
	}

	/**
	 * Returns fatigue equivalent stress name.
	 *
	 * @param stress
	 *            Fatigue equivalent stress.
	 * @param dataset
	 *            Dataset.
	 * @return Fatigue equivalent stress name.
	 */
	private String getFatigueName(FatigueEquivalentStress stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getParentItem().getName() + ", ";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getParentItem().getEID() + ", ";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + ", ";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + ", ";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + ", ";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getParentItem().getMission() + ", ";
		}

		// return name
		return name;
	}

	/**
	 * Returns preffas equivalent stress name.
	 *
	 * @param stress
	 *            Preffas equivalent stress.
	 * @param dataset
	 *            Dataset.
	 * @return Preffas equivalent stress name.
	 */
	private String getPreffasName(PreffasEquivalentStress stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getParentItem().getName() + ", ";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getParentItem().getEID() + ", ";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + ", ";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + ", ";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + ", ";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getParentItem().getMission() + ", ";
		}

		// return name
		return name;
	}

	/**
	 * Returns linear equivalent stress name.
	 *
	 * @param stress
	 *            Linear equivalent stress.
	 * @param dataset
	 *            Dataset.
	 * @return Linear equivalent stress name.
	 */
	private String getLinearName(LinearEquivalentStress stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getParentItem().getName() + ", ";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getParentItem().getEID() + ", ";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + ", ";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + ", ";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + ", ";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getParentItem().getMission() + ", ";
		}

		// return name
		return name;
	}

	/**
	 * Checks series name for uniqueness.
	 *
	 * @param name
	 *            Series name to check.
	 * @param dataset
	 *            Dataset containing the data.
	 * @return The name.
	 */
	private String checkSeriesName(String name, XYSeriesCollection dataset) {
		if (dataset.getSeriesIndex(name) == -1)
			return name;
		name += " ";
		return checkSeriesName(name, dataset);
	}
}
