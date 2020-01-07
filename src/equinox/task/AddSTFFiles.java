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

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.RandomUtils;

import equinox.Equinox;
import equinox.controller.InputPanel;
import equinox.data.EmbeddedTask;
import equinox.data.ProgramArguments.ArgumentType;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.plugin.FileType;
import equinox.process.LoadSTFFile;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.Utility;

/**
 * Add STF files task.
 *
 * @author Murat Artim
 * @date Feb 12, 2014
 * @time 10:25:22 AM
 */
public class AddSTFFiles extends TemporaryFileCreatingTask<ArrayList<STFFile>> implements LongRunningTask, SingleInputTask<Spectrum>, AutomaticTaskOwner<STFFile> {

	/** STF stress table generation constants. */
	public static final int MAX_STF_FILES_PER_TABLE = 500, MAX_STRESS_TABLES = 10000;

	/** Input STF files. */
	private List<File> stfFiles_;

	/** The owner spectrum. */
	private Spectrum spectrum_ = null;

	/** Directory containing the STF files. */
	private final Path directory_;

	/** Pilot point info. */
	private final List<PilotPointInfo> info_;

	/** Automatic tasks. The key is the STF file name and the value is the task. */
	private HashMap<String, EmbeddedTask<STFFile>> automaticTasks_ = null;

	/** Number of STF files added at the end of process. */
	private int numAdded_ = 0;

	/** True if the STF files should be added to STF file bucket. */
	private boolean addToBucket_ = false;

	/**
	 * Creates add STF files task.
	 *
	 * @param stfFiles
	 *            Input STF files. This can be null to set it from the setter.
	 * @param spectrum
	 *            Spectrum. This can be null for automatic execution.
	 * @param info
	 *            Pilot point info (can be null).
	 */
	public AddSTFFiles(List<File> stfFiles, Spectrum spectrum, List<PilotPointInfo> info) {
		stfFiles_ = stfFiles;
		spectrum_ = spectrum;
		info_ = info;
		directory_ = null;
	}

	/**
	 * Creates add STF files task.
	 *
	 * @param directory
	 *            Directory containing the STF files.
	 * @param spectrum
	 *            Spectrum. This can be null for automatic execution.
	 */
	public AddSTFFiles(Path directory, Spectrum spectrum) {
		stfFiles_ = null;
		spectrum_ = spectrum;
		info_ = null;
		directory_ = directory;
	}

	/**
	 * Sets input STF files.
	 *
	 * @param stfFiles
	 *            Input STF files.
	 */
	public void setSTFFiles(List<File> stfFiles) {
		stfFiles_ = stfFiles;
	}

	/**
	 * Returns input input STF files.
	 *
	 * @return Input STF files.
	 */
	public List<File> getSTFFiles() {
		return stfFiles_;
	}

	/**
	 * Adds automatic task. If <u>multiple STF files are added</u>, task id must be the name of STF file that the automatic task will use as input. Otherwise it should be any unique task identifier.
	 */
	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<STFFile> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<STFFile>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public void setAutomaticInput(Spectrum spectrum) {
		spectrum_ = spectrum;
	}

	@Override
	public String getTaskTitle() {
		return "Load STF files";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected ArrayList<STFFile> call() throws Exception {

		// check permission
		checkPermission(Permission.ADD_NEW_STF_FILE);

		// get maximum visible STF files per spectrum
		int maxVisibleSTFs = Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.MAX_VISIBLE_STFS_PER_SPECTRUM));

		// get maximum allowed number of STF files to be returned
		int currentSTFs = spectrum_.getSTFFiles() == null ? 0 : spectrum_.getSTFFiles().size();
		int allowance = spectrum_.getSTFFileBucket() == null ? maxVisibleSTFs - currentSTFs : 0;

		// initialize list
		ArrayList<STFFile> files = null;

		// update progress info
		updateTitle("Loading STF files...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// add from directory
			if (stfFiles_ == null) {
				files = addFromDirectory(connection, allowance);
			}

			// add individual STF files
			else {
				files = addFromSTFFiles(connection, allowance);
			}
		}

		// return files
		return files;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get STF files
			ArrayList<STFFile> stfFiles = get();

			// show file view panel
			taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.FILE_VIEW_PANEL);

			// add STF files to STF file bucket
			if (addToBucket_) {

				// get current STF files
				ArrayList<STFFile> stfs = spectrum_.getSTFFiles();

				// remove them
				if (stfs != null) {
					spectrum_.getChildren().removeAll(stfs);
				}

				// get STF file bucket
				STFFileBucket bucket = spectrum_.getSTFFileBucket();

				// there is no STF file bucket
				if (bucket == null) {

					// compute total number of STF files
					int numSTFs = numAdded_;
					if (stfs != null) {
						numSTFs += stfs.size();
					}

					// create and add STF file bucket
					bucket = new STFFileBucket(spectrum_.getID(), numSTFs);
					spectrum_.getChildren().add(bucket);
				}
				else {
					bucket.setNumberOfSTFs(bucket.getNumberOfSTFs() + numAdded_);
				}
			}
			else {
				spectrum_.getChildren().addAll(stfFiles);
			}

			// execute automatic tasks
			if (automaticTasks_ != null) {

				// only 1 STF file added
				if (stfFiles.size() == 1) {
					STFFile stfFile = stfFiles.get(0);
					automaticTaskOwnerSucceeded(stfFile, automaticTasks_, taskPanel_);
				}

				// multiple STF files added
				else {
					for (STFFile stfFile : stfFiles) {
						EmbeddedTask<STFFile> task = automaticTasks_.get(stfFile.getName());
						automaticTaskOwnerSucceeded(stfFile, task, taskPanel_);
					}
				}
			}
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
	 * Adds and returns STF files from directly given STF files.
	 *
	 * @param connection
	 *            Database connection.
	 * @param allowance
	 *            Maximum allowed number of STF files to be returned.
	 * @return Added STF files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<STFFile> addFromSTFFiles(Connection connection, int allowance) throws Exception {

		// initialize variables
		PreparedStatement[] insertStresses = null;

		try {

			// initialize list
			ArrayList<STFFile> files = new ArrayList<>();

			// set progress update indicator for sub process
			int numFiles = stfFiles_.size();
			boolean updateProcessProgress = numFiles < 10;

			// compute number of stress tables
			int numTables = numFiles / MAX_STF_FILES_PER_TABLE;
			numTables += numFiles % MAX_STF_FILES_PER_TABLE == 0 ? 0 : 1;

			// create arrays for storing stress table IDs and insert statements
			int[] stressTableIDs = new int[numTables];
			for (int i = 0; i < stressTableIDs.length; i++) {
				stressTableIDs[i] = -1;
			}
			insertStresses = new PreparedStatement[numTables];

			// create STF stress tables
			createStressTables(stressTableIDs, insertStresses, connection);

			// prepare statement for inserting STF files
			String sql = "insert into stf_files(cdf_id, stress_table_id, name, is_2d, description, element_type, frame_rib_position, stringer_position, data_source, generation_source, delivery_ref_num, issue, eid, fatigue_material, preffas_material, linear_material) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement insertFile = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

				// prepare statement for updating stress state
				sql = "update stf_files set is_2d = ? where file_id = ?";
				try (PreparedStatement updateStressState = connection.prepareStatement(sql)) {

					// loop over files
					for (int i = 0; i < numFiles; i++) {

						// update global progress
						if (!updateProcessProgress) {
							updateProgress(i, numFiles);
						}

						// get input file
						File inputFile = stfFiles_.get(i);

						// get pilot point info
						PilotPointInfo info = info_ == null ? null : info_.get(i);

						// get file type
						FileType type = FileType.getFileType(inputFile);

						// input file is a GZIP file
						if (type.equals(FileType.GZ)) {

							// extract
							Path stfFilePath = getWorkingDirectory().resolve(FileType.appendExtension(FileType.getNameWithoutExtension(inputFile.toPath()), FileType.STF));
							updateMessage("Extracting zipped STF file...");
							Utility.extractFileFromGZIP(inputFile.toPath(), stfFilePath);

							// load and add STF file
							try {
								STFFile stfFile = new LoadSTFFile(this, stfFilePath, spectrum_, info, updateProcessProgress, stressTableIDs[i / MAX_STF_FILES_PER_TABLE]).start(connection, insertFile, insertStresses[i / MAX_STF_FILES_PER_TABLE], updateStressState);
								if (automaticTasks_ != null) {
									files.add(stfFile);
								}
								else {
									if (files.size() < allowance) {
										files.add(stfFile);
									}
									else {
										addToBucket_ = true;
									}
									numAdded_++;
								}
							}

							// exception occurred during loading STF file
							catch (Exception e) {
								if (numFiles == 1)
									throw e;
								Path fileNamePath = stfFilePath.getFileName();
								if (fileNamePath != null) {
									addWarning("Loading STF file '" + fileNamePath.toString() + "' has failed due to an exception.", e);
								}
							}
						}

						// input file is ZIP file
						else if (type.equals(FileType.ZIP)) {

							// extract
							updateMessage("Extracting zipped STF file...");
							Path stfFilePath = Utility.extractFileFromZIP(inputFile.toPath(), this, FileType.STF, null);

							// load and add STF file
							try {
								STFFile stfFile = new LoadSTFFile(this, stfFilePath, spectrum_, info, updateProcessProgress, stressTableIDs[i / MAX_STF_FILES_PER_TABLE]).start(connection, insertFile, insertStresses[i / MAX_STF_FILES_PER_TABLE], updateStressState);
								if (automaticTasks_ != null) {
									files.add(stfFile);
								}
								else {
									if (files.size() < allowance) {
										files.add(stfFile);
									}
									else {
										addToBucket_ = true;
									}
									numAdded_++;
								}
							}

							// exception occurred during loading STF file
							catch (Exception e) {
								if (numFiles == 1)
									throw e;
								Path fileNamePath = stfFilePath.getFileName();
								if (fileNamePath != null) {
									addWarning("Loading STF file '" + fileNamePath.toString() + "' has failed due to an exception.", e);
								}
							}
						}

						// input file is STF file
						else if (type.equals(FileType.STF)) {

							// load and add STF file
							try {
								STFFile stfFile = new LoadSTFFile(this, inputFile.toPath(), spectrum_, info, updateProcessProgress, stressTableIDs[i / MAX_STF_FILES_PER_TABLE]).start(connection, insertFile, insertStresses[i / MAX_STF_FILES_PER_TABLE], updateStressState);
								if (automaticTasks_ != null) {
									files.add(stfFile);
								}
								else {
									if (files.size() < allowance) {
										files.add(stfFile);
									}
									else {
										addToBucket_ = true;
									}
									numAdded_++;
								}
							}

							// exception occurred during loading STF file
							catch (Exception e) {
								if (numFiles == 1)
									throw e;
								Path fileNamePath = inputFile.toPath().getFileName();
								if (fileNamePath != null) {
									addWarning("Loading STF file '" + fileNamePath.toString() + "' has failed due to an exception.", e);
								}
							}
						}
					}
				}
			}

			// return files
			return files;
		}

		// close all insert statements
		finally {
			if (insertStresses != null) {
				for (PreparedStatement statement : insertStresses) {
					statement.close();
				}
			}
		}
	}

	/**
	 * Adds STF files from given directory.
	 *
	 * @param connection
	 *            Database connection.
	 * @param allowance
	 *            Maximum allowed number of STF files to be returned.
	 * @return Added STF files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<STFFile> addFromDirectory(Connection connection, int allowance) throws Exception {

		// initialize variables
		PreparedStatement[] insertStresses = null;

		try {

			// initialize list
			ArrayList<STFFile> files = new ArrayList<>();

			// set progress update indicator for sub process
			int numFiles = (int) Files.list(directory_).count();
			boolean updateProcessProgress = numFiles < 10L;

			// compute number of stress tables
			int numTables = numFiles / MAX_STF_FILES_PER_TABLE;
			numTables += numFiles % MAX_STF_FILES_PER_TABLE == 0 ? 0 : 1;

			// create arrays for storing stress table IDs and insert statements
			int[] stressTableIDs = new int[numTables];
			for (int i = 0; i < stressTableIDs.length; i++) {
				stressTableIDs[i] = -1;
			}
			insertStresses = new PreparedStatement[numTables];

			// create STF stress tables
			createStressTables(stressTableIDs, insertStresses, connection);

			// prepare statement for inserting STF files
			String sql = "insert into stf_files(cdf_id, stress_table_id, name, is_2d, description, element_type, frame_rib_position, stringer_position, data_source, generation_source, delivery_ref_num, issue, eid, fatigue_material, preffas_material, linear_material) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement insertFile = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

				// prepare statement for updating stress state
				sql = "update stf_files set is_2d = ? where file_id = ?";
				try (PreparedStatement updateStressState = connection.prepareStatement(sql)) {

					// create directory stream
					try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory_)) {

						// get iterator
						Iterator<Path> iterator = dirStream.iterator();

						// loop over files
						int index = 0;
						while (iterator.hasNext()) {

							// update global progress
							if (!updateProcessProgress) {
								updateProgress(index, numFiles);
							}

							// get file
							Path file = iterator.next();

							// get file type
							FileType type = FileType.getFileType(file.toFile());

							// file type not recognized
							if (type == null) {
								continue;
							}

							// input file is a GZIP file
							if (type.equals(FileType.GZ)) {

								// extract
								Path stfFilePath = getWorkingDirectory().resolve(FileType.appendExtension(FileType.getNameWithoutExtension(file), FileType.STF));
								updateMessage("Extracting zipped STF file...");
								Utility.extractFileFromGZIP(file, stfFilePath);

								// load and add STF file
								try {
									STFFile stfFile = new LoadSTFFile(this, stfFilePath, spectrum_, null, updateProcessProgress, stressTableIDs[index / MAX_STF_FILES_PER_TABLE]).start(connection, insertFile, insertStresses[index / MAX_STF_FILES_PER_TABLE], updateStressState);
									if (automaticTasks_ != null) {
										files.add(stfFile);
									}
									else {
										if (files.size() < allowance) {
											files.add(stfFile);
										}
										else {
											addToBucket_ = true;
										}
										numAdded_++;
									}
								}

								// exception occurred during loading STF file
								catch (Exception e) {
									if (numFiles == 1)
										throw e;
									Path fileNamePath = stfFilePath.getFileName();
									if (fileNamePath != null) {
										addWarning("Loading STF file '" + fileNamePath.toString() + "' has failed due to an exception.", e);
									}
								}
							}

							// input file is STF file
							else if (type.equals(FileType.STF)) {
								// load and add STF file
								try {
									STFFile stfFile = new LoadSTFFile(this, file, spectrum_, null, updateProcessProgress, stressTableIDs[index / MAX_STF_FILES_PER_TABLE]).start(connection, insertFile, insertStresses[index / MAX_STF_FILES_PER_TABLE], updateStressState);
									if (automaticTasks_ != null) {
										files.add(stfFile);
									}
									else {
										if (files.size() < allowance) {
											files.add(stfFile);
										}
										else {
											addToBucket_ = true;
										}
										numAdded_++;
									}
								}

								// exception occurred during loading STF file
								catch (Exception e) {
									if (numFiles == 1)
										throw e;
									Path fileNamePath = file.getFileName();
									if (fileNamePath != null) {
										addWarning("Loading STF file '" + fileNamePath.toString() + "' has failed due to an exception.", e);
									}
								}
							}

							// increment index
							index++;
						}
					}
				}
			}

			// return files
			return files;
		}

		// close all insert statements
		finally {
			if (insertStresses != null) {
				for (PreparedStatement statement : insertStresses) {
					statement.close();
				}
			}
		}
	}

	/**
	 * Creates STF stress tables.
	 *
	 * @param stressTableIDs
	 *            Array to store newly created stress table IDs.
	 * @param insertStresses
	 *            Array to store stress insertion statements for each STF stress table.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void createStressTables(int[] stressTableIDs, PreparedStatement[] insertStresses, Connection connection) throws Exception {

		// create statement to create STF stress tables
		try (Statement createStressTable = connection.createStatement()) {

			// prepare statement to check if generated table ID already exists
			String sql = "select 1 from stf_files where stress_table_id = ?";
			try (PreparedStatement checkTableID = connection.prepareStatement(sql)) {

				// loop over tables
				for (int i = 0; i < stressTableIDs.length; i++) {

					// generate table name
					String tableName = null;
					int tableID = -2;
					generate: while (tableName == null) {
						tableID = RandomUtils.nextInt(0, MAX_STRESS_TABLES);
						for (int j = 0; j < stressTableIDs.length; j++)
							if (stressTableIDs[j] == tableID) {
								continue generate;
							}
						checkTableID.setInt(1, tableID);
						try (ResultSet resultSet = checkTableID.executeQuery()) {
							if (resultSet.next()) {
								continue generate;
							}
						}
						tableName = "stf_stresses_" + tableID;
					}

					// create STF stress table
					createStressTable.executeUpdate("CREATE TABLE AURORA." + tableName + "(FILE_ID INT NOT NULL, ISSY_CODE VARCHAR(10) NOT NULL, STRESS_X DOUBLE NOT NULL, STRESS_Y DOUBLE NOT NULL, STRESS_XY DOUBLE NOT NULL)");
					createStressTable.executeUpdate("CREATE INDEX STF_SELSTRESS_" + tableID + " ON AURORA." + tableName + "(FILE_ID, ISSY_CODE)");

					// prepare insert STF stress statement
					stressTableIDs[i] = tableID;
					sql = "insert into " + tableName + "(file_id, issy_code, stress_x, stress_y, stress_xy) values(?, ?, ?, ?, ?)";
					insertStresses[i] = connection.prepareStatement(sql);
				}
			}
		}
	}
}
