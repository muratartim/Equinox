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
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;

import equinox.data.ExcaliburStressType;
import equinox.data.input.ExcaliburInput;
import equinox.task.Excalibur;
import equinox.utility.Utility;

/**
 * Class for Excalibur load element stresses process.
 *
 * @author Murat Artim
 * @date 30 Nov 2017
 * @time 12:06:48
 */
public class ExcaliburLoadElementStresses implements EquinoxProcess<Long> {

	/** The owner task of this process. */
	private final Excalibur owner;

	/** Analysis input. */
	private final ExcaliburInput input;

	/** Excalibur analysis table names. */
	private final String[] tableNames;

	/**
	 * Creates Excalibur load element stresses process.
	 *
	 * @param owner
	 *            The owner task of this process.
	 * @param input
	 *            Analysis input.
	 * @param tableNames
	 *            Excalibur analysis table names.
	 */
	public ExcaliburLoadElementStresses(Excalibur owner, ExcaliburInput input, String[] tableNames) {
		this.owner = owner;
		this.input = input;
		this.tableNames = tableNames;
	}

	/**
	 * Returns owner task.
	 *
	 * @return The owner task.
	 */
	public Excalibur getOwner() {
		return owner;
	}

	/**
	 * Returns analysis input.
	 *
	 * @return Analysis input.
	 */
	public ExcaliburInput getInput() {
		return input;
	}

	/**
	 * Returns analysis table names.
	 *
	 * @return Analysis table names.
	 */
	public String[] getTableNames() {
		return tableNames;
	}

	@Override
	public Long start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// get path to input stress directory
		Path stressDir = input.getStressDirectory().toPath();

		// get number of stress files in the input directory
		owner.updateMessage("Getting number of stress files...");
		long numFiles = Utility.countFiles(stressDir, ".stf");

		// task cancelled
		if (owner.isCancelled())
			return null;

		// prepare statement to insert stress files into database
		String sql = "insert into " + tableNames[Excalibur.STF_FILES] + "(filename) values(?)";
		try (PreparedStatement insertStressFile = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			// prepare statement to delete invalid stress files from database
			sql = "delete from " + tableNames[Excalibur.STF_FILES] + " where id = ?";
			try (PreparedStatement deleteStressFile = connection.prepareStatement(sql)) {

				// 1D element stresses
				if (input.getStressType().equals(ExcaliburStressType.ELEMENT_1D)) {
					loadElement1DStresses(stressDir, numFiles, insertStressFile, deleteStressFile, connection);
				}

				// 2D element stresses
				else if (input.getStressType().equals(ExcaliburStressType.ELEMENT_2D)) {
					loadElement2DStresses(stressDir, numFiles, insertStressFile, deleteStressFile, connection);
				}

				// frame stresses
				else if (input.getStressType().equals(ExcaliburStressType.FRAME)) {
					loadFrameStresses(stressDir, numFiles, insertStressFile, deleteStressFile, connection);
				}
			}
		}

		// get number of stress files
		long numSTFs = 0L;
		try (Statement getNumberOfSTFs = connection.createStatement()) {
			try (ResultSet resultSet = getNumberOfSTFs.executeQuery("select count(*) as numstfs from " + tableNames[Excalibur.STF_FILES])) {
				if (resultSet.next()) {
					numSTFs = resultSet.getLong("numstfs");
				}
			}
		}

		// return number of stress files
		return numSTFs;
	}

	/**
	 * Loads 1D element stress files into database.
	 *
	 * @param stressDir
	 *            Path to directory where stress files are kept.
	 * @param numFiles
	 *            Number of stress files.
	 * @param insertStressFile
	 *            Prepared statement to insert stress files into database.
	 * @param deleteStressFile
	 *            Prepared statement to delete invalid stress files from database.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadElement1DStresses(Path stressDir, long numFiles, PreparedStatement insertStressFile, PreparedStatement deleteStressFile, Connection connection) throws Exception {

		// prepare statement to insert stresses
		String sql = "insert into " + tableNames[Excalibur.STF_STRESSES] + "(file_id, lc_num, sn) values(?, ?, ?)";
		try (PreparedStatement insertStress = connection.prepareStatement(sql)) {

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(stressDir, Utility.getFileFilter(".stf"))) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				long numLoaded = 0L;
				owner.updateProgress(numLoaded, numFiles);
				nextFile: while (iterator.hasNext()) {

					// task cancelled
					if (owner.isCancelled())
						return;

					// get file
					Path stf = iterator.next();

					// update progress info
					String fileName = stf.getFileName().toString();
					owner.updateMessage("Loading stress file '" + stf.getFileName().toString() + "'...");
					owner.updateProgress(numLoaded, numFiles);
					numLoaded++;

					// insert stress file
					long fileID = 0L;
					insertStressFile.setString(1, fileName);
					insertStressFile.executeUpdate();
					try (ResultSet resultSet = insertStressFile.getGeneratedKeys()) {
						if (resultSet.next()) {
							fileID = resultSet.getBigDecimal(1).longValue();
						}
					}

					// set file id
					insertStress.setLong(1, fileID);

					// create file reader
					try (BufferedReader reader = Files.newBufferedReader(stf, Charset.defaultCharset())) {

						// read file till the end
						String line;
						int lineCount = 0;
						while ((line = reader.readLine()) != null) {

							// task cancelled
							if (owner.isCancelled())
								return;

							// increment line count
							lineCount++;

							// comment lines
							if (lineCount <= 2) {
								continue;
							}

							// empty line
							line = line.trim();
							if (line.isEmpty()) {
								continue;
							}

							// split line from tabs
							String[] split = line.split("\t");

							// not 2 columns
							if (split.length != 2) {
								owner.addWarning("Unexpected number of columns encountered for 1D element in the stress file '" + fileName + "'. Skipping file.");
								deleteStressFile.setLong(1, fileID);
								deleteStressFile.executeUpdate();
								continue nextFile;
							}

							// set loadcase number
							insertStress.setInt(2, Integer.parseInt(split[0].trim()));

							// set stress
							insertStress.setDouble(3, Double.parseDouble(split[1].trim()));

							// execute statement
							insertStress.executeUpdate();
						}
					}
				}
			}
		}
	}

	/**
	 * Loads 2D element stress files into database.
	 *
	 * @param stressDir
	 *            Path to directory where stress files are kept.
	 * @param numFiles
	 *            Number of stress files.
	 * @param insertStressFile
	 *            Prepared statement to insert stress files into database.
	 * @param deleteStressFile
	 *            Prepared statement to delete invalid stress files from database.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadElement2DStresses(Path stressDir, long numFiles, PreparedStatement insertStressFile, PreparedStatement deleteStressFile, Connection connection) throws Exception {

		// prepare statement to insert 2D stresses
		String sql = "insert into " + tableNames[Excalibur.STF_STRESSES] + "(file_id, lc_num, sx, sy, sxy, sigma_1, sigma_2, max_sigma, abs_max_sigma, min_sigma) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement insertStress = connection.prepareStatement(sql)) {

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(stressDir, Utility.getFileFilter(".stf"))) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				long numLoaded = 0L;
				owner.updateProgress(numLoaded, numFiles);
				nextFile: while (iterator.hasNext()) {

					// task cancelled
					if (owner.isCancelled())
						return;

					// get file
					Path stf = iterator.next();

					// update progress info
					String fileName = stf.getFileName().toString();
					owner.updateMessage("Loading stress file '" + stf.getFileName().toString() + "'...");
					owner.updateProgress(numLoaded, numFiles);
					numLoaded++;

					// insert stress file
					long fileID = 0L;
					insertStressFile.setString(1, fileName);
					insertStressFile.executeUpdate();
					try (ResultSet resultSet = insertStressFile.getGeneratedKeys()) {
						if (resultSet.next()) {
							fileID = resultSet.getBigDecimal(1).longValue();
						}
					}

					// set file id
					insertStress.setLong(1, fileID);

					// create file reader
					try (BufferedReader reader = Files.newBufferedReader(stf, Charset.defaultCharset())) {

						// read file till the end
						String line;
						int lineCount = 0;
						while ((line = reader.readLine()) != null) {

							// task cancelled
							if (owner.isCancelled())
								return;

							// increment line count
							lineCount++;

							// comment lines
							if (lineCount <= 2) {
								continue;
							}

							// empty line
							line = line.trim();
							if (line.isEmpty()) {
								continue;
							}

							// split line from tabs
							String[] split = line.split("\t");

							// not 2 columns
							if (split.length != 4) {
								owner.addWarning("Unexpected number of columns encountered for 2D element in the stress file '" + fileName + "'. Skipping file.");
								deleteStressFile.setLong(1, fileID);
								deleteStressFile.executeUpdate();
								continue nextFile;
							}

							// set loadcase number
							insertStress.setInt(2, Integer.parseInt(split[0].trim()));

							// set stresses
							double sx = Double.parseDouble(split[1].trim());
							double sy = Double.parseDouble(split[2].trim());
							double sxy = Double.parseDouble(split[3].trim());
							insertStress.setDouble(3, sx);
							insertStress.setDouble(4, sy);
							insertStress.setDouble(5, sxy);

							// calculate and set principal stresses
							double a = 0.5 * (sx + sy);
							double b = Math.sqrt(Math.pow(0.5 * (sx - sy), 2.0) + Math.pow(sxy, 2.0));
							double sigma1 = a + b;
							double sigma2 = a - b;
							insertStress.setDouble(6, sigma1);
							insertStress.setDouble(7, sigma2);
							insertStress.setDouble(8, Math.max(sigma1, sigma2));
							insertStress.setDouble(9, Math.max(Math.abs(sigma1), Math.abs(sigma2)));
							insertStress.setDouble(10, Math.min(sigma1, sigma2));

							// execute statement
							insertStress.executeUpdate();
						}
					}
				}
			}
		}
	}

	/**
	 * Loads frame stress files into database.
	 *
	 * @param stressDir
	 *            Path to directory where stress files are kept.
	 * @param numFiles
	 *            Number of stress files.
	 * @param insertStressFile
	 *            Prepared statement to insert stress files into database.
	 * @param deleteStressFile
	 *            Prepared statement to delete invalid stress files from database.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadFrameStresses(Path stressDir, long numFiles, PreparedStatement insertStressFile, PreparedStatement deleteStressFile, Connection connection) throws Exception {

		// prepare statement to insert 1D stresses
		String sql = "insert into " + tableNames[Excalibur.STF_STRESSES] + "(file_id, lc_num, sn) values(?, ?, ?)";
		try (PreparedStatement insertStress = connection.prepareStatement(sql)) {

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(stressDir, Utility.getFileFilter(".stf"))) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				long numLoaded = 0L;
				owner.updateProgress(numLoaded, numFiles);
				while (iterator.hasNext()) {

					// task cancelled
					if (owner.isCancelled())
						return;

					// get file
					Path stf = iterator.next();

					// update progress info
					String fileName = stf.getFileName().toString();
					owner.updateMessage("Loading stress file '" + stf.getFileName().toString() + "'...");
					owner.updateProgress(numLoaded, numFiles);
					numLoaded++;

					// insert stress file
					long fileID = 0L;
					insertStressFile.setString(1, fileName);
					insertStressFile.executeUpdate();
					try (ResultSet resultSet = insertStressFile.getGeneratedKeys()) {
						if (resultSet.next()) {
							fileID = resultSet.getBigDecimal(1).longValue();
						}
					}

					// set file id
					insertStress.setLong(1, fileID);

					// create file reader
					try (BufferedReader reader = Files.newBufferedReader(stf, Charset.defaultCharset())) {

						// read file till the end
						String line;
						while ((line = reader.readLine()) != null) {

							// task cancelled
							if (owner.isCancelled())
								return;

							// empty line
							if (line.isEmpty()) {
								continue;
							}

							// set loadcase number
							insertStress.setInt(2, Integer.parseInt(line.substring(0, 7).trim()));

							// set stress
							insertStress.setDouble(3, Double.parseDouble(line.substring(7, 22).trim()));

							// execute statement
							insertStress.executeUpdate();
						}
					}
				}
			}
		}
	}
}
