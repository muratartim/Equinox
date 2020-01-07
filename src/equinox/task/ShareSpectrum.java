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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import equinox.Equinox;
import equinox.data.ConversionTableSheetName;
import equinox.data.fileType.Spectrum;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.plugin.FileType;
import equinox.process.SaveSTFFile;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.SharedFileInfo;
import equinox.task.InternalEquinoxTask.FileSharingTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.SingleInputTask;
import equinox.utility.Utility;

/**
 * Class for share spectrum task.
 *
 * @author Murat Artim
 * @date Sep 23, 2014
 * @time 12:19:23 PM
 */
public class ShareSpectrum extends TemporaryFileCreatingTask<Void> implements LongRunningTask, FileSharingTask, SingleInputTask<Spectrum> {

	/** File item to save. */
	private Spectrum spectrum_ = null;

	/** Recipients. */
	private final List<ExchangeUser> recipients_;

	/**
	 * Creates share spectrum task.
	 *
	 * @param file
	 *            Spectrum to share. This can be null for automatic execution.
	 * @param recipients
	 *            Recipients.
	 */
	public ShareSpectrum(Spectrum file, List<ExchangeUser> recipients) {
		spectrum_ = file;
		recipients_ = recipients;
	}

	@Override
	public String getTaskTitle() {
		return "Share Spectrum";
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
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SHARE_FILE);

		// update progress info
		updateTitle("Sharing spectrum...");

		// save spectrum to temporary file
		Path path = saveSpectrum();

		// upload file to filer
		shareFile(path, recipients_, SharedFileInfo.SPECTRUM);
		return null;
	}

	/**
	 * Saves CDF set to temporary file.
	 *
	 * @return Path to temporary file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path saveSpectrum() throws Exception {

		// update info
		updateMessage("Saving spectrum to temporary file...");

		// create output path
		Path output = getWorkingDirectory().resolve(FileType.appendExtension(Utility.correctFileName(spectrum_.getName()), FileType.SPEC));

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
				files.add(new ConversionTableSheetName(spectrum_.getMission()).write(getWorkingDirectory()));

				// zip files
				Utility.zipFiles(files, output.toFile(), this);
			}
		}

		// return output file
		return output;
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
				Path zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
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
				Path zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
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
				Path zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
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
				Path zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
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
				Path zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
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
				Path output = getWorkingDirectory().resolve(Utility.correctFileName(name));
				new SaveSTFFile(this, stfID, stressTableID, is2D, output).start(connection);
				outputs.add(output);
			}
		}

		// return output files
		return outputs;
	}
}
