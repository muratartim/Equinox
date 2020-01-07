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
import equinox.data.ui.PieLabelGenerator;
import equinox.dataServer.remote.data.ContributionType;
import equinox.plugin.FileType;
import equinox.task.SaveImage;
import equinox.utility.Utility;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for compare damage contributions view panel.
 *
 * @author Murat Artim
 * @date Apr 15, 2015
 * @time 3:21:12 PM
 */
public class CompareDamageContributionsViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart. */
	private JFreeChart chart_;

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// set shadow theme
		ChartFactory.setChartTheme(new StandardChartTheme("JFree/Shadow", true));

		// create multiple chart
		chart_ = ChartFactory.createMultiplePieChart("Contribution Comparison", null, TableOrder.BY_COLUMN, true, false, false);
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
		SwingUtilities.invokeLater(() -> {
			ChartPanel panel = new ChartPanel(chart_);
			panel.setPopupMenu(null);
			panel.setMouseWheelEnabled(true);
			container_.setContent(panel);
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
		return "Contribution Comparison";
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
		fileChooser.setInitialFileName("Damage Contribution Comparison" + FileType.PNG.getExtension());
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
		return "Damage Contribution Comparison";
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
	 * @param contributionType
	 *            Damage contribution type.
	 */
	public void setPlotData(CategoryDataset dataset, ContributionType contributionType) {

		// invoke on AWT event dispatching thread
		SwingUtilities.invokeLater(() -> {

			// set chart title
			String title = contributionType.getName();
			title += " Eq. Stress Contribution Comparison";
			chart_.setTitle(title);

			// set dataset
			MultiplePiePlot mplot = (MultiplePiePlot) chart_.getPlot();
			mplot.setDataset(dataset);

			// setup sub-chart plot
			JFreeChart subchart = mplot.getPieChart();
			PiePlot splot = (PiePlot) subchart.getPlot();

			// set colors
			if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
				for (int i = 0; i < dataset.getRowCount(); i++) {
					if (dataset.getRowKey(i).equals("Rest")) {
						splot.setSectionPaint(dataset.getRowKey(i), Color.LIGHT_GRAY);
					}
					else {
						splot.setSectionPaint(dataset.getRowKey(i), DamageContributionViewPanel.COLORS[i]);
					}
				}
			}
		});
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static CompareDamageContributionsViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareDamageContributionsViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareDamageContributionsViewPanel controller = (CompareDamageContributionsViewPanel) fxmlLoader.getController();

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
