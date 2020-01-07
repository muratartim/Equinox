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

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.Segment;
import equinox.data.fileType.StressSequence;
import equinox.plugin.FileType;
import equinox.task.GetMissionProfilePeakInfo;
import equinox.task.PlotMissionProfile;
import equinox.task.SaveImage;
import equinox.utility.CrosshairListenerXYPlot;
import equinox.utility.CrosshairListenerXYPlot.CrosshairListener;
import equinox.utility.Utility;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for mission profile comparison panel controller.
 *
 * @author Murat Artim
 * @date May 20, 2015
 * @time 12:10:13 PM
 */
public class MissionProfileComparisonViewPanel implements InternalViewSubPanel {

	/** Plot factors. */
	public static final double RANGE_FACTOR = 1.1;

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart. */
	private JFreeChart[] chart_;

	/** Segment markers. */
	private ArrayList<IntervalMarker> segmentMarkers1_, segmentMarkers2_;

	/** Crosshair information. */
	private TextTitle[] info_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	/** Crosshair coordinates. */
	private double crosshairX1_ = 0.0, crosshairY1_ = 0.0, crosshairX2_ = 0.0, crosshairY2_ = 0.0;

	/** Stress sequences to plot. */
	private StressSequence sequence1_, sequence2_;

	/** Plot completion indicator. */
	private int plottingCompleted_ = 0;

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container1_, container2_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create chart-1
		chart_ = new JFreeChart[2];
		chart_[0] = CrosshairListenerXYPlot.createMissionProfileChart("Mission Profile Comparison", "Sequence-1", "Stress", null, PlotOrientation.VERTICAL, false, false, false, new Chart1CrosshairListener());
		chart_[0].setBackgroundPaint(new Color(245, 245, 245));
		chart_[0].setAntiAlias(true);
		chart_[0].setTextAntiAlias(true);

		// create chart-2
		chart_[1] = CrosshairListenerXYPlot.createMissionProfileChart(null, "Sequence-2", "Stress", null, PlotOrientation.VERTICAL, true, false, false, new Chart2CrosshairListener());
		chart_[1].setBackgroundPaint(new Color(245, 245, 245));
		chart_[1].setAntiAlias(true);
		chart_[1].setTextAntiAlias(true);

		// setup plot-1
		XYPlot plot1 = chart_[0].getXYPlot();
		plot1.setOutlinePaint(Color.lightGray);
		plot1.setBackgroundPaint(null);
		plot1.setDomainGridlinePaint(new Color(245, 245, 245, 0));
		plot1.setRangeGridlinePaint(Color.lightGray);
		plot1.getDomainAxis().setTickLabelsVisible(false);
		plot1.getDomainAxis().setTickMarksVisible(false);
		plot1.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot1.setDomainCrosshairVisible(true);
		plot1.setRangeCrosshairVisible(true);
		plot1.getDomainAxis().addChangeListener(new Chart1AxisListener());
		plot1.setDomainPannable(true);
		plot1.setRangePannable(true);

		// setup plot-2
		XYPlot plot2 = chart_[1].getXYPlot();
		plot2.setOutlinePaint(Color.lightGray);
		plot2.setBackgroundPaint(null);
		plot2.setDomainGridlinePaint(new Color(245, 245, 245, 0));
		plot2.setRangeGridlinePaint(Color.lightGray);
		plot2.getDomainAxis().setTickLabelsVisible(false);
		plot2.getDomainAxis().setTickMarksVisible(false);
		plot2.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot2.setDomainCrosshairVisible(true);
		plot2.setRangeCrosshairVisible(true);
		plot2.getDomainAxis().addChangeListener(new Chart2AxisListener());
		plot2.setDomainPannable(true);
		plot2.setRangePannable(true);

		// create segment markers
		segmentMarkers1_ = new ArrayList<>();
		segmentMarkers2_ = new ArrayList<>();

		// setup crosshairs for plot-1
		BasicStroke defdom1 = (BasicStroke) plot1.getDomainCrosshairStroke();
		plot1.setDomainCrosshairStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, defdom1.getMiterLimit(), defdom1.getDashArray(), defdom1.getDashPhase()));
		plot1.setRangeCrosshairStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, defdom1.getMiterLimit(), defdom1.getDashArray(), defdom1.getDashPhase()));
		plot1.setDomainCrosshairPaint(new Color(70, 130, 180));
		plot1.setRangeCrosshairPaint(new Color(70, 130, 180));

		// setup crosshairs for plot-2
		BasicStroke defdom2 = (BasicStroke) plot2.getDomainCrosshairStroke();
		plot2.setDomainCrosshairStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, defdom2.getMiterLimit(), defdom2.getDashArray(), defdom2.getDashPhase()));
		plot2.setRangeCrosshairStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, defdom2.getMiterLimit(), defdom2.getDashArray(), defdom2.getDashPhase()));
		plot2.setDomainCrosshairPaint(new Color(70, 130, 180));
		plot2.setRangeCrosshairPaint(new Color(70, 130, 180));

		// create information box-1
		info_ = new TextTitle[2];
		info_[0] = new TextTitle("Select point to see info.");
		info_[0].setBackgroundPaint(new Color(70, 130, 180));
		info_[0].setPaint(Color.white);
		info_[0].setFrame(new BlockBorder(Color.lightGray));
		info_[0].setPosition(RectangleEdge.BOTTOM);
		XYTitleAnnotation ta1 = new XYTitleAnnotation(0.98, 0.02, info_[0], RectangleAnchor.BOTTOM_RIGHT);
		ta1.setMaxWidth(0.48);
		plot1.addAnnotation(ta1);

		// create information box-1
		info_[1] = new TextTitle("Select point to see info.");
		info_[1].setBackgroundPaint(new Color(70, 130, 180));
		info_[1].setPaint(Color.white);
		info_[1].setFrame(new BlockBorder(Color.lightGray));
		info_[1].setPosition(RectangleEdge.BOTTOM);
		XYTitleAnnotation ta2 = new XYTitleAnnotation(0.98, 0.02, info_[1], RectangleAnchor.BOTTOM_RIGHT);
		ta2.setMaxWidth(0.48);
		plot2.addAnnotation(ta2);

		// create swing node content
		SwingUtilities.invokeLater(() -> {

			// create and set chart panel-1
			ChartPanel panel1 = new ChartPanel(chart_[0]);
			panel1.setPopupMenu(null);
			panel1.setMouseWheelEnabled(true);
			container1_.setContent(panel1);

			// create and set chart panel-2
			ChartPanel panel2 = new ChartPanel(chart_[1]);
			panel2.setPopupMenu(null);
			panel2.setMouseWheelEnabled(true);
			container2_.setContent(panel2);
		});
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public HBox getControls() {
		return null;
	}

	@Override
	public String getHeader() {
		return "Profile Comparison View";
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public String getViewName() {
		return getHeader();
	}

	@Override
	public WritableImage getViewImage() {
		return root_.snapshot(null, null);
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public void hiding() {
		// no implementation
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get chart title
		String title = getHeader();

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName(title + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = root_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	/**
	 * Returns the chart of this panel.
	 *
	 * @return The chart of this panel.
	 */
	public JFreeChart[] getChart() {
		return chart_;
	}

	/**
	 * Returns info title.
	 *
	 * @return Info title.
	 */
	public TextTitle[] getInfo() {
		return info_;
	}

	/**
	 * Plots mission profile for the given stress sequence.
	 *
	 * @param sequence1
	 *            Stress sequence-1.
	 * @param sequence2
	 *            Stress sequence-2.
	 * @param plotPosInc
	 *            True to plot positive increments.
	 * @param plotNegInc
	 *            True to plot negative increments.
	 * @param plotStep
	 *            Step plotting options. Array size must be 8 (for 8 steps). Null can be given for plotting all steps.
	 */
	public void plot(StressSequence sequence1, StressSequence sequence2, boolean plotPosInc, boolean plotNegInc, boolean[] plotStep) {
		sequence1_ = sequence1;
		sequence2_ = sequence2;
		plottingCompleted_ = 0;
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new PlotMissionProfile(sequence1_, plotPosInc, plotNegInc, plotStep, true));
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new PlotMissionProfile(sequence2_, plotPosInc, plotNegInc, plotStep, true));
	}

	/**
	 * Called when plotting process has ended.
	 *
	 * @param incrementDataset
	 *            Dataset which contains increment stresses as intervals.
	 * @param steadyDataset
	 *            Dataset which contains steady stresses.
	 * @param incrementPointsDataset
	 *            Dataset which contains increment stresses as points.
	 * @param segments
	 *            List of segments.
	 * @param maxDiff
	 *            Maximum difference in plot to adjust Y axis.
	 * @param title
	 *            Chart title.
	 * @param sequence
	 *            Stress sequence.
	 */
	public void plottingCompleted(IntervalXYDataset incrementDataset, XYDataset steadyDataset, XYDataset incrementPointsDataset, ArrayList<Segment> segments, double maxDiff, String title, StressSequence sequence) {

		// initialize plot
		XYPlot plot = null;

		// first sequence
		if (sequence.getID() == sequence1_.getID()) {

			// set dataset
			plot = chart_[0].getXYPlot();
			plot.setDataset(incrementDataset);
			plot.setDataset(1, steadyDataset);
			plot.setDataset(2, incrementPointsDataset);

			// set title
			plot.getDomainAxis().setLabel(title);

			// set colors
			for (int i = 0; i < incrementDataset.getSeriesCount(); i++) {
				String seriesName = (String) incrementDataset.getSeriesKey(i);
				if (seriesName.equals("Positive Increments")) {
					plot.getRenderer().setSeriesPaint(i, MissionProfileViewPanel.POSITIVE_INCREMENTS);
				}
				else if (seriesName.equals("Negative Increments")) {
					plot.getRenderer().setSeriesPaint(i, MissionProfileViewPanel.NEGATIVE_INCREMENTS);
				}
			}
			plot.getRenderer(1).setSeriesPaint(0, Color.black);
			plot.getRenderer(1).setSeriesPaint(1, MissionProfileViewPanel.ONEG);
			plot.getRenderer(1).setSeriesPaint(2, MissionProfileViewPanel.DELTA_P);
			plot.getRenderer(1).setSeriesPaint(3, MissionProfileViewPanel.DELTA_T);

			// clear current markers
			plot.clearDomainMarkers();
			segmentMarkers1_.clear();

			// create and add segment markers
			for (int i = 0; i < segments.size(); i++) {
				IntervalMarker marker = new IntervalMarker(i, i + 1);
				marker.setLabelOffsetType(LengthAdjustmentType.EXPAND);
				marker.setPaint(MissionProfileViewPanel.SEGMENT_FILL);
				marker.setLabel(segments.get(i).toString());
				marker.setLabelFont(MissionProfileViewPanel.SEGMENT_FONT);
				marker.setOutlinePaint(MissionProfileViewPanel.SEGMENT_OUTLINE);
				marker.setLabelAnchor(RectangleAnchor.BOTTOM);
				marker.setLabelTextAnchor(TextAnchor.BOTTOM_CENTER);
				segmentMarkers1_.add(marker);
			}

			// set auto range minimum size
			plot.getRangeAxis().setAutoRangeMinimumSize(maxDiff * RANGE_FACTOR, true);

			// remove shadow generator
			plot.setShadowGenerator(null);

			// increment progress
			plottingCompleted_++;
		}

		// second sequence
		else if (sequence.getID() == sequence2_.getID()) {

			// set dataset
			plot = chart_[1].getXYPlot();
			plot.setDataset(incrementDataset);
			plot.setDataset(1, steadyDataset);
			plot.setDataset(2, incrementPointsDataset);

			// set title
			plot.getDomainAxis().setLabel(title);

			// set colors
			for (int i = 0; i < incrementDataset.getSeriesCount(); i++) {
				String seriesName = (String) incrementDataset.getSeriesKey(i);
				if (seriesName.equals("Positive Increments")) {
					plot.getRenderer().setSeriesPaint(i, MissionProfileViewPanel.POSITIVE_INCREMENTS);
				}
				else if (seriesName.equals("Negative Increments")) {
					plot.getRenderer().setSeriesPaint(i, MissionProfileViewPanel.NEGATIVE_INCREMENTS);
				}
			}
			plot.getRenderer(1).setSeriesPaint(0, Color.black);
			plot.getRenderer(1).setSeriesPaint(1, MissionProfileViewPanel.ONEG);
			plot.getRenderer(1).setSeriesPaint(2, MissionProfileViewPanel.DELTA_P);
			plot.getRenderer(1).setSeriesPaint(3, MissionProfileViewPanel.DELTA_T);

			// clear current markers
			plot.clearDomainMarkers();
			segmentMarkers2_.clear();

			// create and add segment markers
			for (int i = 0; i < segments.size(); i++) {
				IntervalMarker marker = new IntervalMarker(i, i + 1);
				marker.setLabelOffsetType(LengthAdjustmentType.EXPAND);
				marker.setPaint(MissionProfileViewPanel.SEGMENT_FILL);
				marker.setLabel(segments.get(i).toString());
				marker.setLabelFont(MissionProfileViewPanel.SEGMENT_FONT);
				marker.setOutlinePaint(MissionProfileViewPanel.SEGMENT_OUTLINE);
				marker.setLabelAnchor(RectangleAnchor.BOTTOM);
				marker.setLabelTextAnchor(TextAnchor.BOTTOM_CENTER);
				segmentMarkers2_.add(marker);
			}

			// set auto range minimum size
			plot.getRangeAxis().setAutoRangeMinimumSize(maxDiff * RANGE_FACTOR, true);

			// remove shadow generator
			plot.setShadowGenerator(null);

			// increment progress
			plottingCompleted_++;
		}

		// notify mission profile panel
		if (plottingCompleted_ == 2) {
			MissionProfilePanel panel = (MissionProfilePanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.MISSION_PROFILE_PANEL);
			panel.plottingCompleted(plot);
		}
	}

	/**
	 * Sets peak info.
	 *
	 * @param info
	 *            Peak info.
	 * @param sequence
	 *            Stress sequence.
	 */
	public void setPeakInfo(String info, StressSequence sequence) {
		if (info == null)
			return;
		int chartIndex = sequence.getID() == sequence1_.getID() ? 0 : 1;
		info_[chartIndex].setText(info);
		chart_[chartIndex].getXYPlot().annotationChanged(null);
	}

	/**
	 * Updates peak info from database.
	 *
	 */
	public void updatePeakInfo() {

		// get peak info
		Platform.runLater(() -> {
			updatePeakInfo(crosshairX1_, crosshairY1_, sequence1_);
			updatePeakInfo(crosshairX2_, crosshairY2_, sequence2_);
		});
	}

	/**
	 * Updates peak info from database.
	 *
	 * @param x
	 *            Crosshair X.
	 * @param y
	 *            Crosshair Y.
	 * @param sequence
	 *            Stress sequence.
	 */
	private void updatePeakInfo(double x, double y, StressSequence sequence) {

		// get selected peak info
		MissionProfilePanel panel = (MissionProfilePanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.MISSION_PROFILE_PANEL);
		int peakInfo = panel.getSelectedPeakInfo();

		// get segment name
		String segment = null;
		if (sequence.getID() == sequence1_.getID()) {
			if (segmentMarkers1_ == null || segmentMarkers1_.isEmpty() || segmentMarkers1_.size() <= (int) x)
				return;
			segment = segmentMarkers1_.get((int) x).getLabel();
		}
		else {
			if (segmentMarkers2_ == null || segmentMarkers2_.isEmpty() || segmentMarkers2_.size() <= (int) x)
				return;
			segment = segmentMarkers2_.get((int) x).getLabel();
		}

		// segment
		if (peakInfo == GetMissionProfilePeakInfo.SEGMENT) {
			setPeakInfo("Segment: " + segment, sequence);
			return;
		}

		// total stress
		else if (peakInfo == GetMissionProfilePeakInfo.TOTAL_STRESS) {
			setPeakInfo("Total stress: " + format_.format(y), sequence);
			return;
		}

		// get segment name and number
		String segmentName = null;
		int segmentNum = -1;
		try {
			String[] split = segment.split(" ");
			segmentName = split[0].trim();
			segmentNum = Integer.parseInt(split[1].substring(1, split[1].length() - 1).trim());
		}

		// exception occurred during parsing segment info
		catch (Exception e) {
			String message = "Cannot parse segment name and number from selection.";
			owner_.getOwner().getNotificationPane().showError("Problem encountered", message, e);
			return;
		}

		// compute factor number
		int factorNum = (int) (0.5 * (16.0 * (x - segmentNum) + 1.0));

		// not an incremental point
		if (factorNum == 0) {
			setPeakInfo("Please select an incremental point.", sequence);
			return;
		}

		// get peak info
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetMissionProfilePeakInfo(peakInfo, sequence, segmentName, segmentNum, factorNum, y, true));
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static MissionProfileComparisonViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("MissionProfileComparisonViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			MissionProfileComparisonViewPanel controller = (MissionProfileComparisonViewPanel) fxmlLoader.getController();

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

	/**
	 * Inner class for listening chart-1 crosshair changes.
	 *
	 * @author Murat Artim
	 * @date May 20, 2015
	 * @time 12:17:27 PM
	 */
	private class Chart1CrosshairListener implements CrosshairListener {

		@Override
		public void crosshairValueChanged(double x, double y) {

			// crosshair coordinates did not change
			if (crosshairX1_ == x && crosshairY1_ == y)
				return;

			// update coordinates
			crosshairX1_ = x;
			crosshairY1_ = y;

			// get peak info
			Platform.runLater(() -> updatePeakInfo(x, y, sequence1_));
		}
	}

	/**
	 * Inner class for listening chart-2 crosshair changes.
	 *
	 * @author Murat Artim
	 * @date May 20, 2015
	 * @time 12:17:27 PM
	 */
	private class Chart2CrosshairListener implements CrosshairListener {

		@Override
		public void crosshairValueChanged(double x, double y) {

			// crosshair coordinates did not change
			if (crosshairX2_ == x && crosshairY2_ == y)
				return;

			// update coordinates
			crosshairX2_ = x;
			crosshairY2_ = y;

			// get peak info
			Platform.runLater(() -> updatePeakInfo(x, y, sequence2_));
		}
	}

	/**
	 * Inner class for chart-1 axis change listener.
	 *
	 * @author Murat Artim
	 * @date May 20, 2015
	 * @time 12:20:41 PM
	 */
	private class Chart1AxisListener implements AxisChangeListener {

		@Override
		public void axisChanged(AxisChangeEvent event) {

			// get plot
			XYPlot plot = (XYPlot) chart_[0].getPlot();

			// get domain axis
			ValueAxis domainAxis = plot.getDomainAxis();

			// get range
			double range = domainAxis.getUpperBound() - domainAxis.getLowerBound();

			// get markers
			Collection<?> c = plot.getDomainMarkers(Layer.BACKGROUND);

			// range too big, clear markers
			if (range > MissionProfileViewPanel.SHOWN_SEGMENTS) {
				if (c != null && !c.isEmpty()) {
					plot.clearDomainMarkers();
				}
			}

			// add markers (if not already added)
			else {
				if (c == null || c.isEmpty()) {
					for (IntervalMarker marker : segmentMarkers1_) {
						plot.addDomainMarker(marker, Layer.BACKGROUND);
					}
				}
			}
		}
	}

	/**
	 * Inner class for chart-2 axis change listener.
	 *
	 * @author Murat Artim
	 * @date May 20, 2015
	 * @time 12:20:41 PM
	 */
	private class Chart2AxisListener implements AxisChangeListener {

		@Override
		public void axisChanged(AxisChangeEvent event) {

			// get plot
			XYPlot plot = (XYPlot) chart_[1].getPlot();

			// get domain axis
			ValueAxis domainAxis = plot.getDomainAxis();

			// get range
			double range = domainAxis.getUpperBound() - domainAxis.getLowerBound();

			// get markers
			Collection<?> c = plot.getDomainMarkers(Layer.BACKGROUND);

			// range too big, clear markers
			if (range > MissionProfileViewPanel.SHOWN_SEGMENTS) {
				if (c != null && !c.isEmpty()) {
					plot.clearDomainMarkers();
				}
			}

			// add markers (if not already added)
			else {
				if (c == null || c.isEmpty()) {
					for (IntervalMarker marker : segmentMarkers2_) {
						plot.addDomainMarker(marker, Layer.BACKGROUND);
					}
				}
			}
		}
	}
}
