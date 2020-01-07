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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.UploadPilotPointsRequest;
import equinox.dataServer.remote.message.UploadPilotPointsResponse;
import equinox.network.DataServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.SingleInputTask;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;
import jxl.Sheet;
import jxl.Workbook;

/**
 * Class for upload pilot points task.
 *
 * @author Murat Artim
 * @date Feb 11, 2016
 * @time 4:38:06 PM
 */
public class UploadPilotPoints extends TemporaryFileCreatingTask<Boolean> implements LongRunningTask, DatabaseQueryListenerTask, SingleInputTask<Path> {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Zip file containing the pilot points to upload. */
	private Path zipFile_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates upload pilot points task.
	 *
	 * @param zipFile
	 *            Zip file containing the pilot points to upload. Can be null for automatic execution.
	 */
	public UploadPilotPoints(Path zipFile) {
		zipFile_ = zipFile;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Upload pilot points";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	public void setAutomaticInput(Path input) {
		zipFile_ = input;
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.UPLOAD_PILOT_POINTS);

		// update progress info
		updateTitle("Uploading pilot points");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isUploaded = false;

		try {

			// create request message
			UploadPilotPointsRequest request = new UploadPilotPointsRequest();
			request.setListenerHashCode(hashCode());

			// upload files to filer
			uploadFiles(request);

			// task cancelled
			if (isCancelled())
				return null;

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getDataServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForDataServer(this, isQueryCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// task cancelled
			if (isCancelled())
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
			else if (message instanceof UploadPilotPointsResponse) {
				isUploaded = ((UploadPilotPointsResponse) message).isUploaded();
			}

			// return result
			return isUploaded;
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
	}

	/**
	 * Uploads files to filer.
	 *
	 * @param request
	 *            Request message.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void uploadFiles(UploadPilotPointsRequest request) throws Exception {

		// get connection to filer
		try (FilerConnection filer = getFilerConnection()) {

			// task cancelled
			if (isCancelled())
				return;

			// progress info
			updateMessage("Processing upload archive '" + zipFile_.getFileName() + "'...");
			updateProgress(0, 100);

			// get working directory
			Path tempDir = getWorkingDirectory();

			// extract zip file
			updateMessage("Extracting upload archive '" + zipFile_.getFileName() + "'...");
			ArrayList<Path> extractedFiles = Utility.extractAllFilesFromZIP(zipFile_, this, tempDir);

			// task cancelled
			if (isCancelled())
				return;

			// get info file
			Path infoFile = getInfoFile(zipFile_, extractedFiles);

			// task cancelled
			if (isCancelled())
				return;

			// upload pilot points
			uploadPilotPoints(zipFile_, tempDir, extractedFiles, infoFile, filer, request);
		}
	}

	/**
	 * Performs pilot point upload.
	 *
	 * @param zipFile
	 *            Zip file containing pilot points to be uploaded.
	 * @param tempDir
	 *            Temporary directory.
	 * @param extractedFiles
	 *            Extracted files.
	 * @param infoFile
	 *            Pilot point info file.
	 * @param filer
	 *            Filer connection.
	 * @param request
	 *            Request message.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void uploadPilotPoints(Path zipFile, Path tempDir, ArrayList<Path> extractedFiles, Path infoFile, FilerConnection filer, UploadPilotPointsRequest request) throws Exception {

		// initialize variables
		Workbook workbook = null;

		try {

			// get workbook
			workbook = Workbook.getWorkbook(infoFile.toFile());

			// get sheet
			Sheet sheet = workbook.getSheet(0);

			// null sheet
			if (sheet == null) {
				String message = "Cannot find worksheet 'Pilot Point Info' in the upload information file 'Pilot_Point_Info.xls' of ";
				message += "the ZIP archive '" + zipFile.toString() + "'. Aborting operation.";
				throw new Exception(message);
			}

			// loop over rows
			int endRow = sheet.getRows() - 1;
			for (int i = 1; i <= endRow; i++) {

				// task cancelled
				if (isCancelled())
					return;

				// get pilot point info
				String directoryName = sheet.getCell(0, i).getContents().trim();
				String pilotPointName = sheet.getCell(1, i).getContents().trim();
				String spectrumName = sheet.getCell(2, i).getContents().trim();
				String program = sheet.getCell(3, i).getContents().trim();
				String section = sheet.getCell(4, i).getContents().trim();
				String mission = sheet.getCell(5, i).getContents().trim();
				String description = sheet.getCell(6, i).getContents().trim();
				String dataSource = sheet.getCell(7, i).getContents().trim();
				String generationSource = sheet.getCell(8, i).getContents().trim();
				String delRef = sheet.getCell(9, i).getContents().trim();
				String issue = sheet.getCell(10, i).getContents().trim();
				String eid = sheet.getCell(11, i).getContents().trim();
				String elementType = sheet.getCell(12, i).getContents().trim();
				String framePos = sheet.getCell(13, i).getContents().trim();
				String stringerPos = sheet.getCell(14, i).getContents().trim();
				String fatigueMaterial = sheet.getCell(15, i).getContents().trim();
				String preffasMaterial = sheet.getCell(16, i).getContents().trim();
				String linearMaterial = sheet.getCell(17, i).getContents().trim();

				// check values
				if (directoryName.isEmpty() || pilotPointName.isEmpty() || spectrumName.isEmpty() || mission.isEmpty() || description.isEmpty() || dataSource.isEmpty() || generationSource.isEmpty() || delRef.isEmpty() || issue.isEmpty()) {
					String message = "The upload information file 'Pilot_Point_Info.xls' of ";
					message += "the ZIP archive '" + zipFile.toString() + "' has missing entries. Aborting operation.";
					throw new Exception(message);
				}

				// progress info
				updateMessage("Uploading pilot point '" + pilotPointName + "'...");
				updateProgress(i, endRow);

				// create and add pilot point info to request
				PilotPointInfo info = new PilotPointInfo();
				info.setInfo(PilotPointInfoType.SPECTRUM_NAME, spectrumName);
				info.setInfo(PilotPointInfoType.NAME, pilotPointName);
				info.setInfo(PilotPointInfoType.AC_PROGRAM, program);
				info.setInfo(PilotPointInfoType.AC_SECTION, section);
				info.setInfo(PilotPointInfoType.FAT_MISSION, mission);
				info.setInfo(PilotPointInfoType.DESCRIPTION, description);
				info.setInfo(PilotPointInfoType.DATA_SOURCE, dataSource);
				info.setInfo(PilotPointInfoType.GENERATION_SOURCE, generationSource);
				info.setInfo(PilotPointInfoType.DELIVERY_REF_NUM, delRef);
				info.setInfo(PilotPointInfoType.ISSUE, issue);
				info.setInfo(PilotPointInfoType.EID, eid);
				info.setInfo(PilotPointInfoType.ELEMENT_TYPE, elementType);
				info.setInfo(PilotPointInfoType.FRAME_RIB_POSITION, framePos);
				info.setInfo(PilotPointInfoType.STRINGER_POSITION, stringerPos);
				info.setInfo(PilotPointInfoType.FATIGUE_MATERIAL, fatigueMaterial);
				info.setInfo(PilotPointInfoType.PREFFAS_MATERIAL, preffasMaterial);
				info.setInfo(PilotPointInfoType.LINEAR_MATERIAL, linearMaterial);
				request.addInfo(info);

				// zip pilot point file
				Path stfFile = zipSTFFile(tempDir.resolve(directoryName), zipFile, pilotPointName);

				// task cancelled
				if (isCancelled())
					return;

				// upload pilot point data
				String dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_DATA), program, section, mission);
				String url = dir + "/" + stfFile.getFileName().toString();
				filer.getSftpChannel().put(stfFile.toString(), url);
				request.addDataUrl(url);

				// upload pilot point attributes (excel file)
				Path excelFile = zipExcelFile(tempDir.resolve(directoryName), pilotPointName);
				if (excelFile == null) {
					request.addAttributeUrl(null);
				}
				else {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_ATTRIBUTES), program, section, mission);
					url = dir + "/" + excelFile.getFileName().toString();
					filer.getSftpChannel().put(excelFile.toString(), url);
					request.addAttributeUrl(url);
				}

				// create mapping for storing image URLs
				HashMap<PilotPointImageType, String> imageUrls = new HashMap<>();

				// upload pilot point image
				Path pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.IMAGE);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_IMAGES), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.IMAGE, url);
				}

				// upload pilot point mission profile plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.MISSION_PROFILE);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_MISSION_PROFILE_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.MISSION_PROFILE, url);
				}

				// upload pilot point longest flight plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.LONGEST_FLIGHT);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_LONGEST_TYPICAL_FLIGHT_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.LONGEST_FLIGHT, url);
				}

				// upload pilot point highest occurrence flight plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_TYPICAL_FLIGHT_WITH_HIGHEST_OCCURRENCE_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE, url);
				}

				// upload pilot point highest stress flight plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_TYPICAL_FLIGHT_WITH_HIGHEST_STRESS_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS, url);
				}

				// upload pilot point level crossing plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.LEVEL_CROSSING);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_LEVEL_CROSSING_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.LEVEL_CROSSING, url);
				}

				// upload pilot point damage angle plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.DAMAGE_ANGLE);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_DAMAGE_ANGLE_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.DAMAGE_ANGLE, url);
				}

				// upload pilot point number of peaks plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.NUMBER_OF_PEAKS);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_NUMBER_OF_PEAKS_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.NUMBER_OF_PEAKS, url);
				}

				// upload pilot point flight occurrence plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.FLIGHT_OCCURRENCE);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_FLIGHT_OCCURRENCE_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.FLIGHT_OCCURRENCE, url);
				}

				// upload pilot point rainflow histogram plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.RAINFLOW_HISTOGRAM);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_RAINFLOW_HISTOGRAM_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.RAINFLOW_HISTOGRAM, url);
				}

				// upload pilot point loadcase damage contribution plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.LOADCASE_DAMAGE_CONTRIBUTION);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_LOADCASE_DAMAGE_CONTRIBUTION_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.LOADCASE_DAMAGE_CONTRIBUTION, url);
				}

				// upload pilot point typical flight damage contribution plot
				pngFile = getPNGFile(tempDir.resolve(directoryName), PilotPointImageType.FLIGHT_DAMAGE_CONTRIBUTION);
				if (pngFile != null) {
					dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.PP_TYPICAL_FLIGHT_DAMAGE_CONTRIBUTION_PLOTS), program, section, mission);
					url = dir + "/" + pilotPointName + ".png";
					filer.getSftpChannel().put(pngFile.toString(), url);
					imageUrls.put(PilotPointImageType.FLIGHT_DAMAGE_CONTRIBUTION, url);
				}

				// add image mapping to request
				request.addImageUrls(imageUrls);
			}
		}

		// close workbook
		finally {
			if (workbook != null) {
				workbook.close();
			}
		}
	}

	/**
	 * Zips pilot point attribute file.
	 *
	 * @param directory
	 *            Directory containing the pilot point files.
	 * @param pilotPointName
	 *            Pilot point name.
	 * @return Path to zipped archive.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path zipExcelFile(Path directory, String pilotPointName) throws Exception {

		// get pilot point files
		Path excelFile = null;
		File[] files = directory.toFile().listFiles();
		if (files != null) {
			for (File file : files) {

				// get file type
				FileType type = FileType.getFileType(file);

				// unknown type
				if (type == null) {
					continue;
				}

				// set files
				if (type.equals(FileType.XLS) || type.equals(FileType.XLSX)) {
					excelFile = file.toPath();
				}
			}
		}

		// check files
		if (excelFile == null)
			return null;

		// create output file path
		Path output = directory.resolve(pilotPointName + "_attributes" + FileType.ZIP.getExtension());

		// zip files
		Utility.zipFile(excelFile, output.toFile(), this);

		// return output zip file
		return output;
	}

	/**
	 * Zips pilot point STF file.
	 *
	 * @param directory
	 *            Directory containing the pilot point files.
	 * @param zipFile
	 *            Zip file.
	 * @param pilotPointName
	 *            Pilot point name.
	 * @return Path to zipped archive.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path zipSTFFile(Path directory, Path zipFile, String pilotPointName) throws Exception {

		// get pilot point files
		Path stfFile = null;
		File[] files = directory.toFile().listFiles();
		if (files != null) {
			for (File file : files) {

				// get file type
				FileType type = FileType.getFileType(file);

				// unknown type
				if (type == null) {
					continue;
				}

				// set files
				if (type.equals(FileType.STF)) {
					stfFile = file.toPath();
				}
			}
		}

		// check files
		if (stfFile == null) {
			String message = "Pilot point contained in directory '" + directory.getFileName() + "' in ";
			message += "the ZIP archive '" + zipFile.toString() + "' does NOT contain STF file. Aborting operation.";
			throw new Exception(message);
		}

		// create output file path
		Path output = directory.resolve(pilotPointName + FileType.ZIP.getExtension());

		// zip files
		Utility.zipFile(stfFile, output.toFile(), this);

		// return output zip file
		return output;
	}

	/**
	 * Zips pilot point PNG file.
	 *
	 * @param directory
	 *            Directory containing the pilot point files.
	 * @param imageType
	 *            Image type.
	 * @return Path to PNG file, or null if there is no PNG file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Path getPNGFile(Path directory, PilotPointImageType imageType) throws Exception {

		// get pilot point files
		Path pngFile = null;
		File[] files = directory.toFile().listFiles();
		if (files != null) {
			for (File file : files) {

				// get file type
				FileType type = FileType.getFileType(file);

				// unknown type
				if (type == null) {
					continue;
				}

				// set files
				if (type.equals(FileType.PNG) && file.getName().equals(imageType.getFileName())) {
					pngFile = file.toPath();
				}
			}
		}

		// return file
		return pngFile;
	}

	/**
	 * Retrieves the spectrum info file from within the given zip file.
	 *
	 * @param zipFile
	 *            Zip file.
	 * @param extractedFiles
	 *            Extracted files.
	 * @return Path to spectrum info file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Path getInfoFile(Path zipFile, ArrayList<Path> extractedFiles) throws Exception {
		for (Path file : extractedFiles) {
			Path fileName = file.getFileName();
			if (fileName == null) {
				continue;
			}
			if (fileName.toString().equals("Pilot_Point_Info.xls"))
				return file;
		}
		String message = "The ZIP archive '" + zipFile.toString() + "' does NOT contain the upload information file 'Pilot_Point_Info.xls'. ";
		message += "Upload information is required in order to perform the pilot point upload. Aborting operation.";
		throw new Exception(message);
	}
}
