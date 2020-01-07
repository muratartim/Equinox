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
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.ConversionTableSheetName;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.process.SaveSTFFile;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.Utility;

/**
 * Class for save CDF set task.
 *
 * @author Murat Artim
 * @date Apr 29, 2014
 * @time 10:32:51 AM
 */
public class SaveSpectrum extends TemporaryFileCreatingTask<Path> implements LongRunningTask, SingleInputTask<Spectrum>, AutomaticTaskOwner<Path> {

	/** File item to save. */
	private Spectrum spectrum_ = null;

	/** Output file. */
	private final File output_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save CDF set task.
	 *
	 * @param spectrum
	 *            File item to save. This can be null for automatic execution.
	 * @param output
	 *            Output file.
	 */
	public SaveSpectrum(Spectrum spectrum, File output) {
		spectrum_ = spectrum;
		output_ = output;
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
	public String getTaskTitle() {
		return "Save spectrum to '" + output_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public void setAutomaticInput(Spectrum spectrum) {
		spectrum_ = spectrum;
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving spectrum to '" + output_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create list to store file paths
				ArrayList<Path> files = new ArrayList<>();

				// save ANA file to temporary directory
				files.add(saveANA(statement));
				if (files.get(files.size() - 1) == null)
					return null;

				// save TXT file to temporary directory
				files.add(saveTXT(statement));
				if (files.get(files.size() - 1) == null)
					return null;

				// save FLS file to temporary directory
				files.add(saveFLS(statement));
				if (files.get(files.size() - 1) == null)
					return null;

				// save CVT file to temporary directory
				files.add(saveCVT(statement));
				if (files.get(files.size() - 1) == null)
					return null;

				// save conversion table to temporary directory
				files.add(saveConvTable(statement));
				if (files.get(files.size() - 1) == null)
					return null;

				// save STF files to temporary directory
				ArrayList<Path> stfPaths = saveSTFs(connection, statement);
				if (!stfPaths.isEmpty()) {
					files.addAll(stfPaths);
				}

				// save conversion table sheet name
				if (FileType.getFileType(output_).equals(FileType.SPEC)) {
					files.add(new ConversionTableSheetName(spectrum_.getMission()).write(getWorkingDirectory()));
				}

				// zip files
				Utility.zipFiles(files, output_, this);
			}
		}

		// return
		return output_.toPath();
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
	 * Saves ANA file of the CDF set to temporary directory.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Output file path.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveANA(Statement statement) throws Exception {

		// update progress info
		updateMessage("Saving ANA file...");

		// initialize output file
		Path anaFile = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from ana_files where file_id = " + spectrum_.getANAFileID())) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
				anaFile = Utility.extractFileFromZIP(zipFile, this, FileType.ANA, null);

				// free blob
				blob.free();
			}
		}

		// return output file
		return anaFile;
	}

	/**
	 * Saves TXT file of the CDF set to temporary directory.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Output file path.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveTXT(Statement statement) throws Exception {

		// update progress info
		updateMessage("Saving TXT file...");

		// initialize output file
		Path txtFile = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from txt_files where file_id = " + spectrum_.getTXTFileID())) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
				txtFile = Utility.extractFileFromZIP(zipFile, this, FileType.TXT, null);

				// free blob
				blob.free();
			}
		}

		// return output file
		return txtFile;
	}

	/**
	 * Saves FLS file of the CDF set to temporary directory.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Output file path.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveFLS(Statement statement) throws Exception {

		// update progress info
		updateMessage("Saving FLS file...");

		// initialize output file
		Path flsFile = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from fls_files where file_id = " + spectrum_.getFLSFileID())) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
				flsFile = Utility.extractFileFromZIP(zipFile, this, FileType.FLS, null);

				// free blob
				blob.free();
			}
		}

		// return output file
		return flsFile;
	}

	/**
	 * Saves CVT file of the CDF set to temporary directory.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Output file path.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveCVT(Statement statement) throws Exception {

		// update progress info
		updateMessage("Saving CVT file...");

		// initialize output file
		Path cvtFile = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from cvt_files where file_id = " + spectrum_.getCVTFileID())) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
				cvtFile = Utility.extractFileFromZIP(zipFile, this, FileType.CVT, null);

				// free blob
				blob.free();
			}
		}

		// return output file
		return cvtFile;
	}

	/**
	 * Saves conversion table of the CDF set to temporary directory.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Output file path.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveConvTable(Statement statement) throws Exception {

		// update progress info
		updateMessage("Saving conversion table...");

		// initialize output file
		Path convTable = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from xls_files where file_id = " + spectrum_.getConversionTableID())) {

			// get data
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
				convTable = Utility.extractFileFromZIP(zipFile, this, FileType.XLS, null);

				// free blob
				blob.free();
			}
		}

		// return output file
		return convTable;
	}

	/**
	 * Saves STF files of the CDF set to temporary directory.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @return Output file paths.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<Path> saveSTFs(Connection connection, Statement statement) throws Exception {

		// create output paths
		ArrayList<Path> outputs = new ArrayList<>();

		// get STF file IDs
		String sql = "select file_id, stress_table_id, name, is_2d from stf_files where cdf_id = " + spectrum_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// loop over STF files
			while (resultSet.next()) {

				// get STF file info
				String name = resultSet.getString("name");
				int stfID = resultSet.getInt("file_id");
				int stressTableID = resultSet.getInt("stress_table_id");
				boolean is2D = resultSet.getBoolean("is_2d");

				// update progress info
				updateMessage("Saving STF file '" + name + "'...");

				// add file path
				Path output = getWorkingDirectory().resolve(name);
				new SaveSTFFile(this, stfID, stressTableID, is2D, output).start(connection);
				outputs.add(output);
			}
		}

		// return output files
		return outputs;
	}
}
