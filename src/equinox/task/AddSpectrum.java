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
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.ConversionTableSheetName;
import equinox.data.EmbeddedTask;
import equinox.data.Pair;
import equinox.data.fileType.Spectrum;
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.plugin.FileType;
import equinox.process.GenerateTXTFile;
import equinox.process.LoadANAFile;
import equinox.process.LoadCVTFile;
import equinox.process.LoadConversionTable;
import equinox.process.LoadFLSFile;
import equinox.process.LoadTXTFile;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.task.serializableTask.SerializableAddSpectrum;
import equinox.utility.Utility;

/**
 * Class for add spectrum task.
 *
 * @author Murat Artim
 * @date Jan 21, 2014
 * @time 10:55:55 PM
 */
public class AddSpectrum extends TemporaryFileCreatingTask<Spectrum> implements LongRunningTask, SavableTask, AutomaticTaskOwner<Spectrum>, SingleInputTask<Pair<Path, SpectrumInfo>> {

	/** Paths to spectrum files. */
	private Path anaFile_, txtFile_, cvtFile_, flsFile_, conversionTable_;

	/** Spectrum bundle file. */
	private Path specFile_, zipFile_;

	/** The selected conversion table sheet. */
	private String sheet_;

	/** Spectrum info. */
	private SpectrumInfo info_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Spectrum>> automaticTasks_ = null;

	/** Add STF files task. */
	private AddSTFFiles addSTFFiles_ = null;

	/**
	 * Creates add spectrum task from individual CDF set files.
	 *
	 * @param anaFile
	 *            Path to ANA file.
	 * @param txtFile
	 *            Path to TXT file. Can be null.
	 * @param cvtFile
	 *            Path to CVT file.
	 * @param flsFile
	 *            Path to FLS file.
	 * @param conversionTable
	 *            Path to conversion table.
	 * @param sheet
	 *            Selected conversion table sheet.
	 * @param info
	 *            Spectrum info (can be null).
	 */
	public AddSpectrum(Path anaFile, Path txtFile, Path cvtFile, Path flsFile, Path conversionTable, String sheet, SpectrumInfo info) {
		anaFile_ = anaFile;
		txtFile_ = txtFile;
		cvtFile_ = cvtFile;
		flsFile_ = flsFile;
		conversionTable_ = conversionTable;
		sheet_ = sheet;
		info_ = info;
		specFile_ = null;
		zipFile_ = null;
	}

	/**
	 * Creates add spectrum task from given spectrum bundle file.
	 *
	 * @param specFile
	 *            Spectrum bundle.
	 */
	public AddSpectrum(Path specFile) {
		specFile_ = specFile;
		anaFile_ = null;
		txtFile_ = null;
		cvtFile_ = null;
		flsFile_ = null;
		conversionTable_ = null;
		sheet_ = null;
		info_ = null;
		zipFile_ = null;
	}

	/**
	 * Creates add spectrum task from given zip archive and spectrum information.
	 *
	 * @param zipFile
	 *            Zip archive containing CDF set files. Can be null for automatic execution.
	 * @param info
	 *            Spectrum information. Can be null for automatic execution.
	 */
	public AddSpectrum(Path zipFile, SpectrumInfo info) {
		zipFile_ = zipFile;
		info_ = info;
		specFile_ = null;
		anaFile_ = null;
		txtFile_ = null;
		cvtFile_ = null;
		flsFile_ = null;
		conversionTable_ = null;
		sheet_ = null;
	}

	/**
	 * Sets spectrum files.
	 *
	 * @param anaFile
	 *            Path to ANA file.
	 * @param txtFile
	 *            Path to TXT file. Can be null.
	 * @param cvtFile
	 *            Path to CVT file.
	 * @param flsFile
	 *            Path to FLS file.
	 * @param conversionTable
	 *            Path to conversion table.
	 * @param sheet
	 *            Selected conversion table sheet.
	 */
	public void setSpectrumFiles(Path anaFile, Path txtFile, Path cvtFile, Path flsFile, Path conversionTable, String sheet) {
		anaFile_ = anaFile;
		txtFile_ = txtFile;
		cvtFile_ = cvtFile;
		flsFile_ = flsFile;
		conversionTable_ = conversionTable;
		sheet_ = sheet;
	}

	/**
	 * Sets spectrum bundle.
	 *
	 * @param specFile
	 *            Spectrum bundle.
	 */
	public void setSpectrumBundle(Path specFile) {
		specFile_ = specFile;
	}

	@Override
	public void setAutomaticInput(Pair<Path, SpectrumInfo> input) {
		zipFile_ = input.getElement1();
		info_ = input.getElement2();
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Spectrum> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Spectrum>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public String getTaskTitle() {
		return "Add spectrum";
	}

	@Override
	public SerializableTask getSerializableTask() {
		if (specFile_ == null)
			return new SerializableAddSpectrum(anaFile_, txtFile_, cvtFile_, flsFile_, conversionTable_, sheet_);
		return new SerializableAddSpectrum(specFile_);
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Spectrum call() throws Exception {

		// check permission
		checkPermission(Permission.ADD_NEW_SPECTRUM);

		// update progress info
		updateTitle("Loading spectrum");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// spectrum bundle
				ArrayList<File> stfFiles = null;
				if (specFile_ != null) {
					stfFiles = extractSpectrumBundle();
				}

				// spectrum archive
				else if (zipFile_ != null) {
					extractSpectrumArchive();
				}

				// create spectrum
				Spectrum spectrum = createSpectrum(connection);

				// load ANA file
				Integer anaID = new LoadANAFile(this, anaFile_, spectrum).start(connection);
				if (anaID == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}
				spectrum.setANAFileID(anaID);

				// load and add FLS file to CDF set
				Integer flsID = new LoadFLSFile(this, flsFile_, spectrum).start(connection);
				if (flsID == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}
				spectrum.setFLSFileID(flsID);

				// load and add conversion table to CDF set
				Integer[] convTableInfo = new LoadConversionTable(this, conversionTable_, spectrum, sheet_).start(connection);
				if (convTableInfo == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}
				spectrum.setConvTableID(convTableInfo[0]);

				// load and add CVT file to CDF set
				Path cvtFile = new LoadCVTFile(this, cvtFile_, spectrum).start(connection);
				if (cvtFile == null || !Files.exists(cvtFile)) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// load and add TXT file to CDF set
				Integer txtID = loadTXTFile(connection, convTableInfo, spectrum, cvtFile);
				if (txtID == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}
				spectrum.setTXTFileID(txtID);

				// create add STF files task
				if (stfFiles != null && !stfFiles.isEmpty()) {
					createAddSTFFilesTask(spectrum, stfFiles);
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return
				return spectrum;
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

			// get spectrum
			Spectrum spectrum = get();

			// add CDF set to file tree
			taskPanel_.getOwner().getOwner().getInputPanel().getFileTreeRoot().getChildren().add(spectrum);

			// load STF files (if any)
			if (addSTFFiles_ != null) {
				taskPanel_.getOwner().runTaskInParallel(addSTFFiles_);
			}

			// manage automatic tasks
			automaticTaskOwnerSucceeded(spectrum, automaticTasks_, taskPanel_);
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
	 * Creates add STF files task.
	 *
	 * @param spectrum
	 *            Spectrum.
	 * @param stfFiles
	 *            STF files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void createAddSTFFilesTask(Spectrum spectrum, ArrayList<File> stfFiles) throws Exception {

		// update info
		updateMessage("Creating add STF files task...");

		// create task
		addSTFFiles_ = new AddSTFFiles(null, spectrum, null);

		// loop over STF files
		for (int i = 0; i < stfFiles.size(); i++) {

			// update progress
			updateProgress(i, stfFiles.size());

			// get STF file
			File stfFile = stfFiles.get(i);

			// copy and replace STF file
			Path stfFileCopy = addSTFFiles_.getWorkingDirectory().resolve(stfFile.getName());
			stfFiles.set(i, Files.copy(stfFile.toPath(), stfFileCopy, StandardCopyOption.REPLACE_EXISTING).toFile());
		}

		// set STF files to task
		addSTFFiles_.setSTFFiles(stfFiles);
	}

	/**
	 * Extracts CDF set files from spectrum zip archive.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void extractSpectrumArchive() throws Exception {

		// update info
		updateMessage("Extracting spectrum archive...");

		// get spectrum file name
		Path zipFileName = zipFile_.getFileName();
		if (zipFileName == null)
			throw new Exception("Cannot get spectrum archive file name.");

		// extract files
		ArrayList<Path> spectrumFiles = Utility.extractAllFilesFromZIP(zipFile_, this, getWorkingDirectory());

		// no file found in archive
		if (spectrumFiles == null || spectrumFiles.isEmpty())
			throw new Exception("No file found in spectrum archive '" + zipFileName.toString() + "'.");

		// get mission name (will be used as conversion table sheet name)
		sheet_ = (String) info_.getInfo(SpectrumInfoType.FAT_MISSION);

		// loop over files
		for (Path file : spectrumFiles) {

			// null file
			if (file == null) {
				continue;
			}

			// get file type
			FileType type = FileType.getFileType(file.toFile());

			// not recognized
			if (type == null) {
				continue;
			}

			// ANA
			if (type.equals(FileType.ANA)) {
				anaFile_ = file;
			}
			else if (type.equals(FileType.TXT)) {
				txtFile_ = file;
			}
			else if (type.equals(FileType.CVT)) {
				cvtFile_ = file;
			}
			else if (type.equals(FileType.FLS)) {
				flsFile_ = file;
			}
			else if (type.equals(FileType.XLS)) {
				conversionTable_ = file;
			}
		}

		// missing file
		if (anaFile_ == null || txtFile_ == null || cvtFile_ == null || flsFile_ == null || conversionTable_ == null || sheet_ == null)
			throw new Exception("Spectrum archive '" + zipFileName.toString() + "' does not contain all CDF set files.");
	}

	/**
	 * Extracts spectrum bundle.
	 *
	 * @return List of STF files, or null if there are no STF files in the spectrum bundle.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<File> extractSpectrumBundle() throws Exception {

		// update info
		updateMessage("Extracting spectrum bundle...");

		// initialize STF files (if any)
		ArrayList<File> stfFiles = null;

		// get spectrum file name
		Path specFileName = specFile_.getFileName();
		if (specFileName == null)
			throw new Exception("Cannot get spectrum file name.");

		// extract files
		ArrayList<Path> spectrumFiles = Utility.extractAllFilesFromZIP(specFile_, this, getWorkingDirectory());

		// no file found in bundle
		if (spectrumFiles == null || spectrumFiles.isEmpty())
			throw new Exception("No file found in spectrum bundle '" + specFileName.toString() + "'.");

		// loop over files
		for (Path file : spectrumFiles) {

			// null file
			if (file == null) {
				continue;
			}

			// get conversion table file name
			Path fileName = file.getFileName();
			if (fileName == null) {
				continue;
			}

			// conversion table sheet name
			if (fileName.toString().equals(ConversionTableSheetName.FILE_NAME)) {
				sheet_ = ConversionTableSheetName.read(file);
				continue;
			}

			// get file type
			FileType type = FileType.getFileType(file.toFile());

			// not recognized
			if (type == null) {
				continue;
			}

			// ANA
			if (type.equals(FileType.ANA)) {
				anaFile_ = file;
			}
			else if (type.equals(FileType.TXT)) {
				txtFile_ = file;
			}
			else if (type.equals(FileType.CVT)) {
				cvtFile_ = file;
			}
			else if (type.equals(FileType.FLS)) {
				flsFile_ = file;
			}
			else if (type.equals(FileType.XLS)) {
				conversionTable_ = file;
			}
			else if (type.equals(FileType.STF)) {
				if (stfFiles == null) {
					stfFiles = new ArrayList<>();
				}
				stfFiles.add(file.toFile());
			}
		}

		// missing file
		if (anaFile_ == null || txtFile_ == null || cvtFile_ == null || flsFile_ == null || conversionTable_ == null || sheet_ == null)
			throw new Exception("Spectrum bundle '" + specFileName.toString() + "' does not contain all CDF set files.");

		// return STF files (if any)
		return stfFiles;
	}

	/**
	 * Loads TXT file. Decides whether to load the TXT file or generate it.
	 *
	 * @param connection
	 *            Database connection.
	 * @param convTableInfo
	 *            Array containing conversion table ID and delta-p loadcase.
	 * @param cdfSet
	 *            CDF set.
	 * @param cvtFile
	 *            Path to CVT file.
	 * @return The loaded TXT file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Integer loadTXTFile(Connection connection, Integer[] convTableInfo, Spectrum cdfSet, Path cvtFile) throws Exception {

		// null TXT file (generate)
		if (txtFile_ == null)
			return new GenerateTXTFile(this, cvtFile, convTableInfo, cdfSet).start(connection);

		// initialize input file and type
		Path txtFile = txtFile_;
		FileType type = FileType.getFileType(txtFile_.toFile());

		// input file is a ZIP file
		if (type.equals(FileType.ZIP)) {

			// extract TXT file from the bundle
			updateMessage("Extracting bundled TXT file...");
			txtFile = Utility.extractFileFromZIP(txtFile_, this, FileType.TXT, null);

			// no TXT file found within the bundle (generate)
			if (txtFile == null)
				return new GenerateTXTFile(this, cvtFile, convTableInfo, cdfSet).start(connection);
		}

		// input file is a GZIP file
		else if (type.equals(FileType.GZ)) {
			txtFile = getWorkingDirectory().resolve(FileType.appendExtension(FileType.getNameWithoutExtension(txtFile_), FileType.TXT));
			updateMessage("Extracting bundled TXT file...");
			Utility.extractFileFromGZIP(txtFile_, txtFile);
		}

		// load TXT file
		return new LoadTXTFile(this, txtFile, cdfSet, convTableInfo[1]).start(connection);
	}

	/**
	 * Creates and returns spectrum in the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @return The newly created spectrum.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Spectrum createSpectrum(Connection connection) throws Exception {

		// update info
		updateMessage("Creating new spectrum in database...");

		// create statement
		String sql = "insert into cdf_sets(name, delivery_ref, description) values(?, ?, ?)";
		try (PreparedStatement update = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// get name
			String name = FileType.getNameWithoutExtension(anaFile_);
			if (name.endsWith(FileType.ANA.getExtension())) {
				name = FileType.getNameWithoutExtension(name);
			}

			// set name
			update.setString(1, name);

			// get spectrum info (if there is)
			String delRef = null, description = null;
			if (info_ != null) {
				delRef = (String) info_.getInfo(SpectrumInfoType.DELIVERY_REF);
				description = (String) info_.getInfo(SpectrumInfoType.DESCRIPTION);
			}

			// set info
			if (delRef == null || delRef.trim().isEmpty()) {
				update.setString(2, "DRAFT");
			}
			else {
				update.setString(2, delRef.trim());
			}
			if (description == null || description.trim().isEmpty()) {
				update.setNull(3, java.sql.Types.VARCHAR);
			}
			else {
				update.setString(3, description.trim());
			}

			// execute update
			update.executeUpdate();

			// get result set
			try (ResultSet resultSet = update.getGeneratedKeys()) {

				// return file ID
				resultSet.next();
				return new Spectrum(name, resultSet.getBigDecimal(1).intValue());
			}
		}
	}
}
