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
package equinox.process;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import equinox.Equinox;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.input.EquivalentStressInput;
import equinox.task.InternalEquinoxTask;

/**
 * Class for save external stress sequence as STH process.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 11:29:00 AM
 */
public class SaveExternalSTH implements EquinoxProcess<ExternalStressSequence> {

	/** The owner task of this process. */
	private final InternalEquinoxTask<?> task_;

	/** Stress sequence to save. */
	private final ExternalStressSequence sequence_;

	/** Output file. */
	private final File output_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.00");

	/** Number of columns. */
	private static final int NUM_COLS = 8;

	/** Stress modification method. */
	private String stressModificationMethod_ = EquivalentStressInput.MULTIPLY;

	/** Stress modifier value. */
	private double stressModifier_ = 1.0;

	/** True if the negative stresses should be set to 0.0. */
	private boolean removeNegativeStresses_ = false;

	/**
	 * Creates save STH process.
	 *
	 * @param task
	 *            The owner task.
	 * @param sequence
	 *            Stress sequence.
	 * @param output
	 *            Output file.
	 */
	public SaveExternalSTH(InternalEquinoxTask<?> task, ExternalStressSequence sequence, File output) {
		task_ = task;
		sequence_ = sequence;
		output_ = output;
	}

	/**
	 * Sets whether the negative stresses should be set to zero.
	 *
	 * @param removeNegativeStresses
	 *            True if the negative stresses should be set to 0.0.
	 */
	public void setRemoveNegativeStresses(boolean removeNegativeStresses) {
		removeNegativeStresses_ = removeNegativeStresses;
	}

	/**
	 * Sets stress modifier. Note that, this is only used for external equivalent stress analysis.
	 *
	 * @param stressModifier
	 *            Stress modification value.
	 * @param stressModificationMethod
	 *            Stress modification method.
	 */
	public void setStressModifier(double stressModifier, String stressModificationMethod) {
		stressModificationMethod_ = stressModificationMethod;
		stressModifier_ = stressModifier;
	}

	/**
	 * Returns input stress sequence.
	 *
	 * @return Stress sequence.
	 */
	public ExternalStressSequence getStressSequence() {
		return sequence_;
	}

	/**
	 * Returns the output file.
	 *
	 * @return The output file.
	 */
	public File getOutputFile() {
		return output_;
	}

	/**
	 * Returns true if negative stresses are set to 0.0.
	 *
	 * @return True if negative stresses are set to 0.0.
	 */
	public boolean getRemoveNegativeStresses() {
		return removeNegativeStresses_;
	}

	@Override
	public ExternalStressSequence start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// create output file writer
		try (BufferedWriter writer = Files.newBufferedWriter(output_.toPath(), Charset.defaultCharset())) {

			// write header
			int numFlights = writeHeader(connection, writer);

			// write flights
			writeFlights(connection, writer, numFlights);
		}
		return sequence_;
	}

	/**
	 * Writes out the flight peaks in STH file format.
	 *
	 * @param connection
	 *            Database connection.
	 * @param writer
	 *            File writer.
	 * @param numFlights
	 *            Number of flights.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlights(Connection connection, BufferedWriter writer, int numFlights) throws Exception {

		// update progress info
		task_.updateMessage("Writing STH flights...");

		// create statement for selecting flights
		try (Statement selectFlights = connection.createStatement()) {

			// create statement for selecting peaks
			String sql = "select peak_val from ext_sth_peaks_" + sequence_.getID() + " where flight_id = ? order by peak_num asc";
			try (PreparedStatement selectPeaks = connection.prepareStatement(sql)) {

				// get flights
				sql = "select flight_id, name, severity, num_peaks, validity, block_size from ext_sth_flights where file_id = " + sequence_.getID() + " order by flight_num asc";
				try (ResultSet flights = selectFlights.executeQuery(sql)) {

					// loop over flights
					int flightCount = 0;
					while (flights.next()) {

						// task cancelled
						if (task_.isCancelled())
							return;

						// update progress info
						String name = flights.getString("name");
						task_.updateMessage("Writing STH flight '" + name + "'...");

						// update progress
						task_.updateProgress(flightCount, numFlights);
						flightCount++;

						// write first line of flight info
						String line = String.format("%10s", format_.format(flights.getDouble("validity")));
						line += String.format("%10s", format_.format(flights.getDouble("block_size")));
						writer.write(line);
						writer.newLine();

						// write second line of flight info
						int numPeaks = flights.getInt("num_peaks");
						line = String.format("%10s", Integer.toString(numPeaks));
						for (int i = 0; i < 62; i++) {
							line += " ";
						}
						String severity = flights.getString("severity");

						// if severity doesn't exist (for SIGMA inputs), assigns a dummy value
						severity = severity.isEmpty() ? "AHAAHHHCHA" : severity;
						line += (name.startsWith("TF_") ? name.substring(3) : name) + " " + severity;
						writer.write(line);
						writer.newLine();

						// write peaks
						writePeaks(selectPeaks, writer, flights, numPeaks);
					}
				}
			}
		}
	}

	/**
	 * Writes peaks of a flight.
	 *
	 * @param selectPeaks
	 *            Database statement for selecting peaks.
	 * @param writer
	 *            File writer.
	 * @param flights
	 *            Flight to write.
	 * @param numPeaks
	 *            NUmber of peaks of the flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writePeaks(PreparedStatement selectPeaks, BufferedWriter writer, ResultSet flights, int numPeaks) throws Exception {

		// get peaks
		int rem = numPeaks % NUM_COLS;
		int numRows = (numPeaks / NUM_COLS) + (rem == 0 ? 0 : 1);
		String line = "";
		selectPeaks.setInt(1, flights.getInt("flight_id"));
		try (ResultSet peaks = selectPeaks.executeQuery()) {

			// loop over peaks
			int rowIndex = 0, colIndex = 0;
			while (peaks.next()) {

				// task cancelled
				if (task_.isCancelled())
					return;

				// last row
				if (rowIndex == (numRows - 1)) {

					// get peak value
					double peakVal = peaks.getDouble("peak_val");
					if (stressModificationMethod_.equals(EquivalentStressInput.MULTIPLY)) {
						peakVal *= stressModifier_;
					}
					else if (stressModificationMethod_.equals(EquivalentStressInput.ADD)) {
						peakVal += stressModifier_;
					}
					else if (stressModificationMethod_.equals(EquivalentStressInput.SET)) {
						peakVal = stressModifier_;
					}

					// remove negative stress (if requested)
					if (removeNegativeStresses_ && (peakVal < 0.0)) {
						peakVal = 0.0;
					}

					// add peak
					line += String.format("%10s", format_.format(peakVal));
					colIndex++;

					// last column
					if (colIndex == (rem == 0 ? NUM_COLS : rem)) {
						writer.write(line);
						writer.newLine();
						line = "";
						colIndex = 0;
						rowIndex++;
					}
				}

				// other rows
				else {

					// get peak value
					double peakVal = peaks.getDouble("peak_val");
					if (stressModificationMethod_.equals(EquivalentStressInput.MULTIPLY)) {
						peakVal *= stressModifier_;
					}
					else if (stressModificationMethod_.equals(EquivalentStressInput.ADD)) {
						peakVal += stressModifier_;
					}
					else if (stressModificationMethod_.equals(EquivalentStressInput.SET)) {
						peakVal = stressModifier_;
					}

					// remove negative stress (if requested)
					if (removeNegativeStresses_ && (peakVal < 0.0)) {
						peakVal = 0.0;
					}

					// add peak
					line += String.format("%10s", format_.format(peakVal));
					colIndex++;

					// last column
					if (colIndex == NUM_COLS) {
						writer.write(line);
						writer.newLine();
						line = "";
						colIndex = 0;
						rowIndex++;
					}
				}
			}
		}
	}

	/**
	 * Writes STH header information.
	 *
	 * @param connection
	 *            Database connection.
	 * @param writer
	 *            File writer.
	 * @return Number of flights.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int writeHeader(Connection connection, BufferedWriter writer) throws Exception {

		// update progress info
		task_.updateMessage("Writing STH file header...");

		// write header info
		writer.write(" STH Generated by Equinox Version " + Equinox.VERSION.toString());
		writer.newLine();
		writer.write(" Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
		writer.newLine();
		int numFlights = writeSequenceInfo(connection, writer);
		writeFlightInfo(connection, writer);

		// return number of flights
		return numFlights;
	}

	/**
	 * Extracts and writes out flight info.
	 *
	 * @param connection
	 *            Database connection.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightInfo(Connection connection, BufferedWriter writer) throws Exception {

		// create statement
		try (Statement statement = connection.createStatement()) {

			// initialize line
			String line = "";

			// get longest flight
			line = getHighestLowest(statement, line, " Longest flight", "Number of peaks", "num_peaks", true);

			// get shortest flight
			line = getHighestLowest(statement, line + ", ", "Shortest flight", "Number of peaks", "num_peaks", false);

			// get highest peak flight
			line = getHighestLowest(statement, line + ", ", "Flight with highest peak", "Highest peak", "max_val", true);

			// get lowest peak flight
			line = getHighestLowest(statement, line + ", ", "Flight with lowest peak", "Lowest peak", "min_val", false);

			// get highest validity flight
			line = getHighestLowest(statement, line + ", ", "Flight with highest validity", "Highest validity", "validity", true);

			// get lowest validity flight
			line = getHighestLowest(statement, line + ", ", "Flight with lowest validity", "Lowest validity", "validity", false);

			// reset statement
			statement.setMaxRows(0);

			// write line
			writer.write(line);
			writer.newLine();
		}
	}

	/**
	 * Gets the flight with the highest or lowest peak of the input STH file from the database.
	 *
	 * @param statement
	 *            SQL statement.
	 * @param line
	 *            Line to modify.
	 * @param label1
	 *            First label.
	 * @param label2
	 *            Second label.
	 * @param colName
	 *            Database column name.
	 * @param isDesc
	 *            True if descending order.
	 * @return The modified line.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getHighestLowest(Statement statement, String line, String label1, String label2, String colName, boolean isDesc) throws Exception {
		String sql = "select name, " + colName + " from ext_sth_flights where file_id = " + sequence_.getID() + " order by " + colName + (isDesc ? " desc" : "");
		statement.setMaxRows(1);
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				line += label1 + ": " + resultSet.getString("name");
				line += " (" + label2 + ": " + format_.format(resultSet.getDouble(colName)) + ")";
			}
		}
		return line;
	}

	/**
	 * Writes stress sequence information.
	 *
	 * @param connection
	 *            Database connection.
	 * @param writer
	 *            File writer.
	 * @return Number of flights.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int writeSequenceInfo(Connection connection, BufferedWriter writer) throws Exception {

		// initialize number of flights
		int numFlights = 0;

		// create statement
		try (Statement statement = connection.createStatement()) {
			String sql = "select num_flights from ext_sth_files where file_id = " + sequence_.getID();
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					numFlights = resultSet.getInt("num_flights");
					String info = " Number of flights: " + Integer.toString(numFlights) + ", ";
					info += "Total number of peaks: " + Integer.toString(getTotalNumberOfPeaks(connection)) + ", ";
					info += "Stress modifier: " + format_.format(stressModifier_) + " (" + stressModificationMethod_ + "), ";
					info += "Remove negative stresses: " + (removeNegativeStresses_ ? "Yes" : "No");
					writer.write(info);
					writer.newLine();
				}
			}
		}

		// return number of flights
		return numFlights;
	}

	/**
	 * Gets the total number of peaks of the input STH file from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @return The total number of peaks of the spectrum.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int getTotalNumberOfPeaks(Connection connection) throws Exception {
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery("select sum(num_peaks) as totalPeaks from ext_sth_flights where file_id = " + sequence_.getID())) {
				if (resultSet.next())
					return resultSet.getInt("totalPeaks");
			}
		}
		return 0;
	}
}
