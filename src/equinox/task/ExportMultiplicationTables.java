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
import java.util.ArrayList;
import java.util.List;

import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableExportMultiplicationTables;
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
 * Class for export multiplication tables task.
 *
 * @author Murat Artim
 * @date Feb 19, 2016
 * @time 12:43:58 PM
 */
public class ExportMultiplicationTables extends TemporaryFileCreatingTask<Void> implements LongRunningTask, SavableTask {

	/** Attributes. */
	private final String spectrumName_, pilotPointName_, program_, section_, mission_, issue_, delRef_, description_;

	/** MUT files. */
	private final List<File> mutFiles_;

	/** Path to output ZIP file. */
	private final File output_;

	/**
	 * Creates export multiplication tables task.
	 *
	 * @param mutFiles
	 *            Multiplication table files.
	 * @param spectrumName
	 *            Spectrum name.
	 * @param pilotPointName
	 *            Pilot point name.
	 * @param program
	 *            A/C program.
	 * @param section
	 *            A/C section.
	 * @param mission
	 *            Fatigue mission.
	 * @param issue
	 *            Multiplication table issue.
	 * @param delRef
	 *            Delivery reference.
	 * @param description
	 *            Description.
	 * @param output
	 *            Path to output ZIP file.
	 */
	public ExportMultiplicationTables(List<File> mutFiles, String spectrumName, String pilotPointName, String program, String section, String mission, String issue, String delRef, String description, File output) {
		mutFiles_ = mutFiles;
		spectrumName_ = spectrumName;
		pilotPointName_ = pilotPointName;
		program_ = program;
		section_ = section;
		mission_ = mission;
		issue_ = issue;
		delRef_ = delRef;
		description_ = description;
		output_ = output;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Export multiplication tables";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableExportMultiplicationTables(mutFiles_, spectrumName_, pilotPointName_, program_, section_, mission_, issue_, delRef_, description_, output_);
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.EXPORT_LOADCASE_FACTOR_FILE);

		// update progress info
		updateTitle("Exporting multiplication tables to '" + output_.getName() + "'");

		// create array to store input files
		ArrayList<Path> inputFiles = new ArrayList<>();

		// write info file
		inputFiles.add(writeInfoFile());

		// task cancelled
		if (isCancelled())
			return null;

		// copy files to temporary folder
		updateMessage("Copying multiplication tables to temporary directory...");
		for (File mutFile : mutFiles_) {
			inputFiles.add(Files.copy(mutFile.toPath(), getWorkingDirectory().resolve(mutFile.getName()), StandardCopyOption.REPLACE_EXISTING));
		}

		// task cancelled
		if (isCancelled())
			return null;

		// zip exported files
		updateMessage("Zipping exported files...");
		Utility.zipFiles(inputFiles, output_, this);

		// return
		return null;
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
		Path infoFile = getWorkingDirectory().resolve("Multiplication_Table_Info.xls");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(infoFile.toFile());

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Multiplication Table Info", 0);

			// write headers
			writeHeaders(sheet);

			// write info
			for (int i = 0; i < mutFiles_.size(); i++) {
				writeInfo(sheet, FileType.getNameWithoutExtension(mutFiles_.get(i).toPath()), i + 1);
			}

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
	 * @param fileName
	 *            Multiplication table file name.
	 * @param index
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeInfo(WritableSheet sheet, String fileName, int index) throws Exception {

		// initialize column index
		int column = 0;

		// multiplication table name
		sheet.addCell(new jxl.write.Label(column, index, fileName, getDataFormat(index, CellType.LABEL, false)));
		column++;

		// spectrum name
		sheet.addCell(new jxl.write.Label(column, index, spectrumName_, getDataFormat(index, CellType.LABEL, false)));
		column++;

		// pilot point name
		sheet.addCell(new jxl.write.Label(column, index, pilotPointName_, getDataFormat(index, CellType.LABEL, false)));
		column++;

		// A/C program
		sheet.addCell(new jxl.write.Label(column, index, program_, getDataFormat(index, CellType.LABEL, false)));
		column++;

		// A/C section
		sheet.addCell(new jxl.write.Label(column, index, section_, getDataFormat(index, CellType.LABEL, false)));
		column++;

		// fatigue mission
		sheet.addCell(new jxl.write.Label(column, index, mission_, getDataFormat(index, CellType.LABEL, false)));
		column++;

		// issue
		sheet.addCell(new jxl.write.Label(column, index, issue_, getDataFormat(index, CellType.LABEL, false)));
		column++;

		// delivery reference
		String deliveryRef = delRef_ == null ? "DRAFT" : delRef_;
		sheet.addCell(new jxl.write.Label(column, index, deliveryRef, getDataFormat(index, CellType.LABEL, false)));
		column++;

		// description
		sheet.addCell(new jxl.write.Label(column, index, description_, getDataFormat(index, CellType.LABEL, false)));
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

		// multiplication table name
		String header = "Multiplication Table Name";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// spectrum name
		header = "Spectrum Name";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// pilot point name
		header = "Pilot Point Name";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// A/C program
		header = "A/C Program";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// A/C section
		header = "A/C Section";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// fatigue mission
		header = "Fatigue Mission";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// issue
		header = "Multiplication Table Issue";
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
