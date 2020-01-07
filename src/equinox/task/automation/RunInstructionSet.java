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
package equinox.task.automation;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import equinox.Equinox;
import equinox.controller.InputPanel;
import equinox.controller.SearchEngineSettingsPanel;
import equinox.controller.TaskExecutionPlanViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.AnalysisEngine;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.InstructedTask;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.MissionParameter;
import equinox.data.Pair;
import equinox.data.Settings;
import equinox.data.fileType.DamageAngle;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.data.input.CompareDamageContributionsInput;
import equinox.data.input.DamageAngleInput;
import equinox.data.input.EquivalentStressComparisonInput;
import equinox.data.input.EquivalentStressInput;
import equinox.data.input.EquivalentStressRatioComparisonInput;
import equinox.data.input.ExternalFlightComparisonInput;
import equinox.data.input.ExternalFlightPlotInput;
import equinox.data.input.ExternalLevelCrossingInput;
import equinox.data.input.ExternalStatisticsInput;
import equinox.data.input.ExternalStatisticsInput.ExternalStatistic;
import equinox.data.input.ExternalStressSequenceComparisonInput;
import equinox.data.input.ExternalStressSequenceComparisonInput.ExternalComparisonCriteria;
import equinox.data.input.FastEquivalentStressInput;
import equinox.data.input.FlightComparisonInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.data.input.HistogramInput;
import equinox.data.input.HistogramInput.HistogramDataType;
import equinox.data.input.LevelCrossingInput;
import equinox.data.input.LifeFactorComparisonInput;
import equinox.data.input.LoadcaseDamageContributionInput;
import equinox.data.input.StressSequenceComparisonInput;
import equinox.data.input.StressSequenceComparisonInput.ComparisonCriteria;
import equinox.dataServer.remote.data.ContributionType;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.data.PilotPointSearchInput;
import equinox.dataServer.remote.data.SearchItem;
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.dataServer.remote.data.SpectrumSearchInput;
import equinox.network.AutomationClientHandler;
import equinox.plugin.FileType;
import equinox.process.automation.ConvertJSONtoXML;
import equinox.process.automation.ReadEquivalentStressAnalysisInput;
import equinox.process.automation.ReadGenerateStressSequenceInput;
import equinox.process.automation.ReadLoadcaseDamageContributionAnalysisInput;
import equinox.task.AddSTFFiles;
import equinox.task.AddSpectrum;
import equinox.task.AddStressSequence;
import equinox.task.AdvancedPilotPointSearch;
import equinox.task.AdvancedSpectrumSearch;
import equinox.task.AssignMissionParameters;
import equinox.task.CompareEquivalentStresses;
import equinox.task.CompareEquivalentStressesWithMissionParameters;
import equinox.task.CompareExternalFlights;
import equinox.task.CompareExternalStressSequences;
import equinox.task.CompareFlights;
import equinox.task.CompareStressSequences;
import equinox.task.CreateDummySTFFile;
import equinox.task.DamageAngleAnalysis;
import equinox.task.DownloadPilotPoint;
import equinox.task.DownloadSpectrum;
import equinox.task.EquivalentStressAnalysis;
import equinox.task.ExportContributions;
import equinox.task.ExportSTF;
import equinox.task.ExportSpectrum;
import equinox.task.FastGenerateStressSequence;
import equinox.task.GenerateExternalStatistics;
import equinox.task.GenerateFlightOccurrencePlot;
import equinox.task.GenerateHOFlightPlot;
import equinox.task.GenerateHSFlightPlot;
import equinox.task.GenerateLFsWithMissionParameters;
import equinox.task.GenerateLevelCrossingsPlot;
import equinox.task.GenerateLifeFactors;
import equinox.task.GenerateLongestFlightPlot;
import equinox.task.GenerateMissionProfilePlot;
import equinox.task.GenerateNumPeaksPlot;
import equinox.task.GenerateStressRatios;
import equinox.task.GenerateStressRatiosWithMissionParameters;
import equinox.task.GenerateStressSequence;
import equinox.task.GetContributionNames;
import equinox.task.GetExternalTypicalFlight;
import equinox.task.GetFlightContributionNames;
import equinox.task.GetSTFInfo2;
import equinox.task.GetSTFInfo3;
import equinox.task.GetSpectrumEditInfo;
import equinox.task.GetTypicalFlight;
import equinox.task.InternalEquinoxTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.LoadcaseDamageContributionAnalysis;
import equinox.task.PlotDamageAngles;
import equinox.task.PlotDamageAngles.ResultOrdering;
import equinox.task.PlotDamageComparison;
import equinox.task.PlotExternalLevelCrossing;
import equinox.task.PlotExternalTypicalFlights;
import equinox.task.PlotHistogram;
import equinox.task.PlotLevelCrossing;
import equinox.task.SaveANA;
import equinox.task.SaveCVT;
import equinox.task.SaveCategoryDataset;
import equinox.task.SaveChart;
import equinox.task.SaveConversionTable;
import equinox.task.SaveDamageAngles;
import equinox.task.SaveDamageContributions;
import equinox.task.SaveEquivalentStressPlotToFile;
import equinox.task.SaveEquivalentStressRatios;
import equinox.task.SaveEquivalentStresses;
import equinox.task.SaveExternalStressSequenceAsSIGMA;
import equinox.task.SaveExternalStressSequenceAsSTH;
import equinox.task.SaveFLS;
import equinox.task.SaveFlightDamageContributionPlot;
import equinox.task.SaveFlightDamageContributions;
import equinox.task.SaveLifeFactors;
import equinox.task.SaveLoadcaseDamageContributionComparisonPlot;
import equinox.task.SaveLoadcaseDamageContributionPlot;
import equinox.task.SaveMissionParameterPlot;
import equinox.task.SaveMissionProfile;
import equinox.task.SaveOutputFile;
import equinox.task.SaveRainflow;
import equinox.task.SaveSTF;
import equinox.task.SaveSpectrum;
import equinox.task.SaveSpectrumInfo;
import equinox.task.SaveStressSequenceAsSIGMA;
import equinox.task.SaveStressSequenceAsSTH;
import equinox.task.SaveStressSequenceInfo;
import equinox.task.SaveStressSequencePlotToFile;
import equinox.task.SaveTXT;
import equinox.task.SaveXYDataset;
import equinox.task.SaveXYSeriesCollection;
import equinox.task.SelectAllExternalFlights;
import equinox.task.SelectExternalFlight;
import equinox.task.SendTextMessage;
import equinox.task.SetSTFMission;
import equinox.task.ShareGeneratedItem;
import equinox.task.ShareSTF;
import equinox.task.ShareSpectrum;
import equinox.task.ShareSpectrumFile;
import equinox.task.TemporaryFileCreatingTask;
import equinox.task.UploadPilotPoints;
import equinox.task.UploadSpectra;
import equinox.utility.XMLUtilities;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.image.Image;

/**
 * Class for run instruction set task.
 *
 * @author Murat Artim
 * @date 17 Aug 2018
 * @time 15:12:11
 */
@SuppressWarnings("unchecked")
public class RunInstructionSet extends TemporaryFileCreatingTask<HashMap<String, InstructedTask>> implements LongRunningTask, ChangeListener<State>, AutomationTask {

	/** Run mode. */
	private ExecutionMode runMode = ExecutionMode.PARALLEL;

	/** Log level. */
	private Level logLevel;

	/** Input file and path to log directory. */
	private Path inputFile, logDirectory;

	/** True to generate execution plan. */
	private final boolean generateExecutionPlan;

	/** Number of all tasks to be executed. */
	private int allTasks = 0, requestId;

	/** Number of completed tasks. */
	private volatile int completedTasks = 0;

	/** Automation client handler. */
	private AutomationClientHandler automationClientHandler = null;

	/**
	 * Creates submit instruction set task.
	 *
	 * @param inputFile
	 *            Input XML/JSON file.
	 * @param generateExecutionPlan
	 *            True to generate execution plan.
	 */
	public RunInstructionSet(Path inputFile, boolean generateExecutionPlan) {
		this.inputFile = inputFile;
		this.generateExecutionPlan = generateExecutionPlan;
	}

	@Override
	public void setAutomationClientHandler(AutomationClientHandler automationClientHandler, int requestId) {
		this.automationClientHandler = automationClientHandler;
		this.requestId = requestId;
	}

	@Override
	public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {

		// succeeded, failed or canceled
		if (newValue.equals(State.SUCCEEDED) || newValue.equals(State.CANCELLED) || newValue.equals(State.FAILED)) {

			// increment completed tasks
			completedTasks++;

			// notify client handler of task progress (if set)
			if (automationClientHandler != null) {
				automationClientHandler.taskProgress(requestId, completedTasks * 100d / allTasks);
			}

			// all tasks completed
			if (completedTasks >= allTasks) {

				// notify client handler of task completion (if set)
				if (automationClientHandler != null) {
					automationClientHandler.taskCompleted(requestId);
				}

				// show completion popup (in javafx event queue)
				Platform.runLater(() -> {
					String title = "Task completed (in " + getDuration() + ")";
					String message = getTaskTitle() + " is successfully completed.";
					taskPanel_.getOwner().getOwner().getNotificationPane().showOk(title, message);
				});
			}
		}
	}

	@Override
	public String getTaskTitle() {
		return "Run instruction set";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected HashMap<String, InstructedTask> call() throws Exception {

		// input file is JSON
		if (FileType.getFileType(inputFile.toFile()).equals(FileType.JSON)) {

			// convert to XML file
			updateMessage("Converting input JSON file to XML file...");
			inputFile = new ConvertJSONtoXML(this, inputFile, null).start(null);
			setFileAsPermanent(inputFile);
		}

		// create list of tasks to be executed
		HashMap<String, InstructedTask> tasks = new HashMap<>();

		// read input file
		updateMessage("Reading input XML file...");

		// get root input element
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(inputFile.toUri().toString());
		Element equinoxInput = document.getRootElement();

		// get settings (if given)
		if (equinoxInput.getChild("settings") != null) {
			getSettings(equinoxInput);
		}

		// download spectrum
		if (equinoxInput.getChild("downloadSpectrum") != null) {
			downloadSpectrum(equinoxInput, tasks);
		}

		// add spectrum
		if (equinoxInput.getChild("addSpectrum") != null) {
			addSpectrum(equinoxInput, tasks);
		}

		// assign mission parameters to spectrum
		if (equinoxInput.getChild("assignMissionParametersToSpectrum") != null) {
			assignMissionParametersToSpectrum(equinoxInput, tasks);
		}

		// edit spectrum info
		if (equinoxInput.getChild("editSpectrumInfo") != null) {
			editSpectrumInfo(equinoxInput, tasks);
		}

		// save spectrum
		if (equinoxInput.getChild("saveSpectrum") != null) {
			saveSpectrum(equinoxInput, tasks);
		}

		// save spectrum file
		if (equinoxInput.getChild("saveSpectrumFile") != null) {
			saveSpectrumFile(equinoxInput, tasks);
		}

		// download STF
		if (equinoxInput.getChild("downloadStf") != null) {
			downloadStf(equinoxInput, tasks);
		}

		// add STF
		if (equinoxInput.getChild("addStf") != null) {
			addStf(equinoxInput, tasks);
		}

		// override fatigue mission
		if (equinoxInput.getChild("overrideFatigueMission") != null) {
			overrideFatigueMission(equinoxInput, tasks);
		}

		// assign mission parameters to STF
		if (equinoxInput.getChild("assignMissionParametersToStf") != null) {
			assignMissionParametersToStf(equinoxInput, tasks);
		}

		// save STF
		if (equinoxInput.getChild("saveStf") != null) {
			saveStf(equinoxInput, tasks);
		}

		// share STF
		if (equinoxInput.getChild("shareStf") != null) {
			shareStf(equinoxInput, tasks);
		}

		// add headless stress sequence
		if (equinoxInput.getChild("addHeadlessStressSequence") != null) {
			addHeadlessStressSequence(equinoxInput, tasks);
		}

		// generate stress sequence
		if (equinoxInput.getChild("generateStressSequence") != null) {
			generateStressSequence(equinoxInput, tasks);
		}

		// assign mission parameters to headless stress sequence
		if (equinoxInput.getChild("assignMissionParametersToStressSequence") != null) {
			assignMissionParametersToStressSequence(equinoxInput, tasks);
		}

		// edit stress sequence info
		if (equinoxInput.getChild("editStressSequenceInfo") != null) {
			editStressSequenceInfo(equinoxInput, tasks);
		}

		// save stress sequence
		if (equinoxInput.getChild("saveStressSequence") != null) {
			saveStressSequence(equinoxInput, tasks);
		}

		// fast equivalent stress analysis
		if (equinoxInput.getChild("fastEquivalentStressAnalysis") != null) {
			fastEquivalentStressAnalysis(equinoxInput, tasks);
		}

		// plot mission profile
		if (equinoxInput.getChild("plotMissionProfile") != null) {
			plotMissionProfile(equinoxInput, tasks);
		}

		// save mission profile info
		if (equinoxInput.getChild("saveMissionProfileInfo") != null) {
			saveMissionProfileInfo(equinoxInput, tasks);
		}

		// plot typical flight
		if (equinoxInput.getChild("plotTypicalFlight") != null) {
			plotTypicalFlight(equinoxInput, tasks, "plotTypicalFlight");
		}

		// plot typical flight statistics
		if (equinoxInput.getChild("plotTypicalFlightStatistics") != null) {
			plotTypicalFlight(equinoxInput, tasks, "plotTypicalFlightStatistics");
		}

		// equivalent stress analysis
		if (equinoxInput.getChild("equivalentStressAnalysis") != null) {
			equivalentStressAnalysis(equinoxInput, tasks);
		}

		// loadcase damage contribution analysis
		if (equinoxInput.getChild("loadcaseDamageContributionAnalysis") != null) {
			loadcaseDamageContributionAnalysis(equinoxInput, tasks);
		}

		// plot loadcase damage contribution
		if (equinoxInput.getChild("plotLoadcaseDamageContribution") != null) {
			plotLoadcaseDamageContribution(equinoxInput, tasks);
		}

		// save loadcase damage contributions
		if (equinoxInput.getChild("saveLoadcaseDamageContributions") != null) {
			saveLoadcaseDamageContributions(equinoxInput, tasks);
		}

		// export loadcase damage contributions
		if (equinoxInput.getChild("exportLoadcaseDamageContributions") != null) {
			exportLoadcaseDamageContributions(equinoxInput, tasks);
		}

		// plot loadcase damage contribution comparison
		if (equinoxInput.getChild("plotLoadcaseDamageContributionComparison") != null) {
			plotLoadcaseDamageContributionComparison(equinoxInput, tasks);
		}

		// typical flight damage contribution analysis
		if (equinoxInput.getChild("typicalFlightDamageContributionAnalysis") != null) {
			typicalFlightDamageContributionAnalysis(equinoxInput, tasks);
		}

		// plot typical flight damage contribution
		if (equinoxInput.getChild("plotTypicalFlightDamageContribution") != null) {
			plotTypicalFlightDamageContribution(equinoxInput, tasks);
		}

		// save typical flight damage contributions
		if (equinoxInput.getChild("saveTypicalFlightDamageContributions") != null) {
			saveTypicalFlightDamageContributions(equinoxInput, tasks);
		}

		// damage angle analysis
		if (equinoxInput.getChild("damageAngleAnalysis") != null) {
			damageAngleAnalysis(equinoxInput, tasks);
		}

		// plot damage angles
		if (equinoxInput.getChild("plotDamageAngles") != null) {
			plotDamageAngles(equinoxInput, tasks);
		}

		// save damage angles
		if (equinoxInput.getChild("saveDamageAngles") != null) {
			saveDamageAngles(equinoxInput, tasks);
		}

		// plot level crossing
		if (equinoxInput.getChild("plotLevelCrossing") != null) {
			plotLevelCrossing(equinoxInput, tasks);
		}

		// plot rainflow histogram
		if (equinoxInput.getChild("plotRainflowHistogram") != null) {
			plotRainflowHistogram(equinoxInput, tasks);
		}

		// save rainflow cycle info
		if (equinoxInput.getChild("saveRainflowCycleInfo") != null) {
			saveRainflowCycleInfo(equinoxInput, tasks);
		}

		// save analysis output file
		if (equinoxInput.getChild("saveAnalysisOutputFile") != null) {
			saveAnalysisOutputFile(equinoxInput, tasks);
		}

		// plot stress sequence comparison
		if (equinoxInput.getChild("plotStressSequenceComparison") != null) {
			plotStressSequenceComparison(equinoxInput, tasks);
		}

		// plot level crossing comparison
		if (equinoxInput.getChild("plotLevelCrossingComparison") != null) {
			plotLevelCrossingComparison(equinoxInput, tasks);
		}

		// plot typical flight comparison
		if (equinoxInput.getChild("plotTypicalFlightComparison") != null) {
			plotTypicalFlightComparison(equinoxInput, tasks);
		}

		// plot equivalent stress comparison
		if (equinoxInput.getChild("plotEquivalentStressComparison") != null) {
			plotEquivalentStressComparison(equinoxInput, tasks);
		}

		// plot life factors
		if (equinoxInput.getChild("plotLifeFactors") != null) {
			plotLifeFactors(equinoxInput, tasks);
		}

		// plot equivalent stress ratios
		if (equinoxInput.getChild("plotEquivalentStressRatios") != null) {
			plotEquivalentStressRatios(equinoxInput, tasks);
		}

		// save equivalent stresses
		if (equinoxInput.getChild("saveEquivalentStresses") != null) {
			saveEquivalentStresses(equinoxInput, tasks);
		}

		// save life factors
		if (equinoxInput.getChild("saveLifeFactors") != null) {
			saveLifeFactors(equinoxInput, tasks);
		}

		// save equivalent stress ratios
		if (equinoxInput.getChild("saveEquivalentStressRatios") != null) {
			saveEquivalentStressRatios(equinoxInput, tasks);
		}

		// share spectrum
		if (equinoxInput.getChild("shareSpectrum") != null) {
			shareSpectrum(equinoxInput, tasks);
		}

		// share spectrum file
		if (equinoxInput.getChild("shareSpectrumFile") != null) {
			shareSpectrumFile(equinoxInput, tasks);
		}

		// export spectrum
		if (equinoxInput.getChild("exportSpectrum") != null) {
			exportSpectrum(equinoxInput, tasks);
		}

		// upload spectrum
		if (equinoxInput.getChild("uploadSpectrum") != null) {
			uploadSpectrum(equinoxInput, tasks);
		}

		// export STF
		if (equinoxInput.getChild("exportStf") != null) {
			exportStf(equinoxInput, tasks);
		}

		// upload STF
		if (equinoxInput.getChild("uploadStf") != null) {
			uploadStf(equinoxInput, tasks);
		}

		// send text message
		if (equinoxInput.getChild("sendTextMessage") != null) {
			sendTextMessage(equinoxInput, tasks);
		}

		// share file
		if (equinoxInput.getChild("shareFile") != null) {
			shareFile(equinoxInput, tasks);
		}

		// return tasks to be executed
		return tasks;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// execute tasks
		try {

			// get tasks
			HashMap<String, InstructedTask> tasks = get();

			// no tasks found
			if (tasks == null) {

				// notify client handler of task completion (if set)
				if (automationClientHandler != null) {
					automationClientHandler.taskFailed(requestId);
				}
				return;
			}

			// generate execution plan
			if (generateExecutionPlan) {

				// plot
				TaskExecutionPlanViewPanel panel = (TaskExecutionPlanViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.EXECUTION_TREE_VIEW_PANEL);
				panel.setAutomaticTasks(this, tasks);
				taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.EXECUTION_TREE_VIEW_PANEL);

				// notify client handler of task completion (if set)
				if (automationClientHandler != null) {
					automationClientHandler.taskCompleted(requestId);
				}
			}

			// run tasks
			else {

				// loop over tasks
				Iterator<Entry<String, InstructedTask>> iterator = tasks.entrySet().iterator();
				allTasks = tasks.size();
				while (iterator.hasNext()) {

					// get instructed task
					InstructedTask instructedTask = iterator.next().getValue();

					// get task
					InternalEquinoxTask<?> task = instructedTask.getTask();

					// listen for state change
					task.stateProperty().addListener(this);

					// embedded
					if (instructedTask.isEmbedded()) {
						continue;
					}

					// logging requested
					if (logLevel != null && logDirectory != null) {

						// create logger for the task
						XMLUtilities.createLogger(logDirectory, task, logLevel);

						// create loggers for automatic tasks
						if (task instanceof AutomaticTaskOwner) {
							createLoggersForAutomaticTasks((AutomaticTaskOwner<?>) task);
						}

						// create loggers for follower tasks
						createLoggersForFollowerTasks(task);
					}

					// run task
					taskPanel_.getOwner().runTaskSilently(task, runMode.equals(ExecutionMode.SEQUENTIAL));
				}
			}
		}

		// exception occurred during retrieving results
		catch (Exception e) {
			handleResultRetrievalException(e);

			// notify client handler of task completion (if set)
			if (automationClientHandler != null) {
				automationClientHandler.taskFailed(requestId);
			}
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// notify client handler of task failure (if set)
		if (automationClientHandler != null) {
			automationClientHandler.taskFailed(requestId);
		}
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// notify client handler of task failure (if set)
		if (automationClientHandler != null) {
			automationClientHandler.taskFailed(requestId);
		}
	}

	/**
	 * Creates loggers for automatic tasks.
	 *
	 * @param sourceTask
	 *            Automatic task owner.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	@SuppressWarnings("rawtypes")
	private void createLoggersForAutomaticTasks(AutomaticTaskOwner sourceTask) throws Exception {

		// no automatic task
		if (sourceTask.getAutomaticTasks() == null || sourceTask.getAutomaticTasks().isEmpty())
			return;

		// loop over automatic tasks
		Iterator<EmbeddedTask> iterator = sourceTask.getAutomaticTasks().values().iterator();
		while (iterator.hasNext()) {

			// get task
			AutomaticTask task = iterator.next().getTask();

			// create logger
			XMLUtilities.createLogger(logDirectory, (InternalEquinoxTask<?>) task, logLevel);

			// create loggers for automatic tasks
			if (task instanceof AutomaticTaskOwner) {
				createLoggersForAutomaticTasks((AutomaticTaskOwner) task);
			}

			// create loggers for follower tasks
			createLoggersForFollowerTasks((InternalEquinoxTask<?>) task);
		}
	}

	/**
	 * Creates loggers for follower tasks.
	 *
	 * @param sourceTask
	 *            Source task.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void createLoggersForFollowerTasks(InternalEquinoxTask<?> sourceTask) throws Exception {

		// no follower task
		if (sourceTask.getFollowerTasks() == null || sourceTask.getFollowerTasks().isEmpty())
			return;

		// loop over follower tasks
		for (InternalEquinoxTask<?> task : sourceTask.getFollowerTasks().keySet()) {

			// create logger
			XMLUtilities.createLogger(logDirectory, task, logLevel);

			// create loggers for automatic tasks
			if (task instanceof AutomaticTaskOwner) {
				createLoggersForAutomaticTasks((AutomaticTaskOwner<?>) task);
			}

			// create loggers for follower tasks
			createLoggersForFollowerTasks(task);
		}
	}

	/**
	 * Reads instruction set settings.
	 *
	 * @param equinoxInput
	 *            Root input element
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getSettings(Element equinoxInput) throws Exception {

		// update info
		updateMessage("Reading instruction set settings...");

		// get element
		Element settings = equinoxInput.getChild("settings");

		// check logger (if not generate execution plan mode)
		if (!generateExecutionPlan && settings.getChild("logger") != null) {

			// get logger element
			Element logger = settings.getChild("logger");

			// get log level and directory path
			logLevel = Level.parse(logger.getChildTextNormalize("logLevel"));
			logDirectory = Paths.get(logger.getChildTextNormalize("logDirectoryPath"));

			// create logger for this task
			XMLUtilities.createLogger(logDirectory, this, logLevel);
		}

		// get run mode (if given)
		if (settings.getChild("runMode") != null) {
			runMode = ExecutionMode.valueOf(settings.getChild("runMode").getTextNormalize().toUpperCase());
		}
	}

	/**
	 * Creates send text message tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void sendTextMessage(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating send text message tasks...");

		// loop over send text message elements
		for (Element sendTextMessage : equinoxInput.getChildren("sendTextMessage")) {

			// create task
			String id = sendTextMessage.getChildTextNormalize("id");
			String message = sendTextMessage.getChildTextNormalize("message");
			String recipient = sendTextMessage.getChild("recipient").getTextNormalize();

			// create task
			SendTextMessage task = new SendTextMessage(message, taskPanel_.getOwner().getOwner().getUser(recipient));

			// previous instruction given
			if (sendTextMessage.getChild("previousInstructionId") != null) {

				// get previous instruction id
				String previousInstructionId = sendTextMessage.getChildTextNormalize("previousInstructionId");

				// add to parent task
				InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
				parentTask.addFollowerTask(task, runMode);

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}

			// not given
			else {
				tasks.put(id, new InstructedTask(task, false));
			}
		}
	}

	/**
	 * Creates edit spectrum info tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void editSpectrumInfo(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating edit spectrum info tasks...");

		// loop over edit spectrum info elements
		for (Element editSpectrumInfo : equinoxInput.getChildren("editSpectrumInfo")) {

			// create task
			String id = editSpectrumInfo.getChildTextNormalize("id");
			String spectrumId = editSpectrumInfo.getChildTextNormalize("spectrumId");
			String deliveryReference = editSpectrumInfo.getChildTextNormalize("deliveryReference");
			String description = editSpectrumInfo.getChildTextNormalize("description");

			// set info
			String[] info = new String[9];
			info[GetSpectrumEditInfo.DELIVERY_REF] = deliveryReference;
			info[GetSpectrumEditInfo.DESCRIPTION] = description;

			// create task
			SaveSpectrumInfo task = new SaveSpectrumInfo(null, info);

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates edit stress sequence info tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void editStressSequenceInfo(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating edit stress sequence info tasks...");

		// loop over edit stress sequence info elements
		for (Element editStressSequenceInfo : equinoxInput.getChildren("editStressSequenceInfo")) {

			// create task
			String id = editStressSequenceInfo.getChild("id").getTextNormalize();
			String headlessStressSequenceId = editStressSequenceInfo.getChild("headlessStressSequenceId").getTextNormalize();

			// initialize info array
			String[] info = new String[3];

			// aircraft program
			if (editStressSequenceInfo.getChild("aircraftProgram") != null) {
				info[SaveStressSequenceInfo.PROGRAM] = editStressSequenceInfo.getChildTextNormalize("aircraftProgram");
			}

			// aircraft section
			if (editStressSequenceInfo.getChild("aircraftSection") != null) {
				info[SaveStressSequenceInfo.SECTION] = editStressSequenceInfo.getChildTextNormalize("aircraftSection");
			}

			// fatigue mission
			if (editStressSequenceInfo.getChild("fatigueMission") != null) {
				info[SaveStressSequenceInfo.MISSION] = editStressSequenceInfo.getChildTextNormalize("fatigueMission");
			}

			// create task
			SaveStressSequenceInfo task = new SaveStressSequenceInfo(null, info);

			// add to parent task
			AutomaticTaskOwner<ExternalStressSequence> parentTask = (AutomaticTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates assign mission parameters to headless stress sequence tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void assignMissionParametersToStressSequence(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating assign mission parameters to headless stress sequence tasks...");

		// loop over assign mission parameters to headless stress sequence elements
		for (Element assignMissionParametersToStressSequence : equinoxInput.getChildren("assignMissionParametersToStressSequence")) {

			// create task
			String id = assignMissionParametersToStressSequence.getChild("id").getTextNormalize();
			String headlessStressSequenceId = assignMissionParametersToStressSequence.getChild("headlessStressSequenceId").getTextNormalize();
			ArrayList<MissionParameter> parameters = new ArrayList<>();

			// loop over mission parameter elements
			for (Element missionParameter : assignMissionParametersToStressSequence.getChildren("missionParameter")) {

				// get parameter name and value
				String name = missionParameter.getChildTextNormalize("name");
				double value = Double.parseDouble(missionParameter.getChildTextNormalize("value"));

				// create and add mission parameter
				parameters.add(new MissionParameter(name, value));
			}

			// create task
			AssignMissionParameters<ExternalStressSequence> assignMissionParametersTask = new AssignMissionParameters<>(null, parameters.toArray(new MissionParameter[parameters.size()]));

			// add to parent task
			AutomaticTaskOwner<ExternalStressSequence> parentTask = (AutomaticTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(assignMissionParametersTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(assignMissionParametersTask, true));
		}
	}

	/**
	 * Creates save equivalent stress ratios tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveEquivalentStressRatios(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save equivalent stress ratios tasks...");

		// loop over save equivalent stress ratios elements
		for (Element saveEquivalentStressRatios : equinoxInput.getChildren("saveEquivalentStressRatios")) {

			// get id and output path
			String id = saveEquivalentStressRatios.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveEquivalentStressRatios.getChildTextNormalize("outputPath"));
			String basisMission = saveEquivalentStressRatios.getChildTextNormalize("basisMission");

			// set reference name
			String referenceIdName = null;
			if (saveEquivalentStressRatios.getChild("equivalentStressId") != null) {
				referenceIdName = "equivalentStressId";
			}
			else if (saveEquivalentStressRatios.getChild("fastEquivalentStressId") != null) {
				referenceIdName = "fastEquivalentStressId";
			}
			else if (saveEquivalentStressRatios.getChild("headlessEquivalentStressId") != null) {
				referenceIdName = "headlessEquivalentStressId";
			}

			// options
			BooleanProperty[] options = new BooleanProperty[15];
			for (int i = 0; i < options.length; i++) {
				options[i] = new SimpleBooleanProperty(false);
			}

			// set default options
			if (referenceIdName.equals("equivalentStressId")) {
				options[SaveEquivalentStressRatios.STRESS_RATIO].set(true);
				options[SaveEquivalentStressRatios.PP_NAME].set(true);
				options[SaveEquivalentStressRatios.MISSION].set(true);
			}
			else if (referenceIdName.equals("fastEquivalentStressId")) {
				options[SaveEquivalentStressRatios.STRESS_RATIO].set(true);
				options[SaveEquivalentStressRatios.PP_NAME].set(true);
				options[SaveEquivalentStressRatios.MISSION].set(true);
			}
			else if (referenceIdName.equals("headlessEquivalentStressId")) {
				options[SaveEquivalentStressRatios.STRESS_RATIO].set(true);
				options[SaveEquivalentStressRatios.SEQ_NAME].set(true);
				options[SaveEquivalentStressRatios.MISSION].set(true);
			}

			// read options
			if (saveEquivalentStressRatios.getChild("options") != null) {

				// get element
				Element optionsElement = saveEquivalentStressRatios.getChild("options");

				// set options
				if (optionsElement.getChild("equivalentStressRatio") != null) {
					options[SaveEquivalentStressRatios.STRESS_RATIO] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("equivalentStressRatio")));
				}
				if (optionsElement.getChild("materialName") != null) {
					options[SaveEquivalentStressRatios.MAT_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialName")));
				}
				if (optionsElement.getChild("materialData") != null) {
					options[SaveEquivalentStressRatios.MAT_DATA] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialData")));
				}
				if (optionsElement.getChild("pilotPointName") != null) {
					options[SaveEquivalentStressRatios.PP_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("pilotPointName")));
				}
				if (optionsElement.getChild("elementId") != null) {
					options[SaveEquivalentStressRatios.EID] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("elementId")));
				}
				if (optionsElement.getChild("stressSequenceName") != null) {
					options[SaveEquivalentStressRatios.SEQ_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("stressSequenceName")));
				}
				if (optionsElement.getChild("spectrumName") != null) {
					options[SaveEquivalentStressRatios.SPEC_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumName")));
				}
				if (optionsElement.getChild("aircraftProgram") != null) {
					options[SaveEquivalentStressRatios.PROGRAM] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftProgram")));
				}
				if (optionsElement.getChild("aircraftSection") != null) {
					options[SaveEquivalentStressRatios.SECTION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftSection")));
				}
				if (optionsElement.getChild("fatigueMission") != null) {
					options[SaveEquivalentStressRatios.MISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("fatigueMission")));
				}
				if (optionsElement.getChild("spectrumValidity") != null) {
					options[SaveEquivalentStressRatios.VALIDITY] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumValidity")));
				}
				if (optionsElement.getChild("maximumStress") != null) {
					options[SaveEquivalentStressRatios.MAX_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("maximumStress")));
				}
				if (optionsElement.getChild("minimumStress") != null) {
					options[SaveEquivalentStressRatios.MIN_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("minimumStress")));
				}
				if (optionsElement.getChild("rRatio") != null) {
					options[SaveEquivalentStressRatios.R_RATIO] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("rRatio")));
				}
				if (optionsElement.getChild("omissionLevel") != null) {
					options[SaveEquivalentStressRatios.OMISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("omissionLevel")));
				}
			}

			// create task
			SaveEquivalentStressRatios task = new SaveEquivalentStressRatios(null, options, outputPath.toFile(), basisMission);

			// set input threshold
			List<Element> equivalentStressIds = saveEquivalentStressRatios.getChildren(referenceIdName);
			task.setInputThreshold(equivalentStressIds.size());

			// loop over stress ids
			for (Element equivalentStressIdElement : equivalentStressIds) {

				// get stress id
				String equivalentStressId = equivalentStressIdElement.getTextNormalize();

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates save life factors tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveLifeFactors(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save life factors tasks...");

		// loop over save life factors elements
		for (Element saveLifeFactors : equinoxInput.getChildren("saveLifeFactors")) {

			// get id and output path
			String id = saveLifeFactors.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveLifeFactors.getChildTextNormalize("outputPath"));
			String basisMission = saveLifeFactors.getChildTextNormalize("basisMission");

			// set reference name
			String referenceIdName = null;
			if (saveLifeFactors.getChild("equivalentStressId") != null) {
				referenceIdName = "equivalentStressId";
			}
			else if (saveLifeFactors.getChild("fastEquivalentStressId") != null) {
				referenceIdName = "fastEquivalentStressId";
			}
			else if (saveLifeFactors.getChild("headlessEquivalentStressId") != null) {
				referenceIdName = "headlessEquivalentStressId";
			}

			// options
			BooleanProperty[] options = new BooleanProperty[15];
			for (int i = 0; i < options.length; i++) {
				options[i] = new SimpleBooleanProperty(false);
			}

			// set default options
			if (referenceIdName.equals("equivalentStressId")) {
				options[SaveLifeFactors.LIFE_FACTOR].set(true);
				options[SaveLifeFactors.PP_NAME].set(true);
				options[SaveLifeFactors.MISSION].set(true);
			}
			else if (referenceIdName.equals("fastEquivalentStressId")) {
				options[SaveLifeFactors.LIFE_FACTOR].set(true);
				options[SaveLifeFactors.PP_NAME].set(true);
				options[SaveLifeFactors.MISSION].set(true);
			}
			else if (referenceIdName.equals("headlessEquivalentStressId")) {
				options[SaveLifeFactors.LIFE_FACTOR].set(true);
				options[SaveLifeFactors.SEQ_NAME].set(true);
				options[SaveLifeFactors.MISSION].set(true);
			}

			// read options
			if (saveLifeFactors.getChild("options") != null) {

				// get element
				Element optionsElement = saveLifeFactors.getChild("options");

				// set options
				if (optionsElement.getChild("lifeFactor") != null) {
					options[SaveLifeFactors.LIFE_FACTOR] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("lifeFactor")));
				}
				if (optionsElement.getChild("materialName") != null) {
					options[SaveLifeFactors.MAT_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialName")));
				}
				if (optionsElement.getChild("materialData") != null) {
					options[SaveLifeFactors.MAT_DATA] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialData")));
				}
				if (optionsElement.getChild("pilotPointName") != null) {
					options[SaveLifeFactors.PP_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("pilotPointName")));
				}
				if (optionsElement.getChild("elementId") != null) {
					options[SaveLifeFactors.EID] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("elementId")));
				}
				if (optionsElement.getChild("stressSequenceName") != null) {
					options[SaveLifeFactors.SEQ_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("stressSequenceName")));
				}
				if (optionsElement.getChild("spectrumName") != null) {
					options[SaveLifeFactors.SPEC_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumName")));
				}
				if (optionsElement.getChild("aircraftProgram") != null) {
					options[SaveLifeFactors.PROGRAM] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftProgram")));
				}
				if (optionsElement.getChild("aircraftSection") != null) {
					options[SaveLifeFactors.SECTION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftSection")));
				}
				if (optionsElement.getChild("fatigueMission") != null) {
					options[SaveLifeFactors.MISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("fatigueMission")));
				}
				if (optionsElement.getChild("spectrumValidity") != null) {
					options[SaveLifeFactors.VALIDITY] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumValidity")));
				}
				if (optionsElement.getChild("maximumStress") != null) {
					options[SaveLifeFactors.MAX_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("maximumStress")));
				}
				if (optionsElement.getChild("minimumStress") != null) {
					options[SaveLifeFactors.MIN_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("minimumStress")));
				}
				if (optionsElement.getChild("rRatio") != null) {
					options[SaveLifeFactors.R_RATIO] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("rRatio")));
				}
				if (optionsElement.getChild("omissionLevel") != null) {
					options[SaveLifeFactors.OMISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("omissionLevel")));
				}
			}

			// create task
			SaveLifeFactors task = new SaveLifeFactors(null, options, outputPath.toFile(), basisMission);

			// set input threshold
			List<Element> equivalentStressIds = saveLifeFactors.getChildren(referenceIdName);
			task.setInputThreshold(equivalentStressIds.size());

			// loop over stress ids
			for (Element equivalentStressIdElement : equivalentStressIds) {

				// get stress id
				String equivalentStressId = equivalentStressIdElement.getTextNormalize();

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates plot loadcase damage contribution comparison tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLoadcaseDamageContributionComparison(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot loadcase damage contribution comparison tasks...");

		// loop over plot loadcase damage contribution comparison elements
		for (Element plotLoadcaseDamageContributionComparison : equinoxInput.getChildren("plotLoadcaseDamageContributionComparison")) {

			// get id and output path
			String id = plotLoadcaseDamageContributionComparison.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotLoadcaseDamageContributionComparison.getChildTextNormalize("outputPath"));
			String contributionName = plotLoadcaseDamageContributionComparison.getChildTextNormalize("contributionName");

			// get series naming
			boolean[] namingOptions = new boolean[7];
			for (int i = 0; i < namingOptions.length; i++) {
				namingOptions[i] = false;
			}
			namingOptions[CompareDamageContributionsInput.STF_NAME] = true;
			if (plotLoadcaseDamageContributionComparison.getChild("seriesNaming") != null) {
				Element seriesNaming = plotLoadcaseDamageContributionComparison.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					namingOptions[CompareDamageContributionsInput.SPECTRUM_NAME] = Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName"));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					namingOptions[CompareDamageContributionsInput.STF_NAME] = Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName"));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					namingOptions[CompareDamageContributionsInput.EID] = Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId"));
				}
				if (seriesNaming.getChild("includeMaterialName") != null) {
					namingOptions[CompareDamageContributionsInput.MATERIAL_NAME] = Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName"));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					namingOptions[CompareDamageContributionsInput.PROGRAM] = Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram"));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					namingOptions[CompareDamageContributionsInput.SECTION] = Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection"));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					namingOptions[CompareDamageContributionsInput.MISSION] = Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission"));
				}
			}

			// get contribution type
			ContributionType contributionType = ContributionType.INCREMENT;
			for (ContributionType type : ContributionType.values()) {
				if (type.getName().equals(contributionName)) {
					contributionType = type;
					break;
				}
			}

			// create input
			CompareDamageContributionsInput input = new CompareDamageContributionsInput(Arrays.asList(contributionName), namingOptions);

			// create tasks
			PlotDamageComparison plotTask = new PlotDamageComparison(input, contributionType);
			SaveLoadcaseDamageContributionComparisonPlot saveTask = new SaveLoadcaseDamageContributionComparisonPlot(null, null, outputPath);
			plotTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));

			// set input threshold
			List<Element> loadcaseDamageContributionIds = plotLoadcaseDamageContributionComparison.getChildren("loadcaseDamageContributionId");
			plotTask.setInputThreshold(loadcaseDamageContributionIds.size());

			// loop over loadcase damage contribution ids
			for (Element loadcaseDamageContributionIdElement : loadcaseDamageContributionIds) {

				// get loadcase damage contribution id
				String loadcaseDamageContributionId = loadcaseDamageContributionIdElement.getTextNormalize();

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(loadcaseDamageContributionId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(plotTask, runMode));
			}

			// put task to tasks
			tasks.put("ownerOf_" + id, new InstructedTask(plotTask, true));
			tasks.put(id, new InstructedTask(saveTask, true));
		}
	}

	/**
	 * Creates export loadcase damage contributions tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void exportLoadcaseDamageContributions(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating export loadcase damage contributions tasks...");

		// loop over export loadcase damage contributions elements
		for (Element exportLoadcaseDamageContributions : equinoxInput.getChildren("exportLoadcaseDamageContributions")) {

			// get id and output path
			String id = exportLoadcaseDamageContributions.getChildTextNormalize("id");
			Path outputPath = Paths.get(exportLoadcaseDamageContributions.getChildTextNormalize("outputPath"));

			// create task
			ExportContributions task = new ExportContributions(outputPath.toFile(), null);

			// set input threshold
			List<Element> loadcaseDamageContributionIds = exportLoadcaseDamageContributions.getChildren("loadcaseDamageContributionId");
			task.setInputThreshold(loadcaseDamageContributionIds.size());

			// loop over loadcase damage contribution ids
			for (Element loadcaseDamageContributionIdElement : loadcaseDamageContributionIds) {

				// get loadcase damage contribution id
				String loadcaseDamageContributionId = loadcaseDamageContributionIdElement.getTextNormalize();

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(loadcaseDamageContributionId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates save damage angles tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveDamageAngles(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save damage angles tasks...");

		// loop over save damage angles elements
		for (Element saveDamageAngles : equinoxInput.getChildren("saveDamageAngles")) {

			// get id and output path
			String id = saveDamageAngles.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveDamageAngles.getChildTextNormalize("outputPath"));

			// create default options
			BooleanProperty[] options = new BooleanProperty[12];
			for (int i = 0; i < options.length; i++) {
				options[i] = new SimpleBooleanProperty(false);
			}
			options[SaveDamageAngles.DAM_ANGLE].set(true);
			options[SaveDamageAngles.FAT_STRESS].set(true);
			options[SaveDamageAngles.MAT_NAME].set(true);
			options[SaveDamageAngles.PP_NAME].set(true);
			options[SaveDamageAngles.MISSION].set(true);

			// read options
			if (saveDamageAngles.getChild("options") != null) {

				// get element
				Element optionsElement = saveDamageAngles.getChild("options");

				// set options
				if (optionsElement.getChild("maxDamageAngle") != null) {
					options[SaveDamageAngles.DAM_ANGLE].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("maxDamageAngle")));
				}
				if (optionsElement.getChild("maxEquivalentStress") != null) {
					options[SaveDamageAngles.FAT_STRESS].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("maxEquivalentStress")));
				}
				if (optionsElement.getChild("materialName") != null) {
					options[SaveDamageAngles.MAT_NAME].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialName")));
				}
				if (optionsElement.getChild("materialSlope") != null) {
					options[SaveDamageAngles.FAT_P].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialSlope")));
				}
				if (optionsElement.getChild("materialConstant") != null) {
					options[SaveDamageAngles.FAT_Q].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialConstant")));
				}
				if (optionsElement.getChild("pilotPointName") != null) {
					options[SaveDamageAngles.PP_NAME].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("pilotPointName")));
				}
				if (optionsElement.getChild("elementId") != null) {
					options[SaveDamageAngles.EID].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("elementId")));
				}
				if (optionsElement.getChild("spectrumName") != null) {
					options[SaveDamageAngles.SPEC_NAME].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumName")));
				}
				if (optionsElement.getChild("aircraftProgram") != null) {
					options[SaveDamageAngles.PROGRAM].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftProgram")));
				}
				if (optionsElement.getChild("aircraftSection") != null) {
					options[SaveDamageAngles.SECTION].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftSection")));
				}
				if (optionsElement.getChild("fatigueMission") != null) {
					options[SaveDamageAngles.MISSION].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("fatigueMission")));
				}
				if (optionsElement.getChild("omissionLevel") != null) {
					options[SaveDamageAngles.OMISSION].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("omissionLevel")));
				}
			}

			// create task
			SaveDamageAngles task = new SaveDamageAngles(null, options, outputPath.toFile());

			// set input threshold
			List<Element> damageAngleIds = saveDamageAngles.getChildren("damageAngleId");
			task.setInputThreshold(damageAngleIds.size());

			// loop over damage angle ids
			for (Element damageAngleIdElement : damageAngleIds) {

				// get damage angle id
				String damageAngleId = damageAngleIdElement.getTextNormalize();

				// connect to parent task
				AutomaticTaskOwner<DamageAngle> parentTask = (AutomaticTaskOwner<DamageAngle>) tasks.get(damageAngleId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates save typical flight damage contributions tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveTypicalFlightDamageContributions(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save typical flight damage contributions tasks...");

		// loop over save typical flight damage contributions elements
		for (Element saveTypicalFlightDamageContributions : equinoxInput.getChildren("saveTypicalFlightDamageContributions")) {

			// get id and output path
			String id = saveTypicalFlightDamageContributions.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveTypicalFlightDamageContributions.getChildTextNormalize("outputPath"));

			// create default options
			BooleanProperty[] options = new BooleanProperty[10];
			for (int i = 0; i < options.length; i++) {
				options[i] = new SimpleBooleanProperty(false);
			}
			options[SaveFlightDamageContributions.PP_NAME].set(true);
			options[SaveFlightDamageContributions.MISSION].set(true);

			// read options
			if (saveTypicalFlightDamageContributions.getChild("options") != null) {

				// get element
				Element optionsElement = saveTypicalFlightDamageContributions.getChild("options");

				// set options
				if (optionsElement.getChild("materialName") != null) {
					options[SaveFlightDamageContributions.MAT_NAME].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialName")));
				}
				if (optionsElement.getChild("materialSlope") != null) {
					options[SaveFlightDamageContributions.FAT_P].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialSlope")));
				}
				if (optionsElement.getChild("materialConstant") != null) {
					options[SaveFlightDamageContributions.FAT_Q].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialConstant")));
				}
				if (optionsElement.getChild("pilotPointName") != null) {
					options[SaveFlightDamageContributions.PP_NAME].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("pilotPointName")));
				}
				if (optionsElement.getChild("elementId") != null) {
					options[SaveFlightDamageContributions.EID].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("elementId")));
				}
				if (optionsElement.getChild("spectrumName") != null) {
					options[SaveFlightDamageContributions.SPEC_NAME].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumName")));
				}
				if (optionsElement.getChild("aircraftProgram") != null) {
					options[SaveFlightDamageContributions.PROGRAM].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftProgram")));
				}
				if (optionsElement.getChild("aircraftSection") != null) {
					options[SaveFlightDamageContributions.SECTION].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftSection")));
				}
				if (optionsElement.getChild("fatigueMission") != null) {
					options[SaveFlightDamageContributions.MISSION].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("fatigueMission")));
				}
				if (optionsElement.getChild("omissionLevel") != null) {
					options[SaveFlightDamageContributions.OMISSION].set(Boolean.parseBoolean(optionsElement.getChildTextNormalize("omissionLevel")));
				}
			}

			// create tasks
			GetFlightContributionNames getNamesTask = new GetFlightContributionNames(null, null);
			SaveFlightDamageContributions saveTask = new SaveFlightDamageContributions(null, null, null, options, outputPath.toFile());
			getNamesTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));

			// set input threshold
			List<Element> typicalFlightDamageContributionIds = saveTypicalFlightDamageContributions.getChildren("typicalFlightDamageContributionId");
			getNamesTask.setInputThreshold(typicalFlightDamageContributionIds.size());

			// loop over typical flight damage contribution ids
			for (Element typicalFlightDamageContributionIdElement : typicalFlightDamageContributionIds) {

				// get typical flight damage contribution id
				String typicalFlightDamageContributionId = typicalFlightDamageContributionIdElement.getTextNormalize();

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(typicalFlightDamageContributionId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(getNamesTask, runMode));
			}

			// put task to tasks
			tasks.put("ownerOf_" + id, new InstructedTask(getNamesTask, true));
			tasks.put(id, new InstructedTask(saveTask, true));
		}
	}

	/**
	 * Creates save loadcase damage contributions tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveLoadcaseDamageContributions(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save loadcase damage contributions tasks...");

		// loop over save loadcase damage contributions elements
		for (Element saveLoadcaseDamageContributions : equinoxInput.getChildren("saveLoadcaseDamageContributions")) {

			// get id and output path
			String id = saveLoadcaseDamageContributions.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveLoadcaseDamageContributions.getChildTextNormalize("outputPath"));

			// create default options
			boolean[] options = new boolean[17];
			for (int i = 0; i < options.length; i++) {
				options[i] = false;
			}
			options[SaveDamageContributions.PERCENT] = true;
			options[SaveDamageContributions.INC] = true;
			options[SaveDamageContributions.ONEG] = true;
			options[SaveDamageContributions.PP_NAME] = true;
			options[SaveDamageContributions.SPEC_NAME] = true;
			options[SaveDamageContributions.PROGRAM] = true;
			options[SaveDamageContributions.SECTION] = true;
			options[SaveDamageContributions.MISSION] = true;

			// read options
			if (saveLoadcaseDamageContributions.getChild("options") != null) {

				// get element
				Element optionsElement = saveLoadcaseDamageContributions.getChild("options");

				// set options
				if (optionsElement.getChild("percentContributions") != null) {
					options[SaveDamageContributions.PERCENT] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("percentContributions"));
				}
				if (optionsElement.getChild("totalEquivalentStresses") != null) {
					options[SaveDamageContributions.FULL] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("totalEquivalentStresses"));
				}
				if (optionsElement.getChild("incrementContributions") != null) {
					options[SaveDamageContributions.INC] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("incrementContributions"));
				}
				if (optionsElement.getChild("onegContributions") != null) {
					options[SaveDamageContributions.ONEG] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("onegContributions"));
				}
				if (optionsElement.getChild("gagContributions") != null) {
					options[SaveDamageContributions.GAG] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("gagContributions"));
				}
				if (optionsElement.getChild("deltaPContributions") != null) {
					options[SaveDamageContributions.DP] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("deltaPContributions"));
				}
				if (optionsElement.getChild("deltaTContributions") != null) {
					options[SaveDamageContributions.DT] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("deltaTContributions"));
				}
				if (optionsElement.getChild("materialName") != null) {
					options[SaveDamageContributions.MAT_NAME] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialName"));
				}
				if (optionsElement.getChild("materialSlope") != null) {
					options[SaveDamageContributions.FAT_P] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialSlope"));
				}
				if (optionsElement.getChild("materialConstant") != null) {
					options[SaveDamageContributions.FAT_Q] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialConstant"));
				}
				if (optionsElement.getChild("pilotPointName") != null) {
					options[SaveDamageContributions.PP_NAME] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("pilotPointName"));
				}
				if (optionsElement.getChild("elementId") != null) {
					options[SaveDamageContributions.EID] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("elementId"));
				}
				if (optionsElement.getChild("spectrumName") != null) {
					options[SaveDamageContributions.SPEC_NAME] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumName"));
				}
				if (optionsElement.getChild("aircraftProgram") != null) {
					options[SaveDamageContributions.PROGRAM] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftProgram"));
				}
				if (optionsElement.getChild("aircraftSection") != null) {
					options[SaveDamageContributions.SECTION] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftSection"));
				}
				if (optionsElement.getChild("fatigueMission") != null) {
					options[SaveDamageContributions.MISSION] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("fatigueMission"));
				}
				if (optionsElement.getChild("omissionLevel") != null) {
					options[SaveDamageContributions.OMISSION] = Boolean.parseBoolean(optionsElement.getChildTextNormalize("omissionLevel"));
				}
			}

			// create tasks
			GetContributionNames getNamesTask = new GetContributionNames(null, null);
			SaveDamageContributions saveTask = new SaveDamageContributions(null, null, options, outputPath.toFile());
			getNamesTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));

			// set input threshold
			List<Element> loadcaseDamageContributionIds = saveLoadcaseDamageContributions.getChildren("loadcaseDamageContributionId");
			getNamesTask.setInputThreshold(loadcaseDamageContributionIds.size());

			// loop over loadcase damage contribution ids
			for (Element loadcaseDamageContributionIdElement : loadcaseDamageContributionIds) {

				// get loadcase damage contribution id
				String loadcaseDamageContributionId = loadcaseDamageContributionIdElement.getTextNormalize();

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(loadcaseDamageContributionId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(getNamesTask, runMode));
			}

			// put task to tasks
			tasks.put("ownerOf_" + id, new InstructedTask(getNamesTask, true));
			tasks.put(id, new InstructedTask(saveTask, true));
		}
	}

	/**
	 * Creates save equivalent stresses tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveEquivalentStresses(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save equivalent stresses tasks...");

		// loop over save equivalent stresses elements
		for (Element saveEquivalentStresses : equinoxInput.getChildren("saveEquivalentStresses")) {

			// get id and output path
			String id = saveEquivalentStresses.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveEquivalentStresses.getChildTextNormalize("outputPath"));

			// set reference name
			String referenceIdName = null;
			if (saveEquivalentStresses.getChild("equivalentStressId") != null) {
				referenceIdName = "equivalentStressId";
			}
			else if (saveEquivalentStresses.getChild("fastEquivalentStressId") != null) {
				referenceIdName = "fastEquivalentStressId";
			}
			else if (saveEquivalentStresses.getChild("headlessEquivalentStressId") != null) {
				referenceIdName = "headlessEquivalentStressId";
			}

			// options
			BooleanProperty[] options = new BooleanProperty[15];
			for (int i = 0; i < options.length; i++) {
				options[i] = new SimpleBooleanProperty(false);
			}

			// set default options
			if (referenceIdName.equals("equivalentStressId")) {
				options[SaveEquivalentStresses.EQUIVALENT_STRESS] = new SimpleBooleanProperty(true);
				options[SaveEquivalentStresses.MAT_NAME] = new SimpleBooleanProperty(true);
				options[SaveEquivalentStresses.PP_NAME] = new SimpleBooleanProperty(true);
				options[SaveEquivalentStresses.MISSION] = new SimpleBooleanProperty(true);
			}
			else if (referenceIdName.equals("fastEquivalentStressId")) {
				options[SaveEquivalentStresses.EQUIVALENT_STRESS] = new SimpleBooleanProperty(true);
				options[SaveEquivalentStresses.MAT_NAME] = new SimpleBooleanProperty(true);
				options[SaveEquivalentStresses.PP_NAME] = new SimpleBooleanProperty(true);
				options[SaveEquivalentStresses.MISSION] = new SimpleBooleanProperty(true);
			}
			else if (referenceIdName.equals("headlessEquivalentStressId")) {
				options[SaveEquivalentStresses.EQUIVALENT_STRESS] = new SimpleBooleanProperty(true);
				options[SaveEquivalentStresses.MAT_NAME] = new SimpleBooleanProperty(true);
				options[SaveEquivalentStresses.SEQ_NAME] = new SimpleBooleanProperty(true);
				options[SaveEquivalentStresses.MISSION] = new SimpleBooleanProperty(true);
			}

			// read options
			if (saveEquivalentStresses.getChild("options") != null) {

				// get element
				Element optionsElement = saveEquivalentStresses.getChild("options");

				// set options
				if (optionsElement.getChild("equivalentStress") != null) {
					options[SaveEquivalentStresses.EQUIVALENT_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("equivalentStress")));
				}
				if (optionsElement.getChild("materialName") != null) {
					options[SaveEquivalentStresses.MAT_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialName")));
				}
				if (optionsElement.getChild("materialData") != null) {
					options[SaveEquivalentStresses.MAT_DATA] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("materialData")));
				}
				if (optionsElement.getChild("pilotPointName") != null) {
					options[SaveEquivalentStresses.PP_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("pilotPointName")));
				}
				if (optionsElement.getChild("elementId") != null) {
					options[SaveEquivalentStresses.EID] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("elementId")));
				}
				if (optionsElement.getChild("stressSequenceName") != null) {
					options[SaveEquivalentStresses.SEQ_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("stressSequenceName")));
				}
				if (optionsElement.getChild("spectrumName") != null) {
					options[SaveEquivalentStresses.SPEC_NAME] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumName")));
				}
				if (optionsElement.getChild("aircraftProgram") != null) {
					options[SaveEquivalentStresses.PROGRAM] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftProgram")));
				}
				if (optionsElement.getChild("aircraftSection") != null) {
					options[SaveEquivalentStresses.SECTION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("aircraftSection")));
				}
				if (optionsElement.getChild("fatigueMission") != null) {
					options[SaveEquivalentStresses.MISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("fatigueMission")));
				}
				if (optionsElement.getChild("spectrumValidity") != null) {
					options[SaveEquivalentStresses.VALIDITY] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("spectrumValidity")));
				}
				if (optionsElement.getChild("maximumStress") != null) {
					options[SaveEquivalentStresses.MAX_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("maximumStress")));
				}
				if (optionsElement.getChild("minimumStress") != null) {
					options[SaveEquivalentStresses.MIN_STRESS] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("minimumStress")));
				}
				if (optionsElement.getChild("rRatio") != null) {
					options[SaveEquivalentStresses.R_RATIO] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("rRatio")));
				}
				if (optionsElement.getChild("omissionLevel") != null) {
					options[SaveEquivalentStresses.OMISSION] = new SimpleBooleanProperty(Boolean.parseBoolean(optionsElement.getChildTextNormalize("omissionLevel")));
				}
			}

			// create task
			SaveEquivalentStresses task = new SaveEquivalentStresses(null, options, outputPath.toFile());

			// set input threshold
			List<Element> equivalentStressIds = saveEquivalentStresses.getChildren(referenceIdName);
			task.setInputThreshold(equivalentStressIds.size());

			// loop over stress ids
			for (Element equivalentStressIdElement : equivalentStressIds) {

				// get stress id
				String equivalentStressId = equivalentStressIdElement.getTextNormalize();

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates plot equivalent stress ratios tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotEquivalentStressRatios(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot equivalent stress ratios tasks...");

		// loop over plot equivalent stress ratios elements
		for (Element plotEquivalentStressRatios : equinoxInput.getChildren("plotEquivalentStressRatios")) {

			// get id and output path
			String id = plotEquivalentStressRatios.getChildTextNormalize("id");
			String basisMission = plotEquivalentStressRatios.getChildTextNormalize("basisMission");
			Path outputPath = Paths.get(plotEquivalentStressRatios.getChildTextNormalize("outputPath"));

			// set reference name
			String referenceIdName = null;
			if (plotEquivalentStressRatios.getChild("equivalentStressId") != null) {
				referenceIdName = "equivalentStressId";
			}
			else if (plotEquivalentStressRatios.getChild("fastEquivalentStressId") != null) {
				referenceIdName = "fastEquivalentStressId";
			}
			else if (plotEquivalentStressRatios.getChild("headlessEquivalentStressId") != null) {
				referenceIdName = "headlessEquivalentStressId";
			}

			// create input
			EquivalentStressRatioComparisonInput input = new EquivalentStressRatioComparisonInput();
			input.setBasisMission(basisMission);

			// options
			if (plotEquivalentStressRatios.getChild("options") != null) {

				// get element
				Element options = plotEquivalentStressRatios.getChild("options");

				// include basis mission
				if (options.getChild("includeBasisMission") != null) {
					input.setIncludeBasisMission(Boolean.parseBoolean(options.getChildTextNormalize("includeBasisMission")));
				}

				// show data labels
				if (options.getChild("showDataLabels") != null) {
					input.setLabelDisplay(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
				}
			}

			// set series naming
			if (plotEquivalentStressRatios.getChild("seriesNaming") != null) {
				Element seriesNaming = plotEquivalentStressRatios.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
				}
				if (seriesNaming.getChild("includeStressSequenceName") != null) {
					input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
				}
				if (seriesNaming.getChild("includeMaterialName") != null) {
					input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
				}
				if (seriesNaming.getChild("includeOmissionLevel") != null) {
					input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
				}
			}

			// mission parameter given
			if (plotEquivalentStressRatios.getChild("missionParameter") != null) {

				// get mission parameter name
				input.setMissionParameterName(plotEquivalentStressRatios.getChildTextNormalize("missionParameter"));

				// create tasks
				SaveMissionParameterPlot saveTask = new SaveMissionParameterPlot(null, null, outputPath);
				GenerateStressRatiosWithMissionParameters compareTask = new GenerateStressRatiosWithMissionParameters(input, null);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));

				// set input threshold
				List<Element> equivalentStressIds = plotEquivalentStressRatios.getChildren(referenceIdName);
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(compareTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}

			// no mission parameter given
			else {

				// create tasks
				SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
				GenerateStressRatios compareTask = new GenerateStressRatios(input);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveDatasetTask, runMode));

				// set input threshold
				List<Element> equivalentStressIds = plotEquivalentStressRatios.getChildren(referenceIdName);
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(compareTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}
		}
	}

	/**
	 * Creates plot life factors tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLifeFactors(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot life factors tasks...");

		// loop over plot life factors elements
		for (Element plotLifeFactors : equinoxInput.getChildren("plotLifeFactors")) {

			// get id and output path
			String id = plotLifeFactors.getChildTextNormalize("id");
			String basisMission = plotLifeFactors.getChildTextNormalize("basisMission");
			Path outputPath = Paths.get(plotLifeFactors.getChildTextNormalize("outputPath"));

			// set reference name
			String referenceIdName = null;
			if (plotLifeFactors.getChild("equivalentStressId") != null) {
				referenceIdName = "equivalentStressId";
			}
			else if (plotLifeFactors.getChild("fastEquivalentStressId") != null) {
				referenceIdName = "fastEquivalentStressId";
			}
			else if (plotLifeFactors.getChild("headlessEquivalentStressId") != null) {
				referenceIdName = "headlessEquivalentStressId";
			}

			// create input
			LifeFactorComparisonInput input = new LifeFactorComparisonInput();
			input.setBasisMission(basisMission);

			// options
			if (plotLifeFactors.getChild("options") != null) {

				// get element
				Element options = plotLifeFactors.getChild("options");

				// include basis mission
				if (options.getChild("includeBasisMission") != null) {
					input.setIncludeBasisMission(Boolean.parseBoolean(options.getChildTextNormalize("includeBasisMission")));
				}

				// show data labels
				if (options.getChild("showDataLabels") != null) {
					input.setLabelDisplay(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
				}
			}

			// set series naming
			if (plotLifeFactors.getChild("seriesNaming") != null) {
				Element seriesNaming = plotLifeFactors.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
				}
				if (seriesNaming.getChild("includeStressSequenceName") != null) {
					input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
				}
				if (seriesNaming.getChild("includeMaterialName") != null) {
					input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
				}
				if (seriesNaming.getChild("includeOmissionLevel") != null) {
					input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
				}
			}

			// mission parameter given
			if (plotLifeFactors.getChild("missionParameter") != null) {

				// get mission parameter name
				input.setMissionParameterName(plotLifeFactors.getChildTextNormalize("missionParameter"));

				// create tasks
				SaveMissionParameterPlot saveTask = new SaveMissionParameterPlot(null, null, outputPath);
				GenerateLFsWithMissionParameters compareTask = new GenerateLFsWithMissionParameters(input, null);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));

				// set input threshold
				List<Element> equivalentStressIds = plotLifeFactors.getChildren(referenceIdName);
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(compareTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}

			// no mission parameter given
			else {

				// create tasks
				SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
				GenerateLifeFactors compareTask = new GenerateLifeFactors(input);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveDatasetTask, runMode));

				// set input threshold
				List<Element> equivalentStressIds = plotLifeFactors.getChildren(referenceIdName);
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(compareTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}
		}
	}

	/**
	 * Creates plot equivalent stress comparison tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotEquivalentStressComparison(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot equivalent stress comparison tasks...");

		// loop over plot equivalent stress comparison elements
		for (Element plotEquivalentStressComparison : equinoxInput.getChildren("plotEquivalentStressComparison")) {

			// get id and output path
			String id = plotEquivalentStressComparison.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotEquivalentStressComparison.getChildTextNormalize("outputPath"));

			// set reference name
			String referenceIdName = null;
			if (plotEquivalentStressComparison.getChild("equivalentStressId") != null) {
				referenceIdName = "equivalentStressId";
			}
			else if (plotEquivalentStressComparison.getChild("fastEquivalentStressId") != null) {
				referenceIdName = "fastEquivalentStressId";
			}
			else if (plotEquivalentStressComparison.getChild("headlessEquivalentStressId") != null) {
				referenceIdName = "headlessEquivalentStressId";
			}

			// create comparison input
			EquivalentStressComparisonInput input = new EquivalentStressComparisonInput();

			// label display
			if (plotEquivalentStressComparison.getChild("showDataLabels") != null) {
				input.setLabelDisplay(Boolean.parseBoolean(plotEquivalentStressComparison.getChildTextNormalize("showDataLabels")));
			}

			// set series naming
			if (plotEquivalentStressComparison.getChild("seriesNaming") != null) {
				Element seriesNaming = plotEquivalentStressComparison.getChild("seriesNaming");
				if (seriesNaming.getChild("includeSpectrumName") != null) {
					input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
				}
				if (seriesNaming.getChild("includeStfName") != null) {
					input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
				}
				if (seriesNaming.getChild("includeElementId") != null) {
					input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
				}
				if (seriesNaming.getChild("includeStressSequenceName") != null) {
					input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
				}
				if (seriesNaming.getChild("includeMaterialName") != null) {
					input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
				}
				if (seriesNaming.getChild("includeOmissionLevel") != null) {
					input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
				}
				if (seriesNaming.getChild("includeAircraftProgram") != null) {
					input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
				}
				if (seriesNaming.getChild("includeAircraftSection") != null) {
					input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
				}
				if (seriesNaming.getChild("includeFatigueMission") != null) {
					input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
				}
			}

			// mission parameter given
			if (plotEquivalentStressComparison.getChild("missionParameter") != null) {

				// get mission parameter name
				input.setMissionParameterName(plotEquivalentStressComparison.getChildTextNormalize("missionParameter"));

				// create tasks
				SaveMissionParameterPlot saveTask = new SaveMissionParameterPlot(null, null, outputPath);
				CompareEquivalentStressesWithMissionParameters compareTask = new CompareEquivalentStressesWithMissionParameters(input, null);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));

				// set input threshold
				List<Element> equivalentStressIds = plotEquivalentStressComparison.getChildren(referenceIdName);
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(compareTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}

			// no mission parameter given
			else {

				// create tasks
				SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
				CompareEquivalentStresses compareTask = new CompareEquivalentStresses(input);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveDatasetTask, runMode));

				// set input threshold
				List<Element> equivalentStressIds = plotEquivalentStressComparison.getChildren(referenceIdName);
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(compareTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}
		}
	}

	/**
	 * Creates plot typical flight comparison tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotTypicalFlightComparison(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating typical flight comparison tasks...");

		// loop over typical flight comparison elements
		for (Element plotTypicalFlightComparison : equinoxInput.getChildren("plotTypicalFlightComparison")) {

			// get id and output path
			String id = plotTypicalFlightComparison.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotTypicalFlightComparison.getChildTextNormalize("outputPath"));

			// stress sequence
			if (plotTypicalFlightComparison.getChild("typicalFlight").getChild("stressSequenceId") != null) {

				// create comparison input
				FlightComparisonInput input = new FlightComparisonInput(null);
				input.setShowMarkers(false);

				// stress components
				if (plotTypicalFlightComparison.getChild("stressComponents") != null) {
					Element stressComponents = plotTypicalFlightComparison.getChild("stressComponents");
					boolean[] plotComponentOptions = { true, true, true, true };
					if (stressComponents.getChild("plotIncrements") != null) {
						plotComponentOptions[FlightComparisonInput.INCREMENT_STRESS_COMP] = Boolean.parseBoolean(stressComponents.getChild("plotIncrements").getTextNormalize());
					}
					if (stressComponents.getChild("plotDp") != null) {
						plotComponentOptions[FlightComparisonInput.DP_STRESS_COMP] = Boolean.parseBoolean(stressComponents.getChild("plotDp").getTextNormalize());
					}
					if (stressComponents.getChild("plotDt") != null) {
						plotComponentOptions[FlightComparisonInput.DT_STRESS_COMP] = Boolean.parseBoolean(stressComponents.getChild("plotDt").getTextNormalize());
					}
					if (stressComponents.getChild("plot1g") != null) {
						plotComponentOptions[FlightComparisonInput.ONE_G_STRESS_COMP] = Boolean.parseBoolean(stressComponents.getChild("plot1g").getTextNormalize());
					}
					input.setPlotComponentOptions(plotComponentOptions, false);
				}

				// set series naming
				if (plotTypicalFlightComparison.getChild("seriesNaming") != null) {
					Element seriesNaming = plotTypicalFlightComparison.getChild("seriesNaming");
					if (seriesNaming.getChild("includeSpectrumName") != null) {
						input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
					}
					if (seriesNaming.getChild("includeStfName") != null) {
						input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
					}
					if (seriesNaming.getChild("includeElementId") != null) {
						input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
					}
					if (seriesNaming.getChild("includeStressSequenceName") != null) {
						input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
					}
					if (seriesNaming.getChild("includeTypicalFlightName") != null) {
						input.setIncludeFlightName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeTypicalFlightName")));
					}
					if (seriesNaming.getChild("includeAircraftProgram") != null) {
						input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
					}
					if (seriesNaming.getChild("includeAircraftSection") != null) {
						input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
					}
					if (seriesNaming.getChild("includeFatigueMission") != null) {
						input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
					}
				}

				// create tasks
				SaveChart saveTask = new SaveChart(null, outputPath);
				CompareFlights compareTask = new CompareFlights(input);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));

				// set input threshold
				List<Element> typicalFlights = plotTypicalFlightComparison.getChildren("typicalFlight");
				compareTask.setInputThreshold(typicalFlights.size());

				// loop over typical flights
				for (Element typicalFlight : typicalFlights) {

					// get sequence id and flight name
					String stressSequenceId = typicalFlight.getChildText("stressSequenceId");
					String flightName = typicalFlight.getChildText("typicalFlightName");

					// create get flight task
					GetTypicalFlight getFlightTask = new GetTypicalFlight(flightName);
					getFlightTask.addAutomaticTask(Integer.toString(compareTask.hashCode()), new EmbeddedTask<>(compareTask, runMode));

					// connect to parent task
					AutomaticTaskOwner<StressSequence> parentTask = (AutomaticTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
					parentTask.addAutomaticTask(Integer.toString(getFlightTask.hashCode()), new EmbeddedTask<>(getFlightTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}

			// headless stress sequence
			else if (plotTypicalFlightComparison.getChild("typicalFlight").getChild("headlessStressSequenceId") != null) {

				// create comparison input
				ExternalFlightComparisonInput input = new ExternalFlightComparisonInput();
				input.setShowMarkers(false);

				// set series naming
				if (plotTypicalFlightComparison.getChild("seriesNaming") != null) {
					Element seriesNaming = plotTypicalFlightComparison.getChild("seriesNaming");
					if (seriesNaming.getChild("includeElementId") != null) {
						input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
					}
					if (seriesNaming.getChild("includeStressSequenceName") != null) {
						input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
					}
					if (seriesNaming.getChild("includeTypicalFlightName") != null) {
						input.setIncludeFlightName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeTypicalFlightName")));
					}
					if (seriesNaming.getChild("includeAircraftProgram") != null) {
						input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
					}
					if (seriesNaming.getChild("includeAircraftSection") != null) {
						input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
					}
					if (seriesNaming.getChild("includeFatigueMission") != null) {
						input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
					}
				}

				// create tasks
				SaveChart saveTask = new SaveChart(null, outputPath);
				CompareExternalFlights compareTask = new CompareExternalFlights(input);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));

				// set input threshold
				List<Element> typicalFlights = plotTypicalFlightComparison.getChildren("typicalFlight");
				compareTask.setInputThreshold(typicalFlights.size());

				// loop over typical flights
				for (Element typicalFlight : typicalFlights) {

					// get sequence id and flight name
					String headlessStressSequenceId = typicalFlight.getChildText("headlessStressSequenceId");
					String flightName = typicalFlight.getChildText("typicalFlightName");

					// create get flight task
					GetExternalTypicalFlight getFlightTask = new GetExternalTypicalFlight(flightName);
					getFlightTask.addAutomaticTask(Integer.toString(compareTask.hashCode()), new EmbeddedTask<>(compareTask, runMode));

					// connect to parent task
					AutomaticTaskOwner<ExternalStressSequence> parentTask = (AutomaticTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
					parentTask.addAutomaticTask(Integer.toString(getFlightTask.hashCode()), new EmbeddedTask<>(getFlightTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}
		}
	}

	/**
	 * Creates plot level crossing comparison tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLevelCrossingComparison(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating level crossing comparison tasks...");

		// loop over plot level crossing comparison elements
		for (Element plotLevelCrossingComparison : equinoxInput.getChildren("plotLevelCrossingComparison")) {

			// get id and output path
			String id = plotLevelCrossingComparison.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotLevelCrossingComparison.getChildTextNormalize("outputPath"));

			// equivalent stress
			if (plotLevelCrossingComparison.getChild("equivalentStressId") != null) {

				// create comparison input
				LevelCrossingInput input = new LevelCrossingInput(true, null);

				// set series naming
				if (plotLevelCrossingComparison.getChild("seriesNaming") != null) {
					Element seriesNaming = plotLevelCrossingComparison.getChild("seriesNaming");
					if (seriesNaming.getChild("includeSpectrumName") != null) {
						input.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
					}
					if (seriesNaming.getChild("includeStfName") != null) {
						input.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
					}
					if (seriesNaming.getChild("includeElementId") != null) {
						input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
					}
					if (seriesNaming.getChild("includeStressSequenceName") != null) {
						input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
					}
					if (seriesNaming.getChild("includeMaterialName") != null) {
						input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
					}
					if (seriesNaming.getChild("includeOmissionLevel") != null) {
						input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
					}
					if (seriesNaming.getChild("includeAircraftProgram") != null) {
						input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
					}
					if (seriesNaming.getChild("includeAircraftSection") != null) {
						input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
					}
					if (seriesNaming.getChild("includeFatigueMission") != null) {
						input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
					}
				}

				// create tasks
				SaveXYSeriesCollection saveDatasetTask = new SaveXYSeriesCollection(null, outputPath);
				PlotLevelCrossing compareTask = new PlotLevelCrossing(input);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveDatasetTask, runMode));

				// set input threshold
				List<Element> equivalentStressIds = plotLevelCrossingComparison.getChildren("equivalentStressId");
				compareTask.setInputThreshold(equivalentStressIds.size());

				// loop over equivalent stress ids
				for (Element equivalentStressIdElement : equivalentStressIds) {

					// get stress id
					String equivalentStressId = equivalentStressIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(compareTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}

			// headless equivalent stress
			else if (plotLevelCrossingComparison.getChild("headlessEquivalentStressId") != null) {

				// create input
				ExternalLevelCrossingInput input = new ExternalLevelCrossingInput(true, null);

				// set series naming
				if (plotLevelCrossingComparison.getChild("seriesNaming") != null) {
					Element seriesNaming = plotLevelCrossingComparison.getChild("seriesNaming");
					if (seriesNaming.getChild("includeElementId") != null) {
						input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
					}
					if (seriesNaming.getChild("includeStressSequenceName") != null) {
						input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
					}
					if (seriesNaming.getChild("includeMaterialName") != null) {
						input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
					}
					if (seriesNaming.getChild("includeOmissionLevel") != null) {
						input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
					}
					if (seriesNaming.getChild("includeAircraftProgram") != null) {
						input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
					}
					if (seriesNaming.getChild("includeAircraftSection") != null) {
						input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
					}
					if (seriesNaming.getChild("includeFatigueMission") != null) {
						input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
					}
				}

				// create tasks
				SaveXYSeriesCollection saveDatasetTask = new SaveXYSeriesCollection(null, outputPath);
				PlotExternalLevelCrossing plotTask = new PlotExternalLevelCrossing(input);
				plotTask.addAutomaticTask(id, new EmbeddedTask<>(saveDatasetTask, runMode));

				// set input threshold
				List<Element> headlessEquivalentStressIds = plotLevelCrossingComparison.getChildren("headlessEquivalentStressId");
				plotTask.setInputThreshold(headlessEquivalentStressIds.size());

				// loop over equivalent stress ids
				for (Element headlessEquivalentStressIdElement : headlessEquivalentStressIds) {

					// get stress id
					String headlessEquivalentStressId = headlessEquivalentStressIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(headlessEquivalentStressId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(plotTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(plotTask, true));
			}
		}
	}

	/**
	 * Creates plot damage angles tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotDamageAngles(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot damage angles tasks...");

		// loop over plot damage angles elements
		for (Element plotDamageAngles : equinoxInput.getChildren("plotDamageAngles")) {

			// get id and output path
			String id = plotDamageAngles.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotDamageAngles.getChildTextNormalize("outputPath"));

			// get options
			ResultOrdering orderBy = ResultOrdering.ANGLE;
			boolean showLabels = true;
			if (plotDamageAngles.getChild("options") != null) {
				Element options = plotDamageAngles.getChild("options");
				if (options.getChild("orderBy") != null) {
					orderBy = ResultOrdering.valueOf(options.getChildTextNormalize("orderBy"));
				}
				if (options.getChild("showDataLabels") != null) {
					showLabels = Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels"));
				}
			}

			// create tasks
			SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
			PlotDamageAngles plotTask = new PlotDamageAngles(null, orderBy, showLabels);
			plotTask.addAutomaticTask(id, new EmbeddedTask<>(saveDatasetTask, runMode));

			// set input threshold
			List<Element> damageAngleIds = plotDamageAngles.getChildren("damageAngleId");
			plotTask.setInputThreshold(damageAngleIds.size());

			// loop over damage angle ids
			for (Element damageAngleIdElement : damageAngleIds) {

				// get damage angle id
				String damageAngleId = damageAngleIdElement.getTextNormalize();

				// connect to parent task
				AutomaticTaskOwner<DamageAngle> parentTask = (AutomaticTaskOwner<DamageAngle>) tasks.get(damageAngleId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(plotTask, runMode));
			}

			// put task to tasks
			tasks.put(id, new InstructedTask(saveDatasetTask, true));
			tasks.put("ownerOf_" + id, new InstructedTask(plotTask, true));
		}
	}

	/**
	 * Creates plot stress sequence comparison tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotStressSequenceComparison(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot stress sequence comparison tasks...");

		// loop over plot stress sequence comparison elements
		for (Element plotStressSequenceComparison : equinoxInput.getChildren("plotStressSequenceComparison")) {

			// get id and output path
			String id = plotStressSequenceComparison.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotStressSequenceComparison.getChildTextNormalize("outputPath"));

			// stress sequence
			if (plotStressSequenceComparison.getChild("stressSequenceId") != null) {

				// create comparison input
				StressSequenceComparisonInput comparisonInput = new StressSequenceComparisonInput();
				comparisonInput.setCriteria(ComparisonCriteria.valueOf(plotStressSequenceComparison.getChildTextNormalize("comparisonCriteria")));

				// set options
				if (plotStressSequenceComparison.getChild("options") != null) {
					Element options = plotStressSequenceComparison.getChild("options");
					if (options.getChild("resultsOrder") != null) {
						comparisonInput.setOrder(options.getChildTextNormalize("resultsOrder").equals("descending"));
					}
					if (options.getChild("showDataLabels") != null) {
						comparisonInput.setLabelDisplay(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
					}
				}

				// set series naming
				if (plotStressSequenceComparison.getChild("seriesNaming") != null) {
					Element seriesNaming = plotStressSequenceComparison.getChild("seriesNaming");
					if (seriesNaming.getChild("includeSpectrumName") != null) {
						comparisonInput.setIncludeSpectrumName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeSpectrumName")));
					}
					if (seriesNaming.getChild("includeStfName") != null) {
						comparisonInput.setIncludeSTFName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStfName")));
					}
					if (seriesNaming.getChild("includeElementId") != null) {
						comparisonInput.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
					}
					if (seriesNaming.getChild("includeStressSequenceName") != null) {
						comparisonInput.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
					}
					if (seriesNaming.getChild("includeAircraftProgram") != null) {
						comparisonInput.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
					}
					if (seriesNaming.getChild("includeAircraftSection") != null) {
						comparisonInput.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
					}
					if (seriesNaming.getChild("includeFatigueMission") != null) {
						comparisonInput.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
					}
				}

				// create tasks
				SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
				CompareStressSequences compareTask = new CompareStressSequences(comparisonInput);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveDatasetTask, runMode));

				// set input threshold
				List<Element> stressSequenceIds = plotStressSequenceComparison.getChildren("stressSequenceId");
				compareTask.setInputThreshold(stressSequenceIds.size());

				// loop over sequence ids
				for (Element stressSequenceIdElement : stressSequenceIds) {

					// get stress sequence id
					String stressSequenceId = stressSequenceIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<StressSequence> parentTask = (AutomaticTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(compareTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}

			// headless stress sequence
			else if (plotStressSequenceComparison.getChild("headlessStressSequenceId") != null) {

				// create comparison input
				ExternalStressSequenceComparisonInput comparisonInput = new ExternalStressSequenceComparisonInput();
				comparisonInput.setCriteria(ExternalComparisonCriteria.valueOf(plotStressSequenceComparison.getChildTextNormalize("comparisonCriteria")));

				// set options
				if (plotStressSequenceComparison.getChild("options") != null) {
					Element options = plotStressSequenceComparison.getChild("options");
					if (options.getChild("resultsOrder") != null) {
						comparisonInput.setOrder(options.getChildTextNormalize("resultsOrder").equals("descending"));
					}
					if (options.getChild("showDataLabels") != null) {
						comparisonInput.setLabelDisplay(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
					}
				}

				// set series naming
				if (plotStressSequenceComparison.getChild("seriesNaming") != null) {
					Element seriesNaming = plotStressSequenceComparison.getChild("seriesNaming");
					if (seriesNaming.getChild("includeElementId") != null) {
						comparisonInput.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
					}
					if (seriesNaming.getChild("includeStressSequenceName") != null) {
						comparisonInput.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
					}
					if (seriesNaming.getChild("includeAircraftProgram") != null) {
						comparisonInput.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
					}
					if (seriesNaming.getChild("includeAircraftSection") != null) {
						comparisonInput.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
					}
					if (seriesNaming.getChild("includeFatigueMission") != null) {
						comparisonInput.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
					}
				}

				// create tasks
				SaveCategoryDataset saveDatasetTask = new SaveCategoryDataset(null, outputPath);
				CompareExternalStressSequences compareTask = new CompareExternalStressSequences(comparisonInput);
				compareTask.addAutomaticTask(id, new EmbeddedTask<>(saveDatasetTask, runMode));

				// set input threshold
				List<Element> headlessStressSequenceIds = plotStressSequenceComparison.getChildren("headlessStressSequenceId");
				compareTask.setInputThreshold(headlessStressSequenceIds.size());

				// loop over sequence ids
				for (Element headlessStressSequenceIdElement : headlessStressSequenceIds) {

					// get stress sequence id
					String headlessStressSequenceId = headlessStressSequenceIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<ExternalStressSequence> parentTask = (AutomaticTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(compareTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(compareTask, true));
			}
		}
	}

	/**
	 * Creates share file tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void shareFile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating share file tasks...");

		// loop over save stress sequence elements
		for (Element shareFile : equinoxInput.getChildren("shareFile")) {

			// create task
			String id = shareFile.getChild("id").getTextNormalize();
			String fileId = shareFile.getChild("fileId").getTextNormalize();
			String recipient = shareFile.getChild("recipient").getTextNormalize();

			// create tasks
			ShareGeneratedItem shareTask = new ShareGeneratedItem(null, Arrays.asList(taskPanel_.getOwner().getOwner().getUser(recipient)));

			// add to parent task
			AutomaticTaskOwner<Path> parentTask = (AutomaticTaskOwner<Path>) tasks.get(fileId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(shareTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(shareTask, true));
		}
	}

	/**
	 * Creates save analysis output file tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveAnalysisOutputFile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save analysis output file tasks...");

		// loop over save analysis output file elements
		for (Element saveAnalysisOutputFile : equinoxInput.getChildren("saveAnalysisOutputFile")) {

			// get inputs
			String id = saveAnalysisOutputFile.getChild("id").getTextNormalize();
			Path outputPath = Paths.get(saveAnalysisOutputFile.getChild("outputPath").getTextNormalize());

			// get sequence id
			String equivalentStressId = null;
			if (saveAnalysisOutputFile.getChild("equivalentStressId") != null) {
				equivalentStressId = saveAnalysisOutputFile.getChildTextNormalize("equivalentStressId");
			}
			else if (saveAnalysisOutputFile.getChild("fastEquivalentStressId") != null) {
				equivalentStressId = saveAnalysisOutputFile.getChildTextNormalize("fastEquivalentStressId");
			}
			else if (saveAnalysisOutputFile.getChild("headlessEquivalentStressId") != null) {
				equivalentStressId = saveAnalysisOutputFile.getChildTextNormalize("headlessEquivalentStressId");
			}

			// create task
			SaveOutputFile task = new SaveOutputFile(null, outputPath);

			// connect to parent task
			AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates save rainflow cycle info tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveRainflowCycleInfo(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating rainflow cycle info tasks...");

		// loop over rainflow cycle info elements
		for (Element saveRainflowCycleInfo : equinoxInput.getChildren("saveRainflowCycleInfo")) {

			// get inputs
			String id = saveRainflowCycleInfo.getChildTextNormalize("id");
			Path outputPath = Paths.get(saveRainflowCycleInfo.getChildTextNormalize("outputPath"));

			// get sequence id
			String equivalentStressId = null;
			if (saveRainflowCycleInfo.getChild("equivalentStressId") != null) {
				equivalentStressId = saveRainflowCycleInfo.getChildTextNormalize("equivalentStressId");
			}
			else if (saveRainflowCycleInfo.getChild("headlessEquivalentStressId") != null) {
				equivalentStressId = saveRainflowCycleInfo.getChildTextNormalize("headlessEquivalentStressId");
			}

			// create task
			SaveRainflow task = new SaveRainflow(null, outputPath.toFile());

			// connect to parent task
			AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates plot rainflow histogram tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotRainflowHistogram(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot rainflow histogram tasks...");

		// loop over plot rainflow histogram elements
		for (Element plotRainflowHistogram : equinoxInput.getChildren("plotRainflowHistogram")) {

			// get inputs
			String id = plotRainflowHistogram.getChild("id").getTextNormalize();
			Path outputPath = Paths.get(plotRainflowHistogram.getChild("outputPath").getTextNormalize());

			// equivalent stress
			if (plotRainflowHistogram.getChild("equivalentStressId") != null) {

				// get id
				String equivalentStressId = plotRainflowHistogram.getChildTextNormalize("equivalentStressId");

				// create task
				SaveEquivalentStressPlotToFile task = new SaveEquivalentStressPlotToFile(null, PilotPointImageType.RAINFLOW_HISTOGRAM, outputPath);

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}

			// fast equivalent stress
			else if (plotRainflowHistogram.getChild("fastEquivalentStressId") != null) {

				// get fast equivalent stress id
				String fastEquivalentStressId = plotRainflowHistogram.getChild("fastEquivalentStressId").getTextNormalize();

				// create task
				GenerateLevelCrossingsPlot task = new GenerateLevelCrossingsPlot(null, false, false, outputPath);

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(fastEquivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}

			// headless equivalent stress
			else if (plotRainflowHistogram.getChild("headlessEquivalentStressId") != null) {

				// get id
				String headlessEquivalentStressId = plotRainflowHistogram.getChildTextNormalize("headlessEquivalentStressId");

				// create input
				HistogramInput input = new HistogramInput();

				// histogram data
				if (plotRainflowHistogram.getChild("histogramData") != null) {
					input.setDataType(HistogramDataType.valueOf(plotRainflowHistogram.getChildTextNormalize("histogramData")));
				}

				// options
				if (plotRainflowHistogram.getChild("options") != null) {

					// get options element
					Element options = plotRainflowHistogram.getChild("options");

					// max results
					if (options.getChild("maxResults") != null) {
						input.setLimit(Integer.parseInt(options.getChildTextNormalize("maxResults")));
					}

					// show data labels
					if (options.getChild("showDataLabels") != null) {
						input.setLabelsVisible(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
					}

					// results order
					if (options.getChild("resultsOrder") != null) {
						input.setOrder(options.getChildTextNormalize("resultsOrder").equals("descending"));
					}

					// decimal places
					if (options.getChild("decimalPlaces") != null) {
						input.setDigits(Integer.parseInt(options.getChildTextNormalize("decimalPlaces")));
					}
				}

				// create tasks
				SaveCategoryDataset saveTask = new SaveCategoryDataset(null, outputPath);
				PlotHistogram plotTask = new PlotHistogram(input, null);
				plotTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(headlessEquivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(plotTask, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(saveTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(plotTask, true));
			}
		}
	}

	/**
	 * Creates plot level crossing tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLevelCrossing(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot level crossing tasks...");

		// loop over plot level crossing elements
		for (Element plotLevelCrossing : equinoxInput.getChildren("plotLevelCrossing")) {

			// get inputs
			String id = plotLevelCrossing.getChild("id").getTextNormalize();
			Path outputPath = Paths.get(plotLevelCrossing.getChild("outputPath").getTextNormalize());

			// equivalent stress id
			if (plotLevelCrossing.getChild("equivalentStressId") != null) {

				// create task
				SaveEquivalentStressPlotToFile task = new SaveEquivalentStressPlotToFile(null, PilotPointImageType.LEVEL_CROSSING, outputPath);

				// connect to parent task
				String equivalentStressId = plotLevelCrossing.getChild("equivalentStressId").getTextNormalize();
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(equivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}

			// fast equivalent stress id
			else if (plotLevelCrossing.getChild("fastEquivalentStressId") != null) {

				// get fast equivalent stress id
				String fastEquivalentStressId = plotLevelCrossing.getChild("fastEquivalentStressId").getTextNormalize();

				// create task
				GenerateLevelCrossingsPlot task = new GenerateLevelCrossingsPlot(null, false, true, outputPath);

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(fastEquivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}

			// headless equivalent stress id
			else if (plotLevelCrossing.getChild("headlessEquivalentStressId") != null) {

				// create input
				ExternalLevelCrossingInput input = new ExternalLevelCrossingInput(true, null);

				// set series naming
				if (plotLevelCrossing.getChild("seriesNaming") != null) {
					Element seriesNaming = plotLevelCrossing.getChild("seriesNaming");
					if (seriesNaming.getChild("includeElementId") != null) {
						input.setIncludeEID(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeElementId")));
					}
					if (seriesNaming.getChild("includeStressSequenceName") != null) {
						input.setIncludeSequenceName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeStressSequenceName")));
					}
					if (seriesNaming.getChild("includeMaterialName") != null) {
						input.setIncludeMaterialName(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeMaterialName")));
					}
					if (seriesNaming.getChild("includeOmissionLevel") != null) {
						input.setIncludeOmissionLevel(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeOmissionLevel")));
					}
					if (seriesNaming.getChild("includeAircraftProgram") != null) {
						input.setIncludeProgram(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftProgram")));
					}
					if (seriesNaming.getChild("includeAircraftSection") != null) {
						input.setIncludeSection(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeAircraftSection")));
					}
					if (seriesNaming.getChild("includeFatigueMission") != null) {
						input.setIncludeMission(Boolean.parseBoolean(seriesNaming.getChildTextNormalize("includeFatigueMission")));
					}
				}

				// create tasks
				SaveXYSeriesCollection saveDatasetTask = new SaveXYSeriesCollection(null, outputPath);
				PlotExternalLevelCrossing plotTask = new PlotExternalLevelCrossing(input);
				plotTask.addAutomaticTask(id, new EmbeddedTask<>(saveDatasetTask, runMode));

				// set input threshold
				List<Element> headlessEquivalentStressIds = plotLevelCrossing.getChildren("headlessEquivalentStressId");
				plotTask.setInputThreshold(headlessEquivalentStressIds.size());

				// loop over equivalent stress ids
				for (Element headlessEquivalentStressIdElement : headlessEquivalentStressIds) {

					// get stress id
					String headlessEquivalentStressId = headlessEquivalentStressIdElement.getTextNormalize();

					// connect to parent task
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(headlessEquivalentStressId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(plotTask, runMode));
				}

				// put task to tasks
				tasks.put(id, new InstructedTask(saveDatasetTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(plotTask, true));
			}
		}
	}

	/**
	 * Creates plot typical flight damage contribution tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotTypicalFlightDamageContribution(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot typical flight damage contribution tasks...");

		// loop over plot typical flight damage contribution elements
		for (Element plotTypicalFlightDamageContribution : equinoxInput.getChildren("plotTypicalFlightDamageContribution")) {

			// get inputs
			String id = plotTypicalFlightDamageContribution.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotTypicalFlightDamageContribution.getChildTextNormalize("outputPath"));

			// create task
			SaveFlightDamageContributionPlot task = new SaveFlightDamageContributionPlot(null, false, outputPath);

			// connect to parent task
			String typicalFlightDamageContributionId = plotTypicalFlightDamageContribution.getChildTextNormalize("typicalFlightDamageContributionId");
			AutomaticTaskOwner<FlightDamageContributions> parentTask = (AutomaticTaskOwner<FlightDamageContributions>) tasks.get(typicalFlightDamageContributionId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates plot loadcase damage contribution tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLoadcaseDamageContribution(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot loadcase damage contribution tasks...");

		// loop over plot loadcase damage contribution elements
		for (Element plotLoadcaseDamageContribution : equinoxInput.getChildren("plotLoadcaseDamageContribution")) {

			// get inputs
			String id = plotLoadcaseDamageContribution.getChildTextNormalize("id");
			Path outputPath = Paths.get(plotLoadcaseDamageContribution.getChildTextNormalize("outputPath"));

			// create task
			SaveLoadcaseDamageContributionPlot task = new SaveLoadcaseDamageContributionPlot(null, false, outputPath);

			// connect to parent task
			String loadcaseDamageContributionId = plotLoadcaseDamageContribution.getChildTextNormalize("loadcaseDamageContributionId");
			AutomaticTaskOwner<LoadcaseDamageContributions> parentTask = (AutomaticTaskOwner<LoadcaseDamageContributions>) tasks.get(loadcaseDamageContributionId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates fast equivalent stress analysis tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void fastEquivalentStressAnalysis(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating typical flight damage contribution analysis tasks...");

		// get analysis engine settings
		Settings settings = taskPanel_.getOwner().getOwner().getSettings();
		AnalysisEngine engine = (AnalysisEngine) settings.getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) settings.getValue(Settings.ISAMI_VERSION);
		IsamiSubVersion isamiSubVersion = (IsamiSubVersion) settings.getValue(Settings.ISAMI_SUB_VERSION);
		boolean applyCompression = (boolean) settings.getValue(Settings.APPLY_COMPRESSION);

		// input mapping
		HashMap<Path, GenerateStressSequenceInput> generateStressSequenceInputs = new HashMap<>();
		HashMap<Path, EquivalentStressInput> equivalentStressAnalysisInputs = new HashMap<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// loop over fast equivalent stress analysis elements
			for (Element fastEquivalentStressAnalysis : equinoxInput.getChildren("fastEquivalentStressAnalysis")) {

				// get inputs
				String id = fastEquivalentStressAnalysis.getChildTextNormalize("id");
				String stfId = fastEquivalentStressAnalysis.getChildTextNormalize("stfId");
				Path generateStressSequenceInputPath = Paths.get(fastEquivalentStressAnalysis.getChildTextNormalize("generateStressSequenceInputPath"));
				Path equivalentStressAnalysisInputPath = Paths.get(fastEquivalentStressAnalysis.getChildTextNormalize("equivalentStressAnalysisInputPath"));

				// get generate stress sequence input parameters
				GenerateStressSequenceInput generateStressSequenceInput = generateStressSequenceInputs.get(generateStressSequenceInputPath);
				if (generateStressSequenceInput == null) {
					generateStressSequenceInput = new ReadGenerateStressSequenceInput(this, generateStressSequenceInputPath).start(null);
					generateStressSequenceInputs.put(generateStressSequenceInputPath, generateStressSequenceInput);
				}

				// get equivalent stress input parameters
				EquivalentStressInput equivalentStressAnalysisInput = equivalentStressAnalysisInputs.get(equivalentStressAnalysisInputPath);
				if (equivalentStressAnalysisInput == null) {
					equivalentStressAnalysisInput = new ReadEquivalentStressAnalysisInput(this, equivalentStressAnalysisInputPath, isamiVersion).start(connection);
					equivalentStressAnalysisInputs.put(equivalentStressAnalysisInputPath, equivalentStressAnalysisInput);
				}

				// create fast equivalent stress input
				FastEquivalentStressInput input = new FastEquivalentStressInput();
				input.setApplyOmission(equivalentStressAnalysisInput.applyOmission());
				input.setDPLoadcase(generateStressSequenceInput.getDPLoadcase());
				input.setDTInterpolation(generateStressSequenceInput.getDTInterpolation());
				input.setDTLoadcaseInf(generateStressSequenceInput.getDTLoadcaseInf());
				input.setDTLoadcaseSup(generateStressSequenceInput.getDTLoadcaseSup());
				input.setLoadcaseFactors(generateStressSequenceInput.getLoadcaseFactors());
				input.setOmissionLevel(equivalentStressAnalysisInput.getOmissionLevel());
				input.setReferenceDP(generateStressSequenceInput.getReferenceDP());
				input.setReferenceDTInf(generateStressSequenceInput.getReferenceDTInf());
				input.setReferenceDTSup(generateStressSequenceInput.getReferenceDTSup());
				input.setRemoveNegativeStresses(equivalentStressAnalysisInput.removeNegativeStresses());
				input.setRotationAngle(generateStressSequenceInput.getRotationAngle());
				input.setSegmentFactors(generateStressSequenceInput.getSegmentFactors());
				input.setStressComponent(generateStressSequenceInput.getStressComponent());
				input.setStressModifier(GenerateStressSequenceInput.ONEG, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.ONEG), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.ONEG));
				input.setStressModifier(GenerateStressSequenceInput.DELTAP, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.DELTAP), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAP));
				input.setStressModifier(GenerateStressSequenceInput.DELTAT, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.DELTAT), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAT));
				input.setStressModifier(GenerateStressSequenceInput.INCREMENT, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.INCREMENT), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT));

				// create task
				FastGenerateStressSequence task = new FastGenerateStressSequence(null, input, Arrays.asList(equivalentStressAnalysisInput.getMaterial()), false, engine);
				task.setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression);

				// add to parent task
				AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put generate stress sequence task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}
		}
	}

	/**
	 * Creates typical flight damage contribution analysis tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void typicalFlightDamageContributionAnalysis(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating typical flight damage contribution analysis tasks...");

		// get analysis engine settings
		Settings settings = taskPanel_.getOwner().getOwner().getSettings();
		AnalysisEngine engine = (AnalysisEngine) settings.getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) settings.getValue(Settings.ISAMI_VERSION);
		IsamiSubVersion isamiSubVersion = (IsamiSubVersion) settings.getValue(Settings.ISAMI_SUB_VERSION);
		boolean applyCompression = (boolean) settings.getValue(Settings.APPLY_COMPRESSION);

		// input mapping
		HashMap<Path, GenerateStressSequenceInput> generateStressSequenceInputs = new HashMap<>();
		HashMap<Path, EquivalentStressInput> equivalentStressAnalysisInputs = new HashMap<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// loop over typical flight damage contribution analysis elements
			for (Element typicalFlightDamageContributionAnalysis : equinoxInput.getChildren("typicalFlightDamageContributionAnalysis")) {

				// get inputs
				String id = typicalFlightDamageContributionAnalysis.getChildTextNormalize("id");
				String stfId = typicalFlightDamageContributionAnalysis.getChildTextNormalize("stfId");
				Path generateStressSequenceInputPath = Paths.get(typicalFlightDamageContributionAnalysis.getChildTextNormalize("generateStressSequenceInputPath"));
				Path equivalentStressAnalysisInputPath = Paths.get(typicalFlightDamageContributionAnalysis.getChildTextNormalize("equivalentStressAnalysisInputPath"));

				// get generate stress sequence input parameters
				GenerateStressSequenceInput generateStressSequenceInput = generateStressSequenceInputs.get(generateStressSequenceInputPath);
				if (generateStressSequenceInput == null) {
					generateStressSequenceInput = new ReadGenerateStressSequenceInput(this, generateStressSequenceInputPath).start(null);
					generateStressSequenceInputs.put(generateStressSequenceInputPath, generateStressSequenceInput);
				}

				// get equivalent stress input parameters
				EquivalentStressInput equivalentStressAnalysisInput = equivalentStressAnalysisInputs.get(equivalentStressAnalysisInputPath);
				if (equivalentStressAnalysisInput == null) {
					equivalentStressAnalysisInput = new ReadEquivalentStressAnalysisInput(this, equivalentStressAnalysisInputPath, isamiVersion).start(connection);
					equivalentStressAnalysisInputs.put(equivalentStressAnalysisInputPath, equivalentStressAnalysisInput);
				}

				// create fast equivalent stress input
				FastEquivalentStressInput input = new FastEquivalentStressInput();
				input.setApplyOmission(equivalentStressAnalysisInput.applyOmission());
				input.setDPLoadcase(generateStressSequenceInput.getDPLoadcase());
				input.setDTInterpolation(generateStressSequenceInput.getDTInterpolation());
				input.setDTLoadcaseInf(generateStressSequenceInput.getDTLoadcaseInf());
				input.setDTLoadcaseSup(generateStressSequenceInput.getDTLoadcaseSup());
				input.setLoadcaseFactors(generateStressSequenceInput.getLoadcaseFactors());
				input.setOmissionLevel(equivalentStressAnalysisInput.getOmissionLevel());
				input.setReferenceDP(generateStressSequenceInput.getReferenceDP());
				input.setReferenceDTInf(generateStressSequenceInput.getReferenceDTInf());
				input.setReferenceDTSup(generateStressSequenceInput.getReferenceDTSup());
				input.setRemoveNegativeStresses(equivalentStressAnalysisInput.removeNegativeStresses());
				input.setRotationAngle(generateStressSequenceInput.getRotationAngle());
				input.setSegmentFactors(generateStressSequenceInput.getSegmentFactors());
				input.setStressComponent(generateStressSequenceInput.getStressComponent());
				input.setStressModifier(GenerateStressSequenceInput.ONEG, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.ONEG), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.ONEG));
				input.setStressModifier(GenerateStressSequenceInput.DELTAP, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.DELTAP), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAP));
				input.setStressModifier(GenerateStressSequenceInput.DELTAT, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.DELTAT), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAT));
				input.setStressModifier(GenerateStressSequenceInput.INCREMENT, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.INCREMENT), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT));

				// create task
				FastGenerateStressSequence task = new FastGenerateStressSequence(null, input, Arrays.asList(equivalentStressAnalysisInput.getMaterial()), true, engine);
				task.setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression);

				// add to parent task
				AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put generate stress sequence task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}
		}
	}

	/**
	 * Creates loadcase damage contribution analysis tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadcaseDamageContributionAnalysis(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating loadcase damage contribution analysis tasks...");

		// get analysis engine settings
		Settings settings = taskPanel_.getOwner().getOwner().getSettings();
		AnalysisEngine engine = (AnalysisEngine) settings.getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) settings.getValue(Settings.ISAMI_VERSION);

		// input mapping
		HashMap<Path, GenerateStressSequenceInput> generateStressSequenceInputs = new HashMap<>();
		HashMap<Path, LoadcaseDamageContributionInput> loadcaseDamageContributionAnalysisInputs = new HashMap<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// loop over loadcase damage contribution analysis elements
			for (Element damageContributionAnalysis : equinoxInput.getChildren("loadcaseDamageContributionAnalysis")) {

				// get inputs
				String id = damageContributionAnalysis.getChildTextNormalize("id");
				String stfId = damageContributionAnalysis.getChildTextNormalize("stfId");
				Path generateStressSequenceInputPath = Paths.get(damageContributionAnalysis.getChildTextNormalize("generateStressSequenceInputPath"));
				Path loadcaseDamageContributionAnalysisInputPath = Paths.get(damageContributionAnalysis.getChildTextNormalize("loadcaseDamageContributionAnalysisInputPath"));

				// get generate stress sequence input parameters
				GenerateStressSequenceInput generateStressSequenceInput = generateStressSequenceInputs.get(generateStressSequenceInputPath);
				if (generateStressSequenceInput == null) {
					generateStressSequenceInput = new ReadGenerateStressSequenceInput(this, generateStressSequenceInputPath).start(null);
					generateStressSequenceInputs.put(generateStressSequenceInputPath, generateStressSequenceInput);
				}

				// get loadcase damage contribution analysis input parameters
				LoadcaseDamageContributionInput loadcaseDamageContributionAnalysisInput = loadcaseDamageContributionAnalysisInputs.get(loadcaseDamageContributionAnalysisInputPath);
				if (loadcaseDamageContributionAnalysisInput == null) {

					// read input
					loadcaseDamageContributionAnalysisInput = new ReadLoadcaseDamageContributionAnalysisInput(this, loadcaseDamageContributionAnalysisInputPath, isamiVersion).start(connection);

					// set generate stress sequence inputs
					loadcaseDamageContributionAnalysisInput.setDPLoadcase(generateStressSequenceInput.getDPLoadcase());
					loadcaseDamageContributionAnalysisInput.setDTInterpolation(generateStressSequenceInput.getDTInterpolation());
					loadcaseDamageContributionAnalysisInput.setDTLoadcaseInf(generateStressSequenceInput.getDTLoadcaseInf());
					loadcaseDamageContributionAnalysisInput.setDTLoadcaseSup(generateStressSequenceInput.getDTLoadcaseSup());
					loadcaseDamageContributionAnalysisInput.setLoadcaseFactors(generateStressSequenceInput.getLoadcaseFactors());
					loadcaseDamageContributionAnalysisInput.setReferenceDP(generateStressSequenceInput.getReferenceDP());
					loadcaseDamageContributionAnalysisInput.setReferenceDTInf(generateStressSequenceInput.getReferenceDTInf());
					loadcaseDamageContributionAnalysisInput.setReferenceDTSup(generateStressSequenceInput.getReferenceDTSup());
					loadcaseDamageContributionAnalysisInput.setRotationAngle(generateStressSequenceInput.getRotationAngle());
					loadcaseDamageContributionAnalysisInput.setSegmentFactors(generateStressSequenceInput.getSegmentFactors());
					loadcaseDamageContributionAnalysisInput.setStressComponent(generateStressSequenceInput.getStressComponent());
					loadcaseDamageContributionAnalysisInput.setStressModifier(GenerateStressSequenceInput.ONEG, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.ONEG), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.ONEG));
					loadcaseDamageContributionAnalysisInput.setStressModifier(GenerateStressSequenceInput.DELTAP, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.DELTAP), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAP));
					loadcaseDamageContributionAnalysisInput.setStressModifier(GenerateStressSequenceInput.DELTAT, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.DELTAT), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAT));
					loadcaseDamageContributionAnalysisInput.setStressModifier(GenerateStressSequenceInput.INCREMENT, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.INCREMENT),
							generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT));

					// add to input mapping
					loadcaseDamageContributionAnalysisInputs.put(loadcaseDamageContributionAnalysisInputPath, loadcaseDamageContributionAnalysisInput);
				}

				// create task
				LoadcaseDamageContributionAnalysis task = new LoadcaseDamageContributionAnalysis(null, loadcaseDamageContributionAnalysisInput, engine);

				// add to parent task
				AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put generate stress sequence task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}
		}
	}

	/**
	 * Creates damage angle analysis tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void damageAngleAnalysis(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating damage angle analysis tasks...");

		// get analysis engine settings
		Settings settings = taskPanel_.getOwner().getOwner().getSettings();
		AnalysisEngine engine = (AnalysisEngine) settings.getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) settings.getValue(Settings.ISAMI_VERSION);

		// input mapping
		HashMap<Path, EquivalentStressInput> equivalentStressAnalysisInputs = new HashMap<>();
		HashMap<Path, GenerateStressSequenceInput> generateStressSequenceInputs = new HashMap<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// loop over damage angle analysis elements
			for (Element damageAngleAnalysis : equinoxInput.getChildren("damageAngleAnalysis")) {

				// get inputs
				String id = damageAngleAnalysis.getChild("id").getTextNormalize();
				String stfId = damageAngleAnalysis.getChildTextNormalize("stfId");
				Path equivalentStressAnalysisInputPath = Paths.get(damageAngleAnalysis.getChild("equivalentStressAnalysisInputPath").getTextNormalize());
				Path generateStressSequenceInputPath = Paths.get(damageAngleAnalysis.getChildTextNormalize("generateStressSequenceInputPath"));

				// get equivalent stress analysis input
				EquivalentStressInput equivalentStressAnalysisInput = equivalentStressAnalysisInputs.get(equivalentStressAnalysisInputPath);
				if (equivalentStressAnalysisInput == null) {
					equivalentStressAnalysisInput = new ReadEquivalentStressAnalysisInput(this, equivalentStressAnalysisInputPath, isamiVersion).start(connection);
					equivalentStressAnalysisInputs.put(equivalentStressAnalysisInputPath, equivalentStressAnalysisInput);
				}

				// get generate stress sequence input
				GenerateStressSequenceInput generateStressSequenceInput = generateStressSequenceInputs.get(generateStressSequenceInputPath);
				if (generateStressSequenceInput == null) {
					generateStressSequenceInput = new ReadGenerateStressSequenceInput(this, generateStressSequenceInputPath).start(null);
					generateStressSequenceInputs.put(generateStressSequenceInputPath, generateStressSequenceInput);
				}

				// get iteration settings
				int startAngle = 0, endAngle = 180, incrementAngle = 10;
				if (damageAngleAnalysis.getChild("iterationSettings") != null) {
					Element iterationSettings = damageAngleAnalysis.getChild("iterationSettings");
					startAngle = Integer.parseInt(iterationSettings.getChildTextNormalize("startAngle"));
					endAngle = Integer.parseInt(iterationSettings.getChildTextNormalize("endAngle"));
					incrementAngle = Integer.parseInt(iterationSettings.getChildTextNormalize("incrementAngle"));
				}

				// generate maxdam data
				boolean generateMaxdamData = false;
				if (damageAngleAnalysis.getChild("generateMaxdamData") != null) {
					generateMaxdamData = Boolean.parseBoolean(damageAngleAnalysis.getChildTextNormalize("generateMaxdamData"));
				}

				// create input
				DamageAngleInput input = new DamageAngleInput();
				input.setIterationAngles(startAngle, endAngle, incrementAngle);
				input.setGenerateMaxdamData(generateMaxdamData);
				input.setApplyOmission(equivalentStressAnalysisInput.applyOmission());
				input.setDPLoadcase(generateStressSequenceInput.getDPLoadcase());
				input.setDTInterpolation(generateStressSequenceInput.getDTInterpolation());
				input.setDTLoadcaseInf(generateStressSequenceInput.getDTLoadcaseInf());
				input.setDTLoadcaseSup(generateStressSequenceInput.getDTLoadcaseSup());
				input.setLoadcaseFactors(generateStressSequenceInput.getLoadcaseFactors());
				input.setOmissionLevel(equivalentStressAnalysisInput.getOmissionLevel());
				input.setReferenceDP(generateStressSequenceInput.getReferenceDP());
				input.setReferenceDTInf(generateStressSequenceInput.getReferenceDTInf());
				input.setReferenceDTSup(generateStressSequenceInput.getReferenceDTSup());
				input.setRemoveNegativeStresses(equivalentStressAnalysisInput.removeNegativeStresses());
				input.setSegmentFactors(generateStressSequenceInput.getSegmentFactors());
				input.setStressModifier(GenerateStressSequenceInput.ONEG, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.ONEG), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.ONEG));
				input.setStressModifier(GenerateStressSequenceInput.DELTAP, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.DELTAP), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAP));
				input.setStressModifier(GenerateStressSequenceInput.DELTAT, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.DELTAT), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAT));
				input.setStressModifier(GenerateStressSequenceInput.INCREMENT, generateStressSequenceInput.getStressModificationValue(GenerateStressSequenceInput.INCREMENT), generateStressSequenceInput.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT));

				// create task
				DamageAngleAnalysis task = new DamageAngleAnalysis(null, input, (FatigueMaterial) equivalentStressAnalysisInput.getMaterial(), engine);

				// add to parent task
				AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put equivalent stress analysis task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}
		}
	}

	/**
	 * Creates equivalent stress analysis tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void equivalentStressAnalysis(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating equivalent stress analysis tasks...");

		// get analysis engine settings
		Settings settings = taskPanel_.getOwner().getOwner().getSettings();
		AnalysisEngine engine = (AnalysisEngine) settings.getValue(Settings.ANALYSIS_ENGINE);
		IsamiVersion isamiVersion = (IsamiVersion) settings.getValue(Settings.ISAMI_VERSION);
		IsamiSubVersion isamiSubVersion = (IsamiSubVersion) settings.getValue(Settings.ISAMI_SUB_VERSION);
		boolean applyCompression = (boolean) settings.getValue(Settings.APPLY_COMPRESSION);

		// input mapping
		HashMap<Path, EquivalentStressInput> equivalentStressAnalysisInputs = new HashMap<>();
		HashMap<Path, GenerateStressSequenceInput> generateStressSequenceInputs = new HashMap<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// loop over equivalent stress analysis elements
			for (Element equivalentStressAnalysis : equinoxInput.getChildren("equivalentStressAnalysis")) {

				// get inputs
				String id = equivalentStressAnalysis.getChild("id").getTextNormalize();
				Path equivalentStressAnalysisInputPath = Paths.get(equivalentStressAnalysis.getChild("equivalentStressAnalysisInputPath").getTextNormalize());

				// get input parameters
				EquivalentStressInput equivalentStressAnalysisInput = equivalentStressAnalysisInputs.get(equivalentStressAnalysisInputPath);
				if (equivalentStressAnalysisInput == null) {
					equivalentStressAnalysisInput = new ReadEquivalentStressAnalysisInput(this, equivalentStressAnalysisInputPath, isamiVersion).start(connection);
					equivalentStressAnalysisInputs.put(equivalentStressAnalysisInputPath, equivalentStressAnalysisInput);
				}

				// create task
				EquivalentStressAnalysis equivalentStressAnalysisTask = new EquivalentStressAnalysis(null, equivalentStressAnalysisInput, engine);
				equivalentStressAnalysisTask.setIsamiEngineInputs(isamiVersion, isamiSubVersion, applyCompression);

				// stress sequence
				if (equivalentStressAnalysis.getChild("stressSequenceId") != null) {
					String stressSequenceId = equivalentStressAnalysis.getChildTextNormalize("stressSequenceId");
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(stressSequenceId).getTask();
					parentTask.addAutomaticTask(Integer.toString(equivalentStressAnalysisTask.hashCode()), new EmbeddedTask<>(equivalentStressAnalysisTask, runMode));
				}

				// headless stress sequence
				else if (equivalentStressAnalysis.getChild("headlessStressSequenceId") != null) {
					String headlessStressSequenceId = equivalentStressAnalysis.getChildTextNormalize("headlessStressSequenceId");
					AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(headlessStressSequenceId).getTask();
					parentTask.addAutomaticTask(Integer.toString(equivalentStressAnalysisTask.hashCode()), new EmbeddedTask<>(equivalentStressAnalysisTask, runMode));
				}

				// STF file
				else if (equivalentStressAnalysis.getChild("stfId") != null) {

					// parse elements
					String stfId = equivalentStressAnalysis.getChildTextNormalize("stfId");
					Path generateStressSequenceInputPath = Paths.get(equivalentStressAnalysis.getChildTextNormalize("generateStressSequenceInputPath"));

					// get input parameters
					GenerateStressSequenceInput generateStressSequenceInput = generateStressSequenceInputs.get(generateStressSequenceInputPath);
					if (generateStressSequenceInput == null) {
						generateStressSequenceInput = new ReadGenerateStressSequenceInput(this, generateStressSequenceInputPath).start(null);
						generateStressSequenceInputs.put(generateStressSequenceInputPath, generateStressSequenceInput);
					}

					// create task
					GenerateStressSequence generateStressSequenceTask = new GenerateStressSequence(null, generateStressSequenceInput);
					generateStressSequenceTask.addAutomaticTask(Integer.toString(equivalentStressAnalysisTask.hashCode()), new EmbeddedTask<>(equivalentStressAnalysisTask, runMode));

					// add to parent task
					AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(generateStressSequenceTask, runMode));

					// put generate stress sequence task to tasks
					tasks.put("ownerOf_" + id, new InstructedTask(generateStressSequenceTask, true));
				}

				// put equivalent stress analysis task to tasks
				tasks.put(id, new InstructedTask(equivalentStressAnalysisTask, true));
			}
		}
	}

	/**
	 * Creates plot typical flight tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @param elementName
	 *            Element name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotTypicalFlight(Element equinoxInput, HashMap<String, InstructedTask> tasks, String elementName) throws Exception {

		// update info
		updateMessage("Creating plot typical flight tasks...");

		// loop over plot typical flight elements
		for (Element plotTypicalFlight : equinoxInput.getChildren(elementName)) {

			// get inputs
			String id = plotTypicalFlight.getChild("id").getTextNormalize();
			Path outputPath = Paths.get(plotTypicalFlight.getChild("outputPath").getTextNormalize());
			String plotTypeName = plotTypicalFlight.getChild("plotType").getTextNormalize();

			// get plot type
			PilotPointImageType plotType = null;
			for (PilotPointImageType type : PilotPointImageType.values()) {
				if (type.toString().equals(plotTypeName)) {
					plotType = type;
					break;
				}
			}

			// stress sequence
			if (plotTypicalFlight.getChild("stressSequenceId") != null) {

				// get stress sequence id
				String stressSequenceId = plotTypicalFlight.getChild("stressSequenceId").getTextNormalize();

				// create task
				SaveStressSequencePlotToFile task = new SaveStressSequencePlotToFile(null, plotType, outputPath);

				// connect to parent task
				AutomaticTaskOwner<StressSequence> parentTask = (AutomaticTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}

			// fast equivalent stress
			else if (plotTypicalFlight.getChild("fastEquivalentStressId") != null) {

				// get fast equivalent stress id
				String fastEquivalentStressId = plotTypicalFlight.getChild("fastEquivalentStressId").getTextNormalize();

				// create task
				AutomaticTask<SpectrumItem> task = null;
				if (plotType.equals(PilotPointImageType.LONGEST_FLIGHT)) {
					task = new GenerateLongestFlightPlot(null, false, outputPath);
				}
				else if (plotType.equals(PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE)) {
					task = new GenerateHOFlightPlot(null, false, outputPath);
				}
				else if (plotType.equals(PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS)) {
					task = new GenerateHSFlightPlot(null, false, outputPath);
				}
				else if (plotType.equals(PilotPointImageType.NUMBER_OF_PEAKS)) {
					task = new GenerateNumPeaksPlot(null, false, outputPath);
				}
				else if (plotType.equals(PilotPointImageType.FLIGHT_OCCURRENCE)) {
					task = new GenerateFlightOccurrencePlot(null, false, outputPath);
				}

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(fastEquivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask((InternalEquinoxTask<?>) task, true));
			}

			// headless stress sequence
			else if (plotTypicalFlight.getChild("headlessStressSequenceId") != null) {

				// get headless stress sequence id
				String headlessStressSequenceId = plotTypicalFlight.getChild("headlessStressSequenceId").getTextNormalize();

				// plot typical flights
				if (elementName.equals("plotTypicalFlight")) {

					// get typical flight type
					int criteria = -1;
					if (plotType.equals(PilotPointImageType.LONGEST_FLIGHT)) {
						criteria = SelectExternalFlight.LONGEST_FLIGHT;
					}
					else if (plotType.equals(PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE)) {
						criteria = SelectExternalFlight.MAX_VALIDITY;
					}
					else if (plotType.equals(PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS)) {
						criteria = SelectExternalFlight.MAX_PEAK;
					}

					// create tasks
					SaveXYDataset saveTask = new SaveXYDataset(null, outputPath);
					PlotExternalTypicalFlights plotTask = new PlotExternalTypicalFlights(new ExternalFlightPlotInput());
					plotTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));
					plotTask.setInputThreshold(1);
					SelectExternalFlight selectTask = new SelectExternalFlight(null, criteria);
					selectTask.addAutomaticTask(id, new EmbeddedTask<>(plotTask, runMode));

					// connect to parent task
					AutomaticTaskOwner<ExternalStressSequence> parentTask = (AutomaticTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(selectTask, runMode));

					// put task to tasks
					tasks.put(id, new InstructedTask(saveTask, true));
				}

				// plot typical flight statistics
				else if (elementName.equals("plotTypicalFlightStatistics")) {

					// create input
					ExternalStatisticsInput input = new ExternalStatisticsInput();

					// set statistic type
					if (plotType.equals(PilotPointImageType.NUMBER_OF_PEAKS)) {
						input.setStatistic(ExternalStatistic.NUM_PEAKS);
					}
					else if (plotType.equals(PilotPointImageType.FLIGHT_OCCURRENCE)) {
						input.setStatistic(ExternalStatistic.FLIGHT_OCCURRENCE);
					}

					// options
					if (plotTypicalFlight.getChild("options") != null) {

						// get element
						Element options = plotTypicalFlight.getChild("options");

						// data display
						if (options.getChild("showDataLabels") != null) {
							input.setLabelDisplay(Boolean.parseBoolean(options.getChildTextNormalize("showDataLabels")));
						}

						// max flights
						if (options.getChild("maxFlights") != null) {
							input.setLimit(Integer.parseInt(options.getChildTextNormalize("maxFlights")));
						}

						// results order
						if (options.getChild("resultsOrder") != null) {
							input.setOrder(options.getChildTextNormalize("resultsOrder").equals("descending"));
						}
					}

					// create tasks
					SaveCategoryDataset saveTask = new SaveCategoryDataset(null, outputPath);
					GenerateExternalStatistics plotTask = new GenerateExternalStatistics(input);
					plotTask.addAutomaticTask(id, new EmbeddedTask<>(saveTask, runMode));
					SelectAllExternalFlights selectTask = new SelectAllExternalFlights(null);
					selectTask.addAutomaticTask(id, new EmbeddedTask<>(plotTask, runMode));

					// connect to parent task
					AutomaticTaskOwner<ExternalStressSequence> parentTask = (AutomaticTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
					parentTask.addAutomaticTask(id, new EmbeddedTask<>(selectTask, runMode));

					// put task to tasks
					tasks.put(id, new InstructedTask(saveTask, true));
				}
			}
		}
	}

	/**
	 * Creates save mission profile info tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveMissionProfileInfo(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save mission profile info tasks...");

		// loop over save mission profile info elements
		for (Element saveMissionProfileInfo : equinoxInput.getChildren("saveMissionProfileInfo")) {

			// get inputs
			String id = saveMissionProfileInfo.getChild("id").getTextNormalize();
			String stressSequenceId = saveMissionProfileInfo.getChild("stressSequenceId").getTextNormalize();
			Path outputPath = Paths.get(saveMissionProfileInfo.getChild("outputPath").getTextNormalize());

			// create task
			SaveMissionProfile task = new SaveMissionProfile(null, outputPath.toFile());

			// connect to parent task
			AutomaticTaskOwner<StressSequence> parentTask = (AutomaticTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(task, true));
		}
	}

	/**
	 * Creates plot mission profile tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotMissionProfile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating plot mission profile tasks...");

		// loop over plot mission profile elements
		for (Element plotMissionProfile : equinoxInput.getChildren("plotMissionProfile")) {

			// get inputs
			String id = plotMissionProfile.getChild("id").getTextNormalize();
			Path outputPath = Paths.get(plotMissionProfile.getChild("outputPath").getTextNormalize());

			// from stress sequence
			if (plotMissionProfile.getChild("stressSequenceId") != null) {

				// get stress sequence id
				String stressSequenceId = plotMissionProfile.getChild("stressSequenceId").getTextNormalize();

				// create task
				SaveStressSequencePlotToFile task = new SaveStressSequencePlotToFile(null, PilotPointImageType.MISSION_PROFILE, outputPath);

				// connect to parent task
				AutomaticTaskOwner<StressSequence> parentTask = (AutomaticTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}

			// from fast equivalent stress
			else if (plotMissionProfile.getChild("fastEquivalentStressId") != null) {

				// get fast equivalent stress id
				String fastEquivalentStressId = plotMissionProfile.getChild("fastEquivalentStressId").getTextNormalize();

				// create task
				GenerateMissionProfilePlot task = new GenerateMissionProfilePlot(null, false, outputPath);

				// connect to parent task
				AutomaticTaskOwner<SpectrumItem> parentTask = (AutomaticTaskOwner<SpectrumItem>) tasks.get(fastEquivalentStressId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(task, true));
			}
		}
	}

	/**
	 * Creates save stress sequence tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveStressSequence(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save stress sequence tasks...");

		// loop over save stress sequence elements
		for (Element saveStressSequence : equinoxInput.getChildren("saveStressSequence")) {

			// create task
			String id = saveStressSequence.getChild("id").getTextNormalize();
			Path outputPath = Paths.get(saveStressSequence.getChild("outputPath").getTextNormalize());

			// stress sequence
			if (saveStressSequence.getChild("stressSequenceId") != null) {

				// get stress sequence id
				String stressSequenceId = saveStressSequence.getChild("stressSequenceId").getTextNormalize();
				SingleInputTask<StressSequence> task = null;

				// get file type
				FileType fileType = FileType.getFileType(outputPath.toFile());

				// save as SIGMA
				if (fileType.equals(FileType.SIGMA)) {
					task = new SaveStressSequenceAsSIGMA(null, outputPath.toFile());
				}

				// save as STH
				else if (fileType.equals(FileType.STH)) {
					task = new SaveStressSequenceAsSTH(null, outputPath.toFile());
				}

				// add to parent task
				AutomaticTaskOwner<StressSequence> parentTask = (AutomaticTaskOwner<StressSequence>) tasks.get(stressSequenceId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask((InternalEquinoxTask<?>) task, true));
			}

			// headless stress sequence
			else if (saveStressSequence.getChild("headlessStressSequenceId") != null) {

				// get headless stress sequence id
				String headlessStressSequenceId = saveStressSequence.getChild("headlessStressSequenceId").getTextNormalize();
				SingleInputTask<ExternalStressSequence> task = null;

				// get file type
				FileType fileType = FileType.getFileType(outputPath.toFile());

				// save as SIGMA
				if (fileType.equals(FileType.SIGMA)) {
					task = new SaveExternalStressSequenceAsSIGMA(null, outputPath.toFile());
				}

				// save as STH
				else if (fileType.equals(FileType.STH)) {
					task = new SaveExternalStressSequenceAsSTH(null, outputPath.toFile());
				}

				// add to parent task
				AutomaticTaskOwner<ExternalStressSequence> parentTask = (AutomaticTaskOwner<ExternalStressSequence>) tasks.get(headlessStressSequenceId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(task, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask((InternalEquinoxTask<?>) task, true));
			}
		}
	}

	/**
	 * Creates generate stress sequence tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void generateStressSequence(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating generate stress sequence tasks...");

		// input mapping
		HashMap<Path, GenerateStressSequenceInput> inputs = new HashMap<>();

		// loop over generate stress sequence elements
		for (Element generateStressSequence : equinoxInput.getChildren("generateStressSequence")) {

			// parse elements
			String id = generateStressSequence.getChild("id").getTextNormalize();
			String stfId = generateStressSequence.getChild("stfId").getTextNormalize();
			Path inputPath = Paths.get(generateStressSequence.getChild("inputPath").getTextNormalize());

			// get input parameters
			GenerateStressSequenceInput input = inputs.get(inputPath);
			if (input == null) {
				input = new ReadGenerateStressSequenceInput(this, inputPath).start(null);
				inputs.put(inputPath, input);
			}

			// create task
			GenerateStressSequence generateStressSequenceTask = new GenerateStressSequence(null, input);

			// add to parent task
			AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(generateStressSequenceTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(generateStressSequenceTask, true));
		}
	}

	/**
	 * Creates add headless stress sequence tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addHeadlessStressSequence(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating add headless stress sequence tasks...");

		// get add stress sequence elements
		for (Element addHeadlessStressSequence : equinoxInput.getChildren("addHeadlessStressSequence")) {

			// get id
			String id = addHeadlessStressSequence.getChild("id").getTextNormalize();

			// from SIGMA
			if (addHeadlessStressSequence.getChild("sigmaPath") != null) {

				// get path to SIGMA file
				Path sigmaPath = Paths.get(addHeadlessStressSequence.getChild("sigmaPath").getTextNormalize());

				// create task
				AddStressSequence task = new AddStressSequence(sigmaPath);

				// no previous instruction given
				if (addHeadlessStressSequence.getChild("previousInstructionId") == null) {
					tasks.put(id, new InstructedTask(task, false));
				}

				// previous instruction given
				else {

					// get previous instruction id
					String previousInstructionId = addHeadlessStressSequence.getChildTextNormalize("previousInstructionId");

					// add to parent task
					InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
					parentTask.addFollowerTask(task, runMode);

					// put task to tasks
					tasks.put(id, new InstructedTask(task, true));
				}
			}

			// from STH
			else if (addHeadlessStressSequence.getChild("sthPath") != null) {

				// get input paths
				Path sthPath = Paths.get(addHeadlessStressSequence.getChild("sthPath").getTextNormalize());
				Path flsPath = Paths.get(addHeadlessStressSequence.getChild("flsPath").getTextNormalize());

				// create task
				AddStressSequence task = new AddStressSequence(sthPath, flsPath);

				// no previous instruction given
				if (addHeadlessStressSequence.getChild("previousInstructionId") == null) {
					tasks.put(id, new InstructedTask(task, false));
				}

				// previous instruction given
				else {

					// get previous instruction id
					String previousInstructionId = addHeadlessStressSequence.getChildTextNormalize("previousInstructionId");

					// add to parent task
					InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
					parentTask.addFollowerTask(task, runMode);

					// put task to tasks
					tasks.put(id, new InstructedTask(task, true));
				}
			}
		}
	}

	/**
	 * Creates upload STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void uploadStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating upload STF tasks...");

		// loop over upload STF elements
		for (Element uploadStf : equinoxInput.getChildren("uploadStf")) {

			// create task
			String id = uploadStf.getChild("id").getTextNormalize();
			String exportId = uploadStf.getChild("exportId").getTextNormalize();
			UploadPilotPoints uploadStfTask = new UploadPilotPoints(null);

			// add to parent task
			AutomaticTaskOwner<Path> parentTask = (AutomaticTaskOwner<Path>) tasks.get(exportId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(uploadStfTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(uploadStfTask, true));
		}
	}

	/**
	 * Creates export STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void exportStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating export STF tasks...");

		// loop over export STF elements
		for (Element exportStf : equinoxInput.getChildren("exportStf")) {

			// create task
			String id = exportStf.getChild("id").getTextNormalize();
			String stfId = exportStf.getChild("stfId").getTextNormalize();
			Path outputPath = Paths.get(exportStf.getChild("outputPath").getTextNormalize());
			GetSTFInfo3 getSTFInfoTask = new GetSTFInfo3(null);
			ExportSTF exportSTFTask = new ExportSTF(outputPath.toFile());

			// create info array
			String[] info = new String[12];

			// loop over pilot point info elements
			for (Element pilotPointInfo : exportStf.getChildren("pilotPointInfo")) {

				// get attribute name and value
				String attributeName = pilotPointInfo.getChildTextNormalize("attributeName");
				String attributeValue = pilotPointInfo.getChildTextNormalize("attributeValue");

				// fatigue mission
				if (attributeName.equals("fatigueMission")) {
					exportSTFTask.setMission(attributeValue);
				}

				// description
				else if (attributeName.equals("description")) {
					info[GetSTFInfo2.DESCRIPTION] = attributeValue;
				}

				// data source
				else if (attributeName.equals("dataSource")) {
					info[GetSTFInfo2.DATA_SOURCE] = attributeValue;
				}

				// generation source
				else if (attributeName.equals("generationSource")) {
					info[GetSTFInfo2.GEN_SOURCE] = attributeValue;
				}

				// delivery reference
				else if (attributeName.equals("deliveryReference")) {
					info[GetSTFInfo2.DELIVERY_REF] = attributeValue;
				}

				// issue
				else if (attributeName.equals("issue")) {
					info[GetSTFInfo2.ISSUE] = attributeValue;
				}

				// eid
				else if (attributeName.equals("eid")) {
					info[GetSTFInfo2.EID] = attributeValue;
				}

				// element type
				else if (attributeName.equals("elementType")) {
					info[GetSTFInfo2.ELEMENT_TYPE] = attributeValue;
				}

				// frame position
				else if (attributeName.equals("framePosition")) {
					info[GetSTFInfo2.FRAME_RIB_POS] = attributeValue;
				}

				// stringer position
				else if (attributeName.equals("stringerPosition")) {
					info[GetSTFInfo2.STRINGER_POS] = attributeValue;
				}

				// fatigue material
				else if (attributeName.equals("fatigueMaterial")) {
					info[GetSTFInfo2.FATIGUE_MATERIAL] = attributeValue;
				}

				// preffas material
				else if (attributeName.equals("preffasMaterial")) {
					info[GetSTFInfo2.PREFFAS_MATERIAL] = attributeValue;
				}

				// linear material
				else if (attributeName.equals("linearMaterial")) {
					info[GetSTFInfo2.LINEAR_MATERIAL] = attributeValue;
				}
			}

			// set info
			exportSTFTask.setInfo(info);

			// create image mapping
			HashMap<PilotPointImageType, Image> images = new HashMap<>();

			// loop over pilot point image elements
			for (Element pilotPointImage : exportStf.getChildren("pilotPointImage")) {

				// get attribute name and value
				String imageTypeName = pilotPointImage.getChildTextNormalize("imageType");
				Path imagePath = Paths.get(pilotPointImage.getChildTextNormalize("imagePath"));

				// get image type
				PilotPointImageType imageType = null;
				for (PilotPointImageType type : PilotPointImageType.values()) {
					if (type.toString().equals(imageTypeName)) {
						imageType = type;
						break;
					}
				}

				// get image bytes
				Image image = null;
				byte[] imageBytes = new byte[(int) imagePath.toFile().length()];
				try (ImageInputStream imgStream = ImageIO.createImageInputStream(imagePath.toFile())) {
					imgStream.read(imageBytes);
					try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
						image = new Image(inputStream);
					}
				}

				// add to mapping
				images.put(imageType, image);
			}

			// set images
			if (!images.isEmpty()) {
				exportSTFTask.setImages(images);
			}

			// add to first task
			getSTFInfoTask.addAutomaticTask(id, new EmbeddedTask<>(exportSTFTask, runMode));

			// previous instruction id given
			boolean previousInstructionIdGiven = exportStf.getChild("previousInstructionId") != null;
			if (previousInstructionIdGiven) {

				// get previous instruction id
				String previousInstructionId = exportStf.getChildTextNormalize("previousInstructionId");

				// add to parent task
				InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
				parentTask.addFollowerTask(getSTFInfoTask, runMode);
			}

			// add to parent task
			AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(getSTFInfoTask, previousInstructionIdGiven ? ExecutionMode.NO_RUN : runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(exportSTFTask, true));
		}
	}

	/**
	 * Creates share STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void shareStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating share STF tasks...");

		// loop over share STF elements
		for (Element shareStf : equinoxInput.getChildren("shareStf")) {

			// create task
			String id = shareStf.getChild("id").getTextNormalize();
			String stfId = shareStf.getChild("stfId").getTextNormalize();
			String recipient = shareStf.getChild("recipient").getTextNormalize();
			ShareSTF shareSTFTask = new ShareSTF(null, Arrays.asList(taskPanel_.getOwner().getOwner().getUser(recipient)));

			// add to parent task
			AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(shareSTFTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(shareSTFTask, true));
		}
	}

	/**
	 * Creates save STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save STF tasks...");

		// loop over save STF elements
		for (Element saveStf : equinoxInput.getChildren("saveStf")) {

			// create task
			String id = saveStf.getChild("id").getTextNormalize();
			String stfId = saveStf.getChild("stfId").getTextNormalize();
			Path outputPath = Paths.get(saveStf.getChild("outputPath").getTextNormalize());
			SaveSTF saveSTFTask = new SaveSTF(null, outputPath.toFile(), FileType.getFileType(outputPath.toFile()));

			// add to parent task
			AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(saveSTFTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(saveSTFTask, true));
		}
	}

	/**
	 * Creates override mission parameters to STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void assignMissionParametersToStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating override mission parameters to STF tasks...");

		// loop over override mission parameters elements
		for (Element assignMissionParametersToStf : equinoxInput.getChildren("assignMissionParametersToStf")) {

			// create task
			String id = assignMissionParametersToStf.getChild("id").getTextNormalize();
			String stfId = assignMissionParametersToStf.getChild("stfId").getTextNormalize();
			ArrayList<MissionParameter> parameters = new ArrayList<>();

			// loop over mission parameter elements
			for (Element missionParameter : assignMissionParametersToStf.getChildren("missionParameter")) {

				// get parameter name and value
				String name = missionParameter.getChildTextNormalize("name");
				double value = Double.parseDouble(missionParameter.getChildTextNormalize("value"));

				// create and add mission parameter
				parameters.add(new MissionParameter(name, value));
			}

			// create task
			AssignMissionParameters<STFFile> assignMissionParametersTask = new AssignMissionParameters<>(null, parameters.toArray(new MissionParameter[parameters.size()]));

			// add to parent task
			AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(assignMissionParametersTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(assignMissionParametersTask, true));
		}
	}

	/**
	 * Creates override fatigue mission tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void overrideFatigueMission(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating override fatigue mission tasks...");

		// loop over override fatigue mission elements
		for (Element overrideFatigueMission : equinoxInput.getChildren("overrideFatigueMission")) {

			// create task
			String id = overrideFatigueMission.getChild("id").getTextNormalize();
			String stfId = overrideFatigueMission.getChild("stfId").getTextNormalize();
			String fatigueMission = overrideFatigueMission.getChild("fatigueMission").getTextNormalize();
			SetSTFMission setStfMissionTask = new SetSTFMission(null, fatigueMission);

			// add to parent task
			AutomaticTaskOwner<STFFile> parentTask = (AutomaticTaskOwner<STFFile>) tasks.get(stfId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(setStfMissionTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(setStfMissionTask, true));
		}
	}

	/**
	 * Creates add STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating add STF tasks...");

		// loop over add STF elements
		for (Element addStf : equinoxInput.getChildren("addStf")) {

			// get task and spectrum ids
			String id = addStf.getChild("id").getTextNormalize();
			String spectrumId = addStf.getChild("spectrumId").getTextNormalize();

			// STF path given
			if (addStf.getChild("stfPath") != null) {

				// create task
				Path stfPath = Paths.get(addStf.getChild("stfPath").getTextNormalize());
				AddSTFFiles addSTFFilesTask = new AddSTFFiles(Arrays.asList(stfPath.toFile()), null, null);

				// add to parent task
				AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(addSTFFilesTask, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(addSTFFilesTask, true));
			}

			// search entry given
			else if (addStf.getChild("searchEntry") != null) {

				// create search input
				PilotPointSearchInput input = new PilotPointSearchInput();

				// loop over search entries
				for (Element searchEntry : addStf.getChildren("searchEntry")) {

					// get sub elements
					String attributeName = searchEntry.getChild("attributeName").getTextNormalize();
					String keyword = searchEntry.getChild("keyword").getTextNormalize();

					// get criteria
					int criteriaIndex = 0;
					if (searchEntry.getChild("criteria") != null) {
						String criteria = searchEntry.getChild("criteria").getTextNormalize();
						criteriaIndex = Arrays.asList("Contains", "Equals", "Starts with", "Ends with").indexOf(criteria);
					}

					// add input
					PilotPointInfoType infoType = PilotPointInfoType.valueOf(attributeName.toUpperCase());
					input.addInput(infoType, new SearchItem(keyword, criteriaIndex));
				}

				// set search engine settings
				SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
				panel.setEngineSettings(input);

				// create tasks
				AdvancedPilotPointSearch searchPilotPointTask = new AdvancedPilotPointSearch(input);
				DownloadPilotPoint downloadPilotPointTask = new DownloadPilotPoint(null, null, null);

				// add download task to search
				searchPilotPointTask.addAutomaticTask(id, new EmbeddedTask<>(downloadPilotPointTask, runMode));

				// add search to add spectrum
				AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(searchPilotPointTask, runMode));

				// add to tasks
				tasks.put(id, new InstructedTask(downloadPilotPointTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(searchPilotPointTask, true));
			}

			// create dummy STF
			else {

				// create task
				String fileName = addStf.getChild("stfName").getTextNormalize();
				boolean is1D = addStf.getChild("stressState").getTextNormalize().equals("1d");
				CreateDummySTFFile createDummySTFTask = new CreateDummySTFFile(null, fileName, is1D);

				// get 1g stresses
				if (addStf.getChild("onegStresses") != null) {
					Element onegStresses = addStf.getChild("onegStresses");
					double sx = Double.parseDouble(onegStresses.getChildTextNormalize("sx"));
					double sy = Double.parseDouble(onegStresses.getChildTextNormalize("sy"));
					double sxy = Double.parseDouble(onegStresses.getChildTextNormalize("sxy"));
					createDummySTFTask.setOneGStresses(sx, sy, sxy);
				}

				// get increment stresses
				if (addStf.getChild("incrementStresses") != null) {
					Element incrementStresses = addStf.getChild("incrementStresses");
					double sx = Double.parseDouble(incrementStresses.getChildTextNormalize("sx"));
					double sy = Double.parseDouble(incrementStresses.getChildTextNormalize("sy"));
					double sxy = Double.parseDouble(incrementStresses.getChildTextNormalize("sxy"));
					createDummySTFTask.setIncrementStresses(sx, sy, sxy);
				}

				// get delta-p stresses
				if (addStf.getChild("dpStresses") != null) {
					Element dpStresses = addStf.getChild("dpStresses");
					String dpLoadcase = dpStresses.getChildTextNormalize("dpLoadcase");
					double sx = Double.parseDouble(dpStresses.getChildTextNormalize("sx"));
					double sy = Double.parseDouble(dpStresses.getChildTextNormalize("sy"));
					double sxy = Double.parseDouble(dpStresses.getChildTextNormalize("sxy"));
					createDummySTFTask.setDeltaPStresses(dpLoadcase, sx, sy, sxy);
				}

				// get delta-t stresses
				if (addStf.getChild("dtStresses") != null) {

					// get element
					Element dtStresses = addStf.getChild("dtStresses");

					// superior
					if (dtStresses.getChild("superior") != null) {
						Element superior = addStf.getChild("superior");
						String loadcase = superior.getChildTextNormalize("loadcase");
						double sx = Double.parseDouble(superior.getChildTextNormalize("sx"));
						double sy = Double.parseDouble(superior.getChildTextNormalize("sy"));
						double sxy = Double.parseDouble(superior.getChildTextNormalize("sxy"));
						createDummySTFTask.setDeltaTSupStresses(loadcase, sx, sy, sxy);
					}

					// inferior
					if (dtStresses.getChild("inferior") != null) {
						Element inferior = addStf.getChild("inferior");
						String loadcase = inferior.getChildTextNormalize("loadcase");
						double sx = Double.parseDouble(inferior.getChildTextNormalize("sx"));
						double sy = Double.parseDouble(inferior.getChildTextNormalize("sy"));
						double sxy = Double.parseDouble(inferior.getChildTextNormalize("sxy"));
						createDummySTFTask.setDeltaTInfStresses(loadcase, sx, sy, sxy);
					}
				}

				// add to parent task
				AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(createDummySTFTask, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(createDummySTFTask, true));
			}
		}
	}

	/**
	 * Creates download STF tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void downloadStf(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating download STF tasks...");

		// loop over add STF elements
		for (Element downloadStf : equinoxInput.getChildren("downloadStf")) {

			// get sub elements
			String id = downloadStf.getChild("id").getTextNormalize();
			Path outputFile = Paths.get(downloadStf.getChild("outputPath").getTextNormalize());

			// create search input
			PilotPointSearchInput input = new PilotPointSearchInput();

			// loop over search entries
			for (Element searchEntry : downloadStf.getChildren("searchEntry")) {

				// get sub elements
				String attributeName = searchEntry.getChild("attributeName").getTextNormalize();
				String keyword = searchEntry.getChild("keyword").getTextNormalize();

				// get criteria
				int criteriaIndex = 0;
				if (searchEntry.getChild("criteria") != null) {
					String criteria = searchEntry.getChild("criteria").getTextNormalize();
					criteriaIndex = Arrays.asList("Contains", "Equals", "Starts with", "Ends with").indexOf(criteria);
				}

				// add input
				PilotPointInfoType infoType = PilotPointInfoType.valueOf(attributeName.toUpperCase());
				input.addInput(infoType, new SearchItem(keyword, criteriaIndex));
			}

			// set search engine settings
			SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
			panel.setEngineSettings(input);

			// create tasks
			AdvancedPilotPointSearch searchPilotPointTask = new AdvancedPilotPointSearch(input);
			DownloadPilotPoint downloadPilotPointTask = new DownloadPilotPoint(null, outputFile, null);
			searchPilotPointTask.addAutomaticTask(id, new EmbeddedTask<>(downloadPilotPointTask, runMode));

			// no previous instruction given
			if (downloadStf.getChild("previousInstructionId") == null) {

				// add to tasks
				tasks.put(id, new InstructedTask(downloadPilotPointTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(searchPilotPointTask, false));
			}

			// previous instruction given
			else {

				// get previous instruction id
				String previousInstructionId = downloadStf.getChildTextNormalize("previousInstructionId");

				// add to parent task
				InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
				parentTask.addFollowerTask(searchPilotPointTask, runMode);

				// put task to tasks
				tasks.put(id, new InstructedTask(downloadPilotPointTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(searchPilotPointTask, true));
			}
		}
	}

	/**
	 * Creates upload spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void uploadSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating upload spectrum tasks...");

		// loop over upload spectrum elements
		for (Element uploadSpectrum : equinoxInput.getChildren("uploadSpectrum")) {

			// create task
			String id = uploadSpectrum.getChild("id").getTextNormalize();
			String exportId = uploadSpectrum.getChild("exportId").getTextNormalize();
			UploadSpectra uploadSectrumTask = new UploadSpectra(null);

			// add to parent task
			AutomaticTaskOwner<Path> parentTask = (AutomaticTaskOwner<Path>) tasks.get(exportId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(uploadSectrumTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(uploadSectrumTask, true));
		}
	}

	/**
	 * Creates export spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void exportSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating export spectrum tasks...");

		// loop over export spectrum elements
		for (Element exportSpectrum : equinoxInput.getChildren("exportSpectrum")) {

			// create task
			String id = exportSpectrum.getChild("id").getTextNormalize();
			String spectrumId = exportSpectrum.getChild("spectrumId").getTextNormalize();
			Path outputPath = Paths.get(exportSpectrum.getChild("outputPath").getTextNormalize());
			GetSpectrumEditInfo getSpectrumInfoTask = new GetSpectrumEditInfo(null);
			ExportSpectrum exportSpectrumTask = new ExportSpectrum(null, null, outputPath.toFile());

			// set optional parameters
			if (exportSpectrum.getChild("deliveryReference") != null) {
				exportSpectrumTask.setDeliveryReference(exportSpectrum.getChild("deliveryReference").getTextNormalize());
			}
			if (exportSpectrum.getChild("description") != null) {
				exportSpectrumTask.setDescription(exportSpectrum.getChild("description").getTextNormalize());
			}

			// add to first task
			getSpectrumInfoTask.addAutomaticTask(id, new EmbeddedTask<>(exportSpectrumTask, runMode));

			// previous instruction id given
			boolean previousInstructionIdGiven = exportSpectrum.getChild("previousInstructionId") != null;
			if (previousInstructionIdGiven) {

				// get previous instruction id
				String previousInstructionId = exportSpectrum.getChildTextNormalize("previousInstructionId");

				// add to parent task
				InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
				parentTask.addFollowerTask(getSpectrumInfoTask, runMode);
			}

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(getSpectrumInfoTask, previousInstructionIdGiven ? ExecutionMode.NO_RUN : runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(exportSpectrumTask, true));
		}
	}

	/**
	 * Creates share spectrum file tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void shareSpectrumFile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating share spectrum file tasks...");

		// loop over share spectrum file elements
		for (Element shareSpectrumFile : equinoxInput.getChildren("shareSpectrumFile")) {

			// create task
			String id = shareSpectrumFile.getChild("id").getTextNormalize();
			String spectrumId = shareSpectrumFile.getChild("spectrumId").getTextNormalize();
			FileType fileType = FileType.getFileTypeForExtension("." + shareSpectrumFile.getChild("fileType").getTextNormalize());
			String recipient = shareSpectrumFile.getChild("recipient").getTextNormalize();
			ShareSpectrumFile shareSpectrumFileTask = new ShareSpectrumFile(null, fileType, Arrays.asList(taskPanel_.getOwner().getOwner().getUser(recipient)));

			// previous instruction id given
			boolean previousInstructionIdGiven = shareSpectrumFile.getChild("previousInstructionId") != null;
			if (previousInstructionIdGiven) {

				// get previous instruction id
				String previousInstructionId = shareSpectrumFile.getChildTextNormalize("previousInstructionId");

				// add to parent task
				InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
				parentTask.addFollowerTask(shareSpectrumFileTask, runMode);
			}

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(shareSpectrumFileTask, previousInstructionIdGiven ? ExecutionMode.NO_RUN : runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(shareSpectrumFileTask, true));
		}
	}

	/**
	 * Creates share spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void shareSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating share spectrum tasks...");

		// loop over share spectrum elements
		for (Element shareSpectrum : equinoxInput.getChildren("shareSpectrum")) {

			// create task
			String id = shareSpectrum.getChild("id").getTextNormalize();
			String spectrumId = shareSpectrum.getChild("spectrumId").getTextNormalize();
			String recipient = shareSpectrum.getChild("recipient").getTextNormalize();
			ShareSpectrum shareSpectrumTask = new ShareSpectrum(null, Arrays.asList(taskPanel_.getOwner().getOwner().getUser(recipient)));

			// previous instruction id given
			boolean previousInstructionIdGiven = shareSpectrum.getChild("previousInstructionId") != null;
			if (previousInstructionIdGiven) {

				// get previous instruction id
				String previousInstructionId = shareSpectrum.getChildTextNormalize("previousInstructionId");

				// add to parent task
				InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
				parentTask.addFollowerTask(shareSpectrumTask, runMode);
			}

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(shareSpectrumTask, previousInstructionIdGiven ? ExecutionMode.NO_RUN : runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(shareSpectrumTask, true));
		}
	}

	/**
	 * Creates save spectrum file tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveSpectrumFile(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save spectrum file tasks...");

		// loop over save spectrum file elements
		for (Element saveSpectrumFile : equinoxInput.getChildren("saveSpectrumFile")) {

			// create task
			String id = saveSpectrumFile.getChild("id").getTextNormalize();
			String spectrumId = saveSpectrumFile.getChild("spectrumId").getTextNormalize();
			Path outputPath = Paths.get(saveSpectrumFile.getChild("outputPath").getTextNormalize());
			SingleInputTask<Spectrum> saveSpectrumFileTask = null;

			// get file type
			FileType fileType = FileType.getFileType(outputPath.toFile());

			// ANA
			if (fileType.equals(FileType.ANA)) {
				saveSpectrumFileTask = new SaveANA(null, outputPath.toFile(), FileType.ANA);
			}

			// CVT
			else if (fileType.equals(FileType.CVT)) {
				saveSpectrumFileTask = new SaveCVT(null, outputPath.toFile(), FileType.CVT);
			}

			// TXT
			else if (fileType.equals(FileType.TXT)) {
				saveSpectrumFileTask = new SaveTXT(null, outputPath.toFile(), FileType.TXT);
			}

			// FLS
			else if (fileType.equals(FileType.FLS)) {
				saveSpectrumFileTask = new SaveFLS(null, outputPath.toFile(), FileType.FLS);
			}

			// XLS
			else if (fileType.equals(FileType.XLS)) {
				saveSpectrumFileTask = new SaveConversionTable(null, outputPath.toFile(), FileType.XLS);
			}

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(saveSpectrumFileTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask((InternalEquinoxTask<?>) saveSpectrumFileTask, true));
		}
	}

	/**
	 * Creates save spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating save spectrum tasks...");

		// loop over save spectrum elements
		for (Element saveSpectrum : equinoxInput.getChildren("saveSpectrum")) {

			// create task
			String id = saveSpectrum.getChild("id").getTextNormalize();
			String spectrumId = saveSpectrum.getChild("spectrumId").getTextNormalize();
			Path outputPath = Paths.get(saveSpectrum.getChild("outputPath").getTextNormalize());
			SaveSpectrum saveSpectrumTask = new SaveSpectrum(null, outputPath.toFile());

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(saveSpectrumTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(saveSpectrumTask, true));
		}
	}

	/**
	 * Creates assign mission parameters to spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void assignMissionParametersToSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating assign mission parameters to spectrum tasks...");

		// loop over assign mission parameters elements
		for (Element assignMissionParametersToSpectrum : equinoxInput.getChildren("assignMissionParametersToSpectrum")) {

			// create task
			String id = assignMissionParametersToSpectrum.getChild("id").getTextNormalize();
			String spectrumId = assignMissionParametersToSpectrum.getChild("spectrumId").getTextNormalize();
			ArrayList<MissionParameter> parameters = new ArrayList<>();

			// loop over mission parameter elements
			for (Element missionParameter : assignMissionParametersToSpectrum.getChildren("missionParameter")) {

				// get parameter name and value
				String name = missionParameter.getChildTextNormalize("name");
				double value = Double.parseDouble(missionParameter.getChildTextNormalize("value"));

				// create and add mission parameter
				parameters.add(new MissionParameter(name, value));
			}

			// create task
			AssignMissionParameters<Spectrum> assignMissionParametersTask = new AssignMissionParameters<>(null, parameters.toArray(new MissionParameter[parameters.size()]));

			// add to parent task
			AutomaticTaskOwner<Spectrum> parentTask = (AutomaticTaskOwner<Spectrum>) tasks.get(spectrumId).getTask();
			parentTask.addAutomaticTask(id, new EmbeddedTask<>(assignMissionParametersTask, runMode));

			// put task to tasks
			tasks.put(id, new InstructedTask(assignMissionParametersTask, true));
		}
	}

	/**
	 * Creates add spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating add spectrum tasks...");

		// loop over add spectrum elements
		for (Element addSpectrum : equinoxInput.getChildren("addSpectrum")) {

			// get id
			String id = addSpectrum.getChild("id").getTextNormalize();

			// from SPEC bundle
			if (addSpectrum.getChild("specPath") != null) {

				// get path to bundle
				Path specPath = Paths.get(addSpectrum.getChildTextNormalize("specPath"));

				// create task
				AddSpectrum task = new AddSpectrum(specPath);

				// no previous instruction given
				if (addSpectrum.getChild("previousInstructionId") == null) {
					tasks.put(id, new InstructedTask(task, false));
				}

				// previous instruction given
				else {

					// get previous instruction id
					String previousInstructionId = addSpectrum.getChildTextNormalize("previousInstructionId");

					// add to parent task
					InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
					parentTask.addFollowerTask(task, runMode);

					// put task to tasks
					tasks.put(id, new InstructedTask(task, true));
				}
			}

			// from central database
			else if (addSpectrum.getChild("downloadId") != null) {

				// get download id
				String downloadId = addSpectrum.getChild("downloadId").getTextNormalize();

				// create task
				AddSpectrum addSpectrumTask = new AddSpectrum(null, null);

				// add to parent task
				AutomaticTaskOwner<Pair<Path, SpectrumInfo>> parentTask = (AutomaticTaskOwner<Pair<Path, SpectrumInfo>>) tasks.get(downloadId).getTask();
				parentTask.addAutomaticTask(id, new EmbeddedTask<>(addSpectrumTask, runMode));

				// put task to tasks
				tasks.put(id, new InstructedTask(addSpectrumTask, true));
			}

			// from CDF set files
			else {

				// get paths to CDF set files
				Path anaFile = Paths.get(addSpectrum.getChild("anaPath").getTextNormalize());
				Path cvtFile = Paths.get(addSpectrum.getChild("cvtPath").getTextNormalize());
				Path flsFile = Paths.get(addSpectrum.getChild("flsPath").getTextNormalize());
				Path conversionTable = Paths.get(addSpectrum.getChild("xlsPath").getTextNormalize());
				String sheet = addSpectrum.getChild("convSheet").getTextNormalize();
				Path txtFile = null;
				if (addSpectrum.getChild("txtPath") != null) {
					txtFile = Paths.get(addSpectrum.getChild("txtPath").getTextNormalize());
				}

				// create task
				AddSpectrum task = new AddSpectrum(anaFile, txtFile, cvtFile, flsFile, conversionTable, sheet, null);

				// no previous instruction given
				if (addSpectrum.getChild("previousInstructionId") == null) {
					tasks.put(id, new InstructedTask(task, false));
				}

				// previous instruction given
				else {

					// get previous instruction id
					String previousInstructionId = addSpectrum.getChildTextNormalize("previousInstructionId");

					// add to parent task
					InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
					parentTask.addFollowerTask(task, runMode);

					// put task to tasks
					tasks.put(id, new InstructedTask(task, true));
				}
			}
		}
	}

	/**
	 * Creates download spectrum tasks.
	 *
	 * @param equinoxInput
	 *            Root input element.
	 * @param tasks
	 *            List to store tasks to be executed.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void downloadSpectrum(Element equinoxInput, HashMap<String, InstructedTask> tasks) throws Exception {

		// update info
		updateMessage("Creating download spectrum tasks...");

		// loop over download spectrum elements
		for (Element downloadSpectrum : equinoxInput.getChildren("downloadSpectrum")) {

			// get sub elements
			String id = downloadSpectrum.getChild("id").getTextNormalize();
			Path outputFile = Paths.get(downloadSpectrum.getChild("outputPath").getTextNormalize());

			// create search input
			SpectrumSearchInput input = new SpectrumSearchInput();

			// loop over search entries
			for (Element searchEntry : downloadSpectrum.getChildren("searchEntry")) {

				// get sub elements
				String attributeName = searchEntry.getChild("attributeName").getTextNormalize();
				String keyword = searchEntry.getChild("keyword").getTextNormalize();

				// get criteria
				int criteriaIndex = 0;
				if (searchEntry.getChild("criteria") != null) {
					String criteria = searchEntry.getChild("criteria").getTextNormalize();
					criteriaIndex = Arrays.asList("Contains", "Equals", "Starts with", "Ends with").indexOf(criteria);
				}

				// add input
				SpectrumInfoType infoType = SpectrumInfoType.valueOf(attributeName.toUpperCase());
				input.addInput(infoType, new SearchItem(keyword, criteriaIndex));
			}

			// set search engine settings
			SearchEngineSettingsPanel panel = (SearchEngineSettingsPanel) taskPanel_.getOwner().getOwner().getInputPanel().getSubPanel(InputPanel.SEARCH_ENGINE_SETTINGS_PANEL);
			panel.setEngineSettings(input);

			// create tasks
			AdvancedSpectrumSearch searchSpectrumTask = new AdvancedSpectrumSearch(input);
			DownloadSpectrum downloadSpectrumTask = new DownloadSpectrum(null, outputFile, false);
			searchSpectrumTask.addAutomaticTask(id, new EmbeddedTask<>(downloadSpectrumTask, runMode));

			// no previous instruction given
			if (downloadSpectrum.getChild("previousInstructionId") == null) {

				// add to tasks
				tasks.put(id, new InstructedTask(downloadSpectrumTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(searchSpectrumTask, false));
			}

			// previous instruction given
			else {

				// get previous instruction id
				String previousInstructionId = downloadSpectrum.getChildTextNormalize("previousInstructionId");

				// add to parent task
				InternalEquinoxTask<?> parentTask = tasks.get(previousInstructionId).getTask();
				parentTask.addFollowerTask(searchSpectrumTask, runMode);

				// put task to tasks
				tasks.put(id, new InstructedTask(downloadSpectrumTask, true));
				tasks.put("ownerOf_" + id, new InstructedTask(searchSpectrumTask, true));
			}
		}
	}
}