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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.List;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import equinox.Equinox;
import equinox.controller.DamageContributionViewPanel;
import equinox.controller.MissionProfileViewPanel;
import equinox.data.Settings;
import equinox.data.StressComponent;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.Flight;
import equinox.data.fileType.Rfort;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.data.input.FlightComparisonInput;
import equinox.data.input.LevelCrossingInput;
import equinox.data.ui.RfortOmission;
import equinox.plugin.FileType;
import equinox.process.CompareFlightsProcess;
import equinox.process.ExportRfortToExcelProcess;
import equinox.process.PlotLevelCrossingProcess;
import equinox.process.PlotMissionProfileProcess;
import equinox.process.PlotRfortEquivalentStressesProcess;
import equinox.process.PlotRfortPeaksProcess;
import equinox.process.PlotRfortResultsProcess;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.CrosshairListenerXYPlot;
import equinox.utility.Utility;
import javafx.scene.control.TreeItem;

/**
 * Class for generate RFORT report task.
 *
 * @author Murat Artim
 * @date Apr 4, 2016
 * @time 1:17:28 PM
 */
public class GenerateRfortReport extends TemporaryFileCreatingTask<Void> implements LongRunningTask {

	/** RFORT file. */
	private final Rfort rfort_;

	/** Output PDF file. */
	private final Path outputFile_;

	/** Selected pilot points and omissions. */
	private final ArrayList<String> pilotPoints_, omissions_;

	/** Plot options. */
	private final boolean absoluteDeviations_, dataLabels_;

	/** Typical flight name to compare. */
	private final String flight_;

	/** Decimal format. */
	private final DecimalFormat format1_ = new DecimalFormat("#.#####"), format2_ = new DecimalFormat("#.##");

	/** Fonts. */
	private final Font titleFont_ = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLDITALIC, new BaseColor(70, 130, 180)), subTitleFont_ = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLDITALIC, new BaseColor(70, 130, 180)),
			normalFont_ = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, new BaseColor(112, 128, 144)), boldFont_ = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, BaseColor.BLACK),
			boldColorFont_ = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, new BaseColor(112, 128, 144)), emptyLineFont_ = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

	/**
	 * Creates generate RFORT report task.
	 *
	 * @param rfort
	 *            RFORT file.
	 * @param outputFile
	 *            Output Excel file.
	 * @param pilotPoints
	 *            Selected pilot points.
	 * @param omissions
	 *            Selected omissions.
	 * @param absoluteDeviations
	 *            True if absolute deviations should be plotted.
	 * @param dataLabels
	 *            True if data labels should be shown.
	 * @param flight
	 *            Typical flight name to compare.
	 */
	public GenerateRfortReport(Rfort rfort, Path outputFile, ArrayList<String> pilotPoints, ArrayList<String> omissions, boolean absoluteDeviations, boolean dataLabels, String flight) {
		rfort_ = rfort;
		outputFile_ = outputFile;
		pilotPoints_ = pilotPoints;
		omissions_ = omissions;
		absoluteDeviations_ = absoluteDeviations;
		dataLabels_ = dataLabels;
		flight_ = flight;
	}

	@Override
	public String getTaskTitle() {
		return "Generate RFORT report to '" + outputFile_.getFileName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// generate file attachments
			HashMap<String, Path> attachments = generateAttachments(connection);

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create file output stream
				try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile_.toFile()))) {

					// generate report
					generateReport(connection, statement, outputStream, attachments);
				}
			}
		}

		// return
		return null;
	}

	/**
	 * Generates and returns file attachments.
	 *
	 * @param connection
	 *            Database connection.
	 * @return File attachment mapping.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private HashMap<String, Path> generateAttachments(Connection connection) throws Exception {

		// update progress
		updateMessage("Generating file attachments...");

		// create attachments mapping
		HashMap<String, Path> attachments = new HashMap<>();

		// generate table of results attachment
		Path tableOfResults = getWorkingDirectory().resolve("Table_of_Results.xls");
		new ExportRfortToExcelProcess(this, rfort_, tableOfResults, pilotPoints_, omissions_).start(connection);
		attachments.put("Table of Results", tableOfResults);

		// return attachments
		return attachments;
	}

	/**
	 * Generates RFORT report.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param outputStream
	 *            Output stream.
	 * @param attachments
	 *            File attachments of the report.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void generateReport(Connection connection, Statement statement, OutputStream outputStream, HashMap<String, Path> attachments) throws Exception {

		// update progress
		updateMessage("Generating RFORT report...");

		// initialize document
		Document document = null;

		try {

			// create document
			document = new Document();

			// setup document
			PdfWriter writer = Utility.setupPDFReportDocument(this, document, outputStream, "RFORT Analysis Report");

			// open document
			document.open();

			// create cover page
			Utility.createPDFReportCoverPage(this, document, writer, "RFORT Analysis Report", new String[] { "Summary of Inputs", "RFORT Results", "Average Number of Peaks", "Equivalent Stresses", "Level Crossings", "Mission Profiles", "Typical Flight Comparisons" }, attachments);

			// summary of inputs
			summaryOfInputs(document, statement, connection);

			// RFORT results
			rfortResults(document, writer, connection);

			// average number of peaks
			averageNumberOfPeaks(document, writer, connection);

			// equivalent stresses
			equivalentStresses(document, writer, connection);

			// level crossings
			levelCrossings(document, writer, statement, connection);

			// mission profiles
			missionProfiles(document, writer, statement, connection);

			// typical flight comparisons
			typicalFlightComparisons(document, writer, statement, connection);

			// create end page
			Settings settings = taskPanel_.getOwner().getOwner().getSettings();
			String hostname = (String) settings.getValue(Settings.WEB_HOSTNAME);
			String port = (String) settings.getValue(Settings.WEB_PORT);
			Utility.createPDFReportEndPage(this, document, writer, "RFORT Analysis Report", hostname, port);
		}

		// close document
		finally {
			if (document != null) {
				document.close();
			}
		}
	}

	/**
	 * Creates chapter 'Typical Flight Comparisons' of the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void typicalFlightComparisons(Document document, PdfWriter writer, Statement statement, Connection connection) throws Exception {

		// progress info
		updateMessage("Creating chapter 'Typical Flight Comparisons'...");

		// create chapter
		Chunk chunk1 = new Chunk("Typical Flight Comparisons", titleFont_);
		chunk1.setLocalDestination("Typical Flight Comparisons");
		Chapter chapter = new Chapter(new Paragraph(chunk1), 7);
		chapter.setNumberDepth(2);
		document.add(chapter);

		// get file tree root
		TreeItem<String> root = taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot();
		ArrayList<Flight> flights = new ArrayList<>();

		// prepare statement for getting stress IDs
		String sql = "select stress_id from rfort_outputs where stress_type = '" + SaveRfortInfo.FATIGUE + "' and analysis_id = " + rfort_.getID();
		sql += " and pp_name = ? and omission_name = ?";
		try (PreparedStatement getStressIDs = connection.prepareStatement(sql)) {

			// get pilot point names
			sql = "select pp_name from rfort_outputs where omission_name = '" + RfortOmission.INITIAL_ANALYSIS + "' and stress_type = '" + SaveRfortInfo.FATIGUE + "' and analysis_id = " + rfort_.getID();
			try (ResultSet ppNames = statement.executeQuery(sql)) {

				// loop over pilot point names
				int pageIndex = 0;
				while (ppNames.next()) {

					// get pilot point name
					String ppName = ppNames.getString("pp_name");

					// not selected
					if (!pilotPoints_.contains(ppName)) {
						continue;
					}

					// reset flights
					flights.clear();

					// set pilot point name
					getStressIDs.setString(1, ppName);

					// loop over omissions
					for (String omissionName : omissions_) {

						// set omission name
						getStressIDs.setString(2, omissionName);

						// get stress IDs
						try (ResultSet stressIDs = getStressIDs.executeQuery()) {

							// loop over stress IDs
							while (stressIDs.next()) {

								// get stress ID
								int stressID = stressIDs.getInt("stress_id");

								// get fatigue equivalent stress
								FatigueEquivalentStress stress = (FatigueEquivalentStress) Utility.searchFileTree(root, stressID, FatigueEquivalentStress.class);

								// no equivalent stress found
								if (stress == null) {
									String message = "No fatigue equivalent stress found for pilot point '" + ppName;
									message += "' and omission level '" + omissionName + "'. Skipping typical flight comparison plot for this item.";
									addWarning(message);
								}
								else {
									flights.add(getFlight(stress.getParentItem().getFlights().getFlights(), flight_));
								}
							}
						}
					}

					// draw flight comparison
					drawFlightComparison(ppName, flights, document, writer, connection, pageIndex);
					pageIndex++;
				}
			}
		}
	}

	/**
	 * Draws typical flight comparison on the report.
	 *
	 * @param ppName
	 *            Pilot point name.
	 * @param flightsList
	 *            List of flights to compare.
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @param pageIndex
	 *            Page index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void drawFlightComparison(String ppName, ArrayList<Flight> flightsList, Document document, PdfWriter writer, Connection connection, int pageIndex) throws Exception {

		// create input
		FlightComparisonInput input = new FlightComparisonInput(null);
		input.setPlotComponentOptions(new boolean[] { true, true, true, true }, false);
		input.setShowMarkers(false);

		// set naming parameters
		input.setIncludeFlightName(true);
		input.setIncludeSpectrumName(false);
		input.setIncludeSTFName(false);
		input.setIncludeEID(false);
		input.setIncludeSequenceName(false);
		input.setIncludeProgram(false);
		input.setIncludeSection(false);
		input.setIncludeMission(true);

		// set flight visibility
		for (Flight flight : flightsList) {
			input.setFlightVisible(flight.getID(), true);
		}

		// plot
		JFreeChart chart = new CompareFlightsProcess(this, input, flightsList).start(connection);
		chart.setTitle("Typical Flight Comparison\n(" + FileType.getNameWithoutExtension(ppName) + ")");

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom() - (pageIndex == 0 ? 40 : 0);

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height - (pageIndex == 0 ? 40 : 0));

		// new page
		document.newPage();
	}

	/**
	 * Returns the flight with the given name from the flights list.
	 *
	 * @param flights
	 *            List of flights.
	 * @param flightName
	 *            Flight name.
	 * @return The flight with the given name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Flight getFlight(ArrayList<Flight> flights, String flightName) throws Exception {
		for (Flight flight : flights) {
			if (flight.getName().equals(flightName))
				return flight;
		}
		return null;
	}

	/**
	 * Creates chapter 'Mission Profiles' of the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void missionProfiles(Document document, PdfWriter writer, Statement statement, Connection connection) throws Exception {

		// progress info
		updateMessage("Creating chapter 'Mission Profiles'...");

		// create chapter
		Chunk chunk1 = new Chunk("Mission Profiles", titleFont_);
		chunk1.setLocalDestination("Mission Profiles");
		Chapter chapter = new Chapter(new Paragraph(chunk1), 6);
		chapter.setNumberDepth(2);
		document.add(chapter);

		// get file tree root
		TreeItem<String> root = taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot();

		// prepare statement for getting stress IDs
		String sql = "select stress_id from rfort_outputs where stress_type = '" + SaveRfortInfo.FATIGUE + "' and analysis_id = " + rfort_.getID();
		sql += " and pp_name = ? and omission_name = ?";
		try (PreparedStatement getStressIDs = connection.prepareStatement(sql)) {

			// get pilot point names
			sql = "select pp_name from rfort_outputs where omission_name = '" + RfortOmission.INITIAL_ANALYSIS + "' and stress_type = '" + SaveRfortInfo.FATIGUE + "' and analysis_id = " + rfort_.getID();
			try (ResultSet ppNames = statement.executeQuery(sql)) {

				// loop over pilot point names
				int pageIndex = 0;
				while (ppNames.next()) {

					// get pilot point name
					String ppName = ppNames.getString("pp_name");

					// not selected
					if (!pilotPoints_.contains(ppName)) {
						continue;
					}

					// set pilot point name
					getStressIDs.setString(1, ppName);

					// loop over omissions
					for (String omissionName : omissions_) {

						// set omission name
						getStressIDs.setString(2, omissionName);

						// get stress IDs
						try (ResultSet stressIDs = getStressIDs.executeQuery()) {

							// loop over stress IDs
							while (stressIDs.next()) {

								// get stress ID
								int stressID = stressIDs.getInt("stress_id");

								// get fatigue equivalent stress
								FatigueEquivalentStress stress = (FatigueEquivalentStress) Utility.searchFileTree(root, stressID, FatigueEquivalentStress.class);

								// no equivalent stress found
								if (stress == null) {
									String message = "No fatigue equivalent stress found for pilot point '" + ppName;
									message += "' and omission level '" + omissionName + "'. Skipping mission profile plot for this item.";
									addWarning(message);
								}

								// add to stresses
								else {

									// get stress sequence
									StressSequence stressSequence = stress.getParentItem();

									// draw mission profile
									drawMissionProfile(stressSequence, document, writer, connection, pageIndex);
									pageIndex++;
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Draws mission profile for the given stress sequence.
	 *
	 * @param stressSequence
	 *            Stress sequence.
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @param pageIndex
	 *            Page index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void drawMissionProfile(StressSequence stressSequence, Document document, PdfWriter writer, Connection connection, int pageIndex) throws Exception {

		// create mission profile chart
		JFreeChart chart = CrosshairListenerXYPlot.createMissionProfileChart("Mission Profile", "Segment", "Stress", null, PlotOrientation.VERTICAL, true, false, false, null);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(new Color(245, 245, 245, 0));
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.getDomainAxis().setTickLabelsVisible(false);
		plot.getDomainAxis().setTickMarksVisible(false);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);

		// plot
		PlotMissionProfileProcess process = new PlotMissionProfileProcess(this, stressSequence, true, true, null);
		XYDataset[] dataset = process.start(connection);
		double maxDiff = process.getMaxPositiveIncrement() - process.getMinNegativeIncrement();

		// set dataset
		plot.setDataset(dataset[0]);
		plot.setDataset(1, dataset[1]);
		plot.setDataset(2, dataset[2]);

		// set chart title
		String title = "Mission Profile";
		title += "\n(" + stressSequence.getName() + ", " + stressSequence.getParentItem().getMission() + ")";
		chart.setTitle(title);

		// set colors
		for (int i = 0; i < dataset[0].getSeriesCount(); i++) {
			String seriesName = (String) dataset[0].getSeriesKey(i);
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

		// set auto range minimum size
		plot.getRangeAxis().setAutoRangeMinimumSize(maxDiff * MissionProfileViewPanel.RANGE_FACTOR, true);

		// remove shadow generator
		plot.setShadowGenerator(null);

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom() - (pageIndex == 0 ? 40 : 0);

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height - (pageIndex == 0 ? 40 : 0));

		// new page
		document.newPage();
	}

	/**
	 * Creates chapter 'Level Crossings' of the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void levelCrossings(Document document, PdfWriter writer, Statement statement, Connection connection) throws Exception {

		// progress info
		updateMessage("Creating chapter 'Level Crossings'...");

		// create chapter
		Chunk chunk1 = new Chunk("Level Crossings", titleFont_);
		chunk1.setLocalDestination("Level Crossings");
		Chapter chapter = new Chapter(new Paragraph(chunk1), 5);
		chapter.setNumberDepth(2);
		document.add(chapter);

		// get file tree root
		TreeItem<String> root = taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot();
		ArrayList<FatigueEquivalentStress> stresses = new ArrayList<>();

		// prepare statement for getting stress IDs
		String sql = "select stress_id from rfort_outputs where stress_type = '" + SaveRfortInfo.FATIGUE;
		sql += "' and analysis_id = " + rfort_.getID() + " and pp_name = ? and omission_name = ?";
		try (PreparedStatement getStressIDs = connection.prepareStatement(sql)) {

			// get pilot point names
			sql = "select pp_name from rfort_outputs where omission_name = '" + RfortOmission.INITIAL_ANALYSIS + "' and stress_type = '";
			sql += SaveRfortInfo.FATIGUE + "' and analysis_id = " + rfort_.getID();
			try (ResultSet ppNames = statement.executeQuery(sql)) {

				// loop over pilot point names
				int pageIndex = 0;
				while (ppNames.next()) {

					// get pilot point name
					String ppName = ppNames.getString("pp_name");

					// pilot point not selected
					if (!pilotPoints_.contains(ppName)) {
						continue;
					}

					// reset stresses
					stresses.clear();

					// get pilot point name
					getStressIDs.setString(1, ppName);

					// loop over omissions
					for (String omissionName : omissions_) {

						// set omission name
						getStressIDs.setString(2, omissionName);

						// get stress IDs
						try (ResultSet stressIDs = getStressIDs.executeQuery()) {

							// loop over stress IDs
							while (stressIDs.next()) {

								// get stress ID
								int stressID = stressIDs.getInt("stress_id");

								// get fatigue equivalent stress
								FatigueEquivalentStress stress = (FatigueEquivalentStress) Utility.searchFileTree(root, stressID, FatigueEquivalentStress.class);

								// no equivalent stress found
								if (stress == null) {
									String message = "No fatigue equivalent stress found for pilot point '" + ppName;
									message += "' and omission '" + omissionName + "'. Skipping level crossing plot for this item.";
									addWarning(message);
								}
								else {
									stresses.add(stress);
								}
							}
						}
					}

					// draw level crossings
					drawLevelCrossings(ppName, stresses, document, writer, connection, pageIndex);
					pageIndex++;
				}
			}
		}
	}

	/**
	 * Draws level crossings for the given pilot point.
	 *
	 * @param ppName
	 *            Pilot point name.
	 * @param stresses
	 *            Fatigue equivalent stresses.
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @param pageIndex
	 *            Page index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void drawLevelCrossings(String ppName, ArrayList<FatigueEquivalentStress> stresses, Document document, PdfWriter writer, Connection connection, int pageIndex) throws Exception {

		// create input
		LevelCrossingInput input = new LevelCrossingInput(true, null);

		// set naming parameters
		input.setIncludeSpectrumName(false);
		input.setIncludeSTFName(false);
		input.setIncludeEID(false);
		input.setIncludeSequenceName(false);
		input.setIncludeMaterialName(false);
		input.setIncludeOmissionLevel(false);
		input.setIncludeProgram(false);
		input.setIncludeSection(false);
		input.setIncludeMission(true);

		// create empty chart
		String title = "Level Crossings\n(" + FileType.getNameWithoutExtension(ppName) + ")";
		JFreeChart chart = ChartFactory.createXYLineChart(title, "", "", null);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
		LogarithmicAxis xAxis = new LogarithmicAxis("Number of Cycles (Normalized by spectrum validities)");
		xAxis.setAllowNegativesFlag(true);
		plot.setDomainAxis(xAxis);
		plot.setRangeAxis(new NumberAxis("Stress"));
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);

		// plot
		XYSeriesCollection dataset = new PlotLevelCrossingProcess(this, input, new ArrayList<SpectrumItem>(stresses)).start(connection);

		// set dataset
		plot.setDataset(dataset);

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom() - (pageIndex == 0 ? 40 : 0);

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height - (pageIndex == 0 ? 40 : 0));

		// new page
		document.newPage();
	}

	/**
	 * Creates chapter 'Equivalent Stresses' of the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void equivalentStresses(Document document, PdfWriter writer, Connection connection) throws Exception {

		// progress info
		updateMessage("Creating chapter 'Equivalent Stresses'...");

		// create chapter
		Chunk chunk1 = new Chunk("Equivalent Stresses", titleFont_);
		chunk1.setLocalDestination("Equivalent Stresses");
		Chapter chapter = new Chapter(new Paragraph(chunk1), 4);
		chapter.setNumberDepth(2);
		document.add(chapter);

		// plot fatigue stresses
		plotFatigueStresses(document, writer, connection);

		// plot preffas stresses
		if (rfort_.isPreffas()) {
			plotPreffasStresses(document, writer, connection);
		}

		// plot linear propagation stresses
		if (rfort_.isLinear()) {
			plotLinearStresses(document, writer, connection);
		}
	}

	/**
	 * Plots fatigue deviations on the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotFatigueStresses(Document document, PdfWriter writer, Connection connection) throws Exception {

		// create bar chart
		String title = "Fatigue Equivalent Stresses";
		JFreeChart chart = ChartFactory.createBarChart(title, "Pilot Points", title, null, PlotOrientation.VERTICAL, true, false, false);
		chart.getLegend().setVisible(true);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.getDomainAxis().setMaximumCategoryLabelLines(10);

		// set item label generator
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setBaseToolTipGenerator(null);

		// plot
		DefaultCategoryDataset dataset = new PlotRfortEquivalentStressesProcess(this, rfort_, SaveRfortInfo.FATIGUE, pilotPoints_, omissions_).start(connection);

		// set dataset
		plot.setDataset(dataset);

		// set label visibility
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, dataLabels_);
		}

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom() - 40;

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height - 40);

		// new page
		document.newPage();
	}

	/**
	 * Plots preffas deviations on the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotPreffasStresses(Document document, PdfWriter writer, Connection connection) throws Exception {

		// create bar chart
		String title = "Preffas Equivalent Stresses";
		JFreeChart chart = ChartFactory.createBarChart(title, "Pilot Points", title, null, PlotOrientation.VERTICAL, true, false, false);
		chart.getLegend().setVisible(true);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.getDomainAxis().setMaximumCategoryLabelLines(10);

		// set item label generator
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setBaseToolTipGenerator(null);

		// plot
		DefaultCategoryDataset dataset = new PlotRfortEquivalentStressesProcess(this, rfort_, SaveRfortInfo.PREFFAS, pilotPoints_, omissions_).start(connection);

		// set dataset
		plot.setDataset(dataset);

		// set label visibility
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, dataLabels_);
		}

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom();

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height);

		// new page
		document.newPage();
	}

	/**
	 * Plots linear propagation deviations on the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLinearStresses(Document document, PdfWriter writer, Connection connection) throws Exception {

		// create bar chart
		String title = "Linear Prop. Equivalent Stresses";
		JFreeChart chart = ChartFactory.createBarChart(title, "Pilot Points", title, null, PlotOrientation.VERTICAL, true, false, false);
		chart.getLegend().setVisible(true);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.getDomainAxis().setMaximumCategoryLabelLines(10);

		// set item label generator
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setBaseToolTipGenerator(null);

		// plot
		DefaultCategoryDataset dataset = new PlotRfortEquivalentStressesProcess(this, rfort_, SaveRfortInfo.LINEAR, pilotPoints_, omissions_).start(connection);

		// set dataset
		plot.setDataset(dataset);

		// set label visibility
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, dataLabels_);
		}

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom();

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height);

		// new page
		document.newPage();
	}

	/**
	 * Creates chapter 'Average Number of Peaks' of the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void averageNumberOfPeaks(Document document, PdfWriter writer, Connection connection) throws Exception {

		// progress info
		updateMessage("Creating chapter 'Average Number of Peaks'...");

		// create chapter
		Chunk chunk1 = new Chunk("Average Number of Peaks", titleFont_);
		chunk1.setLocalDestination("Average Number of Peaks");
		Chapter chapter = new Chapter(new Paragraph(chunk1), 3);
		chapter.setNumberDepth(2);
		document.add(chapter);

		// create bar chart
		JFreeChart chart = ChartFactory.createBarChart("Average Number of Peaks", "Omissions", "Average Number of Peaks", null, PlotOrientation.VERTICAL, true, false, false);
		chart.getLegend().setVisible(false);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		plot.getDomainAxis().setMaximumCategoryLabelLines(10);

		// set item label generator
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setBaseToolTipGenerator(null);

		// plot
		CategoryDataset dataset = new PlotRfortPeaksProcess(this, rfort_).start(connection);

		// set dataset
		plot.setDataset(dataset);

		// set label visibility
		for (int i = 0; i < dataset.getRowCount(); i++) {
			renderer.setSeriesItemLabelsVisible(i, true);
		}

		// set colors
		if (dataset.getRowCount() <= DamageContributionViewPanel.COLORS.length) {
			for (int i = 0; i < dataset.getRowCount(); i++) {
				renderer.setSeriesPaint(i, DamageContributionViewPanel.COLORS[i]);
			}
		}

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom() - 40;

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height - 40);

		// new page
		document.newPage();
	}

	/**
	 * Creates chapter 'RFORT Results' of the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void rfortResults(Document document, PdfWriter writer, Connection connection) throws Exception {

		// rotate page to landscape
		document.setPageSize(PageSize.A4.rotate());

		// progress info
		updateMessage("Creating chapter 'RFORT Results'...");

		// create chapter
		Chunk chunk1 = new Chunk("RFORT Results", titleFont_);
		chunk1.setLocalDestination("RFORT Results");
		Chapter chapter = new Chapter(new Paragraph(chunk1), 2);
		chapter.setNumberDepth(2);
		document.add(chapter);

		// plot fatigue results
		plotFatigueResults(document, writer, connection, absoluteDeviations_);

		// plot preffas results
		if (rfort_.isPreffas()) {
			plotPreffasResults(document, writer, connection, absoluteDeviations_);
		}

		// plot linear propagation results
		if (rfort_.isLinear()) {
			plotLinearResults(document, writer, connection, absoluteDeviations_);
		}
	}

	/**
	 * Plots fatigue results on the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @param plotAbsoluteDeviations
	 *            True to plot absolute deviations.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotFatigueResults(Document document, PdfWriter writer, Connection connection, boolean plotAbsoluteDeviations) throws Exception {

		// plot chart
		XYSeriesCollection dataset = new PlotRfortResultsProcess(this, rfort_, SaveRfortInfo.FATIGUE, pilotPoints_, omissions_, plotAbsoluteDeviations).start(connection);

		// create chart
		String title = "RFORT Fatigue Results";
		String xAxisLabel = "Number of Peaks";
		String yAxisLabel = (plotAbsoluteDeviations ? "Absolute " : "") + "Fatigue Equivalent Stress Deviations (%)";
		JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setInverted(true);
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setBaseShapesVisible(true);

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom() - 40;

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height - 40);

		// new page
		document.newPage();
	}

	/**
	 * Plots preffas results on the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @param plotAbsoluteDeviations
	 *            True to plot absolute deviations.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotPreffasResults(Document document, PdfWriter writer, Connection connection, boolean plotAbsoluteDeviations) throws Exception {

		// plot chart
		XYSeriesCollection dataset = new PlotRfortResultsProcess(this, rfort_, SaveRfortInfo.PREFFAS, pilotPoints_, omissions_, plotAbsoluteDeviations).start(connection);

		// create chart
		String title = "RFORT Preffas Results";
		String xAxisLabel = "Number of Peaks";
		String yAxisLabel = (plotAbsoluteDeviations ? "Absolute " : "") + "Preffas Equivalent Stress Deviations (%)";
		JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setInverted(true);
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setBaseShapesVisible(true);

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom();

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height);

		// new page
		document.newPage();
	}

	/**
	 * Plots linear propagation results on the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param connection
	 *            Database connection.
	 * @param plotAbsoluteDeviations
	 *            True to plot absolute deviations.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotLinearResults(Document document, PdfWriter writer, Connection connection, boolean plotAbsoluteDeviations) throws Exception {

		// plot chart
		XYSeriesCollection dataset = new PlotRfortResultsProcess(this, rfort_, SaveRfortInfo.LINEAR, pilotPoints_, omissions_, plotAbsoluteDeviations).start(connection);

		// create chart
		String title = "RFORT Linear Prop. Results";
		String xAxisLabel = "Number of Peaks";
		String yAxisLabel = (plotAbsoluteDeviations ? "Absolute " : "") + "Linear Prop. Equivalent Stress Deviations (%)";
		JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset);
		chart.setBackgroundPaint(new Color(245, 245, 245));
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);

		// setup plot
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setInverted(true);
		plot.setOutlinePaint(Color.lightGray);
		plot.setBackgroundPaint(null);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setBaseShapesVisible(true);

		// setup chart dimensions
		float width = document.right() - document.left();
		float height = document.top() - document.bottom();

		// draw chart on report
		PdfContentByte canvas = writer.getDirectContent();
		PdfTemplate template = canvas.createTemplate(width, height);
		Graphics2D g2d = new PdfGraphics2D(template, width, height, new DefaultFontMapper());
		Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
		chart.draw(g2d, r2D);
		g2d.dispose();
		canvas.addTemplate(template, document.left(), document.top() - height);

		// new page
		document.newPage();
	}

	/**
	 * Creates chapter 'Summary of Inputs' of the report.
	 *
	 * @param document
	 *            PDF document.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void summaryOfInputs(Document document, Statement statement, Connection connection) throws Exception {

		// progress info
		updateMessage("Creating chapter 'Summary of Inputs'...");

		// create chapter
		Chunk chunk1 = new Chunk("Summary of Inputs", titleFont_);
		chunk1.setLocalDestination("Summary of Inputs");
		Chapter chapter = new Chapter(new Paragraph(chunk1), 1);
		chapter.setNumberDepth(2);

		// write basic RFORT info
		writeBasicRfortInfo(chapter, statement);

		// write pilot point info
		writePilotPointInfo(chapter, statement, connection);

		// write omission info
		writeOmissionInfo(chapter, statement, connection);

		// add chapter to document
		document.add(chapter);

		// new page
		document.newPage();
	}

	/**
	 * Writes RFORT omission information to given chapter.
	 *
	 * @param chapter
	 *            Chapter.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeOmissionInfo(Chapter chapter, Statement statement, Connection connection) throws Exception {

		// progress info
		updateMessage("Writing RFORT pilot point information...");

		// section header
		Paragraph p2 = new Paragraph(new Chunk("Omissions", subTitleFont_));
		p2.setSpacingBefore(20);
		Section s1 = chapter.addSection(p2);

		// prepare statement getting omission values
		String sql = "select omission_value, pp_name, included_in_rfort, num_peaks from rfort_outputs where analysis_id = " + rfort_.getID() + " and omission_name = ? and stress_type = '" + SaveRfortInfo.FATIGUE + "' order by pp_name";
		try (PreparedStatement statement1 = connection.prepareStatement(sql)) {

			// create and execute statement for omission names
			sql = "select distinct omission_name from rfort_outputs where analysis_id = " + rfort_.getID() + " and stress_type = '";
			sql += SaveRfortInfo.FATIGUE + "'";
			try (ResultSet getOmissionNames = statement.executeQuery(sql)) {
				while (getOmissionNames.next()) {

					// get omission name
					String omissionName = getOmissionNames.getString("omission_name");

					// not selected
					if (!omissions_.contains(omissionName)) {
						continue;
					}

					// initial analysis
					if (omissionName.equals(RfortOmission.INITIAL_ANALYSIS)) {
						continue;
					}

					// add omission info
					Paragraph p3 = new Paragraph(new Chunk(omissionName, boldFont_));
					p3.setIndentationLeft(12);
					p3.setSpacingBefore(12);
					s1.add(p3);
					List list2 = new List(12);
					list2.setListSymbol("\u2022");
					list2.setIndentationLeft(24);

					// get omission values
					statement1.setString(1, omissionName);
					try (ResultSet getOmissionValues = statement1.executeQuery()) {
						while (getOmissionValues.next()) {

							// not included in RFORT
							if (!getOmissionValues.getBoolean("included_in_rfort")) {
								continue;
							}

							// get omission info
							String ppName = getOmissionValues.getString("pp_name");
							double omissionValue = getOmissionValues.getDouble("omission_value");
							list2.add(createListItem(ppName + ": ", format2_.format(omissionValue), true));
						}
					}
					s1.add(list2);
				}
			}
		}
	}

	/**
	 * Writes basic RFORT information to given chapter.
	 *
	 * @param chapter
	 *            Chapter.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeBasicRfortInfo(Chapter chapter, Statement statement) throws Exception {

		// progress info
		updateMessage("Writing basic RFORT information...");

		// write empty line
		chapter.add(new Paragraph("\n", emptyLineFont_));

		// write basic info
		List list1 = new List(12);
		String sql = "select * from rfort_analyses where id = " + rfort_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {

				// general info
				list1.setListSymbol("\u2022");
				list1.setIndentationLeft(12);
				list1.add(createListItem("Input spectrum: ", resultSet.getString("input_spectrum_name"), false));
				String analyses = resultSet.getBoolean("fatigue_analysis") ? "Fatigue" : "";
				analyses += resultSet.getBoolean("preffas_analysis") ? ", Preffas" : "";
				analyses += resultSet.getBoolean("linear_analysis") ? ", Linear propagation" : "";
				list1.add(createListItem("Equivalent stress analyses: ", analyses, false));

				// optional inputs
				Paragraph p2 = new Paragraph(new Chunk("Optional Inputs", subTitleFont_));
				p2.setSpacingBefore(20);
				p2.setSpacingAfter(12);
				Section s1 = chapter.addSection(p2);
				List list2 = new List(12);
				list2.setListSymbol("\u2022");
				list2.setIndentationLeft(12);
				list2.add(createListItem("Add Delta-P to stress sequences: ", resultSet.getBoolean("add_dp") ? "Yes" : "No", false));
				list2.add(createListItem("Reference Delta-P: ", format1_.format(resultSet.getDouble("ref_dp")), false));
				list2.add(createListItem("Delta-P factor: ", format1_.format(resultSet.getDouble("dp_factor")), false));
				list2.add(createListItem("Overall factor: ", format1_.format(resultSet.getDouble("overall_factor")), false));
				String stressComp = resultSet.getString("stress_comp");
				if (stressComp.equals(StressComponent.ROTATED.toString())) {
					stressComp += " (" + Integer.toString(resultSet.getInt("rotation_angle")) + " degrees)";
				}
				list2.add(createListItem("Stress component: ", stressComp, false));
				String runTillFlight = resultSet.getString("run_till_flight");
				list2.add(createListItem("Run till flight: ", runTillFlight == null ? "All flights" : runTillFlight, false));
				String targetFlights = resultSet.getString("target_flights");
				list2.add(createListItem("Target flights: ", targetFlights == null ? "All flights" : targetFlights, false));
				s1.add(list2);
			}
		}

		// add chapter
		chapter.add(1, list1);
	}

	/**
	 * Writes pilot point information to given chapter.
	 *
	 * @param chapter
	 *            Chapter.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writePilotPointInfo(Chapter chapter, Statement statement, Connection connection) throws Exception {

		// progress info
		updateMessage("Writing RFORT pilot point information...");

		// section header
		Paragraph p2 = new Paragraph(new Chunk("Pilot Points", subTitleFont_));
		p2.setSpacingBefore(20);
		Section s1 = chapter.addSection(p2);

		// prepare statement to get material names
		String sql = "select material_name, stress_type from rfort_outputs where omission_name = '" + RfortOmission.INITIAL_ANALYSIS + "' and analysis_id = " + rfort_.getID() + " and pp_name = ?";
		try (PreparedStatement getMaterialNames = connection.prepareStatement(sql)) {

			// get pilot points
			sql = "select distinct pp_name, included_in_rfort, stress_factor from rfort_outputs where omission_name = '" + RfortOmission.INITIAL_ANALYSIS + "' and analysis_id = " + rfort_.getID();
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {

					// get pilot point name
					String ppName = resultSet.getString("pp_name");

					// not selected
					if (!pilotPoints_.contains(ppName)) {
						continue;
					}

					// add pilot point info
					Paragraph p3 = new Paragraph(new Chunk(FileType.getNameWithoutExtension(ppName), boldFont_));
					p3.setIndentationLeft(12);
					p3.setSpacingBefore(12);
					s1.add(p3);
					List list2 = new List(12);
					list2.setListSymbol("\u2022");
					list2.setIndentationLeft(24);
					list2.add(createListItem("Included in RFORT: ", resultSet.getBoolean("included_in_rfort") ? "Yes" : "No", true));
					list2.add(createListItem("Stress factor: ", format1_.format(resultSet.getDouble("stress_factor")), true));

					// add material info
					getMaterialNames.setString(1, ppName);
					try (ResultSet resultSet1 = getMaterialNames.executeQuery()) {
						while (resultSet1.next()) {
							String stressType = resultSet1.getString("stress_type");
							String materialName = resultSet1.getString("material_name");
							if (stressType.equals(SaveRfortInfo.FATIGUE)) {
								list2.add(createListItem("Fatigue material: ", materialName, true));
							}
							else if (stressType.equals(SaveRfortInfo.PREFFAS)) {
								list2.add(createListItem("Preffas material: ", materialName, true));
							}
							else if (stressType.equals(SaveRfortInfo.LINEAR)) {
								list2.add(createListItem("Linear propagation material: ", materialName, true));
							}
						}
					}
					s1.add(list2);
				}
			}
		}
	}

	/**
	 * Creates list item.
	 *
	 * @param boldText
	 *            Bold text.
	 * @param normalText
	 *            Normal text.
	 * @param useBoldColorFont
	 *            True to use bold color font for bold text.
	 * @return Newly created list item.
	 */
	private ListItem createListItem(String boldText, String normalText, boolean useBoldColorFont) {
		Chunk boldChunk = new Chunk(boldText, useBoldColorFont ? boldColorFont_ : boldFont_);
		Chunk normalChunk = new Chunk(normalText, normalFont_);
		ListItem item = new ListItem();
		item.setLeading(20);
		item.add(boldChunk);
		item.add(normalChunk);
		return item;
	}
}
