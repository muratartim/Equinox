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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.Equinox;

/**
 * Class for update workspace task.
 *
 * @author Murat Artim
 * @date May 10, 2016
 * @time 5:34:52 PM
 */
public class UpdateWorkspace extends InternalEquinoxTask<Void> {

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Updating workspace...";
	}

	@Override
	protected Void call() throws Exception {

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// get database version
				double dbVersion = getDatabaseVersion(connection);

				// no update needed
				if (dbVersion == Equinox.VERSION.getNumber()) {
					connection.setAutoCommit(true);
					return null;
				}

				// no version table found
				boolean createVersionTable = dbVersion == -1;

				// update to v2.9
				if (dbVersion < 2.9) {
					dbVersion = updateTo29(connection);
				}

				// update to v3.0
				if (dbVersion < 3.0) {
					dbVersion = updateTo30(connection);
				}

				// update to v3.1
				if (dbVersion < 3.1) {
					dbVersion = updateTo31(connection);
				}

				// update to v3.3
				if (dbVersion < 3.3) {
					dbVersion = updateTo33(connection);
				}

				// update to v3.4
				if (dbVersion < 3.4) {
					dbVersion = updateTo34(connection);
				}

				// update to v3.5
				if (dbVersion < 3.5) {
					dbVersion = updateTo35(connection);
				}

				// update to v3.6
				if (dbVersion < 3.6) {
					dbVersion = updateTo36(connection);
				}

				// update to v3.7
				if (dbVersion < 3.7) {
					dbVersion = updateTo37(connection);
				}

				// update to v3.8
				if (dbVersion < 3.8) {
					dbVersion = updateTo38(connection);
				}

				// update to v3.9
				if (dbVersion < 3.9) {
					dbVersion = updateTo39(connection);
				}

				// update to v4.0
				if (dbVersion < 4.0) {
					dbVersion = updateTo40(connection);
				}

				// update version number
				updateVersionNumber(connection, createVersionTable, dbVersion);

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}

		// return
		return null;
	}

	/**
	 * Updates the database to version 4.0.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo40(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 4.0");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// remove not null constraint from event name columns of event modifier tables
			statement.executeUpdate("ALTER TABLE EVENT_MODIFIERS ALTER COLUMN EVENT_NAME NULL");
			statement.executeUpdate("ALTER TABLE DAM_ANGLE_EVENT_MODIFIERS ALTER COLUMN EVENT_NAME NULL");
			statement.executeUpdate("ALTER TABLE DAM_CONTRIBUTIONS_EVENT_MODIFIERS ALTER COLUMN EVENT_NAME NULL");
			statement.executeUpdate("ALTER TABLE DAM_CONTRIBUTION_EVENT_MODIFIERS ALTER COLUMN EVENT_NAME NULL");
			statement.executeUpdate("ALTER TABLE FLIGHT_DAM_CONTRIBUTIONS_EVENT_MODIFIERS ALTER COLUMN EVENT_NAME NULL");
		}

		// return new version number
		return 4.0;
	}

	/**
	 * Updates the database to version 3.9.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo39(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 3.9");

		// return new version number
		return 3.9;
	}

	/**
	 * Updates the database to version 3.8.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo38(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 3.8");

		// return new version number
		return 3.8;
	}

	/**
	 * Updates the database to version 3.7.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo37(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 3.7");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// drop material tables
			statement.executeUpdate("DROP TABLE AURORA.FATIGUE_MATERIALS");
			statement.executeUpdate("DROP TABLE AURORA.PREFFAS_MATERIALS");
			statement.executeUpdate("DROP TABLE AURORA.LINEAR_MATERIALS");

			// create new material tables
			statement.executeUpdate(
					"CREATE TABLE AURORA.FATIGUE_MATERIALS(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), NAME VARCHAR(500) NOT NULL, SPECIFICATION VARCHAR(500), LIBRARY_VERSION VARCHAR(500), FAMILY VARCHAR(500), ORIENTATION VARCHAR(500), CONFIGURATION VARCHAR(500), PAR_P DOUBLE NOT NULL, PAR_Q DOUBLE NOT NULL, PAR_M DOUBLE NOT NULL, ISAMI_VERSION VARCHAR(500) NOT NULL, UNIQUE(NAME, SPECIFICATION, LIBRARY_VERSION, ORIENTATION, CONFIGURATION, ISAMI_VERSION), PRIMARY KEY(ID))");
			statement.executeUpdate(
					"CREATE TABLE AURORA.PREFFAS_MATERIALS(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), NAME VARCHAR(500) NOT NULL, SPECIFICATION VARCHAR(500), LIBRARY_VERSION VARCHAR(500), FAMILY VARCHAR(500), ORIENTATION VARCHAR(500), CONFIGURATION VARCHAR(500), PAR_CEFF DOUBLE NOT NULL, PAR_M DOUBLE NOT NULL, PAR_A DOUBLE NOT NULL, PAR_B DOUBLE NOT NULL, PAR_C DOUBLE NOT NULL, PAR_FTU DOUBLE NOT NULL, PAR_FTY DOUBLE NOT NULL, ISAMI_VERSION VARCHAR(500) NOT NULL, UNIQUE(NAME, SPECIFICATION, LIBRARY_VERSION, ORIENTATION, CONFIGURATION, ISAMI_VERSION), PRIMARY KEY(ID))");
			statement.executeUpdate(
					"CREATE TABLE AURORA.LINEAR_MATERIALS(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), NAME VARCHAR(500) NOT NULL, SPECIFICATION VARCHAR(500), LIBRARY_VERSION VARCHAR(500), FAMILY VARCHAR(500), ORIENTATION VARCHAR(500), CONFIGURATION VARCHAR(500), PAR_CEFF DOUBLE NOT NULL, PAR_M DOUBLE NOT NULL, PAR_A DOUBLE NOT NULL, PAR_B DOUBLE NOT NULL, PAR_C DOUBLE NOT NULL, PAR_FTU DOUBLE NOT NULL, PAR_FTY DOUBLE NOT NULL, ISAMI_VERSION VARCHAR(500) NOT NULL, UNIQUE(NAME, SPECIFICATION, LIBRARY_VERSION, ORIENTATION, CONFIGURATION, ISAMI_VERSION), PRIMARY KEY(ID))");

			// create indexes
			statement.executeUpdate("CREATE INDEX SEARCH_FATIGUE_MATERIALS_ID ON AURORA.FATIGUE_MATERIALS(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PREFFAS_MATERIALS_ID ON AURORA.PREFFAS_MATERIALS(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_LINEAR_MATERIALS_ID ON AURORA.LINEAR_MATERIALS(ID)");

			// update material related tables
			statement.executeUpdate("ALTER TABLE maxdam_angles ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE ext_fatigue_equivalent_stresses ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE ext_linear_equivalent_stresses ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE ext_preffas_equivalent_stresses ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE fatigue_equivalent_stresses ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE linear_equivalent_stresses ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE preffas_equivalent_stresses ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE fast_fatigue_equivalent_stresses ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE fast_linear_equivalent_stresses ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE fast_preffas_equivalent_stresses ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE flight_dam_contributions ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
			statement.executeUpdate("ALTER TABLE dam_contributions ADD MATERIAL_ISAMI_VERSION VARCHAR(500) DEFAULT 'v9.5.0'");
		}

		// return new version number
		return 3.7;
	}

	/**
	 * Updates the database to version 3.6.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo36(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 3.6");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create excalibur stress sorting table
			statement.executeUpdate("CREATE TABLE AURORA.EXCALIBUR_ANALYSES(ID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), START_TIME TIMESTAMP NOT NULL, PRIMARY KEY(ID))");
		}

		// return new version number
		return 3.6;
	}

	/**
	 * Updates the database to version 3.5.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo35(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 3.5");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// add EID and material name columns to STF_FILES table
			statement.executeUpdate("ALTER TABLE STF_FILES ADD COLUMN EID VARCHAR(50)");
			statement.executeUpdate("ALTER TABLE STF_FILES ADD COLUMN FATIGUE_MATERIAL VARCHAR(500)");
			statement.executeUpdate("ALTER TABLE STF_FILES ADD COLUMN PREFFAS_MATERIAL VARCHAR(500)");
			statement.executeUpdate("ALTER TABLE STF_FILES ADD COLUMN LINEAR_MATERIAL VARCHAR(500)");

			// add material name columns to EXT_STH_FILES table
			statement.executeUpdate("ALTER TABLE EXT_STH_FILES ADD COLUMN FATIGUE_MATERIAL VARCHAR(500)");
			statement.executeUpdate("ALTER TABLE EXT_STH_FILES ADD COLUMN PREFFAS_MATERIAL VARCHAR(500)");
			statement.executeUpdate("ALTER TABLE EXT_STH_FILES ADD COLUMN LINEAR_MATERIAL VARCHAR(500)");
		}

		// return new version number
		return 3.5;
	}

	/**
	 * Updates the database to version 3.4.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo34(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 3.4");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// add name columns to equivalent stress tables
			statement.executeUpdate("ALTER TABLE FATIGUE_EQUIVALENT_STRESSES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Fatigue Eq. Stress'");
			statement.executeUpdate("ALTER TABLE PREFFAS_EQUIVALENT_STRESSES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Preffas Eq. Stress'");
			statement.executeUpdate("ALTER TABLE LINEAR_EQUIVALENT_STRESSES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Linear Prop. Eq. Stress'");

			// add name columns to external equivalent stress tables
			statement.executeUpdate("ALTER TABLE EXT_FATIGUE_EQUIVALENT_STRESSES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Fatigue Eq. Stress'");
			statement.executeUpdate("ALTER TABLE EXT_PREFFAS_EQUIVALENT_STRESSES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Preffas Eq. Stress'");
			statement.executeUpdate("ALTER TABLE EXT_LINEAR_EQUIVALENT_STRESSES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Linear Prop. Eq. Stress'");

			// add name columns to fast equivalent stress tables
			statement.executeUpdate("ALTER TABLE FAST_FATIGUE_EQUIVALENT_STRESSES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Fatigue Eq. Stress'");
			statement.executeUpdate("ALTER TABLE FAST_PREFFAS_EQUIVALENT_STRESSES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Preffas Eq. Stress'");
			statement.executeUpdate("ALTER TABLE FAST_LINEAR_EQUIVALENT_STRESSES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Linear Prop. Eq. Stress'");

			// add name column to damage contributions tables
			statement.executeUpdate("ALTER TABLE DAM_CONTRIBUTIONS ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Loadcase Damage Contributions'");
			statement.executeUpdate("ALTER TABLE FLIGHT_DAM_CONTRIBUTIONS ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Flight Damage Contributions'");

			// add name column to damage angles table
			statement.executeUpdate("ALTER TABLE MAXDAM_ANGLES ADD COLUMN NAME VARCHAR(100) NOT NULL DEFAULT 'Damage Angles'");

			// create equivalent stress outputs table
			statement.executeUpdate("CREATE TABLE AURORA.ANALYSIS_OUTPUT_FILES(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), FILE_EXTENSION VARCHAR(10) NOT NULL, FILE_NAME VARCHAR(100) NOT NULL, DATA BLOB(1M) NOT NULL, PRIMARY KEY(ID))");
			statement.executeUpdate("CREATE INDEX ANALYSIS_OUTPUT_FILE_ID ON AURORA.ANALYSIS_OUTPUT_FILES(ID)");

			// add output columns to equivalent stress tables
			statement.executeUpdate("ALTER TABLE FATIGUE_EQUIVALENT_STRESSES ADD COLUMN OUTPUT_FILE_ID INT");
			statement.executeUpdate("ALTER TABLE PREFFAS_EQUIVALENT_STRESSES ADD COLUMN OUTPUT_FILE_ID INT");
			statement.executeUpdate("ALTER TABLE LINEAR_EQUIVALENT_STRESSES ADD COLUMN OUTPUT_FILE_ID INT");

			// add output columns to external equivalent stress tables
			statement.executeUpdate("ALTER TABLE EXT_FATIGUE_EQUIVALENT_STRESSES ADD COLUMN OUTPUT_FILE_ID INT");
			statement.executeUpdate("ALTER TABLE EXT_PREFFAS_EQUIVALENT_STRESSES ADD COLUMN OUTPUT_FILE_ID INT");
			statement.executeUpdate("ALTER TABLE EXT_LINEAR_EQUIVALENT_STRESSES ADD COLUMN OUTPUT_FILE_ID INT");

			// add output columns to fast equivalent stress tables
			statement.executeUpdate("ALTER TABLE FAST_FATIGUE_EQUIVALENT_STRESSES ADD COLUMN OUTPUT_FILE_ID INT");
			statement.executeUpdate("ALTER TABLE FAST_PREFFAS_EQUIVALENT_STRESSES ADD COLUMN OUTPUT_FILE_ID INT");
			statement.executeUpdate("ALTER TABLE FAST_LINEAR_EQUIVALENT_STRESSES ADD COLUMN OUTPUT_FILE_ID INT");

			// update all tables containing material information
			updateFatigueMaterialInfo("dam_contributions", statement);
			updateFatigueMaterialInfo("ext_fatigue_equivalent_stresses", statement);
			updateLinearMaterialInfo("ext_linear_equivalent_stresses", statement);
			updatePreffasMaterialInfo("ext_preffas_equivalent_stresses", statement);
			updateFatigueMaterialInfo("fast_fatigue_equivalent_stresses", statement);
			updateLinearMaterialInfo("fast_linear_equivalent_stresses", statement);
			updatePreffasMaterialInfo("fast_preffas_equivalent_stresses", statement);
			updateFatigueMaterialInfo("fatigue_equivalent_stresses", statement);
			updateLinearMaterialInfo("linear_equivalent_stresses", statement);
			updatePreffasMaterialInfo("preffas_equivalent_stresses", statement);
			updateFatigueMaterialInfo("flight_dam_contributions", statement);
			updateFatigueMaterialInfo("maxdam_angles", statement);
		}

		// return new version number
		return 3.4;
	}

	/**
	 * Updates the database to version 3.3.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo33(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 3.3");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create typical flight damage contribution tables
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_TF_DC(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate(
					"CREATE TABLE AURORA.FLIGHT_DAM_CONTRIBUTIONS(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), STF_ID INT NOT NULL, ONEG_FAC VARCHAR(50) NOT NULL, INC_FAC VARCHAR(50) NOT NULL, DP_FAC VARCHAR(50) NOT NULL, DT_FAC VARCHAR(50) NOT NULL, REF_DP DOUBLE, DP_LC VARCHAR(20), DT_LC_INF VARCHAR(20), DT_LC_SUP VARCHAR(20), REF_DT_INF DOUBLE, REF_DT_SUP DOUBLE, STRESS_COMP VARCHAR(50) NOT NULL, ROTATION_ANGLE DOUBLE NOT NULL, VALIDITY DOUBLE NOT NULL, REMOVE_NEGATIVE SMALLINT NOT NULL, OMISSION_LEVEL DOUBLE, MATERIAL_DESIGNATION VARCHAR(500) NOT NULL, MATERIAL_HEAT_TREATMENT VARCHAR(500), MATERIAL_FORM VARCHAR(500), MATERIAL_SPECIFICATION VARCHAR(500), MATERIAL_M DOUBLE NOT NULL, MATERIAL_Q DOUBLE NOT NULL, MATERIAL_P DOUBLE NOT NULL, PRIMARY KEY(ID))");
			statement.executeUpdate("CREATE TABLE AURORA.FLIGHT_DAM_CONTRIBUTIONS_EVENT_MODIFIERS(ID INT NOT NULL, LOADCASE_NUMBER VARCHAR(4) NOT NULL, EVENT_NAME VARCHAR(50) NOT NULL, COMMENT VARCHAR(500), VALUE DOUBLE NOT NULL, METHOD VARCHAR(20) NOT NULL)");
			statement.executeUpdate(
					"CREATE TABLE AURORA.FLIGHT_DAM_CONTRIBUTIONS_SEGMENT_MODIFIERS(ID INT NOT NULL, SEGMENT_NAME VARCHAR(50) NOT NULL, SEGMENT_NUMBER INT NOT NULL, ONEG_VALUE DOUBLE NOT NULL, INC_VALUE DOUBLE NOT NULL, DP_VALUE DOUBLE NOT NULL, DT_VALUE DOUBLE NOT NULL, ONEG_METHOD VARCHAR(20) NOT NULL, INC_METHOD VARCHAR(20) NOT NULL, DP_METHOD VARCHAR(20) NOT NULL, DT_METHOD VARCHAR(20) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.FLIGHT_DAM_CONTRIBUTION_WITH_OCCURRENCES(ID INT NOT NULL, FLIGHT_NAME VARCHAR(50) NOT NULL, DAM_PERCENT DOUBLE NOT NULL, UNIQUE(ID, FLIGHT_NAME))");
			statement.executeUpdate("CREATE TABLE AURORA.FLIGHT_DAM_CONTRIBUTION_WITHOUT_OCCURRENCES(ID INT NOT NULL, FLIGHT_NAME VARCHAR(50) NOT NULL, DAM_PERCENT DOUBLE NOT NULL, UNIQUE(ID, FLIGHT_NAME))");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_TF_DC ON AURORA.PILOT_POINT_TF_DC(ID)");
			statement.executeUpdate("CREATE INDEX FLIGHT_CONTRIBUTION_WITH_OCCURRENCES ON AURORA.FLIGHT_DAM_CONTRIBUTION_WITH_OCCURRENCES(ID)");
			statement.executeUpdate("CREATE INDEX FLIGHT_CONTRIBUTION_WITHOUT_OCCURRENCES ON AURORA.FLIGHT_DAM_CONTRIBUTION_WITHOUT_OCCURRENCES(ID)");
			statement.executeUpdate("CREATE INDEX FLIGHT_DAM_CONTRIBUTIONS_EVENT_MODIFIER ON AURORA.FLIGHT_DAM_CONTRIBUTIONS_EVENT_MODIFIERS(ID)");
			statement.executeUpdate("CREATE INDEX FLIGHT_DAM_CONTRIBUTIONS_SEGMENT_MODIFIER ON AURORA.FLIGHT_DAM_CONTRIBUTIONS_SEGMENT_MODIFIERS(ID)");

			// add 'remove negative stresses' column to relevant tables
			statement.executeUpdate("ALTER TABLE FATIGUE_EQUIVALENT_STRESSES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE PREFFAS_EQUIVALENT_STRESSES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE LINEAR_EQUIVALENT_STRESSES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE EXT_FATIGUE_EQUIVALENT_STRESSES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE EXT_PREFFAS_EQUIVALENT_STRESSES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE EXT_LINEAR_EQUIVALENT_STRESSES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE FAST_FATIGUE_EQUIVALENT_STRESSES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE FAST_PREFFAS_EQUIVALENT_STRESSES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE FAST_LINEAR_EQUIVALENT_STRESSES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE DAM_CONTRIBUTIONS ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
			statement.executeUpdate("ALTER TABLE MAXDAM_ANGLES ADD COLUMN REMOVE_NEGATIVE SMALLINT NOT NULL DEFAULT 0");
		}

		// return new version number
		return 3.3;
	}

	/**
	 * Updates the database to version 3.1.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo31(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 3.1");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// drop data column from STF files table
			statement.executeUpdate("ALTER TABLE stf_files DROP COLUMN data");

			// add stress table ID column to STF files table
			statement.executeUpdate("ALTER TABLE stf_files ADD COLUMN stress_table_id INT NOT NULL DEFAULT 1000");

			// rename STF stresses table
			statement.executeUpdate("RENAME TABLE stf_stresses TO stf_stresses_1000");

			// drop all aircraft model tables
			DatabaseMetaData dbmtadta = connection.getMetaData();
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "GRIDS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENTS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUPS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUP_NAMES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASE_NAMES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "PILOT_POINTS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "AC_EQ_STRESSES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "AC_MODELS", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}

			// create new A/C model table
			statement.executeUpdate(
					"CREATE TABLE AURORA.AC_MODELS(MODEL_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), AC_PROGRAM VARCHAR(100) NOT NULL, NAME VARCHAR(100) NOT NULL, DELIVERY_REF VARCHAR(50), DESCRIPTION VARCHAR(200), NUM_ELEMS INT, NUM_GRIDS INT, NUM_QUADS INT, NUM_RODS INT, NUM_BEAMS INT, NUM_TRIAS INT, NUM_SHEARS INT, DATA BLOB(30M) NOT NULL, UNIQUE(AC_PROGRAM, NAME), PRIMARY KEY(MODEL_ID))");
		}

		// return new version number
		return 3.1;
	}

	/**
	 * Updates the database to version 3.0.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo30(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 3.0");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// update fast equivalent stress tables
			statement.executeUpdate("ALTER TABLE fast_fatigue_equivalent_stresses ADD COLUMN ANALYSIS_INPUT BLOB(50K)");
			statement.executeUpdate("ALTER TABLE fast_preffas_equivalent_stresses ADD COLUMN ANALYSIS_INPUT BLOB(50K)");
			statement.executeUpdate("ALTER TABLE fast_linear_equivalent_stresses ADD COLUMN ANALYSIS_INPUT BLOB(50K)");
		}

		// return new version number
		return 3.0;
	}

	/**
	 * Updates the database to version 2.9.
	 *
	 * @param connection
	 *            Database connection.
	 * @return New version number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double updateTo29(Connection connection) throws Exception {

		// update info
		updateMessage("Updating workspace to version 2.9");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create new pilot point info image tables
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_IMAGE(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_MP(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_TF_L(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_TF_HO(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_TF_HS(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_LC(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_DA(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_ST_NOP(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_ST_FO(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_ST_RH(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");
			statement.executeUpdate("CREATE TABLE AURORA.PILOT_POINT_DC(ID INT NOT NULL, IMAGE BLOB(2M) NOT NULL)");

			// create pilot point info image table indices
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_IMAGE ON AURORA.PILOT_POINT_IMAGE(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_MP ON AURORA.PILOT_POINT_MP(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_TF_L ON AURORA.PILOT_POINT_TF_L(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_TF_HO ON AURORA.PILOT_POINT_TF_HO(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_TF_HS ON AURORA.PILOT_POINT_TF_HS(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_LC ON AURORA.PILOT_POINT_LC(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_DA ON AURORA.PILOT_POINT_DA(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_ST_NOP ON AURORA.PILOT_POINT_ST_NOP(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_ST_FO ON AURORA.PILOT_POINT_ST_FO(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_ST_RH ON AURORA.PILOT_POINT_ST_RH(ID)");
			statement.executeUpdate("CREATE INDEX SEARCH_PILOT_POINT_DC ON AURORA.PILOT_POINT_DC(ID)");

			// copy pilot point images to new pilot point info image table
			statement.executeUpdate("insert into PILOT_POINT_IMAGE(id, image) select file_id, image from stf_files where stf_files.image is not null");

			// drop image column from STF files table
			statement.executeUpdate("ALTER TABLE stf_files DROP COLUMN image");

			// drop mission profile table
			statement.executeUpdate("drop table AURORA.MISSION_PROFILE");

			// create fast equivalent stress tables
			statement.executeUpdate(
					"CREATE TABLE AURORA.FAST_FATIGUE_EQUIVALENT_STRESSES(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), STF_ID INT NOT NULL, STRESS DOUBLE, VALIDITY DOUBLE, OMISSION_LEVEL DOUBLE NOT NULL, MATERIAL_DESIGNATION VARCHAR(500) NOT NULL, MATERIAL_HEAT_TREATMENT VARCHAR(500), MATERIAL_FORM VARCHAR(500), MATERIAL_SPECIFICATION VARCHAR(500), MATERIAL_M DOUBLE NOT NULL, MATERIAL_Q DOUBLE NOT NULL, MATERIAL_P DOUBLE NOT NULL, PRIMARY KEY(ID))");
			statement.executeUpdate(
					"CREATE TABLE AURORA.FAST_PREFFAS_EQUIVALENT_STRESSES(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), STF_ID INT NOT NULL, STRESS DOUBLE, VALIDITY DOUBLE, OMISSION_LEVEL DOUBLE NOT NULL, MATERIAL_DESIGNATION VARCHAR(500) NOT NULL, MATERIAL_HEAT_TREATMENT VARCHAR(500), MATERIAL_FORM VARCHAR(500), MATERIAL_SPECIFICATION VARCHAR(500), MATERIAL_MIN_THICKNESS DOUBLE, MATERIAL_MAX_THICKNESS DOUBLE, MATERIAL_CEFF DOUBLE NOT NULL, MATERIAL_M DOUBLE NOT NULL, MATERIAL_A DOUBLE NOT NULL, MATERIAL_YIELD DOUBLE NOT NULL, MATERIAL_ULT DOUBLE NOT NULL, PRIMARY KEY(ID))");
			statement.executeUpdate(
					"CREATE TABLE AURORA.FAST_LINEAR_EQUIVALENT_STRESSES(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), STF_ID INT NOT NULL, STRESS DOUBLE, VALIDITY DOUBLE, OMISSION_LEVEL DOUBLE NOT NULL, MATERIAL_DESIGNATION VARCHAR(500) NOT NULL, MATERIAL_HEAT_TREATMENT VARCHAR(500), MATERIAL_FORM VARCHAR(500), MATERIAL_SPECIFICATION VARCHAR(500), MATERIAL_MIN_THICKNESS DOUBLE, MATERIAL_CEFF DOUBLE NOT NULL, MATERIAL_M DOUBLE NOT NULL, MATERIAL_A DOUBLE NOT NULL, MATERIAL_YIELD DOUBLE NOT NULL, MATERIAL_ULT DOUBLE NOT NULL, PRIMARY KEY(ID))");

			// drop all aircraft model tables
			DatabaseMetaData dbmtadta = connection.getMetaData();
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "GRIDS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENTS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUPS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUP_NAMES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASE_NAMES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "PILOT_POINTS_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
			try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "AC_EQ_STRESSES_%", null)) {
				while (resultSet.next()) {
					statement.executeUpdate("drop table AURORA." + resultSet.getString(3));
				}
			}
		}

		// return new version number
		return 2.9;
	}

	/**
	 * Updates version number of database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param createVersionTable
	 *            True if version table should be created.
	 * @param dbVersion
	 *            Version number to update to.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateVersionNumber(Connection connection, boolean createVersionTable, double dbVersion) throws Exception {

		// update info
		updateMessage("Updating workspace version number...");

		// create statement
		try (Statement statement = connection.createStatement()) {

			// no version table
			if (createVersionTable) {

				// create version table
				statement.executeUpdate("CREATE TABLE AURORA.DB_VERSION(NAME VARCHAR(50) NOT NULL, VALUE DOUBLE NOT NULL)");

				// insert new version
				statement.executeUpdate("insert into db_version(name, value) values('Database Version', " + dbVersion + ")");
			}
			else {
				// update version
				statement.executeUpdate("update db_version set value = " + dbVersion + " where name = 'Database Version'");
			}
		}
	}

	/**
	 * Returns database version or -1 if no version information found in database.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Database version or -1 if no version information found in database.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double getDatabaseVersion(Connection connection) throws Exception {

		// update info
		updateMessage("Getting workspace version...");

		// Initialize version
		double dbVersion = -1;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get database version table
			DatabaseMetaData dbmtadta = connection.getMetaData();
			try (ResultSet getVersionTable = dbmtadta.getTables(null, "AURORA", "DB_VERSION", null)) {
				while (getVersionTable.next()) {
					// get version number
					try (ResultSet getVersion = statement.executeQuery("select value from db_version where name = 'Database Version'")) {
						while (getVersion.next()) {
							dbVersion = getVersion.getDouble("value");
						}
					}
				}
			}
		}

		// return version
		return dbVersion;
	}

	/**
	 * Updates fatigue material info in the given table.
	 *
	 * @param tableName
	 *            Table name.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void updateFatigueMaterialInfo(String tableName, Statement statement) throws Exception {

		// rename column names
		statement.executeUpdate("rename column " + tableName + ".material_designation to material_name");

		// drop columns
		statement.executeUpdate("alter table " + tableName + " drop column material_heat_treatment");
		statement.executeUpdate("alter table " + tableName + " drop column material_form");

		// add new columns
		statement.executeUpdate("alter table " + tableName + " add column material_library_version varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_family varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_orientation varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_configuration varchar(500)");
	}

	/**
	 * Updates linear propagation material info in the given table.
	 *
	 * @param tableName
	 *            Table name.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void updateLinearMaterialInfo(String tableName, Statement statement) throws Exception {

		// rename column names
		statement.executeUpdate("rename column " + tableName + ".material_designation to material_name");
		statement.executeUpdate("rename column " + tableName + ".material_yield to material_fty");
		statement.executeUpdate("rename column " + tableName + ".material_ult to material_ftu");

		// drop columns
		statement.executeUpdate("alter table " + tableName + " drop column material_heat_treatment");
		statement.executeUpdate("alter table " + tableName + " drop column material_form");
		statement.executeUpdate("alter table " + tableName + " drop column material_min_thickness");

		// add new columns
		statement.executeUpdate("alter table " + tableName + " add column material_library_version varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_family varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_orientation varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_configuration varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_b double");
		statement.executeUpdate("alter table " + tableName + " add column material_c double");
	}

	/**
	 * Updates preffas propagation material info in the given table.
	 *
	 * @param tableName
	 *            Table name.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void updatePreffasMaterialInfo(String tableName, Statement statement) throws Exception {

		// rename column names
		statement.executeUpdate("rename column " + tableName + ".material_designation to material_name");
		statement.executeUpdate("rename column " + tableName + ".material_yield to material_fty");
		statement.executeUpdate("rename column " + tableName + ".material_ult to material_ftu");

		// drop columns
		statement.executeUpdate("alter table " + tableName + " drop column material_heat_treatment");
		statement.executeUpdate("alter table " + tableName + " drop column material_form");
		statement.executeUpdate("alter table " + tableName + " drop column material_min_thickness");
		statement.executeUpdate("alter table " + tableName + " drop column material_max_thickness");

		// add new columns
		statement.executeUpdate("alter table " + tableName + " add column material_library_version varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_family varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_orientation varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_configuration varchar(500)");
		statement.executeUpdate("alter table " + tableName + " add column material_b double");
		statement.executeUpdate("alter table " + tableName + " add column material_c double");
	}
}
