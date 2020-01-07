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
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import equinox.Equinox;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.GetPilotPointImagesRequest;
import equinox.dataServer.remote.message.GetPilotPointImagesResponse;
import equinox.network.DataServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.DatabaseQueryListenerTask;
import equinox.task.TemporaryFileCreatingTask;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for loading STF files.
 *
 * @author Murat Artim
 * @date Feb 10, 2014
 * @time 3:29:36 PM
 */
public class LoadSTFFile implements EquinoxProcess<STFFile>, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** The owner task of this process. */
	private final TemporaryFileCreatingTask<?> task_;

	/** Input STF file. */
	private final Path inputFile_;

	/** CDF set. */
	private final Spectrum cdfSet_;

	/** Pilot point info. */
	private final PilotPointInfo info_;

	/** Stress table ID. */
	private final int stressTableID_;

	/** Parameters. */
	private int readLines_, allLines_;

	/** Update message header. */
	private String line_, eid_ = null;

	/** True if progress information should be updated during process. */
	private final boolean updateProgress_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates load STF file process.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param inputFile
	 *            Input STF file.
	 * @param cdfSet
	 *            CDF set.
	 * @param info
	 *            Pilot point info (can be null).
	 * @param updateProgress
	 *            True if progress information should be updated during process.
	 * @param stressTableID
	 *            Stress table ID.
	 */
	public LoadSTFFile(TemporaryFileCreatingTask<?> task, Path inputFile, Spectrum cdfSet, PilotPointInfo info, boolean updateProgress, int stressTableID) {
		task_ = task;
		inputFile_ = inputFile;
		cdfSet_ = cdfSet;
		info_ = info;
		updateProgress_ = updateProgress;
		stressTableID_ = stressTableID;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	/**
	 * Returns the CDF set of the process.
	 *
	 * @return The CDF set of the process.
	 */
	public Spectrum getCDFSet() {
		return cdfSet_;
	}

	/**
	 * Returns the input file of the process.
	 *
	 * @return The input file of the process.
	 */
	public Path getInputFile() {
		return inputFile_;
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, task_, serverMessageRef, isQueryCompleted);
	}

	/**
	 * Starts load STF file process.
	 *
	 * @param connection
	 *            Database connection.
	 * @param preparedStatements
	 *            The following prepared statements must be supplied in order;
	 *            <UL>
	 *            <LI>Prepared statement for inserting STF files,
	 *            <LI>Prepared statement for inserting STF stresses,
	 *            <LI>Prepared statement for updating stress state.
	 *            </UL>
	 * @return The output spectrum item.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	@Override
	public STFFile start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// get number of lines of file
		task_.updateMessage("Getting STF file size...");
		if (updateProgress_) {
			allLines_ = Utility.countLines(inputFile_, task_);
		}
		readLines_ = 0;

		// add file to files table
		int fileID = addToFilesTable(connection, preparedStatements[0], inputFile_);
		boolean is2D = false;

		// create file reader
		try (BufferedReader reader = Files.newBufferedReader(inputFile_, Charset.defaultCharset())) {

			// read file till the end
			while ((line_ = reader.readLine()) != null) {

				// task cancelled
				if (task_.isCancelled())
					return null;

				// increment read lines
				readLines_++;

				// update progress
				if (updateProgress_) {
					task_.updateProgress(readLines_, allLines_);
				}

				// skip comment lines
				if (readLines_ < 2) {
					continue;
				}

				// add stresses
				is2D = addToStressesTable(preparedStatements[1], reader, fileID);
			}
		}

		// update stress state
		if (is2D) {
			preparedStatements[2].setBoolean(1, true);
			preparedStatements[2].setInt(2, fileID);
			preparedStatements[2].executeUpdate();
		}

		// get input file name
		Path inputFileName = inputFile_.getFileName();
		if (inputFileName == null)
			throw new Exception("Cannot get input STF file name.");

		// create STF file
		STFFile stfFile = new STFFile(inputFileName.toString(), fileID, is2D, stressTableID_);

		// set element ID
		stfFile.setEID(eid_);

		// return STF file
		return stfFile;
	}

	/**
	 * Adds input STF file to files table.
	 *
	 * @param connection
	 *            Database connection.
	 * @param insertFile
	 *            Insert STF file statement.
	 * @param inputFile
	 *            Input STF file.
	 * @return The file ID of the added file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int addToFilesTable(Connection connection, PreparedStatement insertFile, Path inputFile) throws Exception {

		// get input file name
		Path inputFileName = inputFile.getFileName();
		if (inputFileName == null)
			throw new Exception("Cannot get input STF file name.");

		// initialize ID
		int id = -1;

		// get pilot point images (if any)
		HashMap<PilotPointImageType, byte[]> images = null;
		if (info_ != null) {
			if (Equinox.USER.hasPermission(Permission.GET_PILOT_POINT_IMAGES, false, task_.getTaskPanel().getOwner().getOwner())) {
				images = getPilotPointImages();
			}
		}

		// update info
		task_.updateMessage("Saving STF file info to database...");

		// set basic info
		insertFile.setInt(1, cdfSet_.getID()); // CDF set ID
		insertFile.setInt(2, stressTableID_); // stress table ID
		insertFile.setString(3, inputFileName.toString()); // file name
		insertFile.setBoolean(4, false); // 1D stress state (for now)

		// get pilot point info
		String description = null, elementType = null, framePos = null, stringerPos = null, dataSource = null, genSource = null, delRef = null, issue = null, fatigueMaterial = null, preffasMaterial = null, linearMaterial = null;
		if (info_ != null) {
			description = (String) info_.getInfo(PilotPointInfoType.DESCRIPTION);
			elementType = (String) info_.getInfo(PilotPointInfoType.ELEMENT_TYPE);
			framePos = (String) info_.getInfo(PilotPointInfoType.FRAME_RIB_POSITION);
			stringerPos = (String) info_.getInfo(PilotPointInfoType.STRINGER_POSITION);
			dataSource = (String) info_.getInfo(PilotPointInfoType.DATA_SOURCE);
			genSource = (String) info_.getInfo(PilotPointInfoType.GENERATION_SOURCE);
			delRef = (String) info_.getInfo(PilotPointInfoType.DELIVERY_REF_NUM);
			issue = (String) info_.getInfo(PilotPointInfoType.ISSUE);
			eid_ = (String) info_.getInfo(PilotPointInfoType.EID);
			fatigueMaterial = (String) info_.getInfo(PilotPointInfoType.FATIGUE_MATERIAL);
			preffasMaterial = (String) info_.getInfo(PilotPointInfoType.PREFFAS_MATERIAL);
			linearMaterial = (String) info_.getInfo(PilotPointInfoType.LINEAR_MATERIAL);
		}

		// set info
		if (description == null || description.trim().isEmpty()) {
			insertFile.setNull(5, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(5, description.trim());
		}
		if (elementType == null || elementType.trim().isEmpty()) {
			insertFile.setNull(6, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(6, elementType.trim());
		}
		if (framePos == null || framePos.trim().isEmpty()) {
			insertFile.setNull(7, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(7, framePos.trim());
		}
		if (stringerPos == null || stringerPos.trim().isEmpty()) {
			insertFile.setNull(8, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(8, stringerPos.trim());
		}
		if (dataSource == null || dataSource.trim().isEmpty()) {
			insertFile.setNull(9, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(9, dataSource.trim());
		}
		if (genSource == null || genSource.trim().isEmpty()) {
			insertFile.setNull(10, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(10, genSource.trim());
		}
		if (delRef == null || delRef.trim().isEmpty()) {
			insertFile.setString(11, "DRAFT");
		}
		else {
			insertFile.setString(11, delRef.trim());
		}
		if (issue == null || issue.trim().isEmpty()) {
			insertFile.setNull(12, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(12, issue.trim());
		}
		if (eid_ == null || eid_.trim().isEmpty()) {

			// extract EID from STF file name
			eid_ = STFFile.getEID(inputFileName.toString());

			// no EID found
			if (eid_ == null || eid_.trim().isEmpty()) {
				insertFile.setNull(13, java.sql.Types.VARCHAR);
			}

			// EID found
			else {
				insertFile.setString(13, eid_.trim());
			}
		}
		else {
			insertFile.setString(13, eid_.trim());
		}
		if (fatigueMaterial == null || fatigueMaterial.trim().isEmpty()) {
			insertFile.setNull(14, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(14, fatigueMaterial.trim());
		}
		if (preffasMaterial == null || preffasMaterial.trim().isEmpty()) {
			insertFile.setNull(15, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(15, preffasMaterial.trim());
		}
		if (linearMaterial == null || linearMaterial.trim().isEmpty()) {
			insertFile.setNull(16, java.sql.Types.VARCHAR);
		}
		else {
			insertFile.setString(16, linearMaterial.trim());
		}

		// execute insertFile
		insertFile.executeUpdate();

		// get result set
		try (ResultSet resultSet = insertFile.getGeneratedKeys()) {

			// return file ID
			resultSet.next();
			id = resultSet.getBigDecimal(1).intValue();
		}

		// set pilot point images
		if (id != -1 && images != null) {
			Iterator<Entry<PilotPointImageType, byte[]>> iterator = images.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<PilotPointImageType, byte[]> entry = iterator.next();
				String sql = "insert into " + entry.getKey().getTableName() + "(id, image) values(?, ?)";
				try (PreparedStatement update = connection.prepareStatement(sql)) {
					byte[] imageBytes = entry.getValue();
					try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
						update.setInt(1, id);
						update.setBlob(2, inputStream, imageBytes.length);
						update.executeUpdate();
					}
				}
			}
		}

		// return id
		return id;
	}

	/**
	 * Downloads and returns pilot point images from global database.
	 *
	 * @return Image bytes.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private HashMap<PilotPointImageType, byte[]> getPilotPointImages() throws Exception {

		// update progress info
		task_.updateTitle("Getting STF image from global database...");
		task_.updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// get pilot point id
			Long id = (Long) info_.getInfo(PilotPointInfoType.ID);
			if (id == null || id < 0L)
				return null;

			// create request message
			GetPilotPointImagesRequest request = new GetPilotPointImagesRequest();
			request.setListenerHashCode(hashCode());
			request.setPilotPointId(id);

			// disable task canceling
			task_.getTaskPanel().updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = task_.getTaskPanel().getOwner().getOwner().getDataServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForDataServer(task_, isQueryCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			task_.getTaskPanel().updateCancelState(true);

			// task cancelled
			if (task_.isCancelled())
				return null;

			// get query message
			DataMessage message = serverMessageRef.get();

			// permission denied
			if (message instanceof DatabaseQueryPermissionDenied)
				throw new PermissionDeniedException(((DatabaseQueryPermissionDenied) message).getPermission());

			// query failed
			else if (message instanceof DatabaseQueryFailed)
				throw new ServerDatabaseQueryFailedException((DatabaseQueryFailed) message);

			// query succeeded
			else if (message instanceof GetPilotPointImagesResponse) {

				// get image URL
				HashMap<PilotPointImageType, String> imageUrls = ((GetPilotPointImagesResponse) message).getImageUrls();

				// no image
				if (imageUrls == null || imageUrls.isEmpty())
					return null;

				// initialize byte array
				HashMap<PilotPointImageType, byte[]> images = new HashMap<>();

				// get connection to filer
				try (FilerConnection filer = task_.getFilerConnection()) {

					// loop over image types
					for (PilotPointImageType imageType : PilotPointImageType.values()) {

						// get image URL
						String url = imageUrls.get(imageType);

						// image not available
						if (url == null) {
							continue;
						}

						// download image
						Path imageFile = task_.getWorkingDirectory().resolve(FileType.getNameWithoutExtension(imageType.getFileName()) + "_" + id.toString() + ".png");
						filer.getSftpChannel().get(url, imageFile.toString());

						// read image bytes
						byte[] imageBytes = new byte[(int) imageFile.toFile().length()];
						try (ImageInputStream imgStream = ImageIO.createImageInputStream(imageFile.toFile())) {
							imgStream.read(imageBytes);
						}

						// put it in image mapping
						images.put(imageType, imageBytes);
					}
				}

				// return image bytes
				return images;
			}

			// invalid server response
			throw new Exception("Invalid server response received to a pilot point image request.");
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}

	/**
	 * Adds STF stresses into stresses table.
	 *
	 * @param insertStresses
	 *            Prepared statement for inserting STF stresses.
	 * @param reader
	 *            File reader.
	 * @param fileID
	 *            STF file ID.
	 * @return True if 2D stress state.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private boolean addToStressesTable(PreparedStatement insertStresses, BufferedReader reader, int fileID) throws Exception {

		// update info
		task_.updateMessage("Saving STF stresses to database...");

		// initialize stress state
		boolean is2D = false;

		// read till the end
		String delimiter = null;
		while ((line_ = reader.readLine()) != null) {

			// task cancelled
			if (task_.isCancelled()) {
				break;
			}

			// increment read lines
			readLines_++;

			// update progress
			if (updateProgress_) {
				task_.updateProgress(readLines_, allLines_);
			}

			// set column delimiter
			if (delimiter == null) {
				delimiter = line_.trim().contains("\t") ? "\t" : " ";
			}

			// split line
			String[] split = line_.trim().split(delimiter);

			// set file ID
			insertStresses.setInt(1, fileID);

			// loop over columns
			int index = 0;
			for (String col : split) {

				// invalid value
				if (col == null || col.isEmpty()) {
					continue;
				}

				// trim spaces
				col = col.trim();

				// invalid value
				if (col.isEmpty()) {
					continue;
				}

				// issy code
				if (index == 0) {
					insertStresses.setString(2, col);
				}
				else if (index == 1) {
					insertStresses.setDouble(3, Double.parseDouble(col));
				}
				else if (index == 2) {
					insertStresses.setDouble(4, Double.parseDouble(col));
				}
				else if (index == 3) {
					insertStresses.setDouble(5, Double.parseDouble(col));
				}

				// increment index
				index++;
			}

			// only 1 column
			if (index == 2) {
				insertStresses.setDouble(4, 0.0);
				insertStresses.setDouble(5, 0.0);
			}

			// only 2 columns
			else if (index == 3) {
				insertStresses.setDouble(5, 0.0);
			}
			else if (index == 4) {
				is2D = true;
			}

			// execute update
			insertStresses.executeUpdate();
		}

		// return stress state
		return is2D;
	}
}
