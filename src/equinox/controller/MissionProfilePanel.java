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
import java.util.ResourceBundle;

import org.controlsfx.control.ToggleSwitch;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleAnchor;

import equinox.controller.InputPanel.InternalInputSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.fileType.StressSequence;
import equinox.task.GetMissionProfilePeakInfo;
import equinox.utility.Utility;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for mission profile panel controller.
 *
 * @author Murat Artim
 * @date Jun 8, 2016
 * @time 5:18:59 PM
 */
public class MissionProfilePanel implements InternalInputSubPanel {

	/** The owner panel. */
	private InputPanel owner_;

	/** Mode of this panel. */
	private boolean isComparison_ = false;

	/** Peak info array. */
	private RadioButton[] peakInfo_;

	@FXML
	private VBox root_;

	@FXML
	private RadioButton totalStress_, dpPressure_, dtTemperature_, classCode_, dpStress_, dtStress_, onegEvent_, onegIssy_, onegStress_, onegComment_, incEvent_, incIssy_, incFac_, incStress_, incComment_, incLinear_, segment_, flightName_;

	@FXML
	private ToggleSwitch steadyStress_, dp_, oneg_, dt_, posInc_, negInc_, step1_, step2_, step3_, step4_, step5_, step6_, step7_, step8_, showCrosshair_, showInfo_;

	@FXML
	private Accordion accordion_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// setup peak info array
		peakInfo_ = new RadioButton[18];
		peakInfo_[GetMissionProfilePeakInfo.TOTAL_STRESS] = totalStress_;
		peakInfo_[GetMissionProfilePeakInfo.CLASS_CODE] = classCode_;
		peakInfo_[GetMissionProfilePeakInfo.ONE_G_FLIGHT_PHASE] = onegEvent_;
		peakInfo_[GetMissionProfilePeakInfo.ONE_G_ISSY_CODE] = onegIssy_;
		peakInfo_[GetMissionProfilePeakInfo.ONE_G_STRESS] = onegStress_;
		peakInfo_[GetMissionProfilePeakInfo.ONE_G_COMMENT] = onegComment_;
		peakInfo_[GetMissionProfilePeakInfo.INCREMENT_FLIGHT_PHASE] = incEvent_;
		peakInfo_[GetMissionProfilePeakInfo.INCREMENT_ISSY_CODE] = incIssy_;
		peakInfo_[GetMissionProfilePeakInfo.INCREMENT_FACTOR] = incFac_;
		peakInfo_[GetMissionProfilePeakInfo.INCREMENT_STRESS] = incStress_;
		peakInfo_[GetMissionProfilePeakInfo.INCREMENT_COMMENT] = incComment_;
		peakInfo_[GetMissionProfilePeakInfo.DELTA_P_PRESSURE] = dpPressure_;
		peakInfo_[GetMissionProfilePeakInfo.DELTA_P_STRESS] = dpStress_;
		peakInfo_[GetMissionProfilePeakInfo.LINEARITY] = incLinear_;
		peakInfo_[GetMissionProfilePeakInfo.SEGMENT] = segment_;
		peakInfo_[GetMissionProfilePeakInfo.FLIGHT_NAME] = flightName_;
		peakInfo_[GetMissionProfilePeakInfo.DELTA_T_TEMPERATURE] = dtTemperature_;
		peakInfo_[GetMissionProfilePeakInfo.DELTA_T_STRESS] = dtStress_;

		// set listeners
		steadyStress_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onSteadyStressSelected();
			}
		});
		oneg_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				on1gStressSelected();
			}
		});
		dp_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onDpStressSelected();
			}
		});
		dt_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onDtStressSelected();
			}
		});
		posInc_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		negInc_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		step1_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		step2_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		step3_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		step4_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		step5_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		step6_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		step7_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		step8_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				plot();
			}
		});
		showCrosshair_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onShowCrosshairSelected();
			}
		});
		showInfo_.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				onShowInfoSelected();
			}
		});

		// expand first tab
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@Override
	public InputPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {

		// expand first tab
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	@Override
	public String getHeader() {
		return "Plot Mission Profile";
	}

	/**
	 * Sets mode of this panel.
	 *
	 * @param isComparison
	 *            True if profile comparison mode.
	 */
	public void setMode(boolean isComparison) {
		isComparison_ = isComparison;
	}

	/**
	 * Plots mission profile.
	 */
	public void plot() {

		// get selected sequence
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();

		// increment stress plot options
		boolean plotPosInc = posInc_.isSelected();
		boolean plotNegInc = negInc_.isSelected();

		// get step plot options
		boolean[] plotStep = { step1_.isSelected(), step2_.isSelected(), step3_.isSelected(), step4_.isSelected(), step5_.isSelected(), step6_.isSelected(), step7_.isSelected(), step8_.isSelected() };

		// single
		if (selected.size() == 1) {

			// get selected stress sequence
			StressSequence sequence = (StressSequence) selected.get(0);

			// plot
			MissionProfileViewPanel panel = (MissionProfileViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_VIEW);
			panel.plot(sequence, plotPosInc, plotNegInc, plotStep);
		}

		// comparison
		else if (selected.size() == 2) {

			// get selected stress sequence
			StressSequence sequence1 = (StressSequence) selected.get(0);
			StressSequence sequence2 = (StressSequence) selected.get(1);

			// plot
			MissionProfileComparisonViewPanel panel = (MissionProfileComparisonViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_COMPARISON_VIEW);
			panel.plot(sequence1, sequence2, plotPosInc, plotNegInc, plotStep);
		}
	}

	/**
	 * Called when plotting completed.
	 *
	 * @param plot
	 *            Plot.
	 */
	public void plottingCompleted(XYPlot plot) {
		onSteadyStressSelected();
		on1gStressSelected();
		onDpStressSelected();
		onDtStressSelected();
	}

	/**
	 * Returns selected peak info.
	 *
	 * @return Selected peak info.
	 */
	public int getSelectedPeakInfo() {
		for (int i = 0; i < peakInfo_.length; i++) {
			if (peakInfo_[i].isSelected())
				return i;
		}
		return 0;
	}

	@FXML
	private void onCancelClicked() {
		owner_.showSubPanel(InputPanel.FILE_VIEW_PANEL);
	}

	@FXML
	private void onHelpClicked() {
		owner_.getOwner().showHelp("How to plot mission profile", null);
	}

	@FXML
	private void onResetClicked() {

		// reset peak information
		if (!totalStress_.isSelected()) {
			totalStress_.setSelected(true);
		}

		// reset steady stresses
		if (!steadyStress_.isSelected()) {
			steadyStress_.setSelected(true);
		}
		if (oneg_.isSelected()) {
			oneg_.setSelected(false);
		}
		if (dp_.isSelected()) {
			dp_.setSelected(false);
		}
		if (dt_.isSelected()) {
			dt_.setSelected(false);
		}

		// reset options
		if (!showCrosshair_.isSelected()) {
			showCrosshair_.setSelected(true);
		}
		if (!showInfo_.isSelected()) {
			showInfo_.setSelected(true);
		}

		// reset incremental stresses
		if (!posInc_.isSelected()) {
			posInc_.setSelected(true);
		}
		if (!negInc_.isSelected()) {
			negInc_.setSelected(true);
		}

		// reset incremental steps
		if (!step1_.isSelected()) {
			step1_.setSelected(true);
		}
		if (!step2_.isSelected()) {
			step2_.setSelected(true);
		}
		if (!step3_.isSelected()) {
			step3_.setSelected(true);
		}
		if (!step4_.isSelected()) {
			step4_.setSelected(true);
		}
		if (!step5_.isSelected()) {
			step5_.setSelected(true);
		}
		if (!step6_.isSelected()) {
			step6_.setSelected(true);
		}
		if (!step7_.isSelected()) {
			step7_.setSelected(true);
		}
		if (!step8_.isSelected()) {
			step8_.setSelected(true);
		}

		// expand first tab
		accordion_.setExpandedPane(accordion_.getPanes().get(0));
	}

	/**
	 * Called when show crosshair option is selected.
	 */
	private void onShowCrosshairSelected() {
		JFreeChart[] charts = getCharts();
		for (JFreeChart chart : charts) {
			XYPlot plot = chart.getXYPlot();
			plot.setDomainCrosshairVisible(showCrosshair_.isSelected());
			plot.setRangeCrosshairVisible(showCrosshair_.isSelected());
		}
	}

	/**
	 * Called when show info option is selected.
	 */
	private void onShowInfoSelected() {

		// get charts
		JFreeChart[] charts = getCharts();

		// initialize info
		TextTitle[] info = null;

		// single profile plot
		if (!isComparison_) {
			MissionProfileViewPanel panel = (MissionProfileViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_VIEW);
			info = panel.getInfo();
		}

		// comparison plot
		else {
			MissionProfileComparisonViewPanel panel = (MissionProfileComparisonViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_COMPARISON_VIEW);
			info = panel.getInfo();
		}

		// loop over charts
		for (int i = 0; i < charts.length; i++) {

			// get plot
			XYPlot plot = charts[i].getXYPlot();

			// clear annotations
			if (!showInfo_.isSelected()) {
				plot.clearAnnotations();
			}
			else {
				if (plot.getAnnotations().isEmpty()) {
					XYTitleAnnotation ta = new XYTitleAnnotation(0.98, 0.02, info[i], RectangleAnchor.BOTTOM_RIGHT);
					ta.setMaxWidth(0.48);
					plot.addAnnotation(ta);
				}
			}
		}
	}

	/**
	 * Called when steady stress option is selected.
	 */
	private void onSteadyStressSelected() {
		boolean visible = steadyStress_.isSelected();
		JFreeChart[] charts = getCharts();
		for (JFreeChart chart : charts) {
			XYPlot plot = chart.getXYPlot();
			if (plot.getRenderer(1).isSeriesVisible(0) != visible) {
				plot.getRenderer(1).setSeriesVisible(0, visible);
			}
		}
	}

	/**
	 * Called when 1g stress option is selected.
	 */
	private void on1gStressSelected() {
		boolean visible = oneg_.isSelected();
		JFreeChart[] charts = getCharts();
		for (JFreeChart chart : charts) {
			XYPlot plot = chart.getXYPlot();
			if (plot.getRenderer(1).isSeriesVisible(1) != visible) {
				plot.getRenderer(1).setSeriesVisible(1, visible);
			}
		}
	}

	/**
	 * Called when delta-p stress option is selected.
	 */
	private void onDpStressSelected() {
		boolean visible = dp_.isSelected();
		JFreeChart[] charts = getCharts();
		for (JFreeChart chart : charts) {
			XYPlot plot = chart.getXYPlot();
			if (plot.getRenderer(1).isSeriesVisible(2) != visible) {
				plot.getRenderer(1).setSeriesVisible(2, visible);
			}
		}
	}

	/**
	 * Called when delta-t stress option is selected.
	 */
	private void onDtStressSelected() {
		boolean visible = dt_.isSelected();
		JFreeChart[] charts = getCharts();
		for (JFreeChart chart : charts) {
			XYPlot plot = chart.getXYPlot();
			if (plot.getRenderer(1).isSeriesVisible(3) != visible) {
				plot.getRenderer(1).setSeriesVisible(3, visible);
			}
		}
	}

	@FXML
	private void onPeakInfoSelected() {

		// single profile mode
		if (!isComparison_) {
			MissionProfileViewPanel panel = (MissionProfileViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_VIEW);
			panel.updatePeakInfo();
		}

		// comparison mode
		else {
			MissionProfileComparisonViewPanel panel = (MissionProfileComparisonViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_COMPARISON_VIEW);
			panel.updatePeakInfo();
		}
	}

	/**
	 * Returns the charts.
	 *
	 * @return The charts.
	 */
	private JFreeChart[] getCharts() {

		// single profile mode
		if (!isComparison_) {
			MissionProfileViewPanel panel = (MissionProfileViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_VIEW);
			return panel.getChart();
		}

		// comparison mode
		MissionProfileComparisonViewPanel panel = (MissionProfileComparisonViewPanel) owner_.getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_COMPARISON_VIEW);
		return panel.getChart();
	}

	/**
	 * Loads and returns generate STH panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded generate STH panel.
	 */
	public static MissionProfilePanel load(InputPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("MissionProfilePanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			MissionProfilePanel controller = (MissionProfilePanel) fxmlLoader.getController();

			// set owner
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
