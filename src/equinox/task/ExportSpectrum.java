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
import equinox.data.EmbeddedTask;
import equinox.data.Pair;
import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
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
 * Class for export spectrum task.
 *
 * @author Murat Artim
 * @date Feb 9, 2016
 * @time 2:45:37 PM
 */
public class ExportSpectrum extends TemporaryFileCreatingTask<Path> implements LongRunningTask, SingleInputTask<Pair<Spectrum, String[]>>, AutomaticTaskOwner<Path> {

	/** Spectrum. */
	private Spectrum spectrum_;

	/** Info array. */
	private String[] info_;

	/** Path to output ZIP file. */
	private final File output_;

	/** Editable spectrum info. */
	private String deliveryReference_, description_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates export spectrum task.
	 *
	 * @param spectrum
	 *            Spectrum to be exported. Can be null for automatic task execution.
	 * @param info
	 *            Spectrum info. Can be null for automatic task execution.
	 * @param output
	 *            Output zip file.
	 */
	public ExportSpectrum(Spectrum spectrum, String[] info, File output) {
		spectrum_ = spectrum;
		info_ = info;
		output_ = output;
	}

	/**
	 * Sets delivery reference.
	 *
	 * @param deliveryReference
	 *            Delivery reference.
	 */
	public void setDeliveryReference(String deliveryReference) {
		deliveryReference_ = deliveryReference;
	}

	/**
	 * Sets description.
	 *
	 * @param description
	 *            Description.
	 */
	public void setDescription(String description) {
		description_ = description;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Export spectrum";
	}

	@Override
	public void setAutomaticInput(Pair<Spectrum, String[]> input) {
		spectrum_ = input.getElement1();
		info_ = input.getElement2();
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
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.EXPORT_SPECTRUM);

		// update progress info
		updateTitle("Exporting spectrum to '" + output_.getName() + "'");

		// create array to store input files
		ArrayList<Path> inputFiles = new ArrayList<>();

		// write info file
		inputFiles.add(writeInfoFile());

		// task cancelled
		if (isCancelled())
			return null;

		// output directory
		Path outputDirectory = Files.createDirectory(getWorkingDirectory().resolve("Spectrum_0"));

		// write spectrum files
		writeSpectrumFiles(outputDirectory);

		// task cancelled
		if (isCancelled())
			return null;

		// add directory to input files
		inputFiles.add(outputDirectory);

		// zip exported files
		updateMessage("Zipping exported files...");
		Utility.zipFiles(inputFiles, output_, this);

		// return output path
		return output_.toPath();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {

			// get output path
			Path output = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(output, automaticTasks_, taskPanel_);
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
	 * Writes out spectrum files to given directory.
	 *
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeSpectrumFiles(Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving spectrum files...");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// save ANA file
				saveANA(statement, outputDirectory);

				// task cancelled
				if (isCancelled())
					return;

				// save TXT file
				saveTXT(statement, outputDirectory);

				// task cancelled
				if (isCancelled())
					return;

				// save FLS file
				saveFLS(statement, outputDirectory);

				// task cancelled
				if (isCancelled())
					return;

				// save CVT file
				saveCVT(statement, outputDirectory);

				// task cancelled
				if (isCancelled())
					return;

				// save conversion table
				saveConvTable(statement, outputDirectory);
			}
		}
	}

	/**
	 * Saves conversion table of the CDF set.
	 *
	 * @param statement
	 *            Database statement.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveConvTable(Statement statement, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving conversion table...");

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
				Utility.extractFileFromZIP(zipFile, this, FileType.XLS, outputDirectory);

				// free blob
				blob.free();
			}
		}
	}

	/**
	 * Saves CVT file of the CDF set.
	 *
	 * @param statement
	 *            Database statement.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveCVT(Statement statement, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving CVT file...");

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
				Utility.extractFileFromZIP(zipFile, this, FileType.CVT, outputDirectory);

				// free blob
				blob.free();
			}
		}
	}

	/**
	 * Saves FLS file of the CDF set.
	 *
	 * @param statement
	 *            Database statement.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveFLS(Statement statement, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving FLS file...");

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
				Utility.extractFileFromZIP(zipFile, this, FileType.FLS, outputDirectory);

				// free blob
				blob.free();
			}
		}
	}

	/**
	 * Saves TXT file of the CDF set.
	 *
	 * @param statement
	 *            Database statement.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveTXT(Statement statement, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving TXT file...");

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
				Utility.extractFileFromZIP(zipFile, this, FileType.TXT, outputDirectory);

				// free blob
				blob.free();
			}
		}
	}

	/**
	 * Saves ANA file of the CDF set.
	 *
	 * @param statement
	 *            Database statement.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveANA(Statement statement, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving ANA file...");

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
				Utility.extractFileFromZIP(zipFile, this, FileType.ANA, outputDirectory);

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
		Path infoFile = getWorkingDirectory().resolve("Spectrum_Info.xls");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(infoFile.toFile());

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Spectrum Info", 0);

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
		sheet.addCell(new jxl.write.Label(column, 1, "Spectrum_0", getDataFormat(1, CellType.LABEL, false)));
		column++;

		// spectrum name
		sheet.addCell(new jxl.write.Label(column, 1, spectrum_.getName(), getDataFormat(1, CellType.LABEL, false)));
		column++;

		// A/C program
		sheet.addCell(new jxl.write.Label(column, 1, info_[GetSpectrumEditInfo.PROGRAM], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// A/C section
		sheet.addCell(new jxl.write.Label(column, 1, info_[GetSpectrumEditInfo.SECTION], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// fatigue mission
		sheet.addCell(new jxl.write.Label(column, 1, info_[GetSpectrumEditInfo.MISSION], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// mission issue
		sheet.addCell(new jxl.write.Label(column, 1, info_[GetSpectrumEditInfo.MISSION_ISSUE], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// FLP issue
		sheet.addCell(new jxl.write.Label(column, 1, info_[GetSpectrumEditInfo.FLP_ISSUE], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// IFLP issue
		sheet.addCell(new jxl.write.Label(column, 1, info_[GetSpectrumEditInfo.IFLP_ISSUE], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// CDF issue
		sheet.addCell(new jxl.write.Label(column, 1, info_[GetSpectrumEditInfo.CDF_ISSUE], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// delivery reference
		String deliveryRef = deliveryReference_ != null ? deliveryReference_ : info_[GetSpectrumEditInfo.DELIVERY_REF];
		sheet.addCell(new jxl.write.Label(column, 1, deliveryRef, getDataFormat(1, CellType.LABEL, false)));
		column++;

		// description
		String description = description_ != null ? description_ : info_[GetSpectrumEditInfo.DESCRIPTION];
		sheet.addCell(new jxl.write.Label(column, 1, description, getDataFormat(1, CellType.LABEL, false)));
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

		// spectrum directory name
		String header = "Directory Name";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// spectrum name
		header = "Spectrum Name";
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

		// mission issue
		header = "Mission Issue";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// FLP issue
		header = "FLP Issue";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// IFLP issue
		header = "IFLP Issue";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// CDF issue
		header = "CDF Issue";
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
