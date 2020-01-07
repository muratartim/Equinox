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

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import equinox.Equinox;
import equinox.data.fileType.STFFile;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Task for saving pilot point image.
 *
 * @author Murat Artim
 * @date Feb 24, 2015
 * @time 2:39:27 PM
 */
public class SavePilotPointImage extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** STF file. */
	private final STFFile stfFile_;

	/** Image file. */
	private final Path imageFile_;

	/** Pilot point image type. */
	private final PilotPointImageType imageType_;

	/**
	 * Creates save pilot point image task.
	 *
	 * @param stfFile
	 *            STF file.
	 * @param imageType
	 *            Pilot point image type.
	 * @param imageFile
	 *            Image file. Can be null for resetting the image.
	 */
	public SavePilotPointImage(STFFile stfFile, PilotPointImageType imageType, Path imageFile) {
		stfFile_ = stfFile;
		imageType_ = imageType;
		imageFile_ = imageFile;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save pilot point image";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving pilot point image to database");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// update
				if (checkIfExists(connection)) {
					updateImage(connection);
				}
				else {
					insertImage(connection);
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return
				return null;
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

	/**
	 * Returns true if the STF file has given type of image.
	 *
	 * @param connection
	 *            Database connection
	 * @return True if the STF file has given type of image.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean checkIfExists(Connection connection) throws Exception {
		int count = 0;
		String sql = "select count(id) as idcount from " + imageType_.getTableName() + " where id = " + stfFile_.getID();
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					count = resultSet.getInt("idcount");
				}
			}
		}
		return count != 0;
	}

	/**
	 * Updates STF info in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void insertImage(Connection connection) throws Exception {

		// no image
		if (imageFile_ == null)
			return;

		// prepare statement
		String sql = "insert into " + imageType_.getTableName() + "(id, image) values(?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql)) {
			byte[] imageBytes = new byte[(int) imageFile_.toFile().length()];
			try (ImageInputStream imgStream = ImageIO.createImageInputStream(imageFile_.toFile())) {
				imgStream.read(imageBytes);
				try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
					update.setInt(1, stfFile_.getID());
					update.setBlob(2, inputStream, imageBytes.length);
					update.executeUpdate();
				}
			}
		}
	}

	/**
	 * Updates STF info in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void updateImage(Connection connection) throws Exception {

		// no image (delete)
		if (imageFile_ == null) {
			try (Statement statement = connection.createStatement()) {
				String sql = "delete from " + imageType_.getTableName() + " where id = " + stfFile_.getID();
				statement.executeUpdate(sql);
			}
			return;
		}

		// prepare statement
		String sql = "update " + imageType_.getTableName() + " set image = ? where id = " + stfFile_.getID();
		try (PreparedStatement update = connection.prepareStatement(sql)) {

			// get image bytes
			updateMessage("Retrieving image bytes...");
			byte[] imageBytes = new byte[(int) imageFile_.toFile().length()];
			try (ImageInputStream imgStream = ImageIO.createImageInputStream(imageFile_.toFile())) {
				imgStream.read(imageBytes);
				try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
					updateMessage("Updating pilot point info...");
					update.setBlob(1, inputStream, imageBytes.length);
					update.executeUpdate();
				}
			}
		}
	}
}
