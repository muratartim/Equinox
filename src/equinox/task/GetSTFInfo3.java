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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.Triple;
import equinox.data.fileType.STFFile;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import javafx.scene.image.Image;

/**
 * Class for get STF info task. This task is intended to be used for automatic task execution.
 *
 * @author Murat Artim
 * @date 25 Aug 2018
 * @time 11:27:52
 */
public class GetSTFInfo3 extends InternalEquinoxTask<Triple<STFFile, String[], HashMap<PilotPointImageType, Image>>> implements ShortRunningTask, SingleInputTask<STFFile>, AutomaticTaskOwner<Triple<STFFile, String[], HashMap<PilotPointImageType, Image>>> {

	/** STF file. */
	private STFFile stfFile;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Triple<STFFile, String[], HashMap<PilotPointImageType, Image>>>> automaticTasks = null;

	/**
	 * Creates get STF info task.
	 *
	 * @param stfFile
	 *            STF file. Can be null for automatic execution.
	 */
	public GetSTFInfo3(STFFile stfFile) {
		this.stfFile = stfFile;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get STF info";
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Triple<STFFile, String[], HashMap<PilotPointImageType, Image>>> task) {
		if (automaticTasks == null) {
			automaticTasks = new HashMap<>();
		}
		automaticTasks.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Triple<STFFile, String[], HashMap<PilotPointImageType, Image>>>> getAutomaticTasks() {
		return automaticTasks;
	}

	@Override
	public void setAutomaticInput(STFFile input) {
		this.stfFile = input;
	}

	@Override
	protected Triple<STFFile, String[], HashMap<PilotPointImageType, Image>> call() throws Exception {

		// create triple
		Triple<STFFile, String[], HashMap<PilotPointImageType, Image>> results = new Triple<>();

		// set STF file
		results.setElement1(stfFile);

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get pilot point info
				results.setElement2(getPilotPointInfo(statement));

				// get pilot point images
				results.setElement3(getPilotPointImages(statement));
			}
		}

		// return results
		return results;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {

			// get results
			Triple<STFFile, String[], HashMap<PilotPointImageType, Image>> results = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(results, automaticTasks, taskPanel_);
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
		automaticTaskOwnerFailed(automaticTasks);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks);
	}

	/**
	 * Retrieves pilot point images from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Pilot point images.
	 * @throws SQLException
	 *             If exception occurs during process.
	 */
	private HashMap<PilotPointImageType, Image> getPilotPointImages(Statement statement) throws SQLException {

		// get pilot point images
		HashMap<PilotPointImageType, Image> images = new HashMap<>();

		// loop over pilot point image types
		for (PilotPointImageType imageType : PilotPointImageType.values()) {

			// create and execute query
			String sql = "select image from " + imageType.getTableName() + " where id = " + stfFile.getID();
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {

					// set image
					Blob blob = resultSet.getBlob("image");
					if (blob != null) {
						byte[] imageBytes = blob.getBytes(1L, (int) blob.length());
						images.put(imageType, imageBytes == null ? null : new Image(new ByteArrayInputStream(imageBytes)));
						blob.free();
					}
				}
			}
		}

		// return images
		return images;
	}

	/**
	 * Retrieves STF information from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @return STF information.
	 * @throws SQLException
	 *             If exception occurs during process.
	 */
	private String[] getPilotPointInfo(Statement statement) throws SQLException {

		// progress info
		updateMessage("Getting STF info from database");

		// create info list
		String[] info = new String[12];

		// get info
		String sql = "select description, element_type, frame_rib_position, stringer_position, data_source, ";
		sql += "generation_source, delivery_ref_num, issue, eid, fatigue_material, preffas_material, linear_material from stf_files where file_id = " + stfFile.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				info[GetSTFInfo2.DESCRIPTION] = resultSet.getString("description");
				info[GetSTFInfo2.ELEMENT_TYPE] = resultSet.getString("element_type");
				info[GetSTFInfo2.FRAME_RIB_POS] = resultSet.getString("frame_rib_position");
				info[GetSTFInfo2.STRINGER_POS] = resultSet.getString("stringer_position");
				info[GetSTFInfo2.DATA_SOURCE] = resultSet.getString("data_source");
				info[GetSTFInfo2.GEN_SOURCE] = resultSet.getString("generation_source");
				info[GetSTFInfo2.DELIVERY_REF] = resultSet.getString("delivery_ref_num");
				info[GetSTFInfo2.ISSUE] = resultSet.getString("issue");
				info[GetSTFInfo2.EID] = resultSet.getString("eid");
				info[GetSTFInfo2.FATIGUE_MATERIAL] = resultSet.getString("fatigue_material");
				info[GetSTFInfo2.PREFFAS_MATERIAL] = resultSet.getString("preffas_material");
				info[GetSTFInfo2.LINEAR_MATERIAL] = resultSet.getString("linear_material");
			}
		}

		// return info
		return info;
	}
}