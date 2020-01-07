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

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.ExternalStressSequence;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;

/**
 * Class for save external stress sequence as SIGMA task.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 12:06:02 PM
 */
public class SaveExternalStressSequenceAsSIGMA extends InternalEquinoxTask<Path> implements LongRunningTask, SingleInputTask<ExternalStressSequence>, AutomaticTaskOwner<Path> {

	/** Stress sequence to save. */
	private ExternalStressSequence sequence_;

	/** Output file. */
	private final File output_;

	/** Number of flights and flight types. */
	private int numFlights_, numFlightTypes_;

	/** Number of columns. */
	private static final int NUM_COLS = 10;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.000000E00");

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save external stress sequence as SIGMA task.
	 *
	 * @param sequence
	 *            Stress sequence to save. Can be null for automatic execution.
	 * @param output
	 *            Output file.
	 */
	public SaveExternalStressSequenceAsSIGMA(ExternalStressSequence sequence, File output) {
		sequence_ = sequence;
		output_ = output;
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
	public void setAutomaticInput(ExternalStressSequence input) {
		sequence_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Save external stress sequence to '" + output_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving external stress sequence to '" + output_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create output file writer
				try (BufferedWriter writer = Files.newBufferedWriter(output_.toPath(), Charset.defaultCharset())) {

					// write header
					writeHeader(statement, writer);

					// task cancelled
					if (isCancelled())
						return null;

					// write flights sequence
					writeFlightSequence(connection, statement, writer);

					// task cancelled
					if (isCancelled())
						return null;

					// write flights
					writeFlights(connection, statement, writer);
				}
			}
		}

		// return
		return output_.toPath();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get output file
			Path file = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(file, automaticTasks_, taskPanel_);
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

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Writes out flights.
	 *
	 * @param connection
	 *            Database connection.
	 * @param selectFlights
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlights(Connection connection, Statement selectFlights, BufferedWriter writer) throws Exception {

		// update progress info
		updateMessage("Writing flights...");

		// create statement for selecting peaks
		String sql = "select peak_val from ext_sth_peaks_" + sequence_.getID() + " where flight_id = ? order by peak_num asc";
		try (PreparedStatement selectPeaks = connection.prepareStatement(sql)) {

			// get flights
			sql = "select flight_id, flight_num, name, num_peaks, validity from ext_sth_flights where file_id = " + sequence_.getID() + " order by flight_num asc";
			try (ResultSet flights = selectFlights.executeQuery(sql)) {

				// loop over flights
				int flightCount = 0;
				while (flights.next()) {

					// task cancelled
					if (isCancelled())
						return;

					// update progress info
					String name = flights.getString("name");
					updateMessage("Writing flight '" + name + "'...");

					// update progress
					updateProgress(flightCount, numFlightTypes_);
					flightCount++;

					// write first line of flight info
					int flightNum = flights.getInt("flight_num") + 1;
					String line = "NUVOL ";
					line += String.format("%6s", flightNum);
					line += " ! FLIGHT ";
					line += name;
					writer.write(line);
					writer.newLine();

					// write second line of flight info
					line = "TITLE FLIGHT NB ";
					line += String.format("%6s", flightNum);
					line += " ! FLIGHT ";
					line += name;
					writer.write(line);
					writer.newLine();

					// write third line of flight info
					line = "NBOCCU ";
					line += String.format("%4s", (int) flights.getDouble("validity"));
					writer.write(line);
					writer.newLine();

					// write fourth line of flight info
					int numPeaks = flights.getInt("num_peaks");
					line = "NBVAL ";
					line += String.format("%6s", numPeaks);
					writer.write(line);
					writer.newLine();

					// write peaks
					writePeaks(selectPeaks, writer, flights, numPeaks);
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
		int numRows = numPeaks / NUM_COLS + (rem == 0 ? 0 : 1);
		String line = "";
		selectPeaks.setInt(1, flights.getInt("flight_id"));
		try (ResultSet peaks = selectPeaks.executeQuery()) {

			// loop over peaks
			int rowIndex = 0, colIndex = 0;
			while (peaks.next()) {

				// task cancelled
				if (isCancelled())
					return;

				// last row
				if (rowIndex == numRows - 1) {

					// add peak
					line += String.format("%14s", format_.format(peaks.getDouble("peak_val")));
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

					// add peak
					line += String.format("%14s", format_.format(peaks.getDouble("peak_val")));
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

		// pass 1 line
		writer.newLine();
	}

	/**
	 * Writes out flight sequence.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightSequence(Connection connection, Statement statement, BufferedWriter writer) throws Exception {

		// update progress info
		updateMessage("Writing flight sequence...");

		// write header
		writer.write("FLIGHTS SEQUENCE");
		writer.newLine();

		// prepare statement for getting flight numbers
		String sql = "select flight_num from ext_sth_flights where file_id = " + sequence_.getID() + " and name = ?";
		try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

			// get flight names
			sql = "select name from ext_fls_flights where sth_id = " + sequence_.getID() + " order by flight_num asc";
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// loop over flights
				int flightCount = 0;
				while (resultSet.next()) {

					// task cancelled
					if (isCancelled())
						return;

					// update progress
					updateProgress(flightCount, numFlights_);
					flightCount++;

					// get name
					String name = resultSet.getString("name");
					statement2.setString(1, name);

					// get flight numbers
					try (ResultSet resultSet2 = statement2.executeQuery()) {

						// loop over flight numbers
						while (resultSet2.next()) {
							String line = String.format("%6s", resultSet2.getInt("flight_num") + 1);
							line += " ! FLIGHT ";
							line += name;
							writer.write(line);
							writer.newLine();
						}
					}

				}
			}
		}

		// pass 1 line
		writer.newLine();
	}

	/**
	 * Writes out SIGMA file header.
	 *
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeHeader(Statement statement, BufferedWriter writer) throws Exception {

		// update progress info
		updateMessage("Writing SIGMA file header...");

		// write total number of flights
		String sql = "select sum(validity) as totalFlights from ext_sth_flights where file_id = " + sequence_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				String line = "NBVOL ";
				numFlights_ = (int) resultSet.getDouble("totalFlights");
				line += String.format("%6s", numFlights_);
				line += " ! TOTAL NUMBER OF FLIGHTS";
				writer.write(line);
				writer.newLine();
			}
		}

		// write total number of flight types
		sql = "select num_flights from ext_sth_files where file_id = " + sequence_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				String line = "NBTYPEVOL ";
				numFlightTypes_ = resultSet.getInt("num_flights");
				line += String.format("%6s", numFlightTypes_);
				line += " ! TOTAL NUMBER OF TYPE FLIGHTS";
				writer.write(line);
				writer.newLine();
			}
		}

		// pass 1 line
		writer.newLine();
	}
}
