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

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.MultiplePiePlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.util.TableOrder;

import equinox.Equinox;
import equinox.controller.DamageContributionViewPanel;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.ui.PieLabelGenerator;
import equinox.plugin.FileType;
import equinox.process.PlotFlightDamageContributionsProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.PostProcessingTask;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save typical flight damage contribution plot task.
 *
 * @author Murat Artim
 * @date 1 Nov 2016
 * @time 18:36:43
 */
public class SaveFlightDamageContributionPlot extends TemporaryFileCreatingTask<Path> implements ShortRunningTask, PostProcessingTask, SingleInputTask<FlightDamageContributions>, AutomaticTaskOwner<Path> {

	/** Damage contributions item. */
	private FlightDamageContributions contributions_;

	/** Output file. */
	private Path outputFile_;

	/** True to save the plot to only database. */
	private boolean saveToDatabase_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save typical flight damage contribution plot task.
	 *
	 * @param contributions
	 *            Damage contribution. Can be null for automatic execution.
	 * @param saveToDatabase
	 *            True to save the plot to only database.
	 * @param outputFile
	 *            Output file. Can be null if saving to database.
	 */
	public SaveFlightDamageContributionPlot(FlightDamageContributions contributions, boolean saveToDatabase, Path outputFile) {
		contributions_ = contributions;
		outputFile_ = outputFile;
		saveToDatabase_ = saveToDatabase;
	}

	@Override
	public void setAutomaticInput(FlightDamageContributions input) {
		contributions_ = input;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Path> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Path>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save typical flight damage contributions plot";
	}

	@Override
	protected Path call() throws Exception {

		// update info
		updateMessage("Saving typical flight damage contributions plot...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// plot
			outputFile_ = plot(connection);

			// save plot
			if (saveToDatabase_) {
				savePlot(connection, outputFile_);
			}
		}

		// return
		return outputFile_;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// no automatic task
		if (automaticTasks_ == null)
			return;

		try {

			// get output file
			Path output = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(output, automaticTasks_, taskPanel_);
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

		// no automatic task
		if (automaticTasks_ == null)
			return;

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// no automatic task
		if (automaticTasks_ == null)
			return;

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Plots damage contributions on a file.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Path to image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path plot(Connection connection) throws Exception {

		// update info
		updateMessage("Plotting typical flight damage contributions...");

		// create path to output image
		Path output = outputFile_ == null ? getWorkingDirectory().resolve("damageContributions.png") : outputFile_;

		// set shadow theme
		ChartFactory.setChartTheme(new StandardChartTheme("JFree/Shadow", true));

		// create chart
		String title = "Typical Flight Damage Contributions of";
		title += "\n" + FileType.getNameWithoutExtension(contributions_.getParentItem().getName()) + "";
		JFreeChart chart = ChartFactory.createMultiplePieChart(title, null, TableOrder.BY_COLUMN, true, false, false);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup multiple plot
		MultiplePiePlot mplot = (MultiplePiePlot) chart.getPlot();
		mplot.setOutlinePaint(null);
		mplot.setBackgroundPaint(null);
		mplot.setNoDataMessage("No data available.");

		// setup sub-chart plot
		JFreeChart subchart = mplot.getPieChart();
		subchart.setBackgroundPaint(null);
		TextTitle subChartTitle = subchart.getTitle();
		subChartTitle.setPaint(new Color(112, 128, 144));
		subChartTitle.setFont(subChartTitle.getFont().deriveFont(14f));
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

		// plot
		DefaultCategoryDataset dataset = new PlotFlightDamageContributionsProcess(this, contributions_).start(connection);

		// set dataset
		mplot.setDataset(dataset);

		// set colors
		if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++)
				if (dataset.getRowKey(i).equals("Rest")) {
					splot.setSectionPaint(dataset.getRowKey(i), Color.LIGHT_GRAY);
				}
				else {
					splot.setSectionPaint(dataset.getRowKey(i), DamageContributionViewPanel.COLORS[i]);
				}
		}

		// setup chart dimensions
		int width = 658;
		int height = 597;

		// plot
		ChartUtilities.saveChartAsPNG(output.toFile(), chart, width, height);

		// return path to output image
		return output;
	}

	/**
	 * Saves the level crossings plot to database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param file
	 *            Path to level crossings plot.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void savePlot(Connection connection, Path file) throws Exception {

		// update info
		updateMessage("Saving flight damage contributions plot to database...");

		// get pilot point id
		int id = contributions_.getParentItem().getID();

		// check if any data exists in database
		boolean exists = false;
		String sql = "select image from pilot_point_tf_dc where id = " + id;
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					exists = true;
				}
			}
		}

		// create statement
		if (exists) {
			sql = "update pilot_point_tf_dc set image = ? where id = " + id;
		}
		else {
			sql = "insert into pilot_point_tf_dc(id, image) values(?, ?)";
		}
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			byte[] imageBytes = new byte[(int) file.toFile().length()];
			try (ImageInputStream imgStream = ImageIO.createImageInputStream(file.toFile())) {
				imgStream.read(imageBytes);
				try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
					if (exists) {
						update.setBlob(1, inputStream, imageBytes.length);
						update.executeUpdate();
					}
					else {
						update.setInt(1, id);
						update.setBlob(2, inputStream, imageBytes.length);
						update.executeUpdate();
					}
				}
			}
		}
	}
}
