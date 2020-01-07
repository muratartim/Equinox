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

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import equinox.Equinox;
import equinox.data.fileType.AircraftModel;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;
import jxl.CellType;
import jxl.Workbook;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.write.NumberFormats;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * Class for export A/C model task.
 *
 * @author Murat Artim
 * @date 16 Aug 2016
 * @time 09:58:00
 */
public class ExportAircraftModel extends TemporaryFileCreatingTask<Void> implements LongRunningTask {

	/** A/C model. */
	private final AircraftModel model_;

	/** Info array. */
	private final String[] info_;

	/** Path to output ZIP file. */
	private final File output_;

	/**
	 * Creates export A/C model task.
	 *
	 * @param model
	 *            A/C model to be exported.
	 * @param info
	 *            A/C model info.
	 * @param output
	 *            Output zip file.
	 */
	public ExportAircraftModel(AircraftModel model, String[] info, File output) {
		model_ = model;
		info_ = info;
		output_ = output;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Export A/C model";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.EXPORT_AIRCRAFT_MODEL);

		// update progress info
		updateTitle("Exporting A/C model to '" + output_.getName() + "'");

		// create array to store input files
		ArrayList<Path> inputFiles = new ArrayList<>();

		// write info file
		inputFiles.add(writeInfoFile());

		// task cancelled
		if (isCancelled())
			return null;

		// output directory
		Path outputDirectory = Files.createDirectory(getWorkingDirectory().resolve("AircraftModel_0"));

		// write model files
		writeModelFiles(outputDirectory);

		// task cancelled
		if (isCancelled())
			return null;

		// add directory to input files
		inputFiles.add(outputDirectory);

		// zip exported files
		updateMessage("Zipping exported files...");
		Utility.zipFiles(inputFiles, output_, this);

		// return
		return null;
	}

	/**
	 * Writes out model files to given directory.
	 *
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeModelFiles(Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving A/C model files...");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// save model files
				saveModelFiles(statement, outputDirectory);

				// task cancelled
				if (isCancelled())
					return;

				// save element groups file (if any)
				saveGroupsFile(statement, connection, outputDirectory);
			}
		}
	}

	/**
	 * Saves element groups file to working directory.
	 *
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveGroupsFile(Statement statement, Connection connection, Path outputDirectory) throws Exception {

		// check if any group exist
		boolean exists = false;
		DatabaseMetaData dbmtadta = connection.getMetaData();
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "ELEMENT_GROUPS_" + model_.getID(), null)) {
			while (resultSet.next()) {
				exists = true;
				break;
			}
		}

		// no groups
		if (!exists)
			return;

		// update progress info
		updateMessage("Saving element groups file...");

		// initialize output file
		Path output = outputDirectory.resolve("elementGroups" + FileType.GRP.getExtension());

		// create output file writer
		try (BufferedWriter writer = Files.newBufferedWriter(output, Charset.defaultCharset())) {

			// write groups file header
			writeGroupsFileHeader(writer);

			// prepare statement EIDs of each group
			String sql = "select eid from element_groups_" + model_.getID() + " where group_id = ? order by eid";
			try (PreparedStatement getEIDs = connection.prepareStatement(sql)) {

				// create query to get element group names
				sql = "select group_id, name from element_group_names_" + model_.getID() + " order by name";
				try (ResultSet groups = statement.executeQuery(sql)) {

					// loop over groups
					while (groups.next()) {

						// get group name
						String name = groups.getString("name");

						// write group start
						writer.write("Group" + "\t" + name);
						writer.newLine();

						// get EIDs
						getEIDs.setInt(1, groups.getInt("group_id"));
						try (ResultSet eids = getEIDs.executeQuery()) {

							// loop over EIDs
							while (eids.next()) {
								writer.write(Integer.toString(eids.getInt("eid")));
								writer.newLine();
							}
						}

						// write group end
						writer.write("End");
						writer.newLine();
						writer.newLine();
					}
				}
			}
		}
	}

	/**
	 * Writes out element groups file header.
	 *
	 * @param writer
	 *            File writer.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void writeGroupsFileHeader(BufferedWriter writer) throws Exception {
		writer.write("# Aircraft Model Element Groups File Generated by Equinox Version " + Equinox.VERSION.toString() + ", Date: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
		writer.newLine();
		writer.write("# This file contains element groups for Equinox A/C models.");
		writer.newLine();
		writer.write("# Empty lines and lines starting with # are ignored. All columns are tab separated.");
		writer.newLine();
		writer.write("# If Equinox cannot find any elements for a given group, the group will not be created and a warning message will be issued at the end of process.");
		writer.newLine();
		writer.write("# There are 2 possible formats. They can coexist within the same file.");
		writer.newLine();
		writer.write("# 1) Starts with the word 'Interval':");
		writer.newLine();
		writer.write("#    Interval<Tab>GroupName<Tab>startEID<Tab>endEID");
		writer.newLine();
		writer.write("# 2) Starts with the word 'Group':");
		writer.newLine();
		writer.write("#    Group<Tab>GroupName");
		writer.newLine();
		writer.write("#    EID-1");
		writer.newLine();
		writer.write("#    EID-2");
		writer.newLine();
		writer.write("#    ...");
		writer.newLine();
		writer.write("#    End");
		writer.newLine();
		writer.newLine();
	}

	/**
	 * Saves model files (F06 and F07) to given output directory.
	 *
	 * @param statement
	 *            Database statement.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveModelFiles(Statement statement, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving F06 and F07 model files...");

		// execute query
		try (ResultSet resultSet = statement.executeQuery("select data from ac_models where model_id = " + model_.getID())) {
			if (resultSet.next()) {

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = getWorkingDirectory().resolve("modelFiles" + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
				Utility.extractAllFilesFromZIP(zipFile, this, outputDirectory);

				// free blob
				blob.free();
			}
		}
	}

	/**
	 * Writes out info XLS file to working directory.
	 *
	 * @return Path to info XLS file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeInfoFile() throws Exception {

		// update progress info
		updateMessage("Writing info Excel file...");

		// create info file
		Path infoFile = getWorkingDirectory().resolve("AircraftModel_Info.xls");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(infoFile.toFile());

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Aircraft Model Info", 0);

			// write headers
			writeHeaders(sheet);

			// write info
			writeInfo(sheet);

			// write data
			workbook.write();
		}

		// close workbook
		finally {
			if (workbook != null) {
				workbook.close();
			}
		}

		// return info file
		return infoFile;
	}

	/**
	 * Writes table info.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeInfo(WritableSheet sheet) throws Exception {

		// initialize column index
		int column = 0;

		// directory name
		sheet.addCell(new jxl.write.Label(column, 1, "AircraftModel_0", getDataFormat(1, CellType.LABEL, false)));
		column++;

		// A/C program
		sheet.addCell(new jxl.write.Label(column, 1, info_[GetAircraftModelEditInfo.PROGRAM], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// model name
		sheet.addCell(new jxl.write.Label(column, 1, model_.getModelName(), getDataFormat(1, CellType.LABEL, false)));
		column++;

		// delivery reference
		String deliveryRef = info_[GetAircraftModelEditInfo.DELIVERY_REF] == null ? "DRAFT" : info_[GetAircraftModelEditInfo.DELIVERY_REF];
		sheet.addCell(new jxl.write.Label(column, 1, deliveryRef, getDataFormat(1, CellType.LABEL, false)));
		column++;

		// description
		sheet.addCell(new jxl.write.Label(column, 1, info_[GetAircraftModelEditInfo.DESCRIPTION], getDataFormat(1, CellType.LABEL, false)));
	}

	/**
	 * Writes table headers.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void writeHeaders(WritableSheet sheet) throws Exception {

		// get header format
		WritableCellFormat format = getHeaderFormat();

		// initialize column index
		int column = 0;

		// model directory name
		String header = "Directory Name";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// A/C program
		header = "A/C Program";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// model name
		header = "Model Name";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// delivery reference
		header = "Delivery Reference";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// description
		header = "Description";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
	}

	/**
	 * Returns header format.
	 *
	 * @return Header format.
	 * @throws WriteException
	 *             If exception occurs during process.
	 */
	private static WritableCellFormat getHeaderFormat() throws WriteException {
		WritableFont cellFont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(cellFont);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
		cellFormat.setBackground(Colour.ORANGE);
		return cellFormat;
	}

	/**
	 * Returns data format.
	 *
	 * @param rowIndex
	 *            Row index.
	 * @param ct
	 *            Cell type.
	 * @param isScientific
	 *            True if scientific format.
	 * @return Data format.
	 * @throws WriteException
	 *             If exception occurs during process.
	 */
	private static WritableCellFormat getDataFormat(int rowIndex, CellType ct, boolean isScientific) throws WriteException {
		WritableCellFormat cellFormat = ct == CellType.NUMBER ? new WritableCellFormat(isScientific ? NumberFormats.EXPONENTIAL : NumberFormats.FLOAT) : new WritableCellFormat();
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
		cellFormat.setBackground(rowIndex % 2 == 0 ? Colour.WHITE : Colour.VERY_LIGHT_YELLOW);
		return cellFormat;
	}
}
