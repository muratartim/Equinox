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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EquinoxTheme;
import equinox.plugin.FileType;
import equinox.process.CompareFlightsProcess;
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
 * Class for compare flights view panel controller.
 *
 * @author Murat Artim
 * @date Sep 16, 2014
 * @time 9:25:52 AM
 */
public class CompareFlightsViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Chart panel. */
	private ChartPanel panel_;

	/** Panel header. */
	private String header_ = "Typical Flight Comparison";

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// create XY line chart
		JFreeChart chart = CompareFlightsProcess.createChart();

		// create swing node content
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				panel_ = new ChartPanel(chart);
				panel_.setPopupMenu(null);
				panel_.setMouseWheelEnabled(true);
				container_.setContent(panel_);
			}
		});
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
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public HBox getControls() {
		return null;
	}

	@Override
	public String getHeader() {
		return header_;
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
		fileChooser.setInitialFileName(header_ + FileType.PNG.getExtension());
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
		return "Typical Flight Comparison";
	}

	@Override
	public WritableImage getViewImage() {
		return container_.snapshot(null, null);
	}

	/**
	 * Returns the chart of this panel.
	 *
	 * @return The chart of this panel.
	 */
	public JFreeChart getChart() {
		return panel_.getChart();
	}

	/**
	 * Sets flight comparison chart to panel.
	 *
	 * @param chart
	 *            Chart to set.
	 * @param isExternal
	 *            True if external flights were compared.
	 */
	public void setFlightComparisonChart(JFreeChart chart, boolean isExternal) {

		// set chart
		panel_.setChart(chart);

		// set header
		header_ = "Typical Flight Comparison";

		// notify compare flights panel
		if (!isExternal) {
			CompareFlightsPanel panel = (CompareFlightsPanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.COMPARE_FLIGHTS_PANEL);
			panel.setChart(chart);
		}
		else {
			CompareExternalFlightsPanel panel = (CompareExternalFlightsPanel) owner_.getOwner().getInputPanel().getSubPanel(InputPanel.COMPARE_EXTERNAL_FLIGHTS_PANEL);
			panel.setChart(chart);
		}
	}

	/**
	 * Loads and returns plot text view panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot text view panel.
	 */
	public static CompareFlightsViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CompareFlightsViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			CompareFlightsViewPanel controller = (CompareFlightsViewPanel) fxmlLoader.getController();

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
