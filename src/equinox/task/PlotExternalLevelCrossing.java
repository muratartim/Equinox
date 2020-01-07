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
package equinox.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import equinox.Equinox;
import equinox.controller.LevelCrossingViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.Pair;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.ExternalLevelCrossingInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;
import equinox.task.automation.PostProcessingTask;

/**
 * Class for plot external level crossing task.
 *
 * @author Murat Artim
 * @date Mar 15, 2015
 * @time 4:05:33 PM
 */
public class PlotExternalLevelCrossing extends InternalEquinoxTask<XYSeriesCollection> implements ShortRunningTask, MultipleInputTask<SpectrumItem>, PostProcessingTask, AutomaticTaskOwner<Pair<XYSeriesCollection, String>> {

	/** Equivalent stresses. */
	private final List<SpectrumItem> stresses_;

	/** Input. */
	private final ExternalLevelCrossingInput input_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<XYSeriesCollection, String>>> automaticTasks_ = null;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/**
	 * Creates plot external level crossing task.
	 *
	 * @param input
	 *            Level crossing input.
	 */
	public PlotExternalLevelCrossing(ExternalLevelCrossingInput input) {
		input_ = input;
		stresses_ = Collections.synchronizedList(new ArrayList<>());
	}

	/**
	 * Adds external equivalent stress.
	 *
	 * @param externalEquivalentStress
	 *            External equivalent stress to add.
	 */
	public void addEquivalentStress(SpectrumItem externalEquivalentStress) {
		stresses_.add(externalEquivalentStress);
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Pair<XYSeriesCollection, String>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Pair<XYSeriesCollection, String>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	synchronized public void setInputThreshold(int inputThreshold) {
		inputThreshold_ = inputThreshold;
	}

	@Override
	synchronized public void addAutomaticInput(AutomaticTaskOwner<SpectrumItem> task, SpectrumItem input, ExecutionMode mode) {
		automaticInputAdded(task, input, mode, stresses_, inputThreshold_);
	}

	@Override
	synchronized public void inputFailed(AutomaticTaskOwner<SpectrumItem> task, ExecutionMode mode) {
		inputThreshold_ = automaticInputFailed(task, mode, stresses_, inputThreshold_);
	}

	@Override
	public String getTaskTitle() {
		return "Plot external level crossing";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected XYSeriesCollection call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_LEVEL_CROSSINGS);

		// update progress info
		updateTitle("Plotting external level crossing...");

		// create data set
		XYSeriesCollection dataset = new XYSeriesCollection();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// set table names
			String stressTable = null, rainflowTable = null;
			if (stresses_.get(0) instanceof ExternalFatigueEquivalentStress) {
				stressTable = "ext_fatigue_equivalent_stresses";
				rainflowTable = "ext_fatigue_rainflow_cycles";
			}
			else if (stresses_.get(0) instanceof ExternalPreffasEquivalentStress) {
				stressTable = "ext_preffas_equivalent_stresses";
				rainflowTable = "ext_preffas_rainflow_cycles";
			}
			else if (stresses_.get(0) instanceof ExternalLinearEquivalentStress) {
				stressTable = "ext_linear_equivalent_stresses";
				rainflowTable = "ext_linear_rainflow_cycles";
			}

			// prepare statement for getting rainflow cycles
			String sql = "select validity from " + stressTable + " where id = ?";
			try (PreparedStatement getValidity = connection.prepareStatement(sql)) {

				// prepare statement for getting rainflow cycles
				sql = "select num_cycles, max_val, min_val from " + rainflowTable + " where stress_id = ? order by cycle_num asc";
				try (PreparedStatement getCycles = connection.prepareStatement(sql)) {

					// loop over equivalent stresses
					int[] dsgs = input_.getDSGs();
					for (int i = 0; i < stresses_.size(); i++) {

						// create series
						XYSeries series = new XYSeries(getSpectrumName(stresses_.get(i), dataset), false, true);

						// get validity
						double validity = -1;
						getValidity.setInt(1, stresses_.get(i).getID());
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
						getCycles.setInt(1, stresses_.get(i).getID());
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
						double dsg = input_.isNormalize() ? validity : dsgs[i];

						// create plot
						createPlot(series, dsg, validity, N, Smax, Smin, max, min);

						// add series to data set
						dataset.addSeries(series);
					}
				}
			}
		}

		// return data set
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get dataset
			XYSeriesCollection dataset = get();
			String xAxisLabel = input_.isNormalize() ? "Number of Cycles (Normalized by spectrum validities)" : "Number of Cycles";

			// user initiated task
			if (automaticTasks_ == null) {

				// get level crossing view panel
				LevelCrossingViewPanel panel = (LevelCrossingViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.LEVEL_CROSSING_VIEW);

				// set data
				panel.plottingCompleted(dataset, xAxisLabel, true);

				// show level crossing view panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.LEVEL_CROSSING_VIEW);
			}

			// automatic task
			else {
				automaticTaskOwnerSucceeded(new Pair<>(dataset, xAxisLabel), automaticTasks_, taskPanel_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
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
		if (stress instanceof ExternalFatigueEquivalentStress) {
			name = getFatigueName((ExternalFatigueEquivalentStress) stress, dataset);
		}
		else if (stress instanceof ExternalPreffasEquivalentStress) {
			name = getPreffasName((ExternalPreffasEquivalentStress) stress, dataset);
		}
		else if (stress instanceof ExternalLinearEquivalentStress) {
			name = getLinearName((ExternalLinearEquivalentStress) stress, dataset);
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
	private String getFatigueName(ExternalFatigueEquivalentStress stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(stress.getParentItem().getName()) + ", ";
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
			name += stress.getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + ", ";
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
	private String getPreffasName(ExternalPreffasEquivalentStress stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(stress.getParentItem().getName()) + ", ";
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
			name += stress.getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + ", ";
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
	private String getLinearName(ExternalLinearEquivalentStress stress, XYSeriesCollection dataset) {

		// initialize name
		String name = "";

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + ", ";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(stress.getParentItem().getName()) + ", ";
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
			name += stress.getParentItem().getProgram() + ", ";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getSection() + ", ";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + ", ";
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
