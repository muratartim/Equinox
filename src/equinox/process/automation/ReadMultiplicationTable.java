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
package equinox.process.automation;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;

import equinox.data.LoadcaseFactor;
import equinox.process.EquinoxProcess;
import equinox.task.InternalEquinoxTask;
import equinox.utility.Utility;

/**
 * Class for read multiplication table process.
 *
 * @author Murat Artim
 * @date 20 Aug 2018
 * @time 00:29:19
 */
public class ReadMultiplicationTable implements EquinoxProcess<Collection<LoadcaseFactor>> {

	/** The owner task of this process. */
	private final InternalEquinoxTask<?> task;

	/** Input MUT file. */
	private final Path inputFile;

	/** Table column. */
	private final int tableColumn;

	/** Stress modification method. */
	private final String method;

	/**
	 * Creates read multiplication table process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input MUT file.
	 * @param tableColumn
	 *            Table column. Note that this is 0 based.
	 * @param method
	 *            Stress modification method.
	 */
	public ReadMultiplicationTable(InternalEquinoxTask<?> task, Path inputFile, int tableColumn, String method) {
		this.task = task;
		this.inputFile = inputFile;
		this.tableColumn = tableColumn;
		this.method = method;
	}

	@Override
	public Collection<LoadcaseFactor> start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// read input file
		task.updateMessage("Reading multiplication table input file...");

		// create list to store loadcase factors
		Collection<LoadcaseFactor> loadcaseFactors = new ArrayList<>();

		// get number of lines of multiplication table
		int allLines = Utility.countLines(inputFile, task);

		// create file reader
		try (BufferedReader reader = Files.newBufferedReader(inputFile, Charset.defaultCharset())) {

			// read file till the end
			int readLines = 0;
			String line;
			String delimiter = null;
			while ((line = reader.readLine()) != null) {

				// task cancelled
				if (task.isCancelled()) {
					break;
				}

				// increment read lines
				readLines++;

				// update progress
				task.updateProgress(readLines, allLines);

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
				String loadcaseNumber = null;
				double value = 1.0;
				for (String col : split) {

					// invalid value
					if (col == null || col.isEmpty()) {
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
						loadcaseNumber = col;
					}

					// loadcase factor value
					else if (index == tableColumn) {
						value = Double.parseDouble(col);
					}

					// increment index
					index++;
				}

				// no loadcase number found
				if (loadcaseNumber == null) {
					continue;
				}

				// create loadcase factor
				LoadcaseFactor loadcaseFactor = new LoadcaseFactor();
				loadcaseFactor.setLoadcaseNumber(loadcaseNumber);
				loadcaseFactor.setModifier(method, value);

				// add to factors
				loadcaseFactors.add(loadcaseFactor);
			}
		}

		// return loadcase factors
		return loadcaseFactors;
	}
}