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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.SpectrumItem;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.PostProcessingTask;
import equinox.task.automation.SingleInputTask;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Class for save equivalent stress plot to file task.
 *
 * @author Murat Artim
 * @date 29 Aug 2018
 * @time 15:32:39
 */
public class SaveEquivalentStressPlotToFile extends InternalEquinoxTask<Path> implements ShortRunningTask, SingleInputTask<SpectrumItem>, PostProcessingTask, AutomaticTaskOwner<Path> {

	/** Equivalent stress. */
	private SpectrumItem equivalentStress;

	/** Image file. */
	private final Path output;

	/** Plot type. */
	private final PilotPointImageType plotType;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save equivalent stress plot to file task.
	 *
	 * @param equivalentStress
	 *            Equivalent stress. Can be null for automatic execution.
	 * @param plotType
	 *            Plot type.
	 * @param output
	 *            Output path.
	 */
	public SaveEquivalentStressPlotToFile(SpectrumItem equivalentStress, PilotPointImageType plotType, Path output) {
		this.equivalentStress = equivalentStress;
		this.plotType = plotType;
		this.output = output;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Path> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Path>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save equivalent stress plot to file";
	}

	@Override
	public void setAutomaticInput(SpectrumItem input) {
		equivalentStress = input;
	}

	@Override
	protected Path call() throws Exception {

		// get mission profile image
		Image image = getImage();

		// write image to file and return file
		return writeImageFile(image);
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get output file
			Path file = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(file, automaticTasks_, taskPanel_);
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
	 * Writes out image file to working directory.
	 *
	 * @param image
	 *            Image.
	 * @return Path to image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeImageFile(Image image) throws Exception {

		// update progress info
		updateMessage("Writing image file...");

		// create buffered image
		BufferedImage bufImg = SwingFXUtils.fromFXImage(image, null);

		// write image to file
		ImageIO.write(bufImg, "png", output.toFile());

		// return file
		return output;
	}

	/**
	 * Retrieves mission profile image from database.
	 *
	 * @return Mission profile image.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Image getImage() throws Exception {

		// update progress info
		updateMessage("Getting equivalent stress image from database...");

		// initialize image
		Image image = null;

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create and execute query
				String sql = "select image from " + plotType.getTableName() + " where id = " + equivalentStress.getParentItem().getParentItem().getID();
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
}