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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.dataServer.remote.data.MultiplicationTableInfo;
import equinox.dataServer.remote.data.MultiplicationTableInfo.MultiplicationTableInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.UploadMultiplicationTablesRequest;
import equinox.dataServer.remote.message.UploadMultiplicationTablesResponse;
import equinox.network.DataServerManager;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;
import jxl.Sheet;
import jxl.Workbook;

/**
 * Class for upload multiplication tables task.
 *
 * @author Murat Artim
 * @date Feb 19, 2016
 * @time 1:38:10 PM
 */
public class UploadMultiplicationTables extends TemporaryFileCreatingTask<Boolean> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Zip files containing the multiplication tables to upload. */
	private final List<File> files_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates upload multiplication tables task.
	 *
	 * @param files
	 *            Zip files containing the multiplication tables to upload.
	 */
	public UploadMultiplicationTables(List<File> files) {
		files_ = files;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Upload multiplication tables";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.UPLOAD_MULTIPLICATION_TABLES);

		// update progress info
		updateTitle("Uploading multiplication tables");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isUploaded = false;

		try {

			// create request message
			UploadMultiplicationTablesRequest request = new UploadMultiplicationTablesRequest();
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
			else if (message instanceof UploadMultiplicationTablesResponse) {
				isUploaded = ((UploadMultiplicationTablesResponse) message).isUploaded();
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
	private void uploadFiles(UploadMultiplicationTablesRequest request) throws Exception {

		// get connection to filer
		try (FilerConnection filer = getFilerConnection()) {

			// loop over zip files
			for (int i = 0; i < files_.size(); i++) {

				// task cancelled
				if (isCancelled())
					return;

				// get zip file
				Path zipFile = files_.get(i).toPath();

				// progress info
				updateMessage("Processing upload archive '" + zipFile.getFileName() + "'...");
				updateProgress(0, 100);

				// create temporary directory for multiplication table
				Path tempDir = Files.createDirectory(getWorkingDirectory().resolve("Package_" + i));

				// extract zip file
				updateMessage("Extracting upload archive '" + zipFile.getFileName() + "'...");
				ArrayList<Path> extractedFiles = Utility.extractAllFilesFromZIP(zipFile, this, tempDir);

				// task cancelled
				if (isCancelled())
					return;

				// get info file
				Path infoFile = getInfoFile(zipFile, extractedFiles);

				// task cancelled
				if (isCancelled())
					return;

				// upload
				upload(request, zipFile, tempDir, extractedFiles, infoFile, filer);
			}
		}
	}

	/**
	 * Performs pilot point upload.
	 *
	 * @param request
	 *            Database request message.
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
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void upload(UploadMultiplicationTablesRequest request, Path zipFile, Path tempDir, ArrayList<Path> extractedFiles, Path infoFile, FilerConnection filer) throws Exception {

		// initialize variables
		Workbook workbook = null;

		try {

			// get workbook
			workbook = Workbook.getWorkbook(infoFile.toFile());

			// get sheet
			Sheet sheet = workbook.getSheet("Multiplication Table Info");

			// null sheet
			if (sheet == null) {
				String message = "Cannot find worksheet 'Multiplication Table Info' in the upload information file 'Multiplication_Table_Info.xls' of ";
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
				String tableName = sheet.getCell(0, i).getContents().trim();
				String spectrumName = sheet.getCell(1, i).getContents().trim();
				String pilotPointName = sheet.getCell(2, i).getContents().trim();
				String program = sheet.getCell(3, i).getContents().trim();
				String section = sheet.getCell(4, i).getContents().trim();
				String mission = sheet.getCell(5, i).getContents().trim();
				String issue = sheet.getCell(6, i).getContents().trim();
				String delRef = sheet.getCell(7, i).getContents().trim();
				String description = sheet.getCell(8, i).getContents().trim();

				// check values
				if (tableName.isEmpty() || spectrumName.isEmpty() || program.isEmpty() || section.isEmpty() || mission.isEmpty()) {
					String message = "The upload information file 'Multiplication_Table_Info.xls' of ";
					message += "the ZIP archive '" + zipFile.toString() + "' has missing entries. Aborting operation.";
					throw new Exception(message);
				}

				// progress info
				updateMessage("Uploading multiplication table '" + tableName + "'...");
				updateProgress(i, endRow);

				// zip MUT file
				Path mutFile = zipMUTFile(extractedFiles, zipFile, tableName);

				// task cancelled
				if (isCancelled())
					return;

				// upload MUT file to filer
				String dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.MULT_TABLES), program, section, mission);
				String url = dir + "/" + mutFile.getFileName().toString();
				filer.getSftpChannel().put(mutFile.toString(), url);

				// create multiplication table info and add to message
				MultiplicationTableInfo info = new MultiplicationTableInfo();
				info.setInfo(MultiplicationTableInfoType.NAME, tableName);
				info.setInfo(MultiplicationTableInfoType.SPECTRUM_NAME, spectrumName);
				info.setInfo(MultiplicationTableInfoType.PILOT_POINT_NAME, pilotPointName);
				info.setInfo(MultiplicationTableInfoType.AC_PROGRAM, program);
				info.setInfo(MultiplicationTableInfoType.AC_SECTION, section);
				info.setInfo(MultiplicationTableInfoType.FAT_MISSION, mission);
				info.setInfo(MultiplicationTableInfoType.ISSUE, issue);
				info.setInfo(MultiplicationTableInfoType.DELIVERY_REF, delRef);
				info.setInfo(MultiplicationTableInfoType.DESCRIPTION, description);
				info.setInfo(MultiplicationTableInfoType.DATA_URL, url);
				request.addMultiplicationTableInfo(info);
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
	 * Zips and returns MUT file.
	 *
	 * @param extractedFiles
	 *            Extracted files.
	 * @param zipFile
	 *            Zip file containing the inputs.
	 * @param tableName
	 *            Multiplication table name.
	 * @return Zipped file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path zipMUTFile(ArrayList<Path> extractedFiles, Path zipFile, String tableName) throws Exception {

		// get MUT file
		Path mutFile = null;
		for (Path file : extractedFiles)
			if (FileType.getNameWithoutExtension(file).equals(tableName)) {
				mutFile = file;
				break;
			}

		// check files
		if (mutFile == null) {
			String message = "Multiplication table file '" + tableName + "' ";
			message += "could not be found in '" + zipFile.toString() + "'. Aborting operation.";
			throw new Exception(message);
		}

		// get parent directory
		Path parentDir = mutFile.getParent();
		if (parentDir == null)
			throw new Exception("Cannot get multiplication table parent directory.");

		// create output file path
		Path output = parentDir.resolve(tableName + FileType.ZIP.getExtension());

		// zip files
		Utility.zipFile(mutFile, output.toFile(), this);

		// return output zip file
		return output;
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
			if (fileName.toString().equals("Multiplication_Table_Info.xls"))
				return file;
		}
		String message = "The ZIP archive '" + zipFile.toString() + "' does NOT contain the upload information file 'Multiplication_Table_Info.xls'. ";
		message += "Upload information is required in order to perform the multiplication table upload. Aborting operation.";
		throw new Exception(message);
	}
}
