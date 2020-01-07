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
import equinox.data.input.LifeFactorComparisonInput;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for generate life factors with mission parameters task.
 *
 * @author Murat Artim
 * @date Nov 27, 2014
 * @time 4:14:20 PM
 */
public class GenerateLFsWithMissionParameters extends InternalEquinoxTask<XYSeriesCollection> implements ShortRunningTask, MultipleInputTask<SpectrumItem>, AutomaticTaskOwner<Pair<XYSeriesCollection, MissionParameterPlotAttributes>> {

	/** Equivalent stresses to compare. */
	private final List<SpectrumItem> stresses_;

	/** Input. */
	private final LifeFactorComparisonInput input_;

	/** Requesting panel. */
	private final PlotCompletionPanel panel_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<XYSeriesCollection, MissionParameterPlotAttributes>>> automaticTasks_ = null;

	/**
	 * Creates generate life factors with mission parameters task.
	 *
	 * @param input
	 *            Comparison input.
	 * @param panel
	 *            Requesting panel. Can be null for automatic execution.
	 */
	public GenerateLFsWithMissionParameters(LifeFactorComparisonInput input, PlotCompletionPanel panel) {
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
		return "Generate life factors with mission parameters";
	}

	@Override
	protected XYSeriesCollection call() throws Exception {

		// check permission
		checkPermission(Permission.GENERATE_LIFE_FACTORS);

		// update progress info
		updateTitle("Generating life factors...");

		// create data set
		XYSeriesCollection dataset = new XYSeriesCollection();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// get type of equivalent stress
			SpectrumItem item = stresses_.get(0);

			// equivalent stress
			if (item instanceof FatigueEquivalentStress || item instanceof PreffasEquivalentStress || item instanceof LinearEquivalentStress || item instanceof FastFatigueEquivalentStress || item instanceof FastPreffasEquivalentStress || item instanceof FastLinearEquivalentStress) {
				generateLifeFactors(connection, dataset);
			}
			else if (item instanceof ExternalFatigueEquivalentStress || item instanceof ExternalPreffasEquivalentStress || item instanceof ExternalLinearEquivalentStress) {
				generateExternalLifeFactors(connection, dataset);
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

			// get chart dataset
			XYSeriesCollection dataset = get();
			String factorType = getFactorType(stresses_.get(0));
			String title = factorType + " based on mission '" + input_.getBasisMission() + "'";
			String xAxisLabel = input_.getMissionParameterName();
			String yAxisLabel = factorType;

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
				attributes.setxAxisLabel(xAxisLabel);
				attributes.setyAxisLabel(yAxisLabel);
				attributes.setxAxisInverted(false);
				attributes.setyAxisInverted(false);

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
	 * Returns life factor type.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Spectrum item.
	 */
	private static String getFactorType(SpectrumItem item) {
		String stressType = null;
		if (item instanceof FatigueEquivalentStress) {
			stressType = "Fatigue Life Factor";
		}
		else if (item instanceof PreffasEquivalentStress) {
			stressType = "Preffas Life Factor";
		}
		else if (item instanceof LinearEquivalentStress) {
			stressType = "Linear Prop. Life Factor";
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			stressType = "Fatigue Life Factor";
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			stressType = "Preffas Life Factor";
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			stressType = "Linear Prop. Life Factor";
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			stressType = "Fatigue Life Factor";
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			stressType = "Preffas Life Factor";
		}
		else if (item instanceof FastLinearEquivalentStress) {
			stressType = "Linear Prop. Life Factor";
		}
		return stressType;
	}

	/**
	 * Generates life factors.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void generateExternalLifeFactors(Connection connection, XYSeriesCollection dataset) throws Exception {

		// update progress info
		updateMessage("Computing life factors...");

		// get inputs
		String basisMission = input_.getBasisMission();
		String missionParameterName = input_.getMissionParameterName();

		// set table name
		String tableName = getTableName(stresses_.get(0));

		// set material column
		String materialCol = getMaterialColumn(stresses_.get(0));

		// prepare statement to get equivalent stress and material slope for the basis
		String sql = "select " + materialCol + ", stress from " + tableName + " where id = ?";
		try (PreparedStatement getBasis = connection.prepareStatement(sql)) {

			// prepare statement to get life factor
			sql = "select id, (power(?/stress, ?)) as lifeFactor from " + tableName + " where id = ?";
			try (PreparedStatement getLF = connection.prepareStatement(sql)) {

				// prepare statement to get mission parameter values
				sql = "select val from ext_sth_mission_parameters where sth_id = ? and name = '" + missionParameterName + "'";
				try (PreparedStatement getMP = connection.prepareStatement(sql)) {

					// loop over equivalent stresses
					for (SpectrumItem item1 : stresses_) {

						// get mission
						String mission = getMission(item1);

						// basis
						if (basisMission.equals(mission)) {

							// get mission parameter value
							Double missionParameterValue = getMissionParameterValueForExternalLifeFactor(item1, getMP);

							// no mission parameter defined
							if (missionParameterValue == null) {
								continue;
							}

							// create series
							String stressName1 = getStressName(item1);
							XYSeries series = getSeries(stressName1, dataset);

							// get basis material slope and equivalent stress
							double p = 0.0, basisStress = 0.0;
							getBasis.setInt(1, item1.getID());
							try (ResultSet resultSet = getBasis.executeQuery()) {
								while (resultSet.next()) {
									p = resultSet.getDouble(materialCol);
									basisStress = resultSet.getDouble("stress");
								}
							}
							getLF.setDouble(1, basisStress);
							getLF.setDouble(2, p);

							// loop over equivalent stresses
							for (SpectrumItem item2 : stresses_) {

								// get mission
								mission = getMission(item2);

								// don't include basis mission
								if (!input_.getIncludeBasisMission() && basisMission.equals(mission)) {
									continue;
								}

								// get stress name
								String stressName2 = getStressName(item2);

								// same name
								if (stressName2.equals(stressName1)) {

									// get mission parameter value
									missionParameterValue = getMissionParameterValueForExternalLifeFactor(item2, getMP);

									// no mission parameter defined
									if (missionParameterValue == null) {
										continue;
									}

									// add data
									getLF.setInt(3, item2.getID());
									try (ResultSet resultSet = getLF.executeQuery()) {
										while (resultSet.next()) {
											double lifeFactor = resultSet.getDouble("lifeFactor");
											series.add(missionParameterValue.doubleValue(), lifeFactor);
										}
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
	 * Generates life factors.
	 *
	 * @param connection
	 *            Database connection.
	 * @param dataset
	 *            Chart dataset.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void generateLifeFactors(Connection connection, XYSeriesCollection dataset) throws Exception {

		// update progress info
		updateMessage("Computing life factors...");

		// get inputs
		String basisMission = input_.getBasisMission();
		String missionParameterName = input_.getMissionParameterName();

		// set table name
		String tableName = getTableName(stresses_.get(0));

		// set material column
		String materialCol = getMaterialColumn(stresses_.get(0));

		// prepare statement to get equivalent stress and material slope for the basis
		String sql = "select " + materialCol + ", stress from " + tableName + " where id = ?";
		try (PreparedStatement getBasis = connection.prepareStatement(sql)) {

			// prepare statement to get life factor
			sql = "select id, (power(?/stress, ?)) as lifeFactor from " + tableName + " where id = ?";
			try (PreparedStatement getLF = connection.prepareStatement(sql)) {

				// prepare statement to get mission parameters from STF file
				sql = "select val from stf_mission_parameters where stf_id = ? and name = '" + missionParameterName + "'";
				try (PreparedStatement getMPFromSTF = connection.prepareStatement(sql)) {

					// prepare statement to get mission parameter values from spectrum
					sql = "select val from cdf_mission_parameters where cdf_id = ? and name = '" + missionParameterName + "'";
					try (PreparedStatement getMPFromCDF = connection.prepareStatement(sql)) {

						// loop over equivalent stresses
						for (SpectrumItem item1 : stresses_) {

							// get mission
							String mission = getMission(item1);

							// basis
							if (basisMission.equals(mission)) {

								// get mission parameter value
								Double missionParameterValue = getMissionParameterValueForLifeFactor(item1, getMPFromSTF, getMPFromCDF);

								// no mission parameter defined
								if (missionParameterValue == null) {
									continue;
								}

								// create series
								String stressName1 = getStressName(item1);
								XYSeries series = getSeries(stressName1, dataset);

								// get basis material slope and equivalent stress
								double p = 0.0, basisStress = 0.0;
								getBasis.setInt(1, item1.getID());
								try (ResultSet resultSet = getBasis.executeQuery()) {
									while (resultSet.next()) {
										p = resultSet.getDouble(materialCol);
										basisStress = resultSet.getDouble("stress");
									}
								}
								getLF.setDouble(1, basisStress);
								getLF.setDouble(2, p);

								// loop over equivalent stresses
								for (SpectrumItem item2 : stresses_) {

									// get mission
									mission = getMission(item2);

									// don't include basis mission
									if (!input_.getIncludeBasisMission() && basisMission.equals(mission)) {
										continue;
									}

									// get stress name
									String stressName2 = getStressName(item2);

									// same name
									if (stressName2.equals(stressName1)) {

										// get mission parameter value
										missionParameterValue = getMissionParameterValueForLifeFactor(item2, getMPFromSTF, getMPFromCDF);

										// no mission parameter defined
										if (missionParameterValue == null) {
											continue;
										}

										// add data
										getLF.setInt(3, item2.getID());
										try (ResultSet resultSet = getLF.executeQuery()) {
											while (resultSet.next()) {
												double lifeFactor = resultSet.getDouble("lifeFactor");
												series.add(missionParameterValue.doubleValue(), lifeFactor);
											}
										}
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
	 * Returns material column name.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return Material column name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getMaterialColumn(SpectrumItem item) throws Exception {
		String materialCol = null;
		if (item instanceof FatigueEquivalentStress || item instanceof ExternalFatigueEquivalentStress || item instanceof FastFatigueEquivalentStress) {
			materialCol = "material_p";
		}
		else {
			materialCol = "material_m";
		}
		return materialCol;
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
	private static Double getMissionParameterValueForLifeFactor(SpectrumItem item, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {
		Double value = null;
		if (item instanceof FatigueEquivalentStress) {
			value = getMissionParameterValueForFatigueLifeFactor((FatigueEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof PreffasEquivalentStress) {
			value = getMissionParameterValueForPreffasLifeFactor((PreffasEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof LinearEquivalentStress) {
			value = getMissionParameterValueForLinearLifeFactor((LinearEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			value = getMissionParameterValueForFastFatigueLifeFactor((FastFatigueEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			value = getMissionParameterValueForFastPreffasLifeFactor((FastPreffasEquivalentStress) item, getMPFromSTF, getMPFromCDF);
		}
		else if (item instanceof FastLinearEquivalentStress) {
			value = getMissionParameterValueForFastLinearLifeFactor((FastLinearEquivalentStress) item, getMPFromSTF, getMPFromCDF);
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
	private static Double getMissionParameterValueForFastFatigueLifeFactor(FastFatigueEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

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
	private static Double getMissionParameterValueForFastPreffasLifeFactor(FastPreffasEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

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
	private static Double getMissionParameterValueForFastLinearLifeFactor(FastLinearEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

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
	private static Double getMissionParameterValueForFatigueLifeFactor(FatigueEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

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
	private static Double getMissionParameterValueForPreffasLifeFactor(PreffasEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

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
	private static Double getMissionParameterValueForLinearLifeFactor(LinearEquivalentStress stress, PreparedStatement getMPFromSTF, PreparedStatement getMPFromCDF) throws Exception {

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
	private static Double getMissionParameterValueForExternalLifeFactor(SpectrumItem item, PreparedStatement getMP) throws Exception {
		Double value = null;
		if (item instanceof ExternalFatigueEquivalentStress) {
			value = getMissionParameterValueForExternalFatigueLifeFactor((ExternalFatigueEquivalentStress) item, getMP);
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			value = getMissionParameterValueForExternalPreffasLifeFactor((ExternalPreffasEquivalentStress) item, getMP);
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			value = getMissionParameterValueForExternalLinearLifeFactor((ExternalLinearEquivalentStress) item, getMP);
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
	private static Double getMissionParameterValueForExternalFatigueLifeFactor(ExternalFatigueEquivalentStress stress, PreparedStatement getMP) throws Exception {

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
	private static Double getMissionParameterValueForExternalPreffasLifeFactor(ExternalPreffasEquivalentStress stress, PreparedStatement getMP) throws Exception {

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
	private static Double getMissionParameterValueForExternalLinearLifeFactor(ExternalLinearEquivalentStress stress, PreparedStatement getMP) throws Exception {

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
}
