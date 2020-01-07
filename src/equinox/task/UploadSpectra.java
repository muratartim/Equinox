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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.UploadSpectraRequest;
import equinox.dataServer.remote.message.UploadSpectraResponse;
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
 * Class for upload spectra task.
 *
 * @author Murat Artim
 * @date Feb 11, 2016
 * @time 10:27:44 AM
 */
public class UploadSpectra extends TemporaryFileCreatingTask<Boolean> implements LongRunningTask, DatabaseQueryListenerTask, SingleInputTask<Path> {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Zip files containing the spectra to upload. */
	private List<File> files_;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates upload spectra task.
	 *
	 * @param files
	 *            Zip files containing the spectra to upload. Can be null for automatic execution.
	 */
	public UploadSpectra(List<File> files) {
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
		return "Upload spectra";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	public void setAutomaticInput(Path input) {
		files_ = Arrays.asList(input.toFile());
	}

	@Override
	protected Boolean call() throws Exception {

		// check permission
		checkPermission(Permission.UPLOAD_SPECTRA);

		// update progress info
		updateTitle("Uploading spectra");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;
		boolean isUploaded = false;

		try {

			// create request message
			UploadSpectraRequest request = new UploadSpectraRequest();
			request.setListenerHashCode(hashCode());

			// get connection to filer
			try (FilerConnection filer = getFilerConnection()) {

				// loop over zip files
				for (int i = 0; i < files_.size(); i++) {

					// task cancelled
					if (isCancelled())
						return null;

					// get zip file
					Path zipFile = files_.get(i).toPath();

					// progress info
					updateMessage("Processing upload archive '" + zipFile.getFileName() + "'...");
					updateProgress(0, 100);

					// create temporary directory for spectrum
					Path tempDir = Files.createDirectory(getWorkingDirectory().resolve("Package_" + i));

					// extract zip file
					updateMessage("Extracting upload archive '" + zipFile.getFileName() + "'...");
					ArrayList<Path> extractedFiles = Utility.extractAllFilesFromZIP(zipFile, this, tempDir);

					// task cancelled
					if (isCancelled())
						return null;

					// get info file
					Path infoFile = getInfoFile(zipFile, extractedFiles);

					// task cancelled
					if (isCancelled())
						return null;

					// upload spectra
					uploadSpectra(zipFile, tempDir, extractedFiles, infoFile, filer, request);
				}
			}

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
			else if (message instanceof UploadSpectraResponse) {
				isUploaded = ((UploadSpectraResponse) message).isUploaded();
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
	 * Performs spectrum upload.
	 *
	 * @param zipFile
	 *            Zip file containing spectra to be uploaded.
	 * @param tempDir
	 *            Temporary directory.
	 * @param extractedFiles
	 *            Extracted files.
	 * @param infoFile
	 *            Spectrum info file.
	 * @param filer
	 *            Filer connection.
	 * @param request
	 *            Request message.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void uploadSpectra(Path zipFile, Path tempDir, ArrayList<Path> extractedFiles, Path infoFile, FilerConnection filer, UploadSpectraRequest request) throws Exception {

		// initialize variables
		Workbook workbook = null;

		try {

			// get workbook
			workbook = Workbook.getWorkbook(infoFile.toFile());

			// get sheet
			Sheet sheet = workbook.getSheet("Spectrum Info");

			// null sheet
			if (sheet == null) {
				String message = "Cannot find worksheet 'Spectrum Info' in the upload information file 'Spectrum_Info.xls' of ";
				message += "the ZIP archive '" + zipFile.toString() + "'. Aborting operation.";
				throw new Exception(message);
			}

			// loop over rows
			int endRow = sheet.getRows() - 1;
			for (int i = 1; i <= endRow; i++) {

				// task cancelled
				if (isCancelled())
					return;

				// get spectrum info
				String directoryName = sheet.getCell(0, i).getContents().trim();
				String spectrumName = sheet.getCell(1, i).getContents().trim();
				String program = sheet.getCell(2, i).getContents().trim();
				String section = sheet.getCell(3, i).getContents().trim();
				String mission = sheet.getCell(4, i).getContents().trim();
				String missionIssue = sheet.getCell(5, i).getContents().trim();
				String flpIssue = sheet.getCell(6, i).getContents().trim();
				String iflpIssue = sheet.getCell(7, i).getContents().trim();
				String cdfIssue = sheet.getCell(8, i).getContents().trim();
				String delRef = sheet.getCell(9, i).getContents().trim();
				String description = sheet.getCell(10, i).getContents().trim();

				// check values
				if (directoryName.isEmpty() || spectrumName.isEmpty() || program.isEmpty() || section.isEmpty() || mission.isEmpty() || missionIssue.isEmpty() || flpIssue.isEmpty() || iflpIssue.isEmpty() || cdfIssue.isEmpty()) {
					String message = "The upload information file 'Spectrum_Info.xls' of ";
					message += "the ZIP archive '" + zipFile.toString() + "' has missing entries. Aborting operation.";
					throw new Exception(message);
				}

				// progress info
				updateMessage("Uploading spectrum '" + spectrumName + "'...");
				updateProgress(i, endRow);

				// zip spectrum files
				Path spectrumFile = zipSpectrumFiles(tempDir.resolve(directoryName), zipFile, spectrumName);

				// task cancelled
				if (isCancelled())
					return;

				// upload spectrum file to filer
				String dir = filer.createDirectories(filer.getDirectoryPath(FilerConnection.SPECTRA), program, section, mission);
				String url = dir + "/" + spectrumFile.getFileName().toString();
				filer.getSftpChannel().put(spectrumFile.toString(), url);

				// create and add spectrum info
				SpectrumInfo info = new SpectrumInfo();
				info.setInfo(SpectrumInfoType.NAME, spectrumName);
				info.setInfo(SpectrumInfoType.AC_PROGRAM, program);
				info.setInfo(SpectrumInfoType.AC_SECTION, section);
				info.setInfo(SpectrumInfoType.FAT_MISSION, mission);
				info.setInfo(SpectrumInfoType.FAT_MISSION_ISSUE, missionIssue);
				info.setInfo(SpectrumInfoType.FLP_ISSUE, flpIssue);
				info.setInfo(SpectrumInfoType.IFLP_ISSUE, iflpIssue);
				info.setInfo(SpectrumInfoType.CDF_ISSUE, cdfIssue);
				info.setInfo(SpectrumInfoType.DELIVERY_REF, delRef == null || delRef.isEmpty() ? "DRAFT" : delRef);
				info.setInfo(SpectrumInfoType.DESCRIPTION, description);
				info.setInfo(SpectrumInfoType.DATA_SIZE, spectrumFile.toFile().length());
				info.setInfo(SpectrumInfoType.DATA_URL, url);
				request.addInfo(info);
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
	 * Zips spectrum files.
	 *
	 * @param directory
	 *            Directory containing the spectrum files.
	 * @param zipFile
	 *            Zip file.
	 * @param spectrumName
	 *            Spectrum name.
	 * @return Path to zipped archive.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path zipSpectrumFiles(Path directory, Path zipFile, String spectrumName) throws Exception {

		// get spectrum files
		Path anaFile = null, cvtFile = null, txtFile = null, flsFile = null, xlsFile = null;
		File[] files = directory.toFile().listFiles();
		if (files != null) {
			for (File file : files) {

				// get file type
				FileType type = FileType.getFileType(file);

				// set files
				if (type.equals(FileType.ANA)) {
					anaFile = file.toPath();
				}
				else if (type.equals(FileType.CVT)) {
					cvtFile = file.toPath();
				}
				else if (type.equals(FileType.TXT)) {
					txtFile = file.toPath();
				}
				else if (type.equals(FileType.FLS)) {
					flsFile = file.toPath();
				}
				else if (type.equals(FileType.XLS)) {
					xlsFile = file.toPath();
				}
			}
		}

		// check files
		if (anaFile == null || cvtFile == null || txtFile == null || flsFile == null || xlsFile == null) {
			String message = "Spectrum contained in directory '" + directory.getFileName().toString() + "' in ";
			message += "the ZIP archive '" + zipFile.toString() + "' does NOT contain all CDF set files. Aborting operation.";
			throw new Exception(message);
		}

		// create output file path
		Path output = directory.resolve(FileType.appendExtension(spectrumName, FileType.ZIP));

		// zip files
		ArrayList<Path> zipFiles = new ArrayList<>();
		zipFiles.add(anaFile);
		zipFiles.add(cvtFile);
		zipFiles.add(txtFile);
		zipFiles.add(flsFile);
		zipFiles.add(xlsFile);
		Utility.zipFiles(zipFiles, output.toFile(), this);

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
			if (fileName.toString().equals("Spectrum_Info.xls"))
				return file;
		}
		String message = "The ZIP archive '" + zipFile.toString() + "' does NOT contain the upload information file 'Spectrum_Info.xls'. ";
		message += "Upload information is required in order to perform the spectrum upload. Aborting operation.";
		throw new Exception(message);
	}
}
