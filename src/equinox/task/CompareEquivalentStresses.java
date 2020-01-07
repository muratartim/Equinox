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

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.Pair;
import equinox.data.StatisticsPlotAttributes;
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
 * Class for compare equivalent stresses task.
 *
 * @author Murat Artim
 * @date Apr 6, 2016
 * @time 3:10:01 PM
 */
public class CompareEquivalentStresses extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask, MultipleInputTask<SpectrumItem>, AutomaticTaskOwner<Pair<CategoryDataset, StatisticsPlotAttributes>> {

	/** Equivalent stresses to compare. */
	private final List<SpectrumItem> stresses_;

	/** Input. */
	private final EquivalentStressComparisonInput input_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>>> automaticTasks_ = null;

	/**
	 * Creates compare equivalent stresses task.
	 *
	 * @param input
	 *            Comparison input.
	 */
	public CompareEquivalentStresses(EquivalentStressComparisonInput input) {
		input_ = input;
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
	public String getTaskTitle() {
		return "Compare equivalent stresses";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
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
	public void addAutomaticTask(String taskID, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_EQUIVALENT_STRESS_COMPARISON);

		// update progress info
		updateTitle("Comparing equivalent stresses...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			compareEquivalentStresses(connection, dataset);
		}

		// return dataset
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get dataset
			CategoryDataset dataset = get();
			String xAxisLabel = "Pilot point";
			String stressType = getStressType(stresses_.get(0));
			String title = stressType + " Comparison";
			boolean legendVisible = dataset.getRowCount() > 1;
			boolean displayLabels = input_.getLabelDisplay();

			// user initiated task
			if (automaticTasks_ == null) {

				// get column plot panel
				StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

				// set chart data to panel
				panel.setPlotData(dataset, title, null, xAxisLabel, stressType, legendVisible, displayLabels, false);

				// show column chart plot panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
			}

			// automatic task
			else {

				// create plot attributes
				StatisticsPlotAttributes plotAttributes = new StatisticsPlotAttributes();
				plotAttributes.setLabelsVisible(displayLabels);
				plotAttributes.setLayered(false);
				plotAttributes.setLegendVisible(legendVisible);
				plotAttributes.setSubTitle(null);
				plotAttributes.setTitle(title);
				plotAttributes.setXAxisLabel(xAxisLabel);
				plotAttributes.setYAxisLabel(stressType);

				// manage automatic tasks
				automaticTaskOwnerSucceeded(new Pair<>(dataset, plotAttributes), automaticTasks_, taskPanel_);
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
	 * Generates comparison for equivalent stresses.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void compareEquivalentStresses(Connection connection, DefaultCategoryDataset dataset) throws Exception {

		// update progress info
		updateMessage("Retreiving equivalent stresses...");

		// set table name
		String tableName = getTableName(stresses_.get(0));

		// prepare statement to get equivalent stress
		String sql = "select stress from " + tableName + " where id = ?";
		try (PreparedStatement getStress = connection.prepareStatement(sql)) {

			// loop over equivalent stresses
			for (SpectrumItem item : stresses_) {

				// get mission
				String mission = getMission(item);
				mission = mission == null ? "Not specified" : mission;

				// set name of stress
				String name = getStressName(item);

				// add data
				getStress.setInt(1, item.getID());
				try (ResultSet resultSet = getStress.executeQuery()) {
					while (resultSet.next()) {
						double stressVal = resultSet.getDouble("stress");
						dataset.addValue(stressVal, mission, name);
					}
				}
			}
		}
	}

	/**
	 * Returns stress name.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Stress name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressName(SpectrumItem item) throws Exception {
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
		else if (item instanceof ExternalFatigueEquivalentStress) {
			name = getStressNameForExternalFatigueEquivalentStress((ExternalFatigueEquivalentStress) item);
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			name = getStressNameForExternalPreffasEquivalentStress((ExternalPreffasEquivalentStress) item);
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			name = getStressNameForExternalLinearEquivalentStress((ExternalLinearEquivalentStress) item);
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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForFastFatigueEquivalentStress(FastFatigueEquivalentStress stress) throws Exception {

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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForFastPreffasEquivalentStress(FastPreffasEquivalentStress stress) throws Exception {

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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForFastLinearEquivalentStress(FastLinearEquivalentStress stress) throws Exception {

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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForFatigueEquivalentStress(FatigueEquivalentStress stress) throws Exception {

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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForPreffasEquivalentStress(PreffasEquivalentStress stress) throws Exception {

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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForLinearEquivalentStress(LinearEquivalentStress stress) throws Exception {

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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForExternalFatigueEquivalentStress(ExternalFatigueEquivalentStress stress) throws Exception {

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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForExternalPreffasEquivalentStress(ExternalPreffasEquivalentStress stress) throws Exception {

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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressNameForExternalLinearEquivalentStress(ExternalLinearEquivalentStress stress) throws Exception {

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
	 * Returns fatigue mission.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Fatigue mission.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getMission(SpectrumItem item) throws Exception {
		String mission = null;
		if (item instanceof FatigueEquivalentStress) {
			mission = ((FatigueEquivalentStress) item).getParentItem().getParentItem().getMission();
		}
		else if (item instanceof PreffasEquivalentStress) {
			mission = ((PreffasEquivalentStress) item).getParentItem().getParentItem().getMission();
		}
		else if (item instanceof LinearEquivalentStress) {
			mission = ((LinearEquivalentStress) item).getParentItem().getParentItem().getMission();
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			mission = ((ExternalFatigueEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			mission = ((ExternalPreffasEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			mission = ((ExternalLinearEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			mission = ((FastFatigueEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			mission = ((FastPreffasEquivalentStress) item).getParentItem().getMission();
		}
		else if (item instanceof FastLinearEquivalentStress) {
			mission = ((FastLinearEquivalentStress) item).getParentItem().getMission();
		}
		return mission;
	}

	/**
	 * Returns database table name.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Database table name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getTableName(SpectrumItem item) throws Exception {
		String tableName = null;
		if (item instanceof FatigueEquivalentStress) {
			tableName = "fatigue_equivalent_stresses";
		}
		else if (item instanceof PreffasEquivalentStress) {
			tableName = "preffas_equivalent_stresses";
		}
		else if (item instanceof LinearEquivalentStress) {
			tableName = "linear_equivalent_stresses";
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			tableName = "ext_fatigue_equivalent_stresses";
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			tableName = "ext_preffas_equivalent_stresses";
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			tableName = "ext_linear_equivalent_stresses";
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			tableName = "fast_fatigue_equivalent_stresses";
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			tableName = "fast_preffas_equivalent_stresses";
		}
		else if (item instanceof FastLinearEquivalentStress) {
			tableName = "fast_linear_equivalent_stresses";
		}
		return tableName;
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
}
