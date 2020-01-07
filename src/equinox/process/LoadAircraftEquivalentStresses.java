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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.data.fileType.AircraftEquivalentStresses;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.AircraftLinearEquivalentStress;
import equinox.data.fileType.AircraftPreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.AircraftEquivalentStressType;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask;
import equinox.utility.Utility;
import jxl.Sheet;
import jxl.Workbook;

/**
 * Class for load A/C model equivalent stresses process.
 *
 * @author Murat Artim
 * @date Sep 4, 2015
 * @time 2:31:34 PM
 */
public class LoadAircraftEquivalentStresses implements EquinoxProcess<SpectrumItem> {

	/** The owner task of this process. */
	private final InternalEquinoxTask<?> task_;

	/** Input MS Excel file. */
	private final Path inputFile_;

	/** Equivalent stresses folder. */
	private final AircraftEquivalentStresses folder_;

	/** Equivalent stress type. */
	private final AircraftEquivalentStressType stressType_;

	/**
	 * Creates load A/C model equivalent stresses process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input MS Excel file.
	 * @param stressType
	 *            Equivalent stress type.
	 * @param folder
	 *            Equivalent stresses folder.
	 */
	public LoadAircraftEquivalentStresses(InternalEquinoxTask<?> task, Path inputFile, AircraftEquivalentStressType stressType, AircraftEquivalentStresses folder) {
		task_ = task;
		inputFile_ = inputFile;
		stressType_ = stressType;
		folder_ = folder;
	}

	@Override
	public SpectrumItem start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// create equivalent stresses table (if necessary)
		createEquivalentStressesTable(connection, folder_.getID());

		// task cancelled
		if (task_.isCancelled())
			return null;

		// create equivalent stress
		SpectrumItem eqStress = createEquivalentStress(connection);

		// task cancelled
		if (task_.isCancelled())
			return null;

		// get file type
		FileType fileType = FileType.getFileType(inputFile_.toFile());

		// XLS file
		if (fileType.equals(FileType.XLS))
			loadFromXLS(connection, eqStress);

		// EQS file
		else if (fileType.equals(FileType.EQS))
			loadFromEQS(connection, eqStress);

		// task cancelled
		if (task_.isCancelled())
			return null;

		// return equivalent stress
		return eqStress;
	}

	/**
	 * Loads equivalent stresses from XLS file.
	 *
	 * @param connection
	 *            Database connection.
	 * @param eqStress
	 *            Equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadFromXLS(Connection connection, SpectrumItem eqStress) throws Exception {

		// initialize variables
		Workbook workbook = null;

		try {

			// get workbook
			workbook = Workbook.getWorkbook(inputFile_.toFile());

			// get sheet
			Sheet sheet = workbook.getSheet(0);

			// task cancelled
			if (task_.isCancelled())
				return;

			// update info
			task_.updateMessage("Loading equivalent stresses...");
			boolean anyAdded = false;

			// prepare statement for inserting stresses
			String sql = "insert into AC_EQ_STRESSES_" + folder_.getID() + "(id, mission, eid, stress) values(?, ?, ?, ?)";
			try (PreparedStatement insertToStresses = connection.prepareStatement(sql)) {

				// set ID
				insertToStresses.setInt(1, eqStress.getID());

				// prepare statement for selecting element IDs
				sql = "select eid from ELEMENTS_" + folder_.getID() + " where eid = ?";
				try (PreparedStatement selectElement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

					// loop over rows
					int rows = sheet.getRows();
					for (int i = 1; i < rows; i++) {

						// task cancelled
						if (task_.isCancelled())
							return;

						// update info
						task_.updateProgress(i, rows);

						// get EID
						int eid = Integer.parseInt(sheet.getCell(1, i).getContents().trim());

						// check if element ID exists in the model
						selectElement.setInt(1, eid);
						try (ResultSet resultSet = selectElement.executeQuery()) {

							// unknown element ID
							if (!resultSet.last())
								continue;
						}

						// get other columns
						String mission = sheet.getCell(0, i).getContents().trim();
						double stress = Double.parseDouble(sheet.getCell(2, i).getContents().trim());

						// insert into stresses
						insertToStresses.setString(2, mission);
						insertToStresses.setInt(3, eid);
						insertToStresses.setDouble(4, stress);
						insertToStresses.executeUpdate();
						anyAdded = true;
					}
				}
			}

			// no stress added
			if (!anyAdded)
				throw new Exception("No equivalent stress added to A/C model. Please check element IDs against A/C model element IDs.");
		}

		// close workbook
		finally {
			if (workbook != null)
				workbook.close();
		}
	}

	/**
	 * Loads equivalent stresses from EQS file.
	 *
	 * @param connection
	 *            Database connection.
	 * @param eqStress
	 *            Equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadFromEQS(Connection connection, SpectrumItem eqStress) throws Exception {

		// get number of lines of file
		task_.updateMessage("Getting EQS file size...");
		int allLines = Utility.countLines(inputFile_, task_);
		int readLines = 0;

		// task cancelled
		if (task_.isCancelled())
			return;

		// update info
		task_.updateMessage("Loading equivalent stresses...");
		boolean anyAdded = false;

		// prepare statement for inserting stresses
		String sql = "insert into AC_EQ_STRESSES_" + folder_.getID() + "(id, mission, eid, stress) values(?, ?, ?, ?)";
		try (PreparedStatement insertToStresses = connection.prepareStatement(sql)) {

			// set ID
			insertToStresses.setInt(1, eqStress.getID());

			// prepare statement for selecting element IDs
			sql = "select eid from ELEMENTS_" + folder_.getID() + " where eid = ?";
			try (PreparedStatement selectElement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

				// create file reader
				try (BufferedReader reader = Files.newBufferedReader(inputFile_, Charset.defaultCharset())) {

					// read file till the end
					String line;
					while ((line = reader.readLine()) != null) {

						// task cancelled
						if (task_.isCancelled())
							return;

						// update info
						task_.updateProgress(readLines, allLines);
						readLines++;

						// trim line
						line = line.trim();

						// empty line
						if (line.isEmpty())
							continue;

						// comment
						if (line.startsWith("#"))
							continue;

						// get element ID
						String[] split = line.split("\t");
						int eid = Integer.parseInt(split[1].trim());

						// check if element ID exists in the model
						selectElement.setInt(1, eid);
						try (ResultSet resultSet = selectElement.executeQuery()) {

							// unknown element ID
							if (!resultSet.last())
								continue;
						}

						// get mission and stress
						String mission = split[0].trim();
						double stress = Double.parseDouble(split[2].trim());

						// insert into stresses
						insertToStresses.setString(2, mission);
						insertToStresses.setInt(3, eid);
						insertToStresses.setDouble(4, stress);
						insertToStresses.executeUpdate();
						anyAdded = true;
					}
				}
			}
		}

		// no stress added
		if (!anyAdded)
			throw new Exception("No equivalent stress added to A/C model. Please check element IDs against A/C model element IDs.");

	}

	/**
	 * Creates and returns A/C fatigue equivalent stress.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Newly created A/C fatigue equivalent stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private SpectrumItem createEquivalentStress(Connection connection) throws Exception {

		// update info
		task_.updateMessage("Creating aircraft fatigue equivalent stress in database...");

		// initialize
		SpectrumItem eqStress = null;

		// create statement
		String sql = "insert into AC_EQ_STRESS_NAMES_" + folder_.getID() + "(name, stress_type) values(?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// get stress name
			String stressName = Utility.correctFileName(FileType.getNameWithoutExtension(inputFile_));

			// set parameters
			update.setString(1, stressName);
			update.setString(2, stressType_.toString());
			update.executeUpdate();

			// create equivalent stress
			try (ResultSet resultSet = update.getGeneratedKeys()) {
				if (resultSet.next()) {

					// fatigue
					if (stressType_.equals(AircraftEquivalentStressType.FATIGUE_EQUIVALENT_STRESS))
						eqStress = new AircraftFatigueEquivalentStress(resultSet.getBigDecimal(1).intValue(), stressName);

					// preffas
					else if (stressType_.equals(AircraftEquivalentStressType.PREFFAS_PROPAGATION_EQUIVALENT_STRESS))
						eqStress = new AircraftPreffasEquivalentStress(resultSet.getBigDecimal(1).intValue(), stressName);

					// linear
					else if (stressType_.equals(AircraftEquivalentStressType.LINEAR_PROPAGATION_EQUIVALENT_STRESS))
						eqStress = new AircraftLinearEquivalentStress(resultSet.getBigDecimal(1).intValue(), stressName);
				}
			}
		}

		// return stress
		return eqStress;
	}

	/**
	 * Creates equivalent stresses table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param modelID
	 *            A/C model ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void createEquivalentStressesTable(Connection connection, int modelID) throws Exception {

		// update info
		task_.updateMessage("Creating equivalent stresses table...");

		// check if table already exists
		DatabaseMetaData dbmtadta = connection.getMetaData();
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "AC_EQ_STRESS_NAMES_" + modelID, null)) {
			while (resultSet.next())
				return;
		}

		// create table and index
		try (Statement statement = connection.createStatement()) {

			// create equivalent stress names table
			String sql = "CREATE TABLE AURORA.AC_EQ_STRESS_NAMES_" + modelID;
			sql += "(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), NAME VARCHAR(100) NOT NULL, STRESS_TYPE VARCHAR(50) NOT NULL, PRIMARY KEY(ID))";
			statement.executeUpdate(sql);

			// create equivalent stress names table indices
			statement.executeUpdate("CREATE INDEX AC_EQ_STRESS_NAMES_INDEX_" + modelID + " ON AURORA.AC_EQ_STRESS_NAMES_" + modelID + "(ID)");

			// create equivalent stresses table
			sql = "CREATE TABLE AURORA.AC_EQ_STRESSES_" + modelID;
			sql += "(ID INT NOT NULL, MISSION VARCHAR(50) NOT NULL, EID INT NOT NULL, STRESS DOUBLE NOT NULL, ";
			sql += "UNIQUE(ID, EID, MISSION))";
			statement.executeUpdate(sql);

			// create groups table indices
			statement.executeUpdate("CREATE INDEX ACEQ_ID_INDEX_" + modelID + " ON AURORA.AC_EQ_STRESSES_" + modelID + "(ID)");
			statement.executeUpdate("CREATE INDEX ACEQ_EID_INDEX_" + modelID + " ON AURORA.AC_EQ_STRESSES_" + modelID + "(EID)");
			statement.executeUpdate("CREATE INDEX ACEQ_MISSION_INDEX_" + modelID + " ON AURORA.AC_EQ_STRESSES_" + modelID + "(MISSION)");
		}
	}
}
