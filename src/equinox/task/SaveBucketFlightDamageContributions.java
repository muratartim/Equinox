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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import equinox.Equinox;
import equinox.data.fileType.STFFileBucket;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableSaveBucketFlightDamageContributions;
import javafx.beans.property.BooleanProperty;
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
 * Class for save typical flight damage contributions from STF file buckets task.
 *
 * @author Murat Artim
 * @date 27 Oct 2016
 * @time 12:04:16
 */
public class SaveBucketFlightDamageContributions extends InternalEquinoxTask<Void> implements LongRunningTask, SavableTask {

	/** STF file buckets. */
	private final List<STFFileBucket> buckets_;

	/** Options. */
	private final BooleanProperty[] options_;

	/** Contribution names. */
	private final List<String> tfNamesWithOccurrences_, tfNamesWithoutOccurrences_;

	/** Output file. */
	private final File output_;

	/**
	 * Creates save typical flight damage contributions from STF file buckets task.
	 *
	 * @param buckets
	 *            STF file buckets.
	 * @param tfNamesWithOccurrences
	 *            Typical flight names with flight occurrences.
	 * @param tfNamesWithoutOccurrences
	 *            Typical flight names without flight occurrences.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SaveBucketFlightDamageContributions(List<STFFileBucket> buckets, List<String> tfNamesWithOccurrences, List<String> tfNamesWithoutOccurrences, BooleanProperty[] options, File output) {
		buckets_ = buckets;
		tfNamesWithOccurrences_ = tfNamesWithOccurrences;
		tfNamesWithoutOccurrences_ = tfNamesWithoutOccurrences;
		options_ = options;
		output_ = output;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Save flight damage contributions";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableSaveBucketFlightDamageContributions(buckets_, tfNamesWithOccurrences_, tfNamesWithoutOccurrences_, options_, output_);
	}

	@Override
	protected Void call() throws Exception {

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(output_);

			// create worksheets
			WritableSheet sheetWithOccurrences = workbook.createSheet("1 With Flight Occurrences", 0);
			WritableSheet sheetWithoutOccurrences = workbook.createSheet("1 Without Flight Occurrences", 1);

			// write headers
			writeHeaders(sheetWithOccurrences, tfNamesWithOccurrences_);
			writeHeaders(sheetWithoutOccurrences, tfNamesWithoutOccurrences_);

			// get total number of STF files to be written
			int numSTFs = 0;
			for (STFFileBucket bucket : buckets_) {
				numSTFs += bucket.getNumberOfSTFs();
			}

			// get database connection
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {

				// prepare statement for getting STF files
				String sql = "select file_id, name, eid from stf_files where cdf_id = ? order by name";
				try (PreparedStatement getSTFs = connection.prepareStatement(sql)) {

					// prepare statement to get overall info
					sql = "select id, omission_level, material_name, material_specification, material_orientation, material_configuration, ";
					sql += "material_p, material_q, material_m ";
					sql += "from flight_dam_contributions where stf_id = ?";
					try (PreparedStatement getOverallInfo = connection.prepareStatement(sql)) {

						// prepare statement to get contribution info with typical flight occurrences
						sql = "select dam_percent from flight_dam_contribution_with_occurrences where id = ? and flight_name = ?";
						try (PreparedStatement getInfoWithOccurrences = connection.prepareStatement(sql)) {

							// prepare statement to get contribution info without typical flight occurrences
							sql = "select dam_percent from flight_dam_contribution_without_occurrences where id = ? and flight_name = ?";
							try (PreparedStatement getInfoWithoutOccurrences = connection.prepareStatement(sql)) {

								// loop over buckets
								int rowIndex = 0, stfCount = 0, pageCount = 0;
								for (STFFileBucket bucket : buckets_) {

									// get STF files
									getSTFs.setInt(1, bucket.getParentItem().getID());
									try (ResultSet stfFiles = getSTFs.executeQuery()) {

										// loop over STF files
										while (stfFiles.next()) {

											// get STF values
											int stfID = stfFiles.getInt("file_id");
											String stfName = stfFiles.getString("name");
											String eid = stfFiles.getString("eid");

											// write progress info
											updateMessage("Writing flight damage contributions of STF file '" + stfName + "'...");
											updateProgress(stfCount, numSTFs);
											stfCount++;

											// get contribution info
											getOverallInfo.setInt(1, stfID);
											try (ResultSet overallInfo = getOverallInfo.executeQuery()) {

												// loop over damage contribution info
												while (overallInfo.next()) {

													// task cancelled
													if (isCancelled())
														return null;

													// maximum row limit reached
													if (rowIndex >= 50000) {

														// create worksheets
														pageCount++;
														sheetWithOccurrences = workbook.createSheet(pageCount + 1 + " With Flight Occurrences", pageCount);
														pageCount++;
														sheetWithOccurrences = workbook.createSheet(pageCount + 1 + " Without Flight Occurrences", pageCount);

														// write headers
														writeHeaders(sheetWithOccurrences, tfNamesWithOccurrences_);
														writeHeaders(sheetWithoutOccurrences, tfNamesWithoutOccurrences_);

														// reset row index
														rowIndex = 0;
													}

													// write contribution info
													getInfoWithOccurrences.setInt(1, overallInfo.getInt("id"));
													getInfoWithoutOccurrences.setInt(1, overallInfo.getInt("id"));
													writeData(sheetWithOccurrences, tfNamesWithOccurrences_, bucket, stfName, eid, overallInfo, getInfoWithOccurrences, rowIndex + 1);
													writeData(sheetWithoutOccurrences, tfNamesWithoutOccurrences_, bucket, stfName, eid, overallInfo, getInfoWithoutOccurrences, rowIndex + 1);

													// increment row index
													rowIndex++;
												}
											}
										}
									}
								}
							}
						}
					}
				}
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

		// return
		return null;
	}

	/**
	 * Writes data rows.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param tfNames
	 *            Typical flight names.
	 * @param bucket
	 *            STF file bucket.
	 * @param stfFileName
	 *            STF file name.
	 * @param eid
	 *            Element ID.
	 * @param overallInfo
	 *            Overall contribution info.
	 * @param getInfo
	 *            Statement to get contribution values.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeData(WritableSheet sheet, List<String> tfNames, STFFileBucket bucket, String stfFileName, String eid, ResultSet overallInfo, PreparedStatement getInfo, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[SaveFlightDamageContributions.PROGRAM].get()) {
			String value = bucket.getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// section
		if (options_[SaveFlightDamageContributions.SECTION].get()) {
			String value = bucket.getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// mission
		if (options_[SaveFlightDamageContributions.MISSION].get()) {
			String value = bucket.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// spectrum name
		if (options_[SaveFlightDamageContributions.SPEC_NAME].get()) {
			String value = bucket.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// pilot point name
		if (options_[SaveFlightDamageContributions.PP_NAME].get()) {
			sheet.addCell(new jxl.write.Label(column, row, stfFileName, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// element ID
		if (options_[SaveFlightDamageContributions.EID].get()) {
			sheet.addCell(new jxl.write.Label(column, row, eid, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// material name
		if (options_[SaveFlightDamageContributions.MAT_NAME].get()) {
			String materialName = overallInfo.getString("material_name");
			materialName += "/" + overallInfo.getString("material_specification");
			materialName += "/" + overallInfo.getString("material_orientation");
			materialName += "/" + overallInfo.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// fatigue slope p
		if (options_[SaveFlightDamageContributions.FAT_P].get()) {
			double value = overallInfo.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// fatigue constant q
		if (options_[SaveFlightDamageContributions.FAT_Q].get()) {
			double value = overallInfo.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// omission level
		if (options_[SaveFlightDamageContributions.OMISSION].get()) {
			double value = overallInfo.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			}
			column++;
		}

		// loop over contribution names
		for (String tfName : tfNames) {
			getInfo.setString(2, tfName);
			try (ResultSet info = getInfo.executeQuery()) {
				while (info.next()) {
					sheet.addCell(new jxl.write.Number(column, row, info.getDouble("dam_percent"), getDataFormat(row, CellType.NUMBER)));
					column++;
				}
			}
		}
	}

	/**
	 * Writes column headers according selected options.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param tfNames
	 *            Typical flight names.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeHeaders(WritableSheet sheet, List<String> tfNames) throws Exception {

		// initialize column index
		int column = 0;

		// get header format
		WritableCellFormat format = getHeaderFormat();

		// program
		if (options_[SaveFlightDamageContributions.PROGRAM].get()) {
			String header = "A/C program";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// section
		if (options_[SaveFlightDamageContributions.SECTION].get()) {
			String header = "A/C section";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// mission
		if (options_[SaveFlightDamageContributions.MISSION].get()) {
			String header = "Fatigue mission";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// spectrum name
		if (options_[SaveFlightDamageContributions.SPEC_NAME].get()) {
			String header = "Spectrum name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// pilot point name
		if (options_[SaveFlightDamageContributions.PP_NAME].get()) {
			String header = "Pilot point name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// element ID
		if (options_[SaveFlightDamageContributions.EID].get()) {
			String header = "Element ID";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// material name
		if (options_[SaveFlightDamageContributions.MAT_NAME].get()) {
			String header = "Material name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue slope p
		if (options_[SaveFlightDamageContributions.FAT_P].get()) {
			String header = "Fatigue material slope (p)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue constant q
		if (options_[SaveFlightDamageContributions.FAT_Q].get()) {
			String header = "Fatigue material constant (q)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// omission level
		if (options_[SaveFlightDamageContributions.OMISSION].get()) {
			String header = "Omission level";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// loop over typical flight names
		for (String tfName : tfNames) {
			sheet.addCell(new jxl.write.Label(column, 0, tfName, format));
			sheet.setColumnView(column, tfName.length());
			column++;
		}
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
	 * @return Data format.
	 * @throws WriteException
	 *             If exception occurs during process.
	 */
	private static WritableCellFormat getDataFormat(int rowIndex, CellType ct) throws WriteException {
		WritableCellFormat cellFormat = ct == CellType.NUMBER ? new WritableCellFormat(NumberFormats.FLOAT) : new WritableCellFormat();
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
		cellFormat.setBackground(rowIndex % 2 == 0 ? Colour.WHITE : Colour.VERY_LIGHT_YELLOW);
		return cellFormat;
	}
}
