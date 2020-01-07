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
import equinox.controller.MissionParameterPlotViewPanel;
import equinox.controller.MissionParameterPlotViewPanel.PlotCompletionPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.MissionParameterPlotAttributes;
import equinox.data.Pair;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.EquivalentStressComparisonInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for compare equivalent stresses with mission parameters.
 *
 * @author Murat Artim
 * @date Jan 9, 2015
 * @time 1:00:10 PM
 */
public class CompareEquivalentStressesWithMissionParameters extends InternalEquinoxTask<XYSeriesCollection> implements ShortRunningTask, MultipleInputTask<SpectrumItem>, AutomaticTaskOwner<Pair<XYSeriesCollection, MissionParameterPlotAttributes>> {

	/** Equivalent stresses to compare. */
	private final List<SpectrumItem> stresses_;

	/** Input. */
	private final EquivalentStressComparisonInput input_;

	/** Requesting panel. */
	private final PlotCompletionPanel panel_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<XYSeriesCollection, MissionParameterPlotAttributes>>> automaticTasks_ = null;

	/**
	 * Creates compare equivalent stresses with mission parameters task.
	 *
	 * @param input
	 *            Comparison input.
	 * @param panel
	 *            Requesting panel. Can be null for automatic execution.
	 */
	public CompareEquivalentStressesWithMissionParameters(EquivalentStressComparisonInput input, PlotCompletionPanel panel) {
		input_ = input;
		panel_ = panel;
		stresses_ = Collections.synchronizedList(new ArrayList<>());
	}

	/**
	 * Adds given stress to this task.
	 *
	 * @param stress
	 *            Equivalent stress to add.
	 */
	public void addEquivalentStress(SpectrumItem stress) {
		stresses_.add(stress);
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
	public void addAutomaticTask(String taskID, EmbeddedTask<Pair<XYSeriesCollection, MissionParameterPlotAttributes>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Pair<XYSeriesCollection, MissionParameterPlotAttributes>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Compare equivalent stresses with mission parameters";
	}

	@Override
	protected XYSeriesCollection call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_EQUIVALENT_STRESS_COMPARISON);

		// update progress info
		updateTitle("Comparing equivalent stresses...");

		// create data set
		XYSeriesCollection dataset = new XYSeriesCollection();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// get type of equivalent stress
			SpectrumItem item = stresses_.get(0);

			// equivalent stress
			if (item instanceof FatigueEquivalentStress || item instanceof PreffasEquivalentStress || item instanceof LinearEquivalentStress || item instanceof FastFatigueEquivalentStress || item instanceof FastPreffasEquivalentStress || item instanceof FastLinearEquivalentStress) {
				compareEquivalentStresses(connection, dataset);
			}
			else if (item instanceof ExternalFatigueEquivalentStress || item instanceof ExternalPreffasEquivalentStress || item instanceof ExternalLinearEquivalentStress) {
				compareExternalEquivalentStresses(connection, dataset);
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
			String xAxisLabel = input_.getMissionParameterName();
			String stressType = getStressType(stresses_.get(0));
			String yAxisLabel = stressType;
			String title = stressType + " Comparison";

			// user initiated task
			if (automaticTasks_ == null) {

				// get mission parameters view panel
				MissionParameterPlotViewPanel panel = (MissionParameterPlotViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);

				// set data
				panel.plottingCompleted(dataset, title, xAxisLabel, yAxisLabel, panel_, false, false, "Mission Parameters View");

				// show mission parameters view panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);
			}

			// automatic task
			else {

				// create plot attributes
				MissionParameterPlotAttributes attributes = new MissionParameterPlotAttributes();
				attributes.setTitle(title);
				attributes.setxAxisInverted(false);
				attributes.setyAxisInverted(false);
				attributes.setxAxisLabel(xAxisLabel);
				attributes.setyAxisLabel(yAxisLabel);

				// manage automatic tasks
				automaticTaskOwnerSucceeded(new Pair<>(dataset, attributes), automaticTasks_, taskPanel_);
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

	/**
	 * Returns stress type.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Spectrum item.
	 */
	private static String getStressType(SpectrumItem item) {
		String stressType = null;
		if (item instanceof FatigueEquivalentStress) {
			stressType = "Fatigue Equivalent Stress";
		}
		else if (item instanceof PreffasEquivalentStress) {
			stressType = "Preffas Equivalent Stress";
		}
		else if (item instanceof LinearEquivalentStress) {
			stressType = "Linear Prop. Equivalent Stress";
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			stressType = "Fatigue Equivalent Stress";
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			stressType = "Preffas Equivalent Stress";
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			stressType = "Linear Prop. Equivalent Stress";
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			stressType = "Fatigue Equivalent Stress";
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			stressType = "Preffas Equivalent Stress";
		}
		else if (item instanceof FastLinearEquivalentStress) {
			stressType = "Linear Prop. Equivalent Stress";
		}
		return stressType;
	}

	/**
	 * Compares equivalent stresses.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void compareEquivalentStresses(Connection connection, XYSeriesCollection dataset) throws Exception {

		// update progress info
		updateMessage("Retreiving equivalent stresses...");

		// set table name
		String tableName = null;
		if (stresses_.get(0) instanceof FatigueEquivalentStress) {
			tableName = "fatigue_equivalent_stresses";
		}
		else if (stresses_.get(0) instanceof PreffasEquivalentStress) {
			tableName = "preffas_equivalent_stresses";
		}
		else if (stresses_.get(0) instanceof LinearEquivalentStress) {
			tableName = "linear_equivalent_stresses";
		}
		else if (stresses_.get(0) instanceof FastFatigueEquivalentStress) {
			tableName = "fast_fatigue_equivalent_stresses";
		}
		else if (stresses_.get(0) instanceof FastPreffasEquivalentStress) {
			tableName = "fast_preffas_equivalent_stresses";
		}
		else if (stresses_.get(0) instanceof FastLinearEquivalentStress) {
			tableName = "fast_linear_equivalent_stresses";
		}

		// prepare statement to get equivalent stress
		String sql = "select stress from " + tableName + " where id = ?";
		try (PreparedStatement getStress = connection.prepareStatement(sql)) {

			// prepare statement to get mission parameter values from STF file
			sql = "select val from stf_mission_parameters where stf_id = ? and name = '" + input_.getMissionParameterName() + "'";
			try (PreparedStatement getMPFromSTF = connection.prepareStatement(sql)) {

				// prepare statement to get mission parameter values from spectrum
				sql = "select val from cdf_mission_parameters where cdf_id = ? and name = '" + input_.getMissionParameterName() + "'";
				try (PreparedStatement getMPFromCDF = connection.prepareStatement(sql)) {

					// loop over equivalent stresses
					for (SpectrumItem item : stresses_) {

						// get mission parameter value
						Double missionParameterValue = getMissionParameterValueForEquivalentStress(item, getMPFromSTF, getMPFromCDF);

						// no mission parameter defined
						if (missionParameterValue == null) {
							continue;
						}

						// set name of stress
						String name = getStressNameForEquivalentStress(item);

						// create series
						XYSeries series = getSeries(name, dataset);

						// add data
						getStress.setInt(1, item.getID());
						try (ResultSet resultSet = getStress.executeQuery()) {
							while (resultSet.next()) {
								double stressVal = resultSet.getDouble("stress");
								series.add(missionParameterValue.doubleValue(), stressVal);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Compares equivalent stresses.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void compareExternalEquivalentStresses(Connection connection, XYSeriesCollection dataset) throws Exception {

		// update progress info
		updateMessage("Retreiving equivalent stresses...");

		// set table name
		String tableName = null;
		if (stresses_.get(0) instanceof ExternalFatigueEquivalentStress) {
			tableName = "ext_fatigue_equivalent_stresses";
		}
		else if (stresses_.get(0) instanceof ExternalPreffasEquivalentStress) {
			tableName = "ext_preffas_equivalent_stresses";
		}
		else if (stresses_.get(0) instanceof ExternalLinearEquivalentStress) {
			tableName = "ext_linear_equivalent_stresses";
		}

		// prepare statement to get equivalent stress
		String sql = "select stress from " + tableName + " where id = ?";
		try (PreparedStatement getStress = connection.prepareStatement(sql)) {

			// prepare statement to get mission parameter values
			sql = "select val from ext_sth_mission_parameters where sth_id = ? and name = '" + input_.getMissionParameterName() + "'";
			try (PreparedStatement getMP = connection.prepareStatement(sql)) {

				// loop over equivalent stresses
				for (SpectrumItem item : stresses_) {

					// get mission parameter value
					Double missionParameterValue = getMissionParameterValueForExternalEquivalentStress(item, getMP);

					// no mission parameter defined
					if (missionParameterValue == null) {
						continue;
					}

					// set name of stress
					String name = getStressNameForExternalEquivalentStress(item);

					// create series
					XYSeries series = getSeries(name, dataset);

					// add data
					getStress.setInt(1, item.getID());
					try (ResultSet resultSet = getStress.executeQuery()) {
						while (resultSet.next()) {
							double stressVal = resultSet.getDouble("stress");
							series.add(missionParameterValue.doubleValue(), stressVal);
						}
					}
				}
			}
		}
	}

	/**
	 * Returns stress name.
	 *
	 * @param item
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForExternalEquivalentStress(SpectrumItem item) {
		String name = "";
		if (item instanceof ExternalFatigueEquivalentStress) {
			name = getStressNameForExternalFatigueEquivalentStress((ExternalFatigueEquivalentStress) item);
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			name = getStressNameForExternalPreffasEquivalentStress((ExternalPreffasEquivalentStress) item);
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			name = getStressNameForExternalLinearEquivalentStress((ExternalLinearEquivalentStress) item);
		}
		return name.substring(0, name.lastIndexOf("\n"));
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForExternalFatigueEquivalentStress(ExternalFatigueEquivalentStress stress) {

		// initialize name
		String name = "";

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(stress.getParentItem().getName()) + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForExternalPreffasEquivalentStress(ExternalPreffasEquivalentStress stress) {

		// initialize name
		String name = "";

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(stress.getParentItem().getName()) + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForExternalLinearEquivalentStress(ExternalLinearEquivalentStress stress) {

		// initialize name
		String name = "";

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += ExternalStressSequence.getEID(stress.getParentItem().getName()) + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param item
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForEquivalentStress(SpectrumItem item) {
		String name = "";
		if (item instanceof FatigueEquivalentStress) {
			name = getStressNameForFatigueEquivalentStress((FatigueEquivalentStress) item);
		}
		else if (item instanceof PreffasEquivalentStress) {
			name = getStressNameForPreffasEquivalentStress((PreffasEquivalentStress) item);
		}
		else if (item instanceof LinearEquivalentStress) {
			name = getStressNameForLinearEquivalentStress((LinearEquivalentStress) item);
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			name = getStressNameForFastFatigueEquivalentStress((FastFatigueEquivalentStress) item);
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			name = getStressNameForFastPreffasEquivalentStress((FastPreffasEquivalentStress) item);
		}
		else if (item instanceof FastLinearEquivalentStress) {
			name = getStressNameForFastLinearEquivalentStress((FastLinearEquivalentStress) item);
		}
		return name.substring(0, name.lastIndexOf("\n"));
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForFastFatigueEquivalentStress(FastFatigueEquivalentStress stress) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getEID() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForFastPreffasEquivalentStress(FastPreffasEquivalentStress stress) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getEID() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForFastLinearEquivalentStress(FastLinearEquivalentStress stress) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getEID() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForFatigueEquivalentStress(FatigueEquivalentStress stress) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getParentItem().getEID() + "\n";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForPreffasEquivalentStress(PreffasEquivalentStress stress) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getParentItem().getEID() + "\n";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Returns stress name.
	 *
	 * @param stress
	 *            Stress.
	 * @return Stress name.
	 */
	private String getStressNameForLinearEquivalentStress(LinearEquivalentStress stress) {

		// initialize name
		String name = "";

		// include spectrum name
		if (input_.getIncludeSpectrumName()) {
			name += stress.getParentItem().getParentItem().getParentItem().getName() + "\n";
		}

		// include STF file name
		if (input_.getIncludeSTFName()) {
			name += stress.getParentItem().getParentItem().getName() + "\n";
		}

		// include EID
		if (input_.getIncludeEID()) {
			name += stress.getParentItem().getParentItem().getEID() + "\n";
		}

		// include stress sequence name
		if (input_.getIncludeSequenceName()) {
			name += stress.getParentItem().getName() + "\n";
		}

		// include material name
		if (input_.getIncludeMaterialName()) {
			name += stress.getMaterialName() + "\n";
		}

		// include omission level
		if (input_.getIncludeOmissionLevel()) {
			double omissionLevel = stress.getOmissionLevel();
			name += "OL: " + (omissionLevel == -1 ? "None" : omissionLevel) + "\n";
		}

		// include A/C program
		if (input_.getIncludeProgram()) {
			name += stress.getParentItem().getParentItem().getParentItem().getProgram() + "\n";
		}

		// include A/C section
		if (input_.getIncludeSection()) {
			name += stress.getParentItem().getParentItem().getParentItem().getSection() + "\n";
		}

		// include fatigue mission
		if (input_.getIncludeMission()) {
			name += stress.getParentItem().getParentItem().getMission() + "\n";
		}

		// return name
		return name;
	}

	/**
	 * Gets the series for the given name.
	 *
	 * @param seriesName
	 *            Series name.
	 * @param dataset
	 *            List containing all the series.
	 * @return The series for the given name.
	 */
	private static XYSeries getSeries(String seriesName, XYSeriesCollection dataset) {

		// series already exists
		int index = dataset.indexOf(seriesName);
		if (index != -1)
			return dataset.getSeries(index);

		// create new series
		XYSeries series = new XYSeries(seriesName, true, false);
		dataset.addSeries(series);

		// return series
		return series;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param item
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForEquivalentStress(SpectrumItem item, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {
		Double value = null;
		if (item instanceof FatigueEquivalentStress) {
			value = getMissionParameterValueForFatigueEquivalentStress((FatigueEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof PreffasEquivalentStress) {
			value = getMissionParameterValueForPreffasEquivalentStress((PreffasEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof LinearEquivalentStress) {
			value = getMissionParameterValueForLinearEquivalentStress((LinearEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			value = getMissionParameterValueForFastFatigueEquivalentStress((FastFatigueEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			value = getMissionParameterValueForFastPreffasEquivalentStress((FastPreffasEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof FastLinearEquivalentStress) {
			value = getMissionParameterValueForFastLinearEquivalentStress((FastLinearEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForFastFatigueEquivalentStress(FastFatigueEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForFastPreffasEquivalentStress(FastPreffasEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForFastLinearEquivalentStress(FastLinearEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForFatigueEquivalentStress(FatigueEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForPreffasEquivalentStress(PreffasEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @param getMPFromSTF
	 *            Database statement for getting mission parameter values from STF table.
	 * @param getMPFromCDF
	 *            Database statement for getting mission parameter values from CDF table.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForLinearEquivalentStress(LinearEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value from STF table
		getMPFromSTF.setInt(1, stress.getParentItem().getParentItem().getID());
		try (ResultSet resultSet = getMPFromSTF.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// no value found
		if (value == null) {
			getMPFromCDF.setInt(1, stress.getParentItem().getParentItem().getParentItem().getID());
			try (ResultSet resultSet = getMPFromCDF.executeQuery()) {
				while (resultSet.next()) {
					value = resultSet.getDouble("val");
				}
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param item
	 *            External equivalent stress.
	 * @param getMP
	 *            Database statement for getting mission parameter values.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForExternalEquivalentStress(SpectrumItem item, PreparedStatement getMP) throws Exception {
		Double value = null;
		if (item instanceof ExternalFatigueEquivalentStress) {
			value = getMissionParameterValueForExternalFatigueEquivalentStress((ExternalFatigueEquivalentStress) item, getMP);
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			value = getMissionParameterValueForExternalPreffasEquivalentStress((ExternalPreffasEquivalentStress) item, getMP);
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			value = getMissionParameterValueForExternalLinearEquivalentStress((ExternalLinearEquivalentStress) item, getMP);
		}
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            External equivalent stress.
	 * @param getMP
	 *            Database statement for getting mission parameter values.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForExternalFatigueEquivalentStress(ExternalFatigueEquivalentStress stress, PreparedStatement getMP) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value
		getMP.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMP.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            External equivalent stress.
	 * @param getMP
	 *            Database statement for getting mission parameter values.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForExternalPreffasEquivalentStress(ExternalPreffasEquivalentStress stress, PreparedStatement getMP) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value
		getMP.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMP.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// return value
		return value;
	}

	/**
	 * Returns the mission parameter value for the given parameter name.
	 *
	 * @param stress
	 *            External equivalent stress.
	 * @param getMP
	 *            Database statement for getting mission parameter values.
	 * @return Mission parameter value.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Double getMissionParameterValueForExternalLinearEquivalentStress(ExternalLinearEquivalentStress stress, PreparedStatement getMP) throws Exception {

		// initialize parameter value
		Double value = null;

		// get value
		getMP.setInt(1, stress.getParentItem().getID());
		try (ResultSet resultSet = getMP.executeQuery()) {
			while (resultSet.next()) {
				value = resultSet.getDouble("val");
			}
		}

		// return value
		return value;
	}
}
