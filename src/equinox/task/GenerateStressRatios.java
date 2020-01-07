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
import equinox.data.input.EquivalentStressRatioComparisonInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for generate stress ratios task.
 *
 * @author Murat Artim
 * @date Apr 7, 2016
 * @time 9:57:13 AM
 */
public class GenerateStressRatios extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask, MultipleInputTask<SpectrumItem>, AutomaticTaskOwner<Pair<CategoryDataset, StatisticsPlotAttributes>> {

	/** Equivalent stresses to compare. */
	private final List<SpectrumItem> stresses_;

	/** Input. */
	private final EquivalentStressRatioComparisonInput input_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<CategoryDataset, StatisticsPlotAttributes>>> automaticTasks_ = null;

	/**
	 * Creates generate stress ratios task.
	 *
	 * @param input
	 *            Comparison input.
	 */
	public GenerateStressRatios(EquivalentStressRatioComparisonInput input) {
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
	public String getTaskTitle() {
		return "Generate equivalent stress ratios";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.GENERATE_EQUIVALENT_STRESS_RATIOS);

		// update progress info
		updateTitle("Generating equivalent stress ratios...");

		// create data set
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			generateStressRatiosForBasisMission(connection, dataset);
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
			String xAxisLabel = "Pilot Point";
			String yAxisLabel = getRatioType(stresses_.get(0));
			String subTitle = "Based on mission '" + input_.getBasisMission() + "'";

			// user initiated task
			if (automaticTasks_ == null) {

				// get column plot panel
				StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

				// set chart data to panel
				panel.setPlotData(dataset, yAxisLabel, subTitle, xAxisLabel, yAxisLabel, true, input_.getLabelDisplay(), false);

				// show column chart plot panel
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
			}

			// automatic task
			else {

				// create plot attributes
				StatisticsPlotAttributes plotAttributes = new StatisticsPlotAttributes();
				plotAttributes.setLabelsVisible(input_.getLabelDisplay());
				plotAttributes.setLayered(false);
				plotAttributes.setLegendVisible(true);
				plotAttributes.setSubTitle(subTitle);
				plotAttributes.setTitle(yAxisLabel);
				plotAttributes.setXAxisLabel(xAxisLabel);
				plotAttributes.setYAxisLabel(yAxisLabel);

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
	 * Generates comparison for life factors for basis fatigue mission.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart series.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void generateStressRatiosForBasisMission(Connection connection, DefaultCategoryDataset dataset) throws Exception {

		// update progress info
		updateMessage("Computing stress ratios...");

		// get inputs
		String basisMission = input_.getBasisMission();

		// set table name
		String tableName = getTableName(stresses_.get(0));

		// prepare statement to get equivalent stress for the basis
		String sql = "select stress from " + tableName + " where id = ?";
		try (PreparedStatement getBasis = connection.prepareStatement(sql)) {

			// prepare statement to get life factor
			sql = "select id, stress/? as ratio from " + tableName + " where id = ?";
			try (PreparedStatement getRatio = connection.prepareStatement(sql)) {

				// loop over equivalent stresses
				for (SpectrumItem stress1 : stresses_) {

					// get mission
					String mission = getMission(stress1);

					// basis
					if (basisMission.equals(mission)) {

						// set name of stress
						String name1 = getStressName(stress1);

						// get basis material slope and equivalent stress
						double basisStress = 0.0;
						getBasis.setInt(1, stress1.getID());
						try (ResultSet resultSet = getBasis.executeQuery()) {
							while (resultSet.next()) {
								basisStress = resultSet.getDouble("stress");
							}
						}
						getRatio.setDouble(1, basisStress);

						// loop over equivalent stresses
						for (SpectrumItem stress2 : stresses_) {

							// get mission
							mission = getMission(stress2);

							// don't include basis mission
							if (!input_.getIncludeBasisMission() && basisMission.equals(mission)) {
								continue;
							}

							// set name of stress
							String name2 = getStressName(stress2);

							// same name
							if (name2.equals(name1)) {

								// add data
								getRatio.setInt(2, stress2.getID());
								try (ResultSet resultSet = getRatio.executeQuery()) {
									while (resultSet.next()) {
										double ratio = resultSet.getDouble("ratio");
										dataset.addValue(ratio, mission, name2);
									}
								}
							}
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
	 * Returns ratio type.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Spectrum item.
	 */
	private static String getRatioType(SpectrumItem item) {
		String stressType = null;
		if (item instanceof FatigueEquivalentStress) {
			stressType = "Fatigue Eq. Stress Ratio";
		}
		else if (item instanceof PreffasEquivalentStress) {
			stressType = "Preffas Eq. Stress Ratio";
		}
		else if (item instanceof LinearEquivalentStress) {
			stressType = "Linear Prop. Eq. Stress Ratio";
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			stressType = "Fatigue Eq. Stress Ratio";
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			stressType = "Preffas Eq. Stress Ratio";
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			stressType = "Linear Prop. Eq. Stress Ratio";
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			stressType = "Fatigue Eq. Stress Ratio";
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			stressType = "Preffas Eq. Stress Ratio";
		}
		else if (item instanceof FastLinearEquivalentStress) {
			stressType = "Linear Prop. Eq. Stress Ratio";
		}
		return stressType;
	}
}
