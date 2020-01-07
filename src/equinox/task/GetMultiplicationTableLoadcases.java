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

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.LoadcaseFactorsPopup;
import equinox.data.LoadcaseItem;
import equinox.data.fileType.Spectrum;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.Utility;

/**
 * Class for get multiplication table loadcases task.
 *
 * @author Murat Artim
 * @date Dec 22, 2015
 * @time 12:48:23 AM
 */
public class GetMultiplicationTableLoadcases extends InternalEquinoxTask<HashMap<LoadcaseItem, ArrayList<Double>>> implements ShortRunningTask {

	/** Requesting panel. */
	private final LoadcaseFactorsPopup panel_;

	/** Spectrum. */
	private final Spectrum spectrum_;

	/** Path to multiplication table. */
	private final Path multiplicationTable_;

	/**
	 * Creates get multiplication table loadcases task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param spectrum
	 *            Spectrum.
	 * @param multiplicationTable
	 *            Path to multiplication table.
	 */
	public GetMultiplicationTableLoadcases(LoadcaseFactorsPopup panel, Spectrum spectrum, Path multiplicationTable) {
		panel_ = panel;
		spectrum_ = spectrum;
		multiplicationTable_ = multiplicationTable;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Get loadcases from multiplication table";
	}

	@Override
	protected HashMap<LoadcaseItem, ArrayList<Double>> call() throws Exception {

		// update progress info
		updateTitle("Retrieving loadcases...");
		updateMessage("Please wait...");

		// get number of lines of multiplication table
		int txtID = spectrum_.getTXTFileID();
		int convID = spectrum_.getConversionTableID();
		int allLines = Utility.countLines(multiplicationTable_, this);

		// initialize map
		HashMap<LoadcaseItem, ArrayList<Double>> loadcases = new HashMap<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement to get event name and increment number from TXT codes
			String sql = "select flight_phase, increment_num from txt_codes where file_id = " + txtID + " and issy_code = ?";
			try (PreparedStatement statement1 = connection.prepareStatement(sql)) {

				// prepare statement to get loadcase comments from conversion table
				sql = "select comment from xls_comments where file_id = " + convID + " and issy_code = ? and fue_translated like ?";
				try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

					// create file reader
					try (BufferedReader reader = Files.newBufferedReader(multiplicationTable_, Charset.defaultCharset())) {

						// read file till the end
						int readLines = 0;
						String line;
						String delimiter = null;
						while ((line = reader.readLine()) != null) {

							// task cancelled
							if (isCancelled()) {
								break;
							}

							// increment read lines
							readLines++;

							// update progress
							updateProgress(readLines, allLines);

							// skip comment lines
							if (readLines < 2) {
								continue;
							}

							// comment line
							if (line.startsWith("#")) {
								continue;
							}

							// set column delimiter
							if (delimiter == null) {
								delimiter = line.trim().contains("\t") ? "\t" : " ";
							}

							// split line
							String[] split = line.trim().split(delimiter);

							// loop over columns
							int index = 0;
							Integer loadcaseNumber = null;
							ArrayList<Double> loadcaseFactors = null;
							for (String col : split) {

								// invalid value
								if ((col == null) || col.isEmpty()) {
									continue;
								}

								// trim spaces
								col = col.trim();

								// invalid value
								if (col.isEmpty()) {
									continue;
								}

								// loadcase number
								if (index == 0) {
									loadcaseNumber = Integer.parseInt(col);
								}
								else {
									if (loadcaseFactors == null) {
										loadcaseFactors = new ArrayList<>();
									}
									loadcaseFactors.add(Double.parseDouble(col));
								}

								// increment index
								index++;
							}

							// no loadcase info found
							if ((loadcaseNumber == null) || (loadcaseFactors == null) || loadcaseFactors.isEmpty()) {
								continue;
							}

							// create loadcase
							LoadcaseItem loadcase = new LoadcaseItem();
							loadcase.setLoadcaseNumber(loadcaseNumber.toString());

							// set event name and increment number
							statement1.setString(1, loadcase.getLoadcaseNumber());
							try (ResultSet resultSet = statement1.executeQuery()) {
								while (resultSet.next()) {
									loadcase.setEventName(resultSet.getString("flight_phase"));
									loadcase.setIsOneg(resultSet.getInt("increment_num") == 0);
								}
							}

							// set loadcase comments
							statement2.setString(1, loadcase.getLoadcaseNumber());
							statement2.setString(2, "%" + loadcase.getEventName() + "%");
							try (ResultSet resultSet = statement2.executeQuery()) {
								while (resultSet.next()) {
									loadcase.setComments(resultSet.getString("comment"));
								}
							}

							// add to loadcases
							loadcases.put(loadcase, loadcaseFactors);
						}
					}
				}
			}
		}

		// return map
		return loadcases;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set events
		try {
			panel_.setMultiplicationTableLoadcases(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
