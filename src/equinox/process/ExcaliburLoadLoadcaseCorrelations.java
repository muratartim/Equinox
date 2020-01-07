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

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import equinox.data.input.ExcaliburInput;
import equinox.task.Excalibur;
import equinox.utility.Utility;
import jxl.Sheet;
import jxl.Workbook;

/**
 * Class for Excalibur load loadcase correlations process.
 *
 * @author Murat Artim
 * @date 30 Nov 2017
 * @time 11:01:12
 */
public class ExcaliburLoadLoadcaseCorrelations implements EquinoxProcess<String[]> {

	/** Correlation attribute index. */
	public static final int SECTION = 0, MISSION = 1;

	/** The owner task of this process. */
	private final Excalibur owner;

	/** Analysis input. */
	private final ExcaliburInput input;

	/** Excalibur analysis table names. */
	private final String[] tableNames;

	/**
	 * Creates Excalibur load loadcase correlations process.
	 *
	 * @param owner
	 *            The owner task of this process.
	 * @param input
	 *            Analysis input.
	 * @param tableNames
	 *            Excalibur analysis table names.
	 */
	public ExcaliburLoadLoadcaseCorrelations(Excalibur owner, ExcaliburInput input, String[] tableNames) {
		this.owner = owner;
		this.input = input;
		this.tableNames = tableNames;
	}

	/**
	 * Returns owner task.
	 *
	 * @return The owner task.
	 */
	public Excalibur getOwner() {
		return owner;
	}

	/**
	 * Returns analysis input.
	 *
	 * @return Analysis input.
	 */
	public ExcaliburInput getInput() {
		return input;
	}

	/**
	 * Returns analysis table names.
	 *
	 * @return Analysis table names.
	 */
	public String[] getTableNames() {
		return tableNames;
	}

	@Override
	public String[] start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// load attributes table
		String[] sectionMission = loadAttributesTable(connection);

		// task cancelled
		if (owner.isCancelled())
			return null;

		// load loadcase keys file
		loadLoadcaseKeysFile(connection, sectionMission[SECTION]);

		// task cancelled
		if (owner.isCancelled())
			return null;

		// return aircraft section and fatigue mission
		return sectionMission;
	}

	/**
	 * Loads attributes table into database.
	 *
	 * @param connection
	 *            Database connections.
	 * @return Aircraft section and fatigue mission.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String[] loadAttributesTable(Connection connection) throws Exception {

		// update progress info
		owner.updateMessage("Loading attributes table...");
		owner.updateProgress(0, 100);

		// initialize variables
		String[] sectionMission = new String[2];
		Workbook workbook = null;

		try {

			// create database statement
			String sql = "insert into " + tableNames[Excalibur.XLS] + "(section, mission, ref_intensity, load_factor, issy_code, event_name, segment, load_type, load_criteria, event_comment) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement statement = connection.prepareStatement(sql)) {

				// get workbook
				workbook = Workbook.getWorkbook(input.getAttributesTable());

				// get worksheet
				Sheet sheet = workbook.getSheet(input.getAttributesTableSheet());

				// cannot find the worksheet
				if (sheet == null)
					throw new Exception("Cannot find worksheet '" + sheet + "' in attributes table excel file '" + input.getAttributesTable().getName() + "'.");

				// set section
				sectionMission[SECTION] = sheet.getCell(9, 3).getContents().trim();
				statement.setString(1, sectionMission[SECTION]);

				// set mission
				sectionMission[MISSION] = sheet.getCell(9, 4).getContents().trim();
				statement.setString(2, sectionMission[MISSION]);

				// set location
				int startRow = 7, endRow = sheet.getRows() - 1;

				// loop over rows
				for (int i = startRow; i <= endRow; i++) {

					// task cancelled
					if (owner.isCancelled())
						return null;

					// empty issy code
					String issyCode = sheet.getCell(3, i).getContents();
					if ((issyCode == null) || issyCode.trim().isEmpty()) {
						continue;
					}

					// get reference intensity
					String refIntensity = sheet.getCell(1, i).getContents();
					if ((refIntensity == null) || refIntensity.isEmpty()) {
						statement.setNull(3, java.sql.Types.VARCHAR);
					}
					else {
						statement.setString(3, refIntensity.trim());
					}

					// get load factor
					statement.setDouble(4, Double.parseDouble(sheet.getCell(2, i).getContents().trim()));

					// get issy code
					statement.setString(5, issyCode.trim());

					// get event name
					String eventName = sheet.getCell(5, i).getContents().trim();
					statement.setString(6, eventName);

					// get segment
					String segment = sheet.getCell(6, i).getContents().trim();
					if (eventName.equals("PressLC") || sheet.getCell(0, i).getContents().trim().equals("Pressure")) {
						statement.setString(7, segment);
					}
					else if (segment.contains("|")) {
						String[] split = segment.split("\\|");
						int segNum1 = Integer.parseInt(split[0].trim());
						split[0] = segNum1 < 10 ? "S0" + segNum1 : "S" + segNum1;
						int segNum2 = Integer.parseInt(split[1].trim());
						split[1] = segNum2 < 10 ? "S0" + segNum2 : "S" + segNum2;
						segment = split[0] + "|" + split[1];
						statement.setString(7, segment);
					}
					else {
						int segmentNum = Integer.parseInt(segment);
						segment = segmentNum < 10 ? "S0" + segmentNum : "S" + segmentNum;
						statement.setString(7, segment);
					}

					// get load type
					statement.setString(8, sheet.getCell(7, i).getContents().trim());

					// get load criteria
					statement.setString(9, sheet.getCell(8, i).getContents().trim());

					// get event comment
					statement.setString(10, sheet.getCell(9, i).getContents().trim());

					// execute update
					statement.executeUpdate();

					// update progress
					owner.updateProgress(i, endRow);
				}
			}

			// return section and mission
			return sectionMission;
		}

		// close workbook
		finally {
			if (workbook != null) {
				workbook.close();
			}
		}
	}

	/**
	 * Loads loadcase keys file into database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param section
	 *            Aircraft section.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadLoadcaseKeysFile(Connection connection, String section) throws Exception {

		// get path to loadcase keys file
		Path lck = input.getLoadcaseKeysFile().toPath();

		// get number of lines
		int numLines = Utility.countLines(lck, owner);

		// update progress info
		owner.updateMessage("Loading loadcase keys file...");
		owner.updateProgress(0, 100);

		// prepare statement
		String sql = "insert into " + tableNames[Excalibur.LCK] + "(section, mission, segment, lc_name, db_name, family_name, tx, ty, tz, mx, my, mz, lc_num, load_type) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {

			// create file reader
			try (BufferedReader reader = Files.newBufferedReader(lck, Charset.defaultCharset())) {

				// read file till the end
				int lineCount = 0;
				String line;
				while ((line = reader.readLine()) != null) {

					// task cancelled
					if (owner.isCancelled())
						return;

					// update progress
					lineCount++;
					owner.updateProgress(lineCount, numLines);

					// trim line
					line = line.trim();

					// empty line
					if (line.isEmpty()) {
						continue;
					}

					// comment line
					if (line.startsWith("#")) {
						continue;
					}

					// set section
					statement.setString(1, section);

					// split line from spaces
					String[] split = line.split(" ");

					// loop over columns
					int index = 0;
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

						// mission
						if (index == 0) {
							statement.setString(2, col);
						}

						// segment
						else if (index == 1) {
							statement.setString(3, col);
						}

						// load case name
						else if (index == 2) {
							statement.setString(4, col);
						}

						// database name
						else if (index == 3) {
							statement.setString(5, col);
						}

						// family name
						else if (index == 4) {
							statement.setString(6, col);
						}

						// tx
						else if (index == 5) {
							statement.setDouble(7, Double.parseDouble(col));
						}

						// ty
						else if (index == 6) {
							statement.setDouble(8, Double.parseDouble(col));
						}

						// tz
						else if (index == 7) {
							statement.setDouble(9, Double.parseDouble(col));
						}

						// mx
						else if (index == 8) {
							statement.setDouble(10, Double.parseDouble(col));
						}

						// my
						else if (index == 9) {
							statement.setDouble(11, Double.parseDouble(col));
						}

						// mz
						else if (index == 10) {
							statement.setDouble(12, Double.parseDouble(col));
						}

						// load case number
						else if (index == 11) {
							if (col.equals("na")) {
								statement.setNull(13, java.sql.Types.INTEGER);
							}
							else {
								statement.setInt(13, Integer.parseInt(col));
							}
						}

						// load type
						else if (index == 12) {
							statement.setString(14, col);
						}

						// increment index
						index++;
					}

					// execute update
					statement.executeUpdate();
				}
			}
		}
	}
}
