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
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.MultiplePiePlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.util.TableOrder;

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.data.GAGEvent;
import equinox.data.ui.PieLabelGenerator;
import equinox.plugin.FileType;
import equinox.task.SaveImage;
import equinox.utility.Utility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for damage contribution view panel controller.
 *
 * @author Murat Artim
 * @date Apr 14, 2015
 * @time 9:32:02 AM
 */
public class DamageContributionViewPanel implements InternalViewSubPanel {

	/** Section colors. */
	public static final Color[] COLORS = { new Color(70, 130, 180), new Color(112, 128, 144), new Color(255, 69, 0), new Color(0, 128, 0), new Color(221, 160, 221), new Color(255, 215, 0), new Color(255, 99, 71), new Color(152, 251, 152) };

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart. */
	private JFreeChart chart_;

	/** STF file name of plot. */
	private String stfFileName_;

	/** GAG events panel. */
	private GAGEventsPanel gagPanel_;

	/** List containing the available users. */
	private ObservableList<GAGEvent> gagEventsList_;

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@FXML
	private Hyperlink gagEvents_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// load GAG events panel
		gagPanel_ = GAGEventsPanel.load(this);

		// create available users list
		gagEventsList_ = FXCollections.observableArrayList();
		gagEventsList_.addListener(gagPanel_);

		// set shadow theme
		ChartFactory.setChartTheme(new StandardChartTheme("JFree/Shadow", true));

		// create chart
		chart_ = ChartFactory.createMultiplePieChart("Damage Contributions", null, TableOrder.BY_COLUMN, true, false, false);
		chart_.setBackgroundPaint(new Color(245, 245, 245));
		chart_.setAntiAlias(true);
		chart_.setTextAntiAlias(true);

		// setup multiple plot
		MultiplePiePlot mplot = (MultiplePiePlot) chart_.getPlot();
		mplot.setOutlinePaint(null);
		mplot.setBackgroundPaint(null);
		mplot.setNoDataMessage("No data available.");

		// setup sub-chart plot
		JFreeChart subchart = mplot.getPieChart();
		subchart.setBackgroundPaint(null);
		TextTitle title = subchart.getTitle();
		title.setPaint(new Color(112, 128, 144));
		title.setFont(title.getFont().deriveFont(14f));
		PiePlot splot = (PiePlot) subchart.getPlot();
		splot.setNoDataMessage("No data available.");
		splot.setLabelGenerator(new PieLabelGenerator("{0} ({2})"));
		splot.setLabelBackgroundPaint(new Color(220, 220, 220));
		splot.setIgnoreZeroValues(true);
		splot.setMaximumLabelWidth(0.20);
		splot.setInteriorGap(0.04);
		splot.setBaseSectionOutlinePaint(new Color(245, 245, 245));
		splot.setSectionOutlinesVisible(true);
		splot.setBaseSectionOutlineStroke(new BasicStroke(1.5f));
		splot.setBackgroundPaint(new Color(112, 128, 144, 20));
		splot.setOutlinePaint(new Color(112, 128, 144));
		splot.setExplodePercent("Rest", 0.20);

		// create swing node content
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				ChartPanel panel = new ChartPanel(chart_);
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
		return null;
	}

	@Override
	public String getHeader() {
		return "Damage Contributions";
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
		fileChooser.setInitialFileName("Damage Contributions" + FileType.PNG.getExtension());
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
		return "Damage Contributions";
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

	/**
	 * Sets plot data.
	 *
	 * @param dataset
	 *            Dataset.
	 * @param stfFileName
	 *            Chart title.
	 * @param gagEvents
	 *            GAG events.
	 */
	public void setPlotData(CategoryDataset dataset, String stfFileName, ArrayList<GAGEvent> gagEvents) {

		// set file name
		stfFileName_ = stfFileName;

		// set chart title
		String title = gagEvents == null ? "Typical Flight Damage Contributions of" : "Loadcase Damage Contributions of";
		title += "\n" + stfFileName_ + "";
		chart_.setTitle(title);

		// set dataset
		MultiplePiePlot mplot = (MultiplePiePlot) chart_.getPlot();
		mplot.setDataset(dataset);

		// setup sub-chart plot
		JFreeChart subchart = mplot.getPieChart();
		PiePlot splot = (PiePlot) subchart.getPlot();

		// set colors
		if (dataset.getRowCount() <= COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				if (dataset.getRowKey(i).equals("Rest")) {
					splot.setSectionPaint(dataset.getRowKey(i), Color.LIGHT_GRAY);
				}
				else {
					splot.setSectionPaint(dataset.getRowKey(i), COLORS[i]);
				}
			}
		}

		// setup GAG events
		if (gagEvents != null) {
			gagEvents_.setVisible(true);
			gagEventsList_.setAll(gagEvents);
			gagEvents_.setVisited(false);
		}
		else {
			gagEvents_.setVisible(false);
		}
	}

	@FXML
	private void onGAGEventsClicked() {
		gagPanel_.show(gagEvents_);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static DamageContributionViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("DamageContributionViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			DamageContributionViewPanel controller = (DamageContributionViewPanel) fxmlLoader.getController();

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
