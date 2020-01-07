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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.AircraftEquivalentStresses;
import equinox.data.fileType.AircraftLoadCases;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.PilotPoints;
import equinox.dataServer.remote.data.AircraftModelInfo;
import equinox.dataServer.remote.data.AircraftModelInfo.AircraftModelInfoType;
import equinox.plugin.FileType;
import equinox.process.LoadElementDataFile;
import equinox.process.LoadElementGroupsFile;
import equinox.process.LoadGridDataFile;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableAddAircraftModel;
import equinox.utility.Utility;

/**
 * Class for add A/C model task.
 *
 * @author Murat Artim
 * @date Jul 6, 2015
 * @time 10:28:59 AM
 */
public class AddAircraftModel extends TemporaryFileCreatingTask<AircraftModel> implements LongRunningTask, SavableTask {

	/** Element and grid data files. */
	private final Path f06File_, f07File_, grpFile_;

	/** A/C model info. */
	private final AircraftModelInfo info_;

	/**
	 * Creates add A/C model task.
	 *
	 * @param info
	 *            A/C model info.
	 * @param f06File
	 *            Element data file.
	 * @param f07File
	 *            Grid data file.
	 * @param grpFile
	 *            Element groups data file. Can be null.
	 */
	public AddAircraftModel(AircraftModelInfo info, Path f06File, Path f07File, Path grpFile) {
		info_ = info;
		f06File_ = f06File;
		f07File_ = f07File;
		grpFile_ = grpFile;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		String program = (String) info_.getInfo(AircraftModelInfoType.AC_PROGRAM);
		String modelName = (String) info_.getInfo(AircraftModelInfoType.MODEL_NAME);
		return "Add aircraft model " + program + ", " + modelName;
	}

	@Override
	public SerializableTask getSerializableTask() {
		String program = (String) info_.getInfo(AircraftModelInfoType.AC_PROGRAM);
		String modelName = (String) info_.getInfo(AircraftModelInfoType.MODEL_NAME);
		return new SerializableAddAircraftModel(program, modelName, f06File_, f07File_, grpFile_);
	}

	@Override
	protected AircraftModel call() throws Exception {

		// check permission
		checkPermission(Permission.ADD_NEW_AIRCRAFT_MODEL);

		// get program and model name
		String program = (String) info_.getInfo(AircraftModelInfoType.AC_PROGRAM);
		String modelName = (String) info_.getInfo(AircraftModelInfoType.MODEL_NAME);

		// update progress info
		updateTitle("Loading aircraft model " + program + ", " + modelName);

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// create aircraft model
				AircraftModel acModel = createAircraftModel(connection, program, modelName);

				// task cancelled
				if (isCancelled() || acModel == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// load grid data file (*.f07)
				ArrayList<Integer> ignoredGridIDs = new LoadGridDataFile(this, acModel, f07File_).start(connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// load element data file (*.f06)
				new LoadElementDataFile(this, acModel, f06File_, ignoredGridIDs).start(connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// load element groups data file (*.grp)
				new LoadElementGroupsFile(this, acModel, grpFile_).start(connection);

				// task cancelled
				if (isCancelled()) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return model
				return acModel;
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
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get aircraft model
			AircraftModel model = get();

			// create and add empty load cases folder
			model.getChildren().add(new AircraftLoadCases(model.getID()));

			// create and add empty equivalent stresses folder
			model.getChildren().add(new AircraftEquivalentStresses(model.getID()));

			// create and add empty pilot points folder
			model.getChildren().add(new PilotPoints(model.getID()));

			// add model to file tree
			taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot().getChildren().add(model);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Creates and returns new A/C model in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param program
	 *            A/C program.
	 * @param modelName
	 *            A/C model name.
	 * @return Newly created A/C model.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private AircraftModel createAircraftModel(Connection connection, String program, String modelName) throws Exception {

		// update info
		updateMessage("Creating new A/C model in database...");

		// check if any model exists with same program and model name
		String sql = "select model_id from ac_models where ac_program = '" + program + "' and name = '" + modelName + "'";
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next())
					throw new Exception("Cannot create A/C model. An A/C model with same program and model name already exists in the database.");
			}
		}

		// zip files
		ArrayList<Path> files = new ArrayList<>();
		files.add(f06File_);
		files.add(f07File_);
		Path zipFile = getWorkingDirectory().resolve(program + FileType.ZIP.getExtension());
		Utility.zipFiles(files, zipFile.toFile(), this);

		// create model
		sql = "insert into ac_models(ac_program, name, delivery_ref, description, data) values(?, ?, ?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// create input stream
			try (InputStream inputStream = Files.newInputStream(zipFile)) {

				// set program and model name
				update.setString(1, program);
				update.setString(2, modelName);

				// get A/C model info (if there is)
				String delRef = null, description = null;
				if (info_ != null) {
					delRef = (String) info_.getInfo(AircraftModelInfoType.DELIVERY_REF);
					description = (String) info_.getInfo(AircraftModelInfoType.DESCRIPTION);
				}

				// set info
				if (delRef == null || delRef.trim().isEmpty()) {
					update.setString(3, "DRAFT");
				}
				else {
					update.setString(3, delRef.trim());
				}
				if (description == null || description.trim().isEmpty()) {
					update.setNull(4, java.sql.Types.VARCHAR);
				}
				else {
					update.setString(4, description.trim());
				}

				// set data
				update.setBlob(5, inputStream, zipFile.toFile().length());

				// execute update
				update.executeUpdate();
			}

			// get result set
			try (ResultSet resultSet = update.getGeneratedKeys()) {

				// return file ID
				resultSet.next();
				return new AircraftModel(program, modelName, resultSet.getBigDecimal(1).intValue());
			}
		}
	}
}
