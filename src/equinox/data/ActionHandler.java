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
package equinox.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.controller.ActiveTasksPanel;
import equinox.controller.AircraftModelInfoPanel;
import equinox.controller.CommentLoadCasePanel;
import equinox.controller.CreateElementGroupFromItemPanel;
import equinox.controller.DamageAnglePanel;
import equinox.controller.ExternalPlotViewPanel;
import equinox.controller.FileViewPanel;
import equinox.controller.GenerateStressSequencePanel;
import equinox.controller.InfoViewPanel;
import equinox.controller.InputPanel;
import equinox.controller.LinkPilotPointsPopup;
import equinox.controller.MainScreen;
import equinox.controller.MissionProfilePanel;
import equinox.controller.NotificationPanel2;
import equinox.controller.OverrideSTFMissionPanel;
import equinox.controller.PlotViewPanel;
import equinox.controller.RenamePanel;
import equinox.controller.RfortEquivalentStressPanel;
import equinox.controller.RfortReportPanel;
import equinox.controller.RfortResultsPanel;
import equinox.controller.STFEquivalentStressPanel;
import equinox.controller.SaveAircraftEquivalentStressRatioPanel;
import equinox.controller.SaveAircraftLifeFactorPanel;
import equinox.controller.SaveDamageAngleInfoPanel;
import equinox.controller.SaveDamageContributionsPanel;
import equinox.controller.SaveEquivalentStressInfoPanel;
import equinox.controller.SaveEquivalentStressRatioPanel;
import equinox.controller.SaveFlightDamageContributionsPanel;
import equinox.controller.SaveLifeFactorPanel;
import equinox.controller.ShareFilePopup;
import equinox.controller.SpectrumInfoPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftLoadCases;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalFlights;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.Flight;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.fileType.Flights;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.PilotPoints;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.Rfort;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.plugin.FileType;
import equinox.process.SaveBucketOutputFilesProcess;
import equinox.task.AddAircraftLoadCases;
import equinox.task.AddSTFFiles;
import equinox.task.CreateElementGroupsFromFile;
import equinox.task.DeleteFiles;
import equinox.task.ExportContributions;
import equinox.task.ExportMultipleSTFs;
import equinox.task.ExportMultipleSpectra;
import equinox.task.GenerateFlightOccurrencePlot;
import equinox.task.GenerateHOFlightPlot;
import equinox.task.GenerateHSFlightPlot;
import equinox.task.GenerateLevelCrossingsPlot;
import equinox.task.GenerateLongestFlightPlot;
import equinox.task.GenerateMissionProfilePlot;
import equinox.task.GenerateNumPeaksPlot;
import equinox.task.PlotDamageContributions;
import equinox.task.PlotFastFlightOccurrences;
import equinox.task.PlotFastHOFlight;
import equinox.task.PlotFastHSFlight;
import equinox.task.PlotFastHistogram;
import equinox.task.PlotFastLevelCrossings;
import equinox.task.PlotFastLongestFlight;
import equinox.task.PlotFastMissionProfile;
import equinox.task.PlotFastNumPeaks;
import equinox.task.PlotFlightDamageContributions;
import equinox.task.PlotRfortPeaks;
import equinox.task.SaveANA;
import equinox.task.SaveAircraftEquivalentStress;
import equinox.task.SaveAircraftModel;
import equinox.task.SaveAircraftModelFile;
import equinox.task.SaveBucketOutputFiles;
import equinox.task.SaveCVT;
import equinox.task.SaveConversionTable;
import equinox.task.SaveExternalFLS;
import equinox.task.SaveExternalStressSequenceAsSIGMA;
import equinox.task.SaveExternalStressSequenceAsSTH;
import equinox.task.SaveFLS;
import equinox.task.SaveLoadCase;
import equinox.task.SaveOutputFile;
import equinox.task.SaveOutputFiles;
import equinox.task.SaveRainflow;
import equinox.task.SaveRfortInfo;
import equinox.task.SaveSTF;
import equinox.task.SaveSTFBucket;
import equinox.task.SaveSpectrum;
import equinox.task.SaveStressSequenceAsSIGMA;
import equinox.task.SaveStressSequenceAsSTH;
import equinox.task.SaveTXT;
import equinox.task.SelectExternalFlight;
import equinox.task.SelectFlight;
import equinox.task.ShowOutputFile;
import equinox.task.ShowSTFFiles;
import equinox.utility.Utility;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Class for file tree action handler.
 *
 * @author Murat Artim
 * @date Apr 9, 2014
 * @time 2:41:57 PM
 */
public class ActionHandler implements EventHandler<ActionEvent> {

	/** The main screen. */
	private final MainScreen owner_;

	/**
	 * Creates action handler.
	 *
	 * @param owner
	 *            The owner main screen.
	 */
	public ActionHandler(MainScreen owner) {
		owner_ = owner;
	}

	@Override
	public void handle(ActionEvent e) {

		// get source ID
		String id = ((MenuItem) e.getSource()).getId();

		// save ANA
		if (id.equals("saveANA")) {
			saveANA();
		}
		else if (id.equals("saveSpectrum")) {
			saveSpectrum();
		}
		else if (id.equals("exportSpectrum")) {
			SpectrumInfoPanel panel = (SpectrumInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SPECTRUM_INFO_PANEL);
			panel.setPanelMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.SPECTRUM_INFO_PANEL);
		}

		// export A/C model
		else if (id.equals("exportModel")) {
			AircraftModelInfoPanel panel = (AircraftModelInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.AC_MODEL_INFO_PANEL);
			panel.setPanelMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.AC_MODEL_INFO_PANEL);
		}

		// export multiplication tables
		else if (id.equals("exportMultTables")) {
			owner_.getInputPanel().showSubPanel(InputPanel.EXPORT_MULTIPLICATION_TABLES_PANEL);
		}
		else if (id.equals("exportMultipleSpectra")) {
			exportMultipleSpectra();
		}
		else if (id.equals("saveCONV")) {
			saveCONV();
		}
		else if (id.equals("saveFLS")) {
			saveFLS();
		}
		else if (id.equals("saveCVT")) {
			saveCVT();
		}
		else if (id.equals("saveSTF")) {
			saveSTF();
		}
		else if (id.equals("saveSTFBucket")) {
			saveSTFBucket();
		}
		else if (id.equals("exportSTF")) {
			owner_.getInputPanel().showSubPanel(InputPanel.EXPORT_STF_PANEL);
		}
		else if (id.equals("exportMultipleSTFs")) {
			exportMultipleSTFs();
		}
		else if (id.equals("saveStressSequence")) {
			saveStressSequence();
		}
		else if (id.equals("saveTXT")) {
			saveTXT();
		}
		else if (id.equals("saveModel")) {
			saveModel();
		}
		else if (id.equals("saveGridFile")) {
			saveModelGrids();
		}
		else if (id.equals("saveElementFile")) {
			saveModelElements();
		}
		else if (id.equals("saveGRP")) {
			saveModelGroups();
		}
		else if (id.equals("generateSpectrum")) {
			((GenerateStressSequencePanel) owner_.getInputPanel().getSubPanel(InputPanel.GENERATE_STRESS_SEQUENCE_PANEL)).enableLoadcaseSegmentFactors(true);
			owner_.getInputPanel().showSubPanel(InputPanel.GENERATE_STRESS_SEQUENCE_PANEL);
		}

		// generate spectrum (no event modifiers)
		else if (id.equals("generateSpectrumNoEventModifiers")) {
			((GenerateStressSequencePanel) owner_.getInputPanel().getSubPanel(InputPanel.GENERATE_STRESS_SEQUENCE_PANEL)).enableLoadcaseSegmentFactors(false);
			owner_.getInputPanel().showSubPanel(InputPanel.GENERATE_STRESS_SEQUENCE_PANEL);
		}

		// damage contribution analysis
		else if (id.equals("damageContribution")) {
			owner_.getInputPanel().showSubPanel(InputPanel.DAMAGE_CONTRIBUTION_PANEL);
		}
		else if (id.equals("damageAngle")) {
			((DamageAnglePanel) owner_.getInputPanel().getSubPanel(InputPanel.DAMAGE_ANGLE_PANEL)).enableLoadcaseSegmentFactors(true);
			owner_.getInputPanel().showSubPanel(InputPanel.DAMAGE_ANGLE_PANEL);
		}

		// damage angle analysis (no event modifiers)
		else if (id.equals("damageAngleNoEventModifiers")) {
			((DamageAnglePanel) owner_.getInputPanel().getSubPanel(InputPanel.DAMAGE_ANGLE_PANEL)).enableLoadcaseSegmentFactors(false);
			owner_.getInputPanel().showSubPanel(InputPanel.DAMAGE_ANGLE_PANEL);
		}

		// calculate equivalent stress from STF
		else if (id.equals("equivalentStressSTF")) {
			STFEquivalentStressPanel panel = (STFEquivalentStressPanel) owner_.getInputPanel().getSubPanel(InputPanel.STF_EQUIVALENT_STRESS_PANEL);
			panel.enableLoadcaseSegmentFactors(true);
			owner_.getInputPanel().showSubPanel(InputPanel.STF_EQUIVALENT_STRESS_PANEL);
		}

		// calculate equivalent stress from STF (no event modifiers)
		else if (id.equals("equivalentStressSTFNoEventModifiers")) {
			STFEquivalentStressPanel panel = (STFEquivalentStressPanel) owner_.getInputPanel().getSubPanel(InputPanel.STF_EQUIVALENT_STRESS_PANEL);
			panel.enableLoadcaseSegmentFactors(false);
			owner_.getInputPanel().showSubPanel(InputPanel.STF_EQUIVALENT_STRESS_PANEL);
		}

		// save as 1D STF
		else if (id.equals("saveAs1D")) {
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_AS_1D_STF_PANEL);
		}
		else if (id.equals("addSTF")) {
			addSTFFiles();
		}
		else if (id.equals("addSTFFromDirectory")) {
			addSTFFilesFromDirectory();
		}
		else if (id.equals("downloadSampleSTF")) {
			owner_.downloadSampleInput("AddStressInputFile");
		}
		else if (id.equals("dummySTF")) {
			owner_.getInputPanel().showSubPanel(InputPanel.DUMMY_STF_PANEL);
		}
		else if (id.equals("delete")) {
			delete();
		}
		else if (id.equals("hide")) {
			hide();
		}
		else if (id.equals("showSTFs")) {
			owner_.getActiveTasksPanel().runTaskInParallel(new ShowSTFFiles((STFFileBucket) getSelectedItems().get(0)));
		}
		else if (id.equals("plotFlight")) {
			plotTypicalFlight();
		}
		else if (id.equals("plotExternalFlight")) {
			plotExternalTypicalFlight();
		}
		else if (id.startsWith("selectFlight")) {
			selectFlight(id);
		}
		else if (id.startsWith("selectExternalFlight")) {
			selectExternalFlight(id);
		}
		else if (id.equals("showSpectrumStats")) {
			owner_.getInputPanel().showSubPanel(InputPanel.SPECTRUM_STATS_PANEL);
		}
		else if (id.equals("showExternalSpectrumStats")) {
			owner_.getInputPanel().showSubPanel(InputPanel.EXTERNAL_STATS_PANEL);
		}
		else if (id.equals("plotMissionProfile")) {
			((MissionProfilePanel) owner_.getInputPanel().getSubPanel(InputPanel.MISSION_PROFILE_PANEL)).plot();
		}
		else if (id.equals("compareSpectra")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_STRESS_SEQUENCE_PANEL);
		}
		else if (id.equals("compareExternalSpectra")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_EXTERNAL_STRESS_SEQUENCE_PANEL);
		}
		else if (id.equals("compareEquivalentStress")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_EQUIVALENT_STRESS_PANEL);
		}
		else if (id.equals("generateLifeFactor")) {
			owner_.getInputPanel().showSubPanel(InputPanel.GENERATE_LIFE_FACTOR_PANEL);
		}
		else if (id.equals("generateStressRatio")) {
			owner_.getInputPanel().showSubPanel(InputPanel.GENERATE_EQUIVALENT_STRESS_RATIO_PANEL);
		}
		else if (id.equals("compareDamageAngleLifeFactor")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_DAMAGE_ANGLE_LIFE_FACTORS_PANEL);
		}
		else if (id.equals("plotDamageContributions")) {
			owner_.getActiveTasksPanel().runTaskInParallel(new PlotDamageContributions((LoadcaseDamageContributions) getSelectedItems().get(0)));
		}
		else if (id.equals("plotFlightDamageContributions")) {
			owner_.getActiveTasksPanel().runTaskInParallel(new PlotFlightDamageContributions((FlightDamageContributions) getSelectedItems().get(0)));
		}
		else if (id.equals("compareDamageContributions")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_DAMAGE_CONTRIBUTIONS_PANEL);
		}
		else if (id.equals("saveDamageContributions")) {
			SaveDamageContributionsPanel panel = (SaveDamageContributionsPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_DAMAGE_CONTRIBUTIONS_PANEL);
			panel.setMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_DAMAGE_CONTRIBUTIONS_PANEL);
		}

		// export damage contributions
		else if (id.equals("exportDamageContributions")) {
			exportDamageContributions();
		}

		// save typical flight damage contributions
		else if (id.equals("saveFlightDamageContributions")) {
			SaveFlightDamageContributionsPanel panel = (SaveFlightDamageContributionsPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_FLIGHT_DAMAGE_CONTRIBUTIONS_PANEL);
			panel.setMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_FLIGHT_DAMAGE_CONTRIBUTIONS_PANEL);
		}

		// share damage contributions
		else if (id.equals("shareDamageContributions")) {
			SaveDamageContributionsPanel panel = (SaveDamageContributionsPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_DAMAGE_CONTRIBUTIONS_PANEL);
			panel.setMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_DAMAGE_CONTRIBUTIONS_PANEL);
		}

		// share typical flight damage contributions
		else if (id.equals("shareFlightDamageContributions")) {
			SaveFlightDamageContributionsPanel panel = (SaveFlightDamageContributionsPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_FLIGHT_DAMAGE_CONTRIBUTIONS_PANEL);
			panel.setMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_FLIGHT_DAMAGE_CONTRIBUTIONS_PANEL);
		}

		// save equivalent stress
		else if (id.equals("saveEqStress")) {
			SaveEquivalentStressInfoPanel panel = (SaveEquivalentStressInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
			panel.setMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
		}

		// save analysis output file
		else if (id.equals("saveAnalysisOutputFile")) {
			saveAnalysisOutputFile();
		}

		// save bucket fatigue equivalent stresses
		else if (id.equals("saveBucketFatigueEqStresses")) {
			SaveEquivalentStressInfoPanel panel = (SaveEquivalentStressInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
			panel.setMode(true);
			panel.setBucketStressType(SaveEquivalentStressInfoPanel.FATIGUE);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
		}

		// save bucket preffas equivalent stresses
		else if (id.equals("saveBucketPreffasEqStresses")) {
			SaveEquivalentStressInfoPanel panel = (SaveEquivalentStressInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
			panel.setMode(true);
			panel.setBucketStressType(SaveEquivalentStressInfoPanel.PREFFAS);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
		}

		// save bucket linear equivalent stresses
		else if (id.equals("saveBucketLinearEqStresses")) {
			SaveEquivalentStressInfoPanel panel = (SaveEquivalentStressInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
			panel.setMode(true);
			panel.setBucketStressType(SaveEquivalentStressInfoPanel.LINEAR);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
		}

		// save bucket fatigue analysis output files
		else if (id.equals("saveBucketFatigueAnalysisOutputFiles")) {
			saveBucketAnalysisOutputFiles(SaveBucketOutputFilesProcess.FATIGUE);
		}

		// save bucket preffas analysis output files
		else if (id.equals("saveBucketPreffasAnalysisOutputFiles")) {
			saveBucketAnalysisOutputFiles(SaveBucketOutputFilesProcess.PREFFAS);
		}

		// save bucket linear propagation analysis output files
		else if (id.equals("saveBucketLinearAnalysisOutputFiles")) {
			saveBucketAnalysisOutputFiles(SaveBucketOutputFilesProcess.LINEAR);
		}

		// save life factors
		else if (id.equals("saveLifeFactors")) {
			SaveLifeFactorPanel panel = (SaveLifeFactorPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_LIFE_FACTOR_PANEL);
			panel.setMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_LIFE_FACTOR_PANEL);
		}

		// save equivalent stress ratio
		else if (id.equals("saveStressRatios")) {
			SaveEquivalentStressRatioPanel panel = (SaveEquivalentStressRatioPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_RATIO_PANEL);
			panel.setMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_RATIO_PANEL);
		}

		// share equivalent stress
		else if (id.equals("shareEquivalentStress")) {
			SaveEquivalentStressInfoPanel panel = (SaveEquivalentStressInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
			panel.setMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
		}

		// share bucket fatigue equivalent stress
		else if (id.equals("shareBucketFatigueEqStresses")) {
			SaveEquivalentStressInfoPanel panel = (SaveEquivalentStressInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
			panel.setMode(false);
			panel.setBucketStressType(SaveEquivalentStressInfoPanel.FATIGUE);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
		}

		// share bucket preffas equivalent stress
		else if (id.equals("shareBucketPreffasEqStresses")) {
			SaveEquivalentStressInfoPanel panel = (SaveEquivalentStressInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
			panel.setMode(false);
			panel.setBucketStressType(SaveEquivalentStressInfoPanel.PREFFAS);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
		}

		// share bucket linear equivalent stress
		else if (id.equals("shareBucketLinearEqStresses")) {
			SaveEquivalentStressInfoPanel panel = (SaveEquivalentStressInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
			panel.setMode(false);
			panel.setBucketStressType(SaveEquivalentStressInfoPanel.LINEAR);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_PANEL);
		}

		// share bucket STF files
		else if (id.equals("shareBucketSTFs")) {
			share(null, "STF");
		}

		// share bucket fatigue analysis output files
		else if (id.equals("shareBucketFatigueAnalysisOutputFiles")) {
			share(null, "fatigueOutputs");
		}

		// share bucket preffas analysis output files
		else if (id.equals("shareBucketPreffasAnalysisOutputFiles")) {
			share(null, "preffasOutputs");
		}

		// share bucket linear analysis output files
		else if (id.equals("shareBucketLinearAnalysisOutputFiles")) {
			share(null, "linearOutputs");
		}

		// share life factor
		else if (id.equals("shareLifeFactors")) {
			SaveLifeFactorPanel panel = (SaveLifeFactorPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_LIFE_FACTOR_PANEL);
			panel.setMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_LIFE_FACTOR_PANEL);
		}

		// share equivalent stress ratio
		else if (id.equals("shareStressRatios")) {
			SaveEquivalentStressRatioPanel panel = (SaveEquivalentStressRatioPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_RATIO_PANEL);
			panel.setMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_EQUIVALENT_STRESS_RATIO_PANEL);
		}

		// save damage angle
		else if (id.equals("saveDamageAngle")) {
			SaveDamageAngleInfoPanel panel = (SaveDamageAngleInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_DAMAGE_ANGLE_PANEL);
			panel.setMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_DAMAGE_ANGLE_PANEL);
		}

		// share damage angle
		else if (id.equals("shareDamageAngle")) {
			SaveDamageAngleInfoPanel panel = (SaveDamageAngleInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_DAMAGE_ANGLE_PANEL);
			panel.setMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_DAMAGE_ANGLE_PANEL);
		}

		// save rainflow
		else if (id.equals("saveRainflow")) {
			saveRainflow();
		}
		else if (id.equals("plotHistogram")) {
			owner_.getInputPanel().showSubPanel(InputPanel.HISTOGRAM_PANEL);
		}
		else if (id.equals("plot3DHistogram")) {
			owner_.getInputPanel().showSubPanel(InputPanel.HISTOGRAM_3D_PANEL);
		}
		else if (id.equals("showAnalysisOutputFile")) {
			owner_.getActiveTasksPanel().runTaskSilently(new ShowOutputFile((SpectrumItem) getSelectedItems().get(0)), false);
		}
		else if (id.equals("plotLevelCrossing")) {
			owner_.getInputPanel().showSubPanel(InputPanel.LEVEL_CROSSING_PANEL);
		}
		else if (id.equals("plotExternalLevelCrossing")) {
			owner_.getInputPanel().showSubPanel(InputPanel.EXTERNAL_LEVEL_CROSSING_PANEL);
		}
		else if (id.equals("equivalentStress")) {
			owner_.getInputPanel().showSubPanel(InputPanel.EQUIVALENT_STRESS_PANEL);
		}
		else if (id.equals("rename")) {
			rename();
		}
		else if (id.equals("find")) {
			find();
		}
		else if (id.equals("editSpectrumInfo")) {
			SpectrumInfoPanel panel = (SpectrumInfoPanel) owner_.getInputPanel().getSubPanel(InputPanel.SPECTRUM_INFO_PANEL);
			panel.setPanelMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.SPECTRUM_INFO_PANEL);
		}

		// edit external stress sequence info
		else if (id.equals("editSequenceInfo")) {
			owner_.getInputPanel().showSubPanel(InputPanel.STRESS_SEQEUNCE_INFO_PANEL);
		}
		else if (id.equals("overrideMission")) {
			overrideMission();
		}
		else if (id.equals("editACInfo")) {
			owner_.getInputPanel().showSubPanel(InputPanel.AC_MODEL_INFO_PANEL);
		}
		else if (id.equals("assignMissionParameters")) {
			owner_.getInputPanel().showSubPanel(InputPanel.MISSION_PARAMETERS_PANEL);
		}
		else if (id.equals("overrideMissionParameters")) {
			owner_.getInputPanel().showSubPanel(InputPanel.MISSION_PARAMETERS_PANEL);
		}
		else if (id.equals("editSTFInfo")) {
			owner_.getInputPanel().showSubPanel(InputPanel.STF_INFO_PANEL);
		}
		else if (id.equals("setMaterial")) {
			owner_.getInputPanel().showSubPanel(InputPanel.MATERIAL_PANEL);
		}
		else if (id.equals("setSTFImage")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.IMAGE);
		}

		// set pilot point mission profile image
		else if (id.equals("setSTFImageMP")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.MISSION_PROFILE);
		}

		// set pilot point longest typical flight plot image
		else if (id.equals("setSTFImageTFL")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.LONGEST_FLIGHT);
		}

		// set pilot point flight with highest occurrence plot image
		else if (id.equals("setSTFImageTFHO")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE);
		}

		// set pilot point flight with highest total stress plot image
		else if (id.equals("setSTFImageTFHS")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS);
		}

		// set pilot point level crossing plot image
		else if (id.equals("setSTFImageLC")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.LEVEL_CROSSING);
		}

		// set pilot point damage angle plot image
		else if (id.equals("setSTFImageDA")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.DAMAGE_ANGLE);
		}

		// set pilot point typical flight number of peaks plot image
		else if (id.equals("setSTFImageSTNOP")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.NUMBER_OF_PEAKS);
		}

		// set pilot point typical flight occurrences plot image
		else if (id.equals("setSTFImageSTFO")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.FLIGHT_OCCURRENCE);
		}

		// set pilot point rainflow histogram plot image
		else if (id.equals("setSTFImageSTRH")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.RAINFLOW_HISTOGRAM);
		}

		// set pilot point loadcase damage contribution plot image
		else if (id.equals("setSTFImageDC")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.LOADCASE_DAMAGE_CONTRIBUTION);
		}

		// set pilot point typical flight damage contribution plot image
		else if (id.equals("setSTFImageTFDC")) {
			InfoViewPanel panel = (InfoViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getSTFView().setImage(PilotPointImageType.FLIGHT_DAMAGE_CONTRIBUTION);
		}

		// plot damage angles
		else if (id.equals("plotAngles")) {
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_DAMAGE_ANGLE_PANEL);
		}
		else if (id.equals("compareFlights")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_FLIGHTS_PANEL);
		}
		else if (id.equals("compareExternalFlights")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_EXTERNAL_FLIGHTS_PANEL);
		}
		else if (id.equals("share")) {
			share(null);
		}
		else if (id.equals("shareRainflowCycle")) {
			share(FileType.RFLOW);
		}
		else if (id.equals("shareANA")) {
			share(FileType.ANA);
		}
		else if (id.equals("shareTXT")) {
			share(FileType.TXT);
		}
		else if (id.equals("shareFLS")) {
			share(FileType.FLS);
		}
		else if (id.equals("shareCVT")) {
			share(FileType.CVT);
		}
		else if (id.equals("shareCONV")) {
			share(FileType.XLS);
		}
		else if (id.equals("shareExternalFLS")) {
			share(FileType.FLS);
		}
		else if (id.equals("shareGridFile")) {
			share(FileType.F07);
		}
		else if (id.equals("shareElementFile")) {
			share(FileType.F06);
		}
		else if (id.equals("shareGRP")) {
			share(FileType.GRP);
		}
		else if (id.equals("plotACStructure")) {
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_AC_STRUCTURE_PANEL);
		}
		else if (id.equals("renameElementGroups")) {
			owner_.getInputPanel().showSubPanel(InputPanel.RENAME_ELEMENT_GROUPS_PANEL);
		}
		else if (id.equals("deleteElementGroups")) {
			owner_.getInputPanel().showSubPanel(InputPanel.DELETE_ELEMENT_GROUPS_PANEL);
		}
		else if (id.equals("createElementGroupsFromFile")) {
			createElementGroupsFromFile();
		}
		else if (id.equals("createElementGroupFromEIDs")) {
			owner_.getInputPanel().showSubPanel(InputPanel.CREATE_ELEMENT_GROUP_FROM_EIDS_PANEL);
		}
		else if (id.equals("createElementGroupFromCoordinates")) {
			owner_.getInputPanel().showSubPanel(InputPanel.CREATE_ELEMENT_GROUP_FROM_COORDS_PANEL);
		}
		else if (id.equals("createElementGroupFromQVLV")) {
			owner_.getInputPanel().showSubPanel(InputPanel.CREATE_ELEMENT_GROUP_FROM_QV_LV_POSITIONS_PANEL);
		}
		else if (id.equals("createElementGroupFromGroups")) {
			owner_.getInputPanel().showSubPanel(InputPanel.CREATE_ELEMENT_GROUP_FROM_GROUPS_PANEL);
		}
		else if (id.equals("addLoadCases")) {
			addLoadCases();
		}
		else if (id.equals("downloadSampleLoadCase")) {
			owner_.downloadSampleInput("AddNewLoadCases");
		}
		else if (id.equals("downloadSampleElementGroupsFile")) {
			owner_.downloadSampleInput("CreateNewElementGroups");
		}
		else if (id.equals("addEquivalentStresses")) {
			owner_.getInputPanel().showSubPanel(InputPanel.ADD_EQUIVALENT_STRESSES_PANEL);
		}
		else if (id.equals("plotElementStresses")) {
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_ELEMENT_STRESSES_PANEL);
		}
		else if (id.equals("compareElementStresses")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_ELEMENT_STRESSES_PANEL);
		}
		else if (id.equals("compareLoadCases")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_LOAD_CASES_PANEL);
		}
		else if (id.equals("createElementGroupsFromLoadCase")) {
			createElementGroupFromItem();
		}
		else if (id.equals("plotAircraftModelEquivalentStresses")) {
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_AC_EQUIVALENT_STRESSES_PANEL);
		}
		else if (id.equals("plotAircraftModelLifeFactors")) {
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_AC_LIFE_FACTORS_PANEL);
		}
		else if (id.equals("plotAircraftModelEquivalentStressRatios")) {
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_AC_EQUIVALENT_STRESS_RATIOS_PANEL);
		}
		else if (id.equals("compareAircraftModelEquivalentStresses")) {
			owner_.getInputPanel().showSubPanel(InputPanel.COMPARE_AC_EQUIVALENT_STRESSES_PANEL);
		}
		else if (id.equals("generateAircraftModelLifeFactor")) {
			owner_.getInputPanel().showSubPanel(InputPanel.GENERATE_AC_LIFE_FACTORS_PANEL);
		}
		else if (id.equals("generateAircraftModelStressRatio")) {
			owner_.getInputPanel().showSubPanel(InputPanel.GENERATE_AC_EQUIVALENT_STRESS_RATIOS_PANEL);
		}
		else if (id.equals("createElementGroupsFromEquivalentStress")) {
			createElementGroupFromItem();
		}
		else if (id.equals("createElementGroupFromLinkedPilotPoints")) {
			createElementGroupFromItem();
		}
		else if (id.equals("saveLoadCase")) {
			saveLoadCase();
		}
		else if (id.equals("commentLoadCase")) {
			commentLoadCase();
		}
		else if (id.equals("linkPilotPoints")) {
			linkPilotPoints();
		}
		else if (id.equals("saveAircraftEquivalentStress")) {
			saveAircraftEquivalentStress();
		}
		else if (id.equals("saveAircraftLifeFactors")) {
			SaveAircraftLifeFactorPanel panel = (SaveAircraftLifeFactorPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_AC_LIFE_FACTOR_PANEL);
			panel.setMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_AC_LIFE_FACTOR_PANEL);
		}

		// share aircraft life factors
		else if (id.equals("shareAircraftLifeFactors")) {
			SaveAircraftLifeFactorPanel panel = (SaveAircraftLifeFactorPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_AC_LIFE_FACTOR_PANEL);
			panel.setMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_AC_LIFE_FACTOR_PANEL);
		}

		// save aircraft equivalent stress ratios
		else if (id.equals("saveAircraftStressRatios")) {
			SaveAircraftEquivalentStressRatioPanel panel = (SaveAircraftEquivalentStressRatioPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_AC_EQUIVALENT_STRESS_RATIO_PANEL);
			panel.setMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_AC_EQUIVALENT_STRESS_RATIO_PANEL);
		}

		// share aircraft equivalent stress ratios
		else if (id.equals("shareAircraftStressRatios")) {
			SaveAircraftEquivalentStressRatioPanel panel = (SaveAircraftEquivalentStressRatioPanel) owner_.getInputPanel().getSubPanel(InputPanel.SAVE_AC_EQUIVALENT_STRESS_RATIO_PANEL);
			panel.setMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.SAVE_AC_EQUIVALENT_STRESS_RATIO_PANEL);
		}

		// plot RFORT fatigue results
		else if (id.equals("plotRfortFatigueResults")) {
			RfortResultsPanel panel = (RfortResultsPanel) owner_.getInputPanel().getSubPanel(InputPanel.PLOT_RFORT_RESULTS_PANEL);
			panel.setStressType(SaveRfortInfo.FATIGUE);
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_RFORT_RESULTS_PANEL);
		}

		// plot RFORT preffas results
		else if (id.equals("plotRfortPreffasResults")) {
			RfortResultsPanel panel = (RfortResultsPanel) owner_.getInputPanel().getSubPanel(InputPanel.PLOT_RFORT_RESULTS_PANEL);
			panel.setStressType(SaveRfortInfo.PREFFAS);
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_RFORT_RESULTS_PANEL);
		}

		// plot RFORT linear results
		else if (id.equals("plotRfortLinearResults")) {
			RfortResultsPanel panel = (RfortResultsPanel) owner_.getInputPanel().getSubPanel(InputPanel.PLOT_RFORT_RESULTS_PANEL);
			panel.setStressType(SaveRfortInfo.LINEAR);
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_RFORT_RESULTS_PANEL);
		}

		// plot RFORT average number of peaks
		else if (id.equals("plotRfortPeaks")) {
			Rfort rfort = (Rfort) getSelectedItems().get(0);
			owner_.getActiveTasksPanel().runTaskInParallel(new PlotRfortPeaks(rfort));
		}

		// plot RFORT fatigue equivalent stresses
		else if (id.equals("plotRfortFatigueStresses")) {
			RfortEquivalentStressPanel panel = (RfortEquivalentStressPanel) owner_.getInputPanel().getSubPanel(InputPanel.PLOT_RFORT_STRESSES_PANEL);
			panel.setStressType(SaveRfortInfo.FATIGUE);
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_RFORT_STRESSES_PANEL);
		}

		// plot RFORT preffas equivalent stresses
		else if (id.equals("plotRfortPreffasStresses")) {
			RfortEquivalentStressPanel panel = (RfortEquivalentStressPanel) owner_.getInputPanel().getSubPanel(InputPanel.PLOT_RFORT_STRESSES_PANEL);
			panel.setStressType(SaveRfortInfo.PREFFAS);
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_RFORT_STRESSES_PANEL);
		}

		// plot RFORT linear propagation equivalent stresses
		else if (id.equals("plotRfortLinearStresses")) {
			RfortEquivalentStressPanel panel = (RfortEquivalentStressPanel) owner_.getInputPanel().getSubPanel(InputPanel.PLOT_RFORT_STRESSES_PANEL);
			panel.setStressType(SaveRfortInfo.LINEAR);
			owner_.getInputPanel().showSubPanel(InputPanel.PLOT_RFORT_STRESSES_PANEL);
		}

		// add RFORT omissions
		else if (id.equals("addRfortOmissions")) {
			owner_.getInputPanel().showSubPanel(InputPanel.ADD_RFORT_OMISSIONS_PANEL);
		}
		else if (id.equals("generateRfortReport")) {
			RfortReportPanel panel = (RfortReportPanel) owner_.getInputPanel().getSubPanel(InputPanel.RFORT_REPORT_PANEL);
			panel.setMode(true);
			owner_.getInputPanel().showSubPanel(InputPanel.RFORT_REPORT_PANEL);
		}

		// share RFORT report
		else if (id.equals("shareRfortReport")) {
			RfortReportPanel panel = (RfortReportPanel) owner_.getInputPanel().getSubPanel(InputPanel.RFORT_REPORT_PANEL);
			panel.setMode(false);
			owner_.getInputPanel().showSubPanel(InputPanel.RFORT_REPORT_PANEL);
		}

		// plot mission profile for fast equivalent stress
		else if (id.equals("plotFastMissionProfile")) {
			SpectrumItem item = (SpectrumItem) getSelectedItems().get(0);
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new PlotFastMissionProfile(item));
		}

		// generate mission profile plots for fast equivalent stresses
		else if (id.equals("generateFastMissionProfile")) {
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			for (TreeItem<String> item : getSelectedItems()) {
				tm.runTaskSequentially(new GenerateMissionProfilePlot((SpectrumItem) item, false, null));
			}
		}

		// plot level crossings for fast equivalent stress
		else if (id.equals("plotFastLevelCrossing")) {
			SpectrumItem item = (SpectrumItem) getSelectedItems().get(0);
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new PlotFastLevelCrossings(item));
		}

		// generate level crossing plots for fast equivalent stresses
		else if (id.equals("generateFastLevelCrossing")) {
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			for (TreeItem<String> item : getSelectedItems()) {
				tm.runTaskSequentially(new GenerateLevelCrossingsPlot((SpectrumItem) item, false, false, null));
			}
		}

		// plot longest typical flight for fast equivalent stress
		else if (id.equals("plotFastLongestFlight")) {
			SpectrumItem item = (SpectrumItem) getSelectedItems().get(0);
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new PlotFastLongestFlight(item));
		}

		// generate longest typical flight plots for fast equivalent stresses
		else if (id.equals("generateFastLongestFlight")) {
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			for (TreeItem<String> item : getSelectedItems()) {
				tm.runTaskSequentially(new GenerateLongestFlightPlot((SpectrumItem) item, false, null));
			}
		}

		// plot highest occurring typical flight for fast equivalent stress
		else if (id.equals("plotFastHOFlight")) {
			SpectrumItem item = (SpectrumItem) getSelectedItems().get(0);
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new PlotFastHOFlight(item));
		}

		// generate highest occurring typical flight plots for fast equivalent
		// stresses
		else if (id.equals("generateFastHOFlight")) {
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			for (TreeItem<String> item : getSelectedItems()) {
				tm.runTaskSequentially(new GenerateHOFlightPlot((SpectrumItem) item, false, null));
			}
		}

		// plot highest total stress typical flight for fast equivalent stress
		else if (id.equals("plotFastHSFlight")) {
			SpectrumItem item = (SpectrumItem) getSelectedItems().get(0);
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new PlotFastHSFlight(item));
		}

		// generate highest total stress typical flight plots for fast
		// equivalent stresses
		else if (id.equals("generateFastHSFlight")) {
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			for (TreeItem<String> item : getSelectedItems()) {
				tm.runTaskSequentially(new GenerateHSFlightPlot((SpectrumItem) item, false, null));
			}
		}

		// plot typical flight number of peaks for fast equivalent stress
		else if (id.equals("plotFastNumPeaks")) {
			SpectrumItem item = (SpectrumItem) getSelectedItems().get(0);
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new PlotFastNumPeaks(item));
		}

		// generate typical flight number of peaks for fast equivalent stresses
		else if (id.equals("generateFastNumPeaks")) {
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			for (TreeItem<String> item : getSelectedItems()) {
				tm.runTaskSequentially(new GenerateNumPeaksPlot((SpectrumItem) item, false, null));
			}
		}

		// plot typical flight occurrences for fast equivalent stress
		else if (id.equals("plotFastOccurrences")) {
			SpectrumItem item = (SpectrumItem) getSelectedItems().get(0);
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new PlotFastFlightOccurrences(item));
		}

		// generate typical flight occurrences for fast equivalent stresses
		else if (id.equals("generateFastOccurrences")) {
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			for (TreeItem<String> item : getSelectedItems()) {
				tm.runTaskSequentially(new GenerateFlightOccurrencePlot((SpectrumItem) item, false, null));
			}
		}

		// plot rainflow histogram for fast equivalent stress
		else if (id.equals("plotFastHistogram")) {
			SpectrumItem item = (SpectrumItem) getSelectedItems().get(0);
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new PlotFastHistogram(item));
		}

		// generate rainflow histogram for fast equivalent stresses
		else if (id.equals("generateFastHistogram")) {
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			for (TreeItem<String> item : getSelectedItems()) {
				tm.runTaskSequentially(new GenerateLevelCrossingsPlot((SpectrumItem) item, false, false, null));
			}
		}
	}

	/**
	 * Exports loadcase damage contributions.
	 */
	private void exportDamageContributions() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// nothing selected
		if (selected.isEmpty())
			return;

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.XLS.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Damage Contributions.xls");
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.setInitialDirectory(selectedFile);

		// append extension if necessary
		File output = FileType.appendExtension(selectedFile, FileType.XLS);

		// convert selected items to spectrum items
		ArrayList<SpectrumItem> items = new ArrayList<>();
		for (TreeItem<String> item : selected) {
			items.add((SpectrumItem) item);
		}

		// export
		owner_.getActiveTasksPanel().runTaskInParallel(new ExportContributions(output, items));
	}

	/**
	 * Saves bucket analysis output files.
	 *
	 * @param stressType
	 *            Equivalent stress type.
	 */
	private void saveBucketAnalysisOutputFiles(int stressType) {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// nothing selected
		if (selected.isEmpty())
			return;

		// get directory chooser
		DirectoryChooser dirChooser = owner_.getDirectoryChooser();

		// show save dialog
		File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

		// no directory selected
		if (selectedDir == null)
			return;

		// set initial directory
		owner_.setInitialDirectory(selectedDir);

		// export
		owner_.getActiveTasksPanel().runTaskInParallel(new SaveBucketOutputFiles(selected.toArray(new STFFileBucket[] {}), selectedDir.toPath(), stressType));
	}

	/**
	 * Saves analysis output file(s).
	 */
	private void saveAnalysisOutputFile() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// no item selected
		if (selected.isEmpty())
			return;

		// only 1 item selected
		if (selected.size() == 1) {

			// get selected item
			SpectrumItem item = (SpectrumItem) selected.get(0);

			// build initial file name
			String fileName = null;
			if (item instanceof FatigueEquivalentStress) {
				fileName = "Fatigue_" + FileType.getNameWithoutExtension(((FatigueEquivalentStress) item).getParentItem().getParentItem().getName());
			}
			else if (item instanceof PreffasEquivalentStress) {
				fileName = "Preffas_" + FileType.getNameWithoutExtension(((PreffasEquivalentStress) item).getParentItem().getParentItem().getName());
			}
			else if (item instanceof LinearEquivalentStress) {
				fileName = "Linear_" + FileType.getNameWithoutExtension(((LinearEquivalentStress) item).getParentItem().getParentItem().getName());
			}
			else if (item instanceof ExternalFatigueEquivalentStress) {
				fileName = "Fatigue_" + FileType.getNameWithoutExtension(((ExternalFatigueEquivalentStress) item).getParentItem().getName());
			}
			else if (item instanceof ExternalPreffasEquivalentStress) {
				fileName = "Preffas_" + FileType.getNameWithoutExtension(((ExternalPreffasEquivalentStress) item).getParentItem().getName());
			}
			else if (item instanceof ExternalLinearEquivalentStress) {
				fileName = "Linear_" + FileType.getNameWithoutExtension(((ExternalLinearEquivalentStress) item).getParentItem().getName());
			}
			else if (item instanceof FastFatigueEquivalentStress) {
				fileName = "Fatigue_" + FileType.getNameWithoutExtension(((FastFatigueEquivalentStress) item).getParentItem().getName());
			}
			else if (item instanceof FastPreffasEquivalentStress) {
				fileName = "Preffas_" + FileType.getNameWithoutExtension(((FastPreffasEquivalentStress) item).getParentItem().getName());
			}
			else if (item instanceof FastLinearEquivalentStress) {
				fileName = "Linear_" + FileType.getNameWithoutExtension(((FastLinearEquivalentStress) item).getParentItem().getName());
			}
			else
				return;

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser();

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(fileName));
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// save
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveOutputFile(item, selectedFile.toPath()));
		}

		// multiple items selected
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// export
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveOutputFiles(selected.toArray(new SpectrumItem[] {}), selectedDir.toPath()));
		}
	}

	/**
	 * Exports multiple STF files.
	 */
	private void exportMultipleSTFs() {

		// get directory chooser
		DirectoryChooser dirChooser = owner_.getDirectoryChooser();

		// show save dialog
		File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

		// no directory selected
		if (selectedDir == null)
			return;

		// set initial directory
		owner_.setInitialDirectory(selectedDir);

		// get selected STF files
		ObservableList<TreeItem<String>> stfFiles = getSelectedItems();

		// export
		owner_.getActiveTasksPanel().runTaskInParallel(new ExportMultipleSTFs(stfFiles, selectedDir));
	}

	/**
	 * Exports multiple spectra.
	 */
	private void exportMultipleSpectra() {

		// get directory chooser
		DirectoryChooser dirChooser = owner_.getDirectoryChooser();

		// show save dialog
		File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

		// no directory selected
		if (selectedDir == null)
			return;

		// set initial directory
		owner_.setInitialDirectory(selectedDir);

		// get selected STF files
		ObservableList<TreeItem<String>> spectra = getSelectedItems();

		// export
		owner_.getActiveTasksPanel().runTaskInParallel(new ExportMultipleSpectra(spectra, selectedDir));
	}

	/**
	 * Links pilot points to A/C model.
	 */
	private void linkPilotPoints() {
		PilotPoints item = (PilotPoints) getSelectedItems().get(0);
		((LinkPilotPointsPopup) owner_.getInputPanel().getPopup(InputPanel.LINK_PILOT_POINTS_POPUP)).show(item.getParentItem());
	}

	/**
	 * Returns the list of selected files.
	 *
	 * @return The list of selected files.
	 */
	private ObservableList<TreeItem<String>> getSelectedItems() {
		return owner_.getInputPanel().getSelectedFiles();
	}

	/**
	 * Selects STH flight according to given criteria.
	 *
	 * @param id
	 *            Criteria.
	 */
	private void selectFlight(String id) {

		// get selected STH file
		TreeItem<String> file = getSelectedItems().get(0);
		StressSequence spectrum = null;
		if (file instanceof Flights) {
			spectrum = ((Flights) file).getParentItem();
		}
		else {
			spectrum = (StressSequence) file;
		}

		// initialize criteria
		int criteria = -1;

		// longest flight
		if (id.equals("selectFlightLongest")) {
			criteria = SelectFlight.LONGEST_FLIGHT;
		}
		else if (id.equals("selectFlightShortest")) {
			criteria = SelectFlight.SHORTEST_FLIGHT;
		}
		else if (id.equals("selectFlightMaxVal")) {
			criteria = SelectFlight.MAX_VALIDITY;
		}
		else if (id.equals("selectFlightMinVal")) {
			criteria = SelectFlight.MIN_VALIDITY;
		}
		else if (id.equals("selectFlightMaxTotal")) {
			criteria = SelectFlight.MAX_TOTAL;
		}
		else if (id.equals("selectFlightMinTotal")) {
			criteria = SelectFlight.MIN_TOTAL;
		}
		else if (id.equals("selectFlightMax1g")) {
			criteria = SelectFlight.MAX_1G;
		}
		else if (id.equals("selectFlightMin1g")) {
			criteria = SelectFlight.MIN_1G;
		}
		else if (id.equals("selectFlightMaxInc")) {
			criteria = SelectFlight.MAX_INC;
		}
		else if (id.equals("selectFlightMinInc")) {
			criteria = SelectFlight.MIN_INC;
		}
		else if (id.equals("selectFlightMaxDP")) {
			criteria = SelectFlight.MAX_DP;
		}
		else if (id.equals("selectFlightMinDP")) {
			criteria = SelectFlight.MIN_DP;
		}
		else if (id.equals("selectFlightMaxDT")) {
			criteria = SelectFlight.MAX_DT;
		}
		else if (id.equals("selectFlightMinDT")) {
			criteria = SelectFlight.MIN_DT;
		}

		// select flight
		owner_.getActiveTasksPanel().runTaskInParallel(new SelectFlight(spectrum, criteria));
	}

	/**
	 * Selects external STH flight according to given criteria.
	 *
	 * @param id
	 *            Criteria.
	 */
	private void selectExternalFlight(String id) {

		// get selected STH file
		TreeItem<String> file = getSelectedItems().get(0);
		ExternalStressSequence sequence = null;
		if (file instanceof ExternalFlights) {
			sequence = ((ExternalFlights) file).getParentItem();
		}
		else {
			sequence = (ExternalStressSequence) file;
		}

		// initialize criteria
		int criteria = -1;

		// longest flight
		if (id.equals("selectExternalFlightLongest")) {
			criteria = SelectExternalFlight.LONGEST_FLIGHT;
		}
		else if (id.equals("selectExternalFlightShortest")) {
			criteria = SelectExternalFlight.SHORTEST_FLIGHT;
		}
		else if (id.equals("selectExternalFlightMaxVal")) {
			criteria = SelectExternalFlight.MAX_VALIDITY;
		}
		else if (id.equals("selectExternalFlightMinVal")) {
			criteria = SelectExternalFlight.MIN_VALIDITY;
		}
		else if (id.equals("selectExternalFlightMaxPeak")) {
			criteria = SelectExternalFlight.MAX_PEAK;
		}
		else if (id.equals("selectExternalFlightMinPeak")) {
			criteria = SelectExternalFlight.MIN_PEAK;
		}

		// select flight
		owner_.getActiveTasksPanel().runTaskInParallel(new SelectExternalFlight(sequence, criteria));
	}

	/**
	 * Saves A/C model to disk.
	 */
	private void saveModel() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get model
			AircraftModel file = (AircraftModel) selected.get(0);

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(FileType.ZIP.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.ZIP.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, FileType.ZIP);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveAircraftModel(file, output));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				AircraftModel file = (AircraftModel) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.ZIP.getExtension()).toFile();
				tm.runTaskInParallel(new SaveAircraftModel(file, output));
			}
		}
	}

	/**
	 * Saves A/C model groups to disk.
	 */
	private void saveModelGroups() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get conversion table
			AircraftModel file = (AircraftModel) selected.get(0);

			// get extension filters
			ExtensionFilter txtFilter = FileType.GRP.getExtensionFilter();
			ExtensionFilter zipFilter = FileType.ZIP.getExtensionFilter();
			ExtensionFilter gzipFilter = FileType.GZ.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(txtFilter, zipFilter, gzipFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.GRP.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get selected file type
			ExtensionFilter selFilter = fileChooser.getSelectedExtensionFilter();
			FileType fileType = FileType.GRP;
			if (selFilter.equals(zipFilter)) {
				fileType = FileType.ZIP;
			}
			else if (selFilter.equals(gzipFilter)) {
				fileType = FileType.GZ;
			}

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, fileType);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveAircraftModelFile(file, FileType.GRP, output));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get progress panel
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				AircraftModel file = (AircraftModel) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.GRP.getExtension()).toFile();
				tm.runTaskInParallel(new SaveAircraftModelFile(file, FileType.GRP, output));
			}
		}
	}

	/**
	 * Saves A/C model elements to disk.
	 */
	private void saveModelElements() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get conversion table
			AircraftModel file = (AircraftModel) selected.get(0);

			// get extension filters
			ExtensionFilter txtFilter = FileType.F06.getExtensionFilter();
			ExtensionFilter zipFilter = FileType.ZIP.getExtensionFilter();
			ExtensionFilter gzipFilter = FileType.GZ.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(txtFilter, zipFilter, gzipFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.F06.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get selected file type

			ExtensionFilter selFilter = fileChooser.getSelectedExtensionFilter();
			FileType fileType = FileType.F06;
			if (selFilter.equals(zipFilter)) {
				fileType = FileType.ZIP;
			}
			else if (selFilter.equals(gzipFilter)) {
				fileType = FileType.GZ;
			}

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, fileType);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveAircraftModelFile(file, FileType.F06, output));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get progress panel
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				AircraftModel file = (AircraftModel) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.F06.getExtension()).toFile();
				tm.runTaskInParallel(new SaveAircraftModelFile(file, FileType.F06, output));
			}
		}
	}

	/**
	 * Saves A/C model grids to disk.
	 */
	private void saveModelGrids() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get conversion table
			AircraftModel file = (AircraftModel) selected.get(0);

			// get extension filters
			ExtensionFilter txtFilter = FileType.F07.getExtensionFilter();
			ExtensionFilter zipFilter = FileType.ZIP.getExtensionFilter();
			ExtensionFilter gzipFilter = FileType.GZ.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(txtFilter, zipFilter, gzipFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.F07.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get selected file type
			ExtensionFilter selFilter = fileChooser.getSelectedExtensionFilter();
			FileType fileType = FileType.F07;
			if (selFilter.equals(zipFilter)) {
				fileType = FileType.ZIP;
			}
			else if (selFilter.equals(gzipFilter)) {
				fileType = FileType.GZ;
			}

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, fileType);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveAircraftModelFile(file, FileType.F07, output));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get progress panel
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				AircraftModel file = (AircraftModel) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.F07.getExtension()).toFile();
				tm.runTaskInParallel(new SaveAircraftModelFile(file, FileType.F07, output));
			}
		}
	}

	/**
	 * Saves spectrum to disk.
	 */
	private void saveSpectrum() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get CDF set
			Spectrum file = (Spectrum) selected.get(0);

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(FileType.SPEC.getExtensionFilter(), FileType.ZIP.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.SPEC.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveSpectrum(file, selectedFile));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				Spectrum file = (Spectrum) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.SPEC.getExtension()).toFile();
				tm.runTaskInParallel(new SaveSpectrum(file, output));
			}
		}
	}

	/**
	 * Saves ANA files to disk.
	 */
	private void saveANA() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get spectrum
			Spectrum file = (Spectrum) selected.get(0);

			// get extension filters
			ExtensionFilter anaFilter = FileType.ANA.getExtensionFilter();
			ExtensionFilter zipFilter = FileType.ZIP.getExtensionFilter();
			ExtensionFilter gzipFilter = FileType.GZ.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(anaFilter, zipFilter, gzipFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.ANA.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get selected file type
			ExtensionFilter selFilter = fileChooser.getSelectedExtensionFilter();
			FileType fileType = FileType.ANA;
			if (selFilter.equals(zipFilter)) {
				fileType = FileType.ZIP;
			}
			else if (selFilter.equals(gzipFilter)) {
				fileType = FileType.GZ;
			}

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, fileType);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveANA(file.getANAFileID(), output, fileType));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get progress panel
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				Spectrum file = (Spectrum) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.ANA.getExtension()).toFile();
				tm.runTaskInParallel(new SaveANA(file.getANAFileID(), output, FileType.ANA));
			}
		}
	}

	/**
	 * Saves TXT file to disk.
	 */
	private void saveTXT() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get conversion table
			Spectrum file = (Spectrum) selected.get(0);

			// get extension filters
			ExtensionFilter txtFilter = FileType.TXT.getExtensionFilter();
			ExtensionFilter zipFilter = FileType.ZIP.getExtensionFilter();
			ExtensionFilter gzipFilter = FileType.GZ.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(txtFilter, zipFilter, gzipFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.TXT.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get selected file type
			ExtensionFilter selFilter = fileChooser.getSelectedExtensionFilter();
			FileType fileType = FileType.TXT;
			if (selFilter.equals(zipFilter)) {
				fileType = FileType.ZIP;
			}
			else if (selFilter.equals(gzipFilter)) {
				fileType = FileType.GZ;
			}

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, fileType);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveTXT(file.getTXTFileID(), output, fileType));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get progress panel
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				Spectrum file = (Spectrum) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.TXT.getExtension()).toFile();
				tm.runTaskInParallel(new SaveTXT(file.getTXTFileID(), output, FileType.TXT));
			}
		}
	}

	/**
	 * Saves FLS files to disk.
	 */
	private void saveFLS() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get selected item
			SpectrumItem file = (SpectrumItem) selected.get(0);

			// get extension filters
			ExtensionFilter flsFilter = FileType.FLS.getExtensionFilter();
			ExtensionFilter zipFilter = FileType.ZIP.getExtensionFilter();
			ExtensionFilter gzipFilter = FileType.GZ.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(flsFilter, zipFilter, gzipFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.FLS.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get selected file type
			ExtensionFilter selFilter = fileChooser.getSelectedExtensionFilter();
			FileType fileType = FileType.FLS;
			if (selFilter.equals(zipFilter)) {
				fileType = FileType.ZIP;
			}
			else if (selFilter.equals(gzipFilter)) {
				fileType = FileType.GZ;
			}

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, fileType);

			// spectrum
			if (file instanceof Spectrum) {
				owner_.getActiveTasksPanel().runTaskInParallel(new SaveFLS(((Spectrum) file).getFLSFileID(), output, fileType));
			}
			else if (file instanceof ExternalStressSequence) {
				owner_.getActiveTasksPanel().runTaskInParallel(new SaveExternalFLS(((ExternalStressSequence) file).getID(), output, fileType));
			}
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				Spectrum file = (Spectrum) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.FLS.getExtension()).toFile();
				tm.runTaskInParallel(new SaveFLS(file.getFLSFileID(), output, FileType.FLS));
			}
		}
	}

	/**
	 * Saves CVT files to disk.
	 */
	private void saveCVT() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get CVT file
			Spectrum file = (Spectrum) selected.get(0);

			// get extension filters
			ExtensionFilter cvtFilter = FileType.CVT.getExtensionFilter();
			ExtensionFilter zipFilter = FileType.ZIP.getExtensionFilter();
			ExtensionFilter gzipFilter = FileType.GZ.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(cvtFilter, zipFilter, gzipFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.CVT.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get selected file type

			ExtensionFilter selFilter = fileChooser.getSelectedExtensionFilter();
			FileType fileType = FileType.CVT;
			if (selFilter.equals(zipFilter)) {
				fileType = FileType.ZIP;
			}
			else if (selFilter.equals(gzipFilter)) {
				fileType = FileType.GZ;
			}

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, fileType);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveCVT(file.getCVTFileID(), output, fileType));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				Spectrum file = (Spectrum) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.CVT.getExtension()).toFile();
				tm.runTaskInParallel(new SaveCVT(file.getCVTFileID(), output, FileType.CVT));
			}
		}
	}

	/**
	 * Saves conversion tables to disk.
	 */
	private void saveCONV() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get conversion table
			Spectrum file = (Spectrum) selected.get(0);

			// get extension filters
			ExtensionFilter xlsFilter = FileType.XLS.getExtensionFilter();
			ExtensionFilter zipFilter = FileType.ZIP.getExtensionFilter();
			ExtensionFilter gzipFilter = FileType.GZ.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(xlsFilter, zipFilter, gzipFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()) + FileType.XLS.getExtension());
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get selected file type
			ExtensionFilter selFilter = fileChooser.getSelectedExtensionFilter();
			FileType fileType = FileType.XLS;
			if (selFilter.equals(zipFilter)) {
				fileType = FileType.ZIP;
			}
			else if (selFilter.equals(gzipFilter)) {
				fileType = FileType.GZ;
			}

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, fileType);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveConversionTable(file.getConversionTableID(), output, fileType));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				Spectrum file = (Spectrum) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName()) + FileType.XLS.getExtension()).toFile();
				tm.runTaskInParallel(new SaveConversionTable(file.getConversionTableID(), output, FileType.XLS));
			}
		}
	}

	/**
	 * Saves STF file to disk.
	 */
	private void saveSTF() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get conversion table
			STFFile file = (STFFile) selected.get(0);

			// get extension filters
			ExtensionFilter stfFilter = FileType.STF.getExtensionFilter();
			ExtensionFilter zipFilter = FileType.ZIP.getExtensionFilter();
			ExtensionFilter gzipFilter = FileType.GZ.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(stfFilter, zipFilter, gzipFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()));
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get selected file type
			ExtensionFilter selFilter = fileChooser.getSelectedExtensionFilter();
			FileType fileType = FileType.STF;
			if (selFilter.equals(zipFilter)) {
				fileType = FileType.ZIP;
			}
			else if (selFilter.equals(gzipFilter)) {
				fileType = FileType.GZ;
			}

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, fileType);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveSTF(file, output, fileType));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				STFFile file = (STFFile) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName())).toFile();
				tm.runTaskInParallel(new SaveSTF(file, output, FileType.STF));
			}
		}
	}

	/**
	 * Saves STF files to disk.
	 */
	private void saveSTFBucket() {

		// get directory chooser
		DirectoryChooser dirChooser = owner_.getDirectoryChooser();

		// show save dialog
		File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

		// no directory selected
		if (selectedDir == null)
			return;

		// set initial directory
		owner_.setInitialDirectory(selectedDir);

		// get task manager
		ActiveTasksPanel tm = owner_.getActiveTasksPanel();

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// add tasks
		for (TreeItem<String> item : selected) {
			tm.runTaskInParallel(new SaveSTFBucket((STFFileBucket) item, selectedDir.toPath()));
		}
	}

	/**
	 * Saves load case to disk.
	 */
	private void saveLoadCase() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get load case
			AircraftLoadCase file = (AircraftLoadCase) selected.get(0);

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(FileType.LCS.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()));
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new SaveLoadCase(file, FileType.appendExtension(selectedFile, FileType.LCS)));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {

				// set output file
				AircraftLoadCase file = (AircraftLoadCase) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName())).toFile();
				output = FileType.appendExtension(output, FileType.LCS);

				// save
				tm.runTaskInParallel(new SaveLoadCase(file, output));
			}
		}
	}

	/**
	 * Saves A/C equivalent stresses to disk.
	 */
	private void saveAircraftEquivalentStress() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get equivalent stress
			AircraftFatigueEquivalentStress file = (AircraftFatigueEquivalentStress) selected.get(0);

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(FileType.XLS.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()));
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();
			tm.runTaskInParallel(new SaveAircraftEquivalentStress(file, FileType.appendExtension(selectedFile, FileType.XLS)));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {

				// set output file
				AircraftFatigueEquivalentStress file = (AircraftFatigueEquivalentStress) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName())).toFile();
				output = FileType.appendExtension(output, FileType.XLS);

				// save
				tm.runTaskInParallel(new SaveAircraftEquivalentStress(file, output));
			}
		}
	}

	/**
	 * Saves stress sequence to disk.
	 */
	private void saveStressSequence() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get conversion table
			SpectrumItem file = (SpectrumItem) selected.get(0);

			// get extension filters
			ExtensionFilter sthFilter = FileType.STH.getExtensionFilter();
			ExtensionFilter sigmaFilter = FileType.SIGMA.getExtensionFilter();

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(sthFilter, sigmaFilter);

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()));
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add task
			ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();

			// STH
			if (selectedFilter.equals(sthFilter)) {

				// stress sequence
				if (file instanceof StressSequence) {
					tm.runTaskInParallel(new SaveStressSequenceAsSTH((StressSequence) file, FileType.appendExtension(selectedFile, FileType.STH)));
				}
				else if (file instanceof ExternalStressSequence) {
					tm.runTaskInParallel(new SaveExternalStressSequenceAsSTH((ExternalStressSequence) file, FileType.appendExtension(selectedFile, FileType.STH)));
				}
			}

			// SIGMA
			else if (selectedFilter.equals(sigmaFilter))
				// stress sequence
				if (file instanceof StressSequence) {
					tm.runTaskInParallel(new SaveStressSequenceAsSIGMA((StressSequence) file, FileType.appendExtension(selectedFile, FileType.SIGMA)));
				}
				else if (file instanceof ExternalStressSequence) {
					tm.runTaskInParallel(new SaveExternalStressSequenceAsSIGMA((ExternalStressSequence) file, FileType.appendExtension(selectedFile, FileType.SIGMA)));
				}
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get task manager
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {

				// set output file
				SpectrumItem file = (SpectrumItem) item;
				File output = selectedDir.toPath().resolve(Utility.correctFileName(file.getName())).toFile();
				output = FileType.appendExtension(output, FileType.STH);

				// stress sequence
				if (file instanceof StressSequence) {
					tm.runTaskInParallel(new SaveStressSequenceAsSTH((StressSequence) file, output));
				}
				else if (file instanceof ExternalStressSequence) {
					tm.runTaskInParallel(new SaveExternalStressSequenceAsSTH((ExternalStressSequence) file, output));
				}
			}
		}
	}

	/**
	 * Saves the rainflow cycles counts to disk.
	 */
	private void saveRainflow() {

		// get selected items
		ObservableList<TreeItem<String>> selected = getSelectedItems();

		// single file
		if (selected.size() == 1) {

			// get equivalent stress
			SpectrumItem file = (SpectrumItem) selected.get(0);

			// get file chooser
			FileChooser fileChooser = owner_.getFileChooser(FileType.RFLOW.getExtensionFilter());

			// show save dialog
			fileChooser.setInitialFileName(Utility.correctFileName(file.getName()));
			File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getStage());

			// no file selected
			if (selectedFile == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedFile);

			// append extension if necessary
			File output = FileType.appendExtension(selectedFile, FileType.RFLOW);

			// save file
			owner_.getActiveTasksPanel().runTaskInParallel(new SaveRainflow(file, output));
		}

		// multiple files
		else {

			// get directory chooser
			DirectoryChooser dirChooser = owner_.getDirectoryChooser();

			// show save dialog
			File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

			// no directory selected
			if (selectedDir == null)
				return;

			// set initial directory
			owner_.setInitialDirectory(selectedDir);

			// get progress panel
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// add tasks
			for (TreeItem<String> item : selected) {
				SpectrumItem file = (SpectrumItem) item;
				String fileName = FileType.appendExtension(Utility.correctFileName(file.getName()), FileType.RFLOW);
				File output = selectedDir.toPath().resolve(fileName).toFile();
				tm.runTaskInParallel(new SaveRainflow(file, output));
			}
		}
	}

	/**
	 * Creates element groups from GRP files.
	 */
	private void createElementGroupsFromFile() {

		// get selected item
		AircraftModel model = (AircraftModel) getSelectedItems().get(0);

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.GRP.getExtensionFilter(), FileType.ZIP.getExtensionFilter(), FileType.GZ.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// get progress panel
		ActiveTasksPanel tm = owner_.getActiveTasksPanel();

		// add tasks
		for (File file : files) {
			tm.runTaskInParallel(new CreateElementGroupsFromFile(model, file.toPath()));
		}
	}

	/**
	 * Adds element stresses to A/C model.
	 */
	private void addLoadCases() {

		// get selected item
		SpectrumItem item = (SpectrumItem) getSelectedItems().get(0);
		AircraftLoadCases folder = null;
		if (item instanceof AircraftLoadCases) {
			folder = (AircraftLoadCases) item;
		}
		else if (item instanceof AircraftModel) {
			folder = ((AircraftModel) item).getLoadCases();
		}

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.F07.getExtensionFilter(), FileType.LCS.getExtensionFilter(), FileType.ZIP.getExtensionFilter(), FileType.GZ.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// create tasks
		AddAircraftLoadCases[] tasks = new AddAircraftLoadCases[files.size()];
		for (int i = 0; i < files.size(); i++) {
			tasks[i] = new AddAircraftLoadCases(files.get(i).toPath(), folder);
		}

		// execute tasks sequentially
		ActiveTasksPanel tm = owner_.getActiveTasksPanel();
		tm.runTasksSequentially(tasks);
	}

	/**
	 * Adds STF files to this CDF set.
	 */
	private void addSTFFiles() {

		// get selected item
		Spectrum cdfSet = (Spectrum) getSelectedItems().get(0);

		// get file chooser
		FileChooser fileChooser = owner_.getFileChooser(FileType.STF.getExtensionFilter(), FileType.GZ.getExtensionFilter());

		// show open dialog
		List<File> files = fileChooser.showOpenMultipleDialog(owner_.getOwner().getStage());

		// no file selected
		if (files == null || files.isEmpty())
			return;

		// set initial directory
		owner_.setInitialDirectory(files.get(0));

		// run task
		ActiveTasksPanel tm = owner_.getActiveTasksPanel();
		tm.runTaskSequentially(new AddSTFFiles(files, cdfSet, null));
	}

	/**
	 * Adds STF files from selected directory.
	 */
	private void addSTFFilesFromDirectory() {

		// get selected spectrum
		Spectrum spectrum = (Spectrum) getSelectedItems().get(0);

		// get directory chooser
		DirectoryChooser dirChooser = owner_.getDirectoryChooser();

		// show dialog
		File selectedDir = dirChooser.showDialog(owner_.getOwner().getStage());

		// no directory selected
		if (selectedDir == null || !selectedDir.exists())
			return;

		// set initial directory
		owner_.setInitialDirectory(selectedDir);

		// get progress panel
		ActiveTasksPanel tm = owner_.getActiveTasksPanel();

		// run task
		tm.runTaskSequentially(new AddSTFFiles(selectedDir.toPath(), spectrum));
	}

	/**
	 * Renames selected file.
	 */
	private void rename() {

		// create popup
		ObservableList<TreeItem<String>> selected = getSelectedItems();
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(RenamePanel.load(popOver, owner_.getInputPanel(), selected.size() == 1 ? ((SpectrumItem) selected.get(0)).getName() : null));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);

		// show popup
		FileViewPanel panel = (FileViewPanel) owner_.getInputPanel().getSubPanel(InputPanel.FILE_VIEW_PANEL);
		panel.showPopOverOnSelectedItem(popOver, "Rename File");
	}

	/**
	 * Edits load case comments.
	 */
	private void commentLoadCase() {

		// create popup
		AircraftLoadCase selected = (AircraftLoadCase) getSelectedItems().get(0);
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(CommentLoadCasePanel.load(popOver, owner_.getInputPanel(), selected));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);

		// show popup
		FileViewPanel panel = (FileViewPanel) owner_.getInputPanel().getSubPanel(InputPanel.FILE_VIEW_PANEL);
		panel.showPopOverOnSelectedItem(popOver, "Edit Load Case Comment");
	}

	/**
	 * Overrides STF file mission.
	 */
	private void overrideMission() {

		// create popup
		STFFile selected = (STFFile) getSelectedItems().get(0);
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(OverrideSTFMissionPanel.load(popOver, owner_.getInputPanel(), selected));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);

		// show popup
		FileViewPanel panel = (FileViewPanel) owner_.getInputPanel().getSubPanel(InputPanel.FILE_VIEW_PANEL);
		panel.showPopOverOnSelectedItem(popOver, "Ovveride Mission");
	}

	/**
	 * Creates element group from selected spectrum item.
	 */
	private void createElementGroupFromItem() {

		// create popup
		SpectrumItem selected = (SpectrumItem) getSelectedItems().get(0);
		PopOver popOver = new PopOver();
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(CreateElementGroupFromItemPanel.load(popOver, owner_.getInputPanel(), selected.getName()));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);

		// show popup
		FileViewPanel panel = (FileViewPanel) owner_.getInputPanel().getSubPanel(InputPanel.FILE_VIEW_PANEL);
		panel.showPopOverOnSelectedItem(popOver, "Create Element Group");
	}

	/**
	 * Searches for similar items.
	 */
	private void find() {
		ObservableList<TreeItem<String>> selected = getSelectedItems();
		if (selected.isEmpty())
			return;
		SpectrumItem[] items = new SpectrumItem[selected.size()];
		for (int i = 0; i < items.length; i++) {
			items[i] = (SpectrumItem) selected.get(i);
		}
		owner_.getInputPanel().search(items);
	}

	/**
	 * Delete files.
	 */
	private void delete() {

		// create confirmation action
		PopOver popOver = new PopOver();
		EventHandler<ActionEvent> handler = event -> {

			// get selected items
			ObservableList<TreeItem<String>> selected = getSelectedItems();

			// get progress panel
			ActiveTasksPanel tm = owner_.getActiveTasksPanel();

			// create task
			tm.runTaskInParallel(new DeleteFiles<>(selected));

			// hide pop-over
			popOver.hide();
		};

		// show message
		String message = "Are you sure you want to delete selected files from the database?";
		popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel2.load(popOver, message, 50, "Delete", handler, NotificationPanel2.QUESTION));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);

		// show popup
		FileViewPanel panel = (FileViewPanel) owner_.getInputPanel().getSubPanel(InputPanel.FILE_VIEW_PANEL);
		panel.showPopOverOnSelectedItem(popOver, "Delete File");
	}

	/**
	 * Hides files.
	 */
	private void hide() {

		// run later
		Platform.runLater(() -> owner_.getInputPanel().getFileTreeRoot().getChildren().removeAll(getSelectedItems()));
	}

	/**
	 * Plots selected typical flights.
	 */
	private void plotTypicalFlight() {

		// get selected flights
		ObservableList<TreeItem<String>> selected = getSelectedItems();
		Flight[] flights = new Flight[selected.size()];
		for (int i = 0; i < selected.size(); i++) {
			flights[i] = (Flight) selected.get(i);
		}

		// plot flights
		((PlotViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.PLOT_VIEW)).plot(flights);
	}

	/**
	 * Plots selected external typical flights.
	 */
	private void plotExternalTypicalFlight() {

		// get selected flights
		ObservableList<TreeItem<String>> selected = getSelectedItems();
		ExternalFlight[] flights = new ExternalFlight[selected.size()];
		for (int i = 0; i < selected.size(); i++) {
			flights[i] = (ExternalFlight) selected.get(i);
		}

		// plot flights
		((ExternalPlotViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.EXTERNAL_PLOT_VIEW)).plot(flights);
	}

	/**
	 * Shares selected file.
	 *
	 * @param fileType
	 *            Type of file to share. Can be null.
	 * @param params
	 *            Additional parameters.
	 */
	private void share(FileType fileType, String... params) {
		SpectrumItem selected = (SpectrumItem) getSelectedItems().get(0);
		((ShareFilePopup) owner_.getInputPanel().getPopup(InputPanel.SHARE_FILE_POPUP)).show(selected, fileType, params);
	}
}
