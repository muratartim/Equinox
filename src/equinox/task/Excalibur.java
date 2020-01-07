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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;

import equinox.Equinox;
import equinox.data.ExcaliburStressType;
import equinox.data.ProgramArguments.ArgumentType;
import equinox.data.input.ExcaliburInput;
import equinox.process.ExcaliburLoadElementStresses;
import equinox.process.ExcaliburLoadLoadcaseCorrelations;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.DirectoryOutputtingTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableExcalibur;

/**
 * Class for Excalibur stress sorting task.
 *
 * @author Murat Artim
 * @date 29 Nov 2017
 * @time 15:28:19
 */
public class Excalibur extends InternalEquinoxTask<Void> implements LongRunningTask, DirectoryOutputtingTask, SavableTask {

	/** Excalibur version. */
	public static final String VERSION = "v1.0";

	/** Analysis table index. */
	public static final int XLS = 0, LCK = 1, STF_FILES = 2, STF_STRESSES = 3;

	/** Analysis input. */
	private final ExcaliburInput input_;

	/** Number of completed analyses. */
	private long completed_ = 0L;

	/**
	 * Creates Excalibur stress sorting task.
	 *
	 * @param input
	 *            Analysis input.
	 */
	public Excalibur(ExcaliburInput input) {
		input_ = input;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Excalibur stress sorting";
	}

	@Override
	public Path getOutputDirectory() {
		return input_.getOutputDirectory().toPath();
	}

	@Override
	public String getOutputMessage() {
		String message = getTaskTitle() + " is successfully completed. ";
		message += "Click 'Outputs' to see outputs of the task.";
		return message;
	}

	@Override
	public String getOutputButtonText() {
		return "Outputs";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableExcalibur(input_);
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.RUN_EXCALIBUR_PLUGIN);

		// initialize variables
		String[] analysisTables = null, sectionMission = null;
		long numSTFs = 0L, analysisID = 0L;

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create analysis ID
				analysisID = createAnalysisID(connection, new Timestamp(System.currentTimeMillis()));

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// create analysis tables
				analysisTables = createAnalysisTables(connection, analysisID);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// load loadcase correlations
				sectionMission = new ExcaliburLoadLoadcaseCorrelations(this, input_, analysisTables).start(connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// load element stresses
				numSTFs = new ExcaliburLoadElementStresses(this, input_, analysisTables).start(connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

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

			try {

				// create output directories
				Path outputDirectory = input_.getOutputDirectory().toPath();
				Path outputSTFDirectory = Files.createDirectories(outputDirectory.resolve("stfs"));
				Path outputLOGDirectory = input_.getLogLevel().equals(Level.OFF) ? null : Files.createDirectories(outputDirectory.resolve("logs"));

				// sort stresses
				sortStresses(analysisTables, sectionMission, numSTFs, outputSTFDirectory, outputLOGDirectory, connection);

				// remove analysis data
				removeAnalysisData(analysisID, analysisTables, connection);
			}

			// exception occurred during process
			catch (Exception e) {
				removeAnalysisData(analysisID, analysisTables, connection);
				throw e;
			}
		}

		// return
		return null;
	}

	/**
	 * Removes all analysis data. Analysis data includes analysis tables and analysis entry in the Excalibur analyses table.
	 *
	 * @param analysisID
	 *            Analysis id.
	 * @param analysisTables
	 *            All analysis tables.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void removeAnalysisData(long analysisID, String[] analysisTables, Connection connection) throws Exception {
		updateMessage("Removing temporary analysis data...");
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("drop table aurora." + analysisTables[XLS]);
			statement.executeUpdate("drop table aurora." + analysisTables[LCK]);
			statement.executeUpdate("drop table aurora." + analysisTables[STF_STRESSES]);
			statement.executeUpdate("drop table aurora." + analysisTables[STF_FILES]);
			statement.executeUpdate("delete from excalibur_analyses where id = " + analysisID);
		}
	}

	/**
	 * Starts stress sorting process.
	 *
	 * @param analysisTables
	 *            Analysis table names.
	 * @param sectionMission
	 *            Aircraft section and fatigue mission.
	 * @param numSTFs
	 *            Number of stress files to sort.
	 * @param outputSTFDirectory
	 *            Output directory where the STF files will be written to.
	 * @param outputLOGDirectory
	 *            Output directory where the log files will be written to. Can be null for no logging.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void sortStresses(String[] analysisTables, String[] sectionMission, long numSTFs, Path outputSTFDirectory, Path outputLOGDirectory, Connection connection) throws Exception {

		// get number of parallel processes
		int maxParallel = Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.MAX_PARALLEL_TASKS));

		// prepare statement for getting STF files incrementally
		String sql = "select id, filename from " + analysisTables[STF_FILES] + " where id > ? order by id asc";
		try (PreparedStatement getSTFFiles = connection.prepareStatement(sql)) {

			// set limit to rows returned
			getSTFFiles.setMaxRows(maxParallel);

			// execute as long as all tasks are completed
			long fileID = 0L;
			while (fileID >= 0L) {

				// task cancelled
				if (isCancelled()) {
					break;
				}

				// execute
				getSTFFiles.setLong(1, fileID);
				fileID = executeTasks(getSTFFiles, analysisTables, sectionMission, numSTFs, outputSTFDirectory, outputLOGDirectory);
			}

			// reset statement
			getSTFFiles.setMaxRows(0);
		}
	}

	/**
	 * Executes tasks and waits for all of them to complete.
	 *
	 * @param getSTFFiles
	 *            Database statement to get STF files incrementally.
	 * @param analysisTables
	 *            Analysis table names.
	 * @param sectionMission
	 *            Aircraft section and fatigue mission.
	 * @param numSTFs
	 *            Number of stress files to sort.
	 * @param outputSTFDirectory
	 *            Output directory where the STF files will be written to.
	 * @param outputLOGDirectory
	 *            Output directory where the log files will be written to. Can be null for no logging.
	 * @return Maximum STF file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private long executeTasks(PreparedStatement getSTFFiles, String[] analysisTables, String[] sectionMission, long numSTFs, Path outputSTFDirectory, Path outputLOGDirectory) throws Exception {

		// initialize variables
		long maxFileID = -1L;
		ArrayList<Future<?>> results = null;

		// get next STF files
		try (ResultSet resultSet = getSTFFiles.executeQuery()) {

			// loop over STF files
			while (resultSet.next()) {

				// task cancelled
				if (isCancelled()) {
					break;
				}

				// get STF file info
				long fileID = resultSet.getLong("id");
				String fileName = resultSet.getString("filename");

				// create stress sorting task
				InternalEquinoxTask<?> task = null;
				if (input_.getStressType().equals(ExcaliburStressType.ELEMENT_1D)) {
					task = new ExcaliburSort1DElementStresses(fileID, fileName, analysisTables, sectionMission, input_, outputSTFDirectory, outputLOGDirectory);
				}
				else if (input_.getStressType().equals(ExcaliburStressType.ELEMENT_2D)) {
					task = new ExcaliburSort2DElementStresses(fileID, fileName, analysisTables, sectionMission, input_, outputSTFDirectory, outputLOGDirectory);
				}
				else if (input_.getStressType().equals(ExcaliburStressType.FRAME)) {
					task = new ExcaliburSortFrameStresses(fileID, fileName, analysisTables, sectionMission, input_, outputSTFDirectory, outputLOGDirectory);
				}

				// execute task silently and in parallel
				if (results == null) {
					results = new ArrayList<>();
				}
				results.add(taskPanel_.getOwner().runTaskSilently(task, !input_.isRunInParallel()));

				// update maximum file ID
				if (fileID >= maxFileID) {
					maxFileID = fileID;
				}
			}
		}

		// there are results
		if (results != null) {

			// loop over results
			for (Future<?> result : results) {

				// task cancelled
				if (isCancelled()) {
					result.cancel(false);
				}

				// task completed
				else {

					// get task result
					try {
						result.get();
						completed_++;
						updateProgress(completed_, numSTFs);
					}

					// exception occurred (ignore since it is handled within the task)
					catch (Exception e) {
						completed_++;
						updateProgress(completed_, numSTFs);
					}
				}
			}
		}

		// return maximum file ID
		return maxFileID;
	}

	/**
	 * Creates and returns analysis tables.
	 *
	 * @param connection
	 *            Database connection.
	 * @param analysisID
	 *            Analysis ID.
	 * @return Analysis table names.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String[] createAnalysisTables(Connection connection, long analysisID) throws Exception {

		// update progress info
		updateMessage("Creating analysis tables...");

		// create table names
		String[] tableNames = new String[4];
		tableNames[XLS] = "excalibur_xls_" + analysisID;
		tableNames[LCK] = "excalibur_lck_" + analysisID;
		tableNames[STF_FILES] = "excalibur_stf_files_" + analysisID;
		tableNames[STF_STRESSES] = "excalibur_stf_stresses_" + analysisID;

		// create statement
		try (Statement statement = connection.createStatement()) {

			// create attributes table
			statement.executeUpdate("create table aurora." + tableNames[XLS] + "(id bigint not null generated always as identity (start with 1, increment by 1), section varchar(30) not null, mission varchar(30) not null, "
					+ "ref_intensity varchar(100), load_factor double not null, issy_code varchar(20) not null, event_name varchar(100) not null, segment varchar(30) not null, load_type varchar(100) not null, load_criteria "
					+ "varchar(100) not null, event_comment varchar(500) not null, primary key(id))");

			// create loadcase keys table
			statement.executeUpdate("create table aurora." + tableNames[LCK] + "(id bigint not null generated always as identity (start with 1, increment by 1), section varchar(30) not null, mission varchar(30) not null, "
					+ "segment varchar(30) not null, lc_name varchar(100) not null, db_name varchar(100) not null, family_name varchar(50) not null, tx double not null, ty double not null, tz double not null, mx double not "
					+ "null, my double not null, mz double not null, lc_num int, load_type varchar(100) not null, primary key(id))");

			// create stress files table
			statement.executeUpdate("create table aurora." + tableNames[STF_FILES] + "(id bigint not null generated always as identity (start with 1, increment by 1), filename varchar(100) unique not null, primary key(id))");

			// create stress table for 1D stresses
			if (input_.getStressType().equals(ExcaliburStressType.ELEMENT_1D)) {
				statement.executeUpdate("create table aurora." + tableNames[STF_STRESSES] + "(file_id bigint not null, lc_num int not null, sn double, unique(file_id, lc_num), foreign key(file_id) references aurora." + tableNames[STF_FILES] + "(id))");
			}

			// create stress table for 2D stresses
			else if (input_.getStressType().equals(ExcaliburStressType.ELEMENT_2D)) {
				statement.executeUpdate("create table aurora." + tableNames[STF_STRESSES] + "(file_id bigint not null, lc_num int not null, sx double, sy double, sxy double, sigma_1 double, sigma_2 double, max_sigma double, "
						+ "abs_max_sigma double, min_sigma double, unique(file_id, lc_num), foreign key(file_id) references aurora." + tableNames[STF_FILES] + "(id))");
			}

			// create stress table for frame stresses
			else if (input_.getStressType().equals(ExcaliburStressType.FRAME)) {
				statement.executeUpdate("create table aurora." + tableNames[STF_STRESSES] + "(file_id bigint not null, lc_num int not null, sn double, unique(file_id, lc_num), foreign key(file_id) references aurora." + tableNames[STF_FILES] + "(id))");
			}

			// create indexes
			statement.executeUpdate("create index select_events_" + analysisID + " on aurora." + tableNames[XLS] + "(section, mission)");
			statement.executeUpdate("create index select_keys_" + analysisID + " on aurora." + tableNames[LCK] + "(section, mission, segment, load_type)");
			statement.executeUpdate("create index select_stress_" + analysisID + " on aurora." + tableNames[STF_STRESSES] + "(file_id, lc_num)");
		}

		// return table names
		return tableNames;
	}

	/**
	 * Creates and returns analysis ID.
	 *
	 * @param connection
	 *            Database connection.
	 * @param startTime
	 *            Start time.
	 * @return Analysis ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private long createAnalysisID(Connection connection, Timestamp startTime) throws Exception {

		// update progress info
		updateMessage("Creating stress sorting analysis...");

		// insert new analysis
		String sql = "insert into excalibur_analyses(start_time) values(?)";
		try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setTimestamp(1, startTime);
			statement.executeUpdate();
			try (ResultSet resultSet = statement.getGeneratedKeys()) {
				resultSet.next();
				return resultSet.getBigDecimal(1).longValue();
			}
		}
	}
}
