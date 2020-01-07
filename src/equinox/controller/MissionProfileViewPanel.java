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
import java.awt.Font;
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
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for mission profile view panel controller.
 *
 * @author Murat Artim
 * @date May 30, 2016
 * @time 9:43:28 PM
 */
public class MissionProfileViewPanel implements InternalViewSubPanel, CrosshairListener, AxisChangeListener {

	/** Chart colors. */
	public static final Color POSITIVE_INCREMENTS = new Color(70, 130, 180), NEGATIVE_INCREMENTS = new Color(178, 34, 34), ONEG = new Color(128, 0, 128), DELTA_P = new Color(255, 165, 0), DELTA_T = new Color(34, 139, 34), SEGMENT_FILL = new Color(245, 245, 245, 0),
			SEGMENT_OUTLINE = new Color(211, 211, 211, 180);

	/** Segment font. */
	public static final Font SEGMENT_FONT = new Font("SansSerif", Font.PLAIN, 10);

	/** Plot factors. */
	public static final double SHOWN_SEGMENTS = 15.0, RANGE_FACTOR = 2.0;

	/** The owner panel. */
	private ViewPanel owner_;

	/** Controls. */
	private MissionProfileViewControls controls_;

	/** Segment markers. */
	private ArrayList<IntervalMarker> segmentMarkers_;

	/** Crosshair information. */
	private TextTitle[] info_;

	/** Chart subtitle. */
	private TextTitle subTitle_;

	/** Crosshair coordinates. */
	private double crosshairX_ = 0.0, crosshairY_ = 0.0;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	/** Chart. */
	private JFreeChart[] chart_;

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create controls
		controls_ = MissionProfileViewControls.load(this);

		// create mission profile chart
		chart_ = new JFreeChart[1];
		chart_[0] = CrosshairListenerXYPlot.createMissionProfileChart("Mission Profile", "Segment", "Stress", null, PlotOrientation.VERTICAL, true, false, false, this);
		chart_[0].setBackgroundPaint(new Color(245, 245, 245));
		chart_[0].setAntiAlias(true);
		chart_[0].setTextAntiAlias(true);

		// create subtitle
		subTitle_ = new TextTitle();
		chart_[0].addSubtitle(1, subTitle_);

		// setup plot
		XYPlot plot = chart_[0].getXYPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(new Color(245, 245, 245, 0));
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.getDomainAxis().setTickLabelsVisible(false);
		plot.getDomainAxis().setTickMarksVisible(false);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.getDomainAxis().addChangeListener(this);
		plot.setDomainPannable(true);
		plot.setRangePannable(true);

		// create segment markers
		segmentMarkers_ = new ArrayList<>();

		// setup crosshairs
		BasicStroke defdom = (BasicStroke) plot.getDomainCrosshairStroke();
		plot.setDomainCrosshairStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, defdom.getMiterLimit(), defdom.getDashArray(), defdom.getDashPhase()));
		plot.setRangeCrosshairStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, defdom.getMiterLimit(), defdom.getDashArray(), defdom.getDashPhase()));
		plot.setDomainCrosshairPaint(new Color(70, 130, 180));
		plot.setRangeCrosshairPaint(new Color(70, 130, 180));

		// create information box
		info_ = new TextTitle[1];
		info_[0] = new TextTitle("Select point to see info.");
		info_[0].setBackgroundPaint(new Color(70, 130, 180));
		info_[0].setPaint(Color.white);
		info_[0].setFrame(new BlockBorder(Color.lightGray));
		info_[0].setPosition(RectangleEdge.BOTTOM);
		XYTitleAnnotation ta = new XYTitleAnnotation(0.98, 0.02, info_[0], RectangleAnchor.BOTTOM_RIGHT);
		ta.setMaxWidth(0.48);
		plot.addAnnotation(ta);

		// create swing node content
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				ChartPanel panel = new ChartPanel(chart_[0]);
				panel.setPopupMenu(null);
				panel.setMouseWheelEnabled(true);
				container_.setContent(panel);
			}
		});
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public HBox getControls() {
		return controls_.getRoot();
	}

	@Override
	public String getHeader() {
		return "Mission Profile View";
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Mission Profile" + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = container_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	@Override
	public String getViewName() {
		return "Mission Profile";
	}

	@Override
	public WritableImage getViewImage() {
		return container_.snapshot(null, null);
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
	public void crosshairValueChanged(double x, double y) {

		// crosshair coordinates did not change
		if ((crosshairX_ == x) && (crosshairY_ == y))
			return;

		// update coordinates
		crosshairX_ = x;
		crosshairY_ = y;

		// get peak info
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				updatePeakInfo(x, y);
			}
		});
	}

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
		if (range > SHOWN_SEGMENTS) {
			if ((c != null) && !c.isEmpty()) {
				plot.clearDomainMarkers();
			}
		}

		// add markers (if not already added)
		else {
			if ((c == null) || c.isEmpty()) {
				for (IntervalMarker marker : segmentMarkers_) {
					plot.addDomainMarker(marker, Layer.BACKGROUND);
				}
			}
		}
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
	 * @param sequence
	 *            Stress sequence.
	 * @param plotPosInc
	 *            True to plot positive increments.
	 * @param plotNegInc
	 *            True to plot negative increments.
	 * @param plotStep
	 *            Step plotting options. Array size must be 8 (for 8 steps). Null can be given for plotting all steps.
	 */
	public void plot(StressSequence sequence, boolean plotPosInc, boolean plotNegInc, boolean[] plotStep) {
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new PlotMissionProfile(sequence, plotPosInc, plotNegInc, plotStep, false));
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
	 * @param subTitle
	 *            Chart subtitle.
	 */
	public void plottingCompleted(IntervalXYDataset incrementDataset, XYDataset steadyDataset, XYDataset incrementPointsDataset, ArrayList<Segment> segments, double maxDiff, String title, String subTitle) {

		// set dataset
		XYPlot plot = chart_[0].getXYPlot();
		plot.setDataset(incrementDataset);
		plot.setDataset(1, steadyDataset);
		plot.setDataset(2, incrementPointsDataset);

		// set title
		chart_[0].setTitle(title);

		// set sub title
		if (subTitle == null) {
			subTitle_.setVisible(false);
		}
		else {
			subTitle_.setText(subTitle);
			subTitle_.setVisible(true);
		}

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
		segmentMarkers_.clear();

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
			segmentMarkers_.add(marker);
		}

		// set auto range minimum size
		plot.getRangeAxis().setAutoRangeMinimumSize(maxDiff * RANGE_FACTOR, true);

		// remove shadow generator
		plot.setShadowGenerator(null);

		// notify mission profile panel
		MissionProfilePanel panel = (MissionProfilePanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.MISSION_PROFILE_PANEL);
		panel.plottingCompleted(plot);
	}

	/**
	 * Updates peak info from database.
	 *
	 */
	public void updatePeakInfo() {

		// get peak info
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				updatePeakInfo(crosshairX_, crosshairY_);
			}
		});
	}

	/**
	 * Updates peak info from database.
	 *
	 * @param x
	 *            Crosshair X.
	 * @param y
	 *            Crosshair Y.
	 */
	private void updatePeakInfo(double x, double y) {

		// get selected peak info
		MissionProfilePanel panel = (MissionProfilePanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.MISSION_PROFILE_PANEL);
		int peakInfo = panel.getSelectedPeakInfo();

		// no segments available
		if ((segmentMarkers_ == null) || segmentMarkers_.isEmpty() || (segmentMarkers_.size() <= (int) x))
			return;

		// get segment name
		String segment = segmentMarkers_.get((int) x).getLabel();

		// segment
		if (peakInfo == GetMissionProfilePeakInfo.SEGMENT) {
			setPeakInfo("Segment: " + segment);
			return;
		}

		// total stress
		else if (peakInfo == GetMissionProfilePeakInfo.TOTAL_STRESS) {
			setPeakInfo("Total stress: " + format_.format(y));
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
		int factorNum = (int) (0.5 * ((16.0 * (x - segmentNum)) + 1.0));

		// not an incremental point
		if (factorNum == 0) {
			setPeakInfo("Please select an incremental point.");
			return;
		}

		// get selected stress sequence
		ObservableList<TreeItem<String>> items = owner_.getOwner().getInputPanel().getSelectedFiles();
		if (items.isEmpty())
			return;
		TreeItem<String> item = items.get(0);
		if ((item == null) || ((item instanceof StressSequence) == false))
			return;
		StressSequence sequence = (StressSequence) item;

		// get peak info
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetMissionProfilePeakInfo(peakInfo, sequence, segmentName, segmentNum, factorNum, y, false));
	}

	/**
	 * Sets peak info.
	 *
	 * @param info
	 *            Peak info.
	 */
	public void setPeakInfo(String info) {
		if (info == null)
			return;
		info_[0].setText(info);
		chart_[0].getXYPlot().annotationChanged(null);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static MissionProfileViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("MissionProfileViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			MissionProfileViewPanel controller = (MissionProfileViewPanel) fxmlLoader.getController();

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
