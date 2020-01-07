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
import java.util.concurrent.ExecutionException;

import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.Utility;
import jxl.Sheet;
import jxl.Workbook;
import jxl.format.CellFormat;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

/**
 * Class for prepare pilot point upload task.
 *
 * @author Murat Artim
 * @date 10 Aug 2018
 * @time 21:26:00
 */
public class PreparePilotPointUpload extends TemporaryFileCreatingTask<ArrayList<UploadPilotPoints>> implements ShortRunningTask {

	/** Maximum number of pilot points in upload package. */
	public static final int MAX_PILOT_POINTS_IN_UPLOAD_PACKAGE = 40;

	/** Zip files containing the pilot points to upload. */
	private final List<File> files_;

	/**
	 * Creates prepare pilot point upload task.
	 *
	 * @param files
	 *            Zip files containing the data to upload.
	 */
	public PreparePilotPointUpload(List<File> files) {
		files_ = files;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Prepare pilot point upload";
	}

	@Override
	protected ArrayList<UploadPilotPoints> call() throws Exception {

		// update progress info
		updateTitle("Preparing upload packages");
		updateMessage("Please wait...");

		// create list to store upload tasks
		ArrayList<UploadPilotPoints> uploadTasks = new ArrayList<>();

		// loop over zip files
		updateProgress(0, files_.size());
		for (int i = 0; i < files_.size(); i++) {

			// task cancelled
			if (isCancelled())
				return null;

			// get zip file
			Path zipFile = files_.get(i).toPath();

			// progress info
			updateMessage("Processing upload archive '" + zipFile.getFileName() + "'...");
			updateProgress(0, i);

			// create temporary directory to extract zip file contents
			Path tempDir = Files.createDirectory(getWorkingDirectory().resolve("Package_" + i));

			// extract zip file
			updateMessage("Extracting package info from archive '" + zipFile.getFileName() + "'...");
			Path infoFile = Utility.extractFileFromZIP(zipFile, this, FileType.XLS, tempDir);

			// task cancelled
			if (isCancelled())
				return null;

			// cannot find info file in upload archive
			if (infoFile == null) {
				String message = "The ZIP archive '" + zipFile.toString() + "' does NOT contain the upload information file 'Pilot_Point_Info.xls'. ";
				message += "Upload information is required in order to perform the pilot point upload. Ignoring zip archive.";
				addWarning(message);
				continue;
			}

			// get number of pilot points
			int numPPs = getNumberOfPilotPoints(infoFile, zipFile);

			// no pilot points found
			if (isCancelled() || numPPs == 0) {
				continue;
			}

			// no need to split
			if (numPPs <= MAX_PILOT_POINTS_IN_UPLOAD_PACKAGE) {
				uploadTasks.add(new UploadPilotPoints(zipFile));
			}

			// split into smaller chunks
			else {
				uploadTasks.addAll(splitPackage(numPPs, infoFile, zipFile, tempDir));
			}
		}

		// return upload tasks
		return uploadTasks;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// add stress sequence to file tree
		try {
			get().forEach(task -> taskPanel_.getOwner().runTaskInParallel(task));
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Splits given upload archive into smaller chunks, creates and returns upload task for each chunk.
	 *
	 * @param numPPs
	 *            Number of pilot points to be uploaded.
	 * @param infoFile
	 *            Upload info file.
	 * @param zipFile
	 *            Upload archive.
	 * @param tempDir
	 *            Temporary directory.
	 * @return List of upload tasks.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<UploadPilotPoints> splitPackage(int numPPs, Path infoFile, Path zipFile, Path tempDir) throws Exception {

		// create list of upload tasks
		ArrayList<UploadPilotPoints> tasks = new ArrayList<>();

		// extract zip file
		Utility.extractAllFilesFromZIP(zipFile, this, tempDir);

		// update progress info
		updateMessage("Splitting upload archive '" + zipFile.getFileName() + "'...");

		// compute number of packages
		int numPackages = numPPs / MAX_PILOT_POINTS_IN_UPLOAD_PACKAGE;
		numPackages += numPPs % MAX_PILOT_POINTS_IN_UPLOAD_PACKAGE == 0 ? 0 : 1;

		// loop over packages
		for (int i = 0; i < numPackages; i++) {

			// get sub-package files
			ArrayList<Path> subPackageFiles = getSubPackageFiles(infoFile, i, tempDir);

			// zip exported files
			Path subPackage = tempDir.resolve("subPackage_" + i + ".zip");
			Utility.zipFiles(subPackageFiles, subPackage.toFile(), this);

			// create upload task
			setFileAsPermanent(subPackage);
			tasks.add(new UploadPilotPoints(subPackage));
		}

		// return upload tasks
		return tasks;
	}

	/**
	 * Creates and returns sub-package files.
	 *
	 * @param infoFile
	 *            Upload package info file.
	 * @param index
	 *            Index of sub-package.
	 * @param tempDir
	 *            Path to sub-package directory.
	 * @return Sub-package files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static ArrayList<Path> getSubPackageFiles(Path infoFile, int index, Path tempDir) throws Exception {

		// create list store sub-package files
		ArrayList<Path> subPackageFiles = new ArrayList<>();

		// create sub-package directory
		Path subPackageDir = Files.createDirectory(tempDir.resolve("subPackage_" + index));

		// initialize variables
		Workbook sourceWorkbook = null;
		WritableWorkbook targetWorkbook = null;

		try {

			// get source workbook sheet
			sourceWorkbook = Workbook.getWorkbook(infoFile.toFile());
			Sheet sourceSheet = sourceWorkbook.getSheet(0);

			// create target workbook and sheet
			Path subPackageInfoFile = subPackageDir.resolve("Pilot_Point_Info.xls");
			subPackageFiles.add(subPackageInfoFile);
			targetWorkbook = Workbook.createWorkbook(subPackageInfoFile.toFile());
			WritableSheet targetSheet = targetWorkbook.createSheet("Pilot Point Info", 0);

			// write headers
			for (int i = 0; i < sourceSheet.getColumns(); i++) {
				String content = sourceSheet.getCell(i, 0).getContents().trim();
				CellFormat cf = sourceSheet.getCell(i, 0).getCellFormat();
				targetSheet.addCell(new Label(i, 0, content, cf));
			}

			// loop over rows
			int targetRow = 1;
			int startRow = MAX_PILOT_POINTS_IN_UPLOAD_PACKAGE * index + 1;
			int endRow = Math.min(startRow + MAX_PILOT_POINTS_IN_UPLOAD_PACKAGE, sourceSheet.getRows());
			for (int sourceRow = startRow; sourceRow < endRow; sourceRow++) {

				// loop over columns
				int targetCol = 0;
				for (int sourceCol = 0; sourceCol < sourceSheet.getColumns(); sourceCol++) {

					// get source cell content and format
					String content = sourceSheet.getCell(sourceCol, sourceRow).getContents().trim();
					CellFormat cf = sourceSheet.getCell(sourceCol, sourceRow).getCellFormat();

					// add target cell
					targetSheet.addCell(new Label(targetCol, targetRow, content, cf));

					// increment target column
					targetCol++;

					// add pilot point directory path to sub-package files
					if (sourceCol == 0) {
						subPackageFiles.add(tempDir.resolve(content));
					}
				}

				// increment target row
				targetRow++;
			}

			// write data
			targetWorkbook.write();

			// return sub-package files
			return subPackageFiles;
		}

		// close workbooks
		finally {
			if (sourceWorkbook != null) {
				sourceWorkbook.close();
			}
			if (targetWorkbook != null) {
				targetWorkbook.close();
			}
		}
	}

	/**
	 * Returns number of pilot points to be uploaded from the info file.
	 *
	 * @param infoFile
	 *            Pilot point upload info file.
	 * @param zipFile
	 *            Upload archive.
	 * @return Number of pilot points to be uploaded.
	 * @throws Exception
	 *             If exception occurs during reading info file.
	 */
	private int getNumberOfPilotPoints(Path infoFile, Path zipFile) throws Exception {

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
				message += "the ZIP archive '" + zipFile.toString() + "'. Ignoring zip archive.";
				addWarning(message);
				return 0;
			}

			// return number of pilot points
			return sheet.getRows() - 1;
		}

		// close workbook
		finally {
			if (workbook != null) {
				workbook.close();
			}
		}
	}
}