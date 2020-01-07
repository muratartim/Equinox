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
import java.util.List;

import equinox.Equinox;
import equinox.data.fileType.Spectrum;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.SharedFileInfo;
import equinox.task.InternalEquinoxTask.FileSharingTask;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.SingleInputTask;
import equinox.utility.Utility;

/**
 * Class for share core spectrum file task.
 *
 * @author Murat Artim
 * @date Dec 17, 2014
 * @time 12:49:21 PM
 */
public class ShareSpectrumFile extends TemporaryFileCreatingTask<Void> implements LongRunningTask, FileSharingTask, SingleInputTask<Spectrum> {

	/** File item to save. */
	private Spectrum spectrum_;

	/** Type of core spectrum file to share. */
	private final FileType type_;

	/** Recipients. */
	private final List<ExchangeUser> recipients_;

	/**
	 * Creates share core spectrum file task.
	 *
	 * @param spectrum
	 *            Spectrum to share. Can be null for automatic execution.
	 * @param type
	 *            Type of core spectrum file to share.
	 * @param recipients
	 *            Recipients.
	 */
	public ShareSpectrumFile(Spectrum spectrum, FileType type, List<ExchangeUser> recipients) {
		spectrum_ = spectrum;
		type_ = type;
		recipients_ = recipients;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Share spectrum file";
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
		updateTitle("Sharing spectrum file...");

		// save spectrum to temporary file
		Path path = saveSpectrum();

		// upload file to filer
		shareFile(path, recipients_, SharedFileInfo.FILE);
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
		updateMessage("Saving to temporary file...");

		// initialize output path
		Path output = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// save ANA file
				if (type_.equals(FileType.ANA)) {
					output = saveANA(statement);
				}
				else if (type_.equals(FileType.TXT)) {
					output = saveTXT(statement);
				}
				else if (type_.equals(FileType.CVT)) {
					output = saveCVT(statement);
				}
				else if (type_.equals(FileType.FLS)) {
					output = saveFLS(statement);
				}
				else if (type_.equals(FileType.XLS)) {
					output = saveConvTable(statement);
				}
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
		Path zipFile = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from ana_files where file_id = " + spectrum_.getANAFileID())) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);

				// free blob
				blob.free();
			}
		}

		// return output file
		return zipFile;
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
		Path zipFile = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from txt_files where file_id = " + spectrum_.getTXTFileID())) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);

				// free blob
				blob.free();
			}
		}

		// return output file
		return zipFile;
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
		Path zipFile = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from fls_files where file_id = " + spectrum_.getFLSFileID())) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);

				// free blob
				blob.free();
			}
		}

		// return output file
		return zipFile;
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
		Path zipFile = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from cvt_files where file_id = " + spectrum_.getCVTFileID())) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);

				// free blob
				blob.free();
			}
		}

		// return output file
		return zipFile;
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
		Path zipFile = null;

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select name, data from xls_files where file_id = " + spectrum_.getConversionTableID())) {

			// get data
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				zipFile = getWorkingDirectory().resolve(Utility.correctFileName(name) + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);

				// free blob
				blob.free();
			}
		}

		// return output file
		return zipFile;
	}
}
