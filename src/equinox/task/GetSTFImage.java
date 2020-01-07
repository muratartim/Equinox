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
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.STFFile;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.image.Image;

/**
 * Class for get STF image task.
 *
 * @author Murat Artim
 * @date Feb 7, 2016
 * @time 1:30:09 PM
 */
public class GetSTFImage extends InternalEquinoxTask<Image> implements ShortRunningTask {

	/** STF file. */
	private final STFFile stfFile_;

	/** Requesting panel. */
	private final STFImageRequestingPanel panel_;

	/** Image type. */
	private final PilotPointImageType imageType_;

	/**
	 * Creates get STF image task.
	 *
	 * @param stfFile
	 *            STF file.
	 * @param imageType
	 *            Image type.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetSTFImage(STFFile stfFile, PilotPointImageType imageType, STFImageRequestingPanel panel) {
		stfFile_ = stfFile;
		imageType_ = imageType;
		panel_ = panel;
	}

	@Override
	public String getTaskTitle() {
		return "Get STF image for '" + stfFile_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Image call() throws Exception {

		// update progress info
		updateMessage("Getting STF image from database...");

		// initialize image
		Image image = null;

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create and execute query
				String sql = "select image from " + imageType_.getTableName() + " where id = " + stfFile_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// set image
						Blob blob = resultSet.getBlob("image");
						if (blob != null) {
							byte[] imageBytes = blob.getBytes(1L, (int) blob.length());
							image = imageBytes == null ? null : new Image(new ByteArrayInputStream(imageBytes));
							blob.free();
						}
					}
				}
			}
		}

		// return image
		return image;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			panel_.setImage(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for STF image requesting panels.
	 *
	 * @author Murat Artim
	 * @date Feb 7, 2016
	 * @time 1:40:27 PM
	 */
	public interface STFImageRequestingPanel {

		/**
		 * Sets image to this panel.
		 *
		 * @param image
		 *            Requested image.
		 */
		void setImage(Image image);
	}
}
