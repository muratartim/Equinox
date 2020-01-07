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
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import equinox.Equinox;
import equinox.data.fileType.Spectrum;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.Utility;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
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
 * Class for export multiple spectra task.
 *
 * @author Murat Artim
 * @date Feb 9, 2016
 * @time 3:44:23 PM
 */
public class ExportMultipleSpectra extends TemporaryFileCreatingTask<Void> implements LongRunningTask {

	/** Spectra. */
	private final Spectrum[] spectra_;

	/** Output directory. */
	private final Path outputDirectory_;

	/**
	 * Creates export multiple spectra task.
	 *
	 * @param spectra
	 *            Spectra to export.
	 * @param outputDirectory
	 *            Output directory.
	 */
	public ExportMultipleSpectra(ObservableList<TreeItem<String>> spectra, File outputDirectory) {
		spectra_ = new Spectrum[spectra.size()];
		for (int i = 0; i < spectra_.length; i++) {
			spectra_[i] = (Spectrum) spectra.get(i);
		}
		outputDirectory_ = outputDirectory.toPath();
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Export spectra";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.EXPORT_SPECTRUM);

		// update progress info
		updateTitle("Exporting spectra to '" + outputDirectory_.getFileName() + "'");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(outputDirectory_.resolve("Spectrum_Info.xls").toFile());

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Spectrum Info", 0);

			// write headers
			updateMessage("Creating spectrum info Excel sheet...");
			writeHeaders(sheet);

			// task cancelled
			if (isCancelled())
				return null;

			// write data
			writeData(sheet);

			// task cancelled
			if (isCancelled())
				return null;

			// write data
			workbook.write();
		}

		// close workbook
		finally {
			if (workbook != null) {
				workbook.close();
			}
		}

		// return
		return null;
	}

	/**
	 * Writes spectrum data.
	 *
	 * @param sheet
	 *            Spectrum info excel worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeData(WritableSheet sheet) throws Exception {

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement for getting spectrum info
			String sql = "select * from cdf_sets where set_id = ?";
			try (PreparedStatement getSpectrumInfo = connection.prepareStatement(sql)) {

				// prepare statement for getting ANA file
				sql = "select name, data from ana_files where file_id = ?";
				try (PreparedStatement getANAFile = connection.prepareStatement(sql)) {

					// prepare statement for getting TXT file
					sql = "select name, data from txt_files where file_id = ?";
					try (PreparedStatement getTXTFile = connection.prepareStatement(sql)) {

						// prepare statement for getting CVT file
						sql = "select name, data from cvt_files where file_id = ?";
						try (PreparedStatement getCVTFile = connection.prepareStatement(sql)) {

							// prepare statement for getting FLS file
							sql = "select name, data from fls_files where file_id = ?";
							try (PreparedStatement getFLSFile = connection.prepareStatement(sql)) {

								// prepare statement for getting conversion table
								sql = "select name, data from xls_files where file_id = ?";
								try (PreparedStatement getConvTable = connection.prepareStatement(sql)) {

									// loop over spectra
									for (int i = 0; i < spectra_.length; i++) {

										// update info
										updateMessage("Writing data for spectrum '" + spectra_[i].getName() + "'...");
										updateProgress(i, spectra_.length);

										// create spectrum directories
										Path tempDirectory = Files.createDirectory(getWorkingDirectory().resolve("Spectrum_" + i));
										Path outputDirectory = Files.createDirectory(outputDirectory_.resolve("Spectrum_" + i));

										// write info
										writeInfo(spectra_[i], getSpectrumInfo, sheet, i + 1, outputDirectory);

										// task cancelled
										if (isCancelled())
											return;

										// save ANA file
										saveANA(spectra_[i], getANAFile, tempDirectory, outputDirectory);

										// task cancelled
										if (isCancelled())
											return;

										// save TXT file
										saveTXT(spectra_[i], getTXTFile, tempDirectory, outputDirectory);

										// task cancelled
										if (isCancelled())
											return;

										// save FLS file
										saveFLS(spectra_[i], getFLSFile, tempDirectory, outputDirectory);

										// task cancelled
										if (isCancelled())
											return;

										// save CVT file
										saveCVT(spectra_[i], getCVTFile, tempDirectory, outputDirectory);

										// task cancelled
										if (isCancelled())
											return;

										// save conversion table
										saveConvTable(spectra_[i], getConvTable, tempDirectory, outputDirectory);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Writes spectrum info to given worksheet.
	 *
	 * @param spectrum
	 *            Spectrum.
	 * @param statement
	 *            Database statement.
	 * @param sheet
	 *            Info worksheet.
	 * @param row
	 *            Row index.
	 * @param outputDirectory
	 *            Spectrum output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeInfo(Spectrum spectrum, PreparedStatement statement, WritableSheet sheet, int row, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Writing spectrum info...");

		// get output directory name
		Path outputDirName = outputDirectory.getFileName();
		if (outputDirName == null)
			throw new Exception("Cannot get output directory name.");

		// execute query
		statement.setInt(1, spectrum.getID());
		try (ResultSet resultSet = statement.executeQuery()) {
			if (resultSet.next()) {

				// initialize column index
				int column = 0;

				// directory name
				sheet.addCell(new jxl.write.Label(column, row, outputDirName.toString(), getDataFormat(row, CellType.LABEL, false)));
				column++;

				// spectrum name
				sheet.addCell(new jxl.write.Label(column, row, spectrum.getName(), getDataFormat(row, CellType.LABEL, false)));
				column++;

				// A/C program
				sheet.addCell(new jxl.write.Label(column, row, spectrum.getProgram(), getDataFormat(row, CellType.LABEL, false)));
				column++;

				// A/C section
				sheet.addCell(new jxl.write.Label(column, row, spectrum.getSection(), getDataFormat(row, CellType.LABEL, false)));
				column++;

				// fatigue mission
				sheet.addCell(new jxl.write.Label(column, row, spectrum.getMission(), getDataFormat(row, CellType.LABEL, false)));
				column++;

				// mission issue
				sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("fat_mission_issue"), getDataFormat(row, CellType.LABEL, false)));
				column++;

				// FLP issue
				sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("flp_issue"), getDataFormat(row, CellType.LABEL, false)));
				column++;

				// IFLP issue
				sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("iflp_issue"), getDataFormat(row, CellType.LABEL, false)));
				column++;

				// CDF issue
				sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("cdf_issue"), getDataFormat(row, CellType.LABEL, false)));
				column++;

				// delivery reference
				String deliveryRef = resultSet.getString("delivery_ref");
				sheet.addCell(new jxl.write.Label(column, row, deliveryRef == null ? "DRAFT" : deliveryRef, getDataFormat(row, CellType.LABEL, false)));
				column++;

				// description
				sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("description"), getDataFormat(row, CellType.LABEL, false)));
			}
		}
	}

	/**
	 * Saves conversion table of the CDF set.
	 *
	 * @param spectrum
	 *            Spectrum.
	 * @param statement
	 *            Database statement.
	 * @param tempDirectory
	 *            Temporary directory.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveConvTable(Spectrum spectrum, PreparedStatement statement, Path tempDirectory, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving conversion table...");

		// execute query
		statement.setInt(1, spectrum.getConversionTableID());
		try (ResultSet resultSet = statement.executeQuery()) {

			// get data
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = tempDirectory.resolve(name + FileType.ZIP.getExtension());
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
	 * @param spectrum
	 *            Spectrum.
	 * @param statement
	 *            Database statement.
	 * @param tempDirectory
	 *            Temporary directory.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveCVT(Spectrum spectrum, PreparedStatement statement, Path tempDirectory, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving CVT file...");

		// execute query
		statement.setInt(1, spectrum.getCVTFileID());
		try (ResultSet resultSet = statement.executeQuery()) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = tempDirectory.resolve(name + FileType.ZIP.getExtension());
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
	 * @param spectrum
	 *            Spectrum.
	 * @param statement
	 *            Database statement.
	 * @param tempDirectory
	 *            Temporary directory.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveFLS(Spectrum spectrum, PreparedStatement statement, Path tempDirectory, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving FLS file...");

		// execute query
		statement.setInt(1, spectrum.getFLSFileID());
		try (ResultSet resultSet = statement.executeQuery()) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = tempDirectory.resolve(name + FileType.ZIP.getExtension());
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
	 * @param spectrum
	 *            Spectrum.
	 * @param statement
	 *            Database statement.
	 * @param tempDirectory
	 *            Temporary directory.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveTXT(Spectrum spectrum, PreparedStatement statement, Path tempDirectory, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving TXT file...");

		// execute query
		statement.setInt(1, spectrum.getTXTFileID());
		try (ResultSet resultSet = statement.executeQuery()) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = tempDirectory.resolve(name + FileType.ZIP.getExtension());
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
	 * @param spectrum
	 *            Spectrum.
	 * @param statement
	 *            Database statement.
	 * @param tempDirectory
	 *            Temporary directory.
	 * @param outputDirectory
	 *            Output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveANA(Spectrum spectrum, PreparedStatement statement, Path tempDirectory, Path outputDirectory) throws Exception {

		// update progress info
		updateMessage("Saving ANA file...");

		// execute query
		statement.setInt(1, spectrum.getANAFileID());
		try (ResultSet resultSet = statement.executeQuery()) {
			if (resultSet.next()) {

				// get file name
				String name = resultSet.getString("name");

				// get blob
				Blob blob = resultSet.getBlob("data");

				// extract file
				Path zipFile = tempDirectory.resolve(name + FileType.ZIP.getExtension());
				Files.copy(blob.getBinaryStream(), zipFile, StandardCopyOption.REPLACE_EXISTING);
				Utility.extractFileFromZIP(zipFile, this, FileType.ANA, outputDirectory);

				// free blob
				blob.free();
			}
		}
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
