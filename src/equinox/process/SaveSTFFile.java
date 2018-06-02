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

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import equinox.Equinox;
import equinox.data.fileType.STFFile;
import equinox.task.InternalEquinoxTask;

/**
 * Class for save STF file process.
 *
 * @author Murat Artim
 * @date 21 Aug 2016
 * @time 16:39:47
 */
public class SaveSTFFile implements EquinoxProcess<STFFile> {

	/** The owner task of this process. */
	private final InternalEquinoxTask<?> task_;

	/** STF file to save. */
	private final STFFile stfFile_;

	/** Output file. */
	private final Path output_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.00");

	/** STF file ID. */
	private final Integer stfID_, stressTableID_;

	/** True if STF file is for 2D stress state. */
	private final Boolean is2D_;

	/**
	 * Creates save STF file process.
	 *
	 * @param task
	 *            The owner task.
	 * @param stfFile
	 *            STF file to save.
	 * @param output
	 *            Output file.
	 */
	public SaveSTFFile(InternalEquinoxTask<?> task, STFFile stfFile, Path output) {
		task_ = task;
		stfFile_ = stfFile;
		output_ = output;
		stfID_ = null;
		stressTableID_ = null;
		is2D_ = null;
	}

	/**
	 * Creates save STF file process.
	 *
	 * @param task
	 *            The owner task.
	 * @param stfID
	 *            STF file ID.
	 * @param stressTableID
	 *            STF stress table ID.
	 * @param is2D
	 *            True if STF file is for 2D stress state.
	 * @param output
	 *            Output file.
	 */
	public SaveSTFFile(InternalEquinoxTask<?> task, int stfID, int stressTableID, boolean is2D, Path output) {
		task_ = task;
		stfFile_ = null;
		output_ = output;
		stfID_ = stfID;
		stressTableID_ = stressTableID;
		is2D_ = is2D;
	}

	/**
	 * Returns input STF file.
	 *
	 * @return STF file.
	 */
	public STFFile getSTFFile() {
		return stfFile_;
	}

	/**
	 * Returns the output file.
	 *
	 * @return The output file.
	 */
	public Path getOutputFile() {
		return output_;
	}

	@Override
	public STFFile start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// progress info
		task_.updateMessage("Writing STF stresses...");

		// set parameters
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		int stressTableID = stfFile_ == null ? stressTableID_ : stfFile_.getStressTableID();
		boolean is2D = stfFile_ == null ? is2D_ : stfFile_.is2D();

		// create output file writer
		try (BufferedWriter writer = Files.newBufferedWriter(output_, Charset.defaultCharset())) {

			// write STF file header
			writer.write("# STF Generated by Equinox Version " + Equinox.VERSION.toString() + ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
			writer.newLine();
			String line = String.format("%-10s", "LCASE");
			line += String.format("%-10s", "SX");
			if (is2D) {
				line += String.format("%-10s", "SY");
				line += String.format("%-10s", "SXY");
			}
			writer.write(line);
			writer.newLine();

			// create statement
			try (Statement statement = connection.createStatement()) {

				// create and execute query to get STF stresses
				String sql = "select issy_code, stress_x, stress_y, stress_xy from stf_stresses_" + stressTableID + " where file_id = " + stfID + " order by issy_code asc";
				try (ResultSet resultSet = statement.executeQuery(sql)) {

					// loop over stresses
					while (resultSet.next()) {

						// write stresses
						line = String.format("%-10s", resultSet.getString("issy_code"));
						line += String.format("%-10s", format_.format(resultSet.getDouble("stress_x")));
						if (is2D) {
							line += String.format("%-10s", format_.format(resultSet.getDouble("stress_y")));
							line += String.format("%-10s", format_.format(resultSet.getDouble("stress_xy")));
						}
						writer.write(line);
						writer.newLine();
					}
				}
			}
		}

		// return STF file
		return stfFile_;
	}
}
