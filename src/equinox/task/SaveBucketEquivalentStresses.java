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
import java.util.ArrayList;

import equinox.Equinox;
import equinox.controller.SaveEquivalentStressInfoPanel;
import equinox.data.fileType.STFFileBucket;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableSaveBucketEquivalentStresses;
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
 * Class for save bucket equivalent stresses task.
 *
 * @author Murat Artim
 * @date 2 Sep 2016
 * @time 10:08:54
 */
public class SaveBucketEquivalentStresses extends InternalEquinoxTask<Void> implements LongRunningTask, SavableTask {

	/** STF file buckets. */
	private final ArrayList<STFFileBucket> buckets_;

	/** Options. */
	private final BooleanProperty[] options_;

	/** Output file. */
	private final File output_;

	/** Equivalent stress type. */
	private final int stressType_;

	/**
	 * Creates save bucket equivalent stresses task.
	 *
	 * @param buckets
	 *            STF file buckets.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 * @param stressType
	 *            Equivalent stress type.
	 */
	public SaveBucketEquivalentStresses(ArrayList<STFFileBucket> buckets, BooleanProperty[] options, File output, int stressType) {
		buckets_ = buckets;
		options_ = options;
		output_ = output;
		stressType_ = stressType;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Save equivalent stresses";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableSaveBucketEquivalentStresses(buckets_, options_, output_, stressType_);
	}

	@Override
	protected Void call() throws Exception {

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(output_);

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Page 1", 0);

			// write headers
			writeHeaders(sheet);

			// get total number of STF files to be written
			int numSTFs = 0;
			for (STFFileBucket bucket : buckets_) {
				numSTFs += bucket.getNumberOfSTFs();
			}

			// get database connection
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {

				// prepare statement for getting stresses
				String sql = getQuery();
				try (PreparedStatement getStresses = connection.prepareStatement(sql)) {

					// prepare statement for getting STF files
					sql = "select file_id, name, eid from stf_files where cdf_id = ? order by name";
					try (PreparedStatement getSTFs = connection.prepareStatement(sql)) {

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
									updateMessage("Writing equivalent stress of STF file '" + stfName + "'...");
									updateProgress(stfCount, numSTFs);
									stfCount++;

									// get equivalent stresses
									getStresses.setInt(1, stfID);
									try (ResultSet stresses = getStresses.executeQuery()) {

										// loop over stresses
										while (stresses.next()) {

											// task cancelled
											if (isCancelled())
												return null;

											// maximum row limit reached
											if (rowIndex >= 50000) {

												// increment page count
												pageCount++;

												// create worksheet
												sheet = workbook.createSheet("Page " + (pageCount + 1), pageCount);

												// write headers
												writeHeaders(sheet);

												// reset row index
												rowIndex = 0;
											}

											// write stresses
											if (stressType_ == SaveEquivalentStressInfoPanel.FATIGUE) {
												writeDataForFastFatigueEquivalentStress(sheet, bucket, stfName, eid, stresses, rowIndex + 1);
											}
											else if (stressType_ == SaveEquivalentStressInfoPanel.PREFFAS) {
												writeDataForFastPreffasEquivalentStress(sheet, bucket, stfName, eid, stresses, rowIndex + 1);
											}
											else if (stressType_ == SaveEquivalentStressInfoPanel.LINEAR) {
												writeDataForFastLinearEquivalentStress(sheet, bucket, stfName, eid, stresses, rowIndex + 1);
											}
											rowIndex++;
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
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param bucket
	 *            STF file bucket.
	 * @param stfName
	 *            STF file name.
	 * @param eid
	 *            Element ID.
	 * @param stresses
	 *            Result set containing stress information.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForFastFatigueEquivalentStress(WritableSheet sheet, STFFileBucket bucket, String stfName, String eid, ResultSet stresses, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[SaveEquivalentStresses.PROGRAM].get()) {
			String value = bucket.getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SaveEquivalentStresses.SECTION].get()) {
			String value = bucket.getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[SaveEquivalentStresses.MISSION].get()) {
			String value = bucket.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// spectrum name
		if (options_[SaveEquivalentStresses.SPEC_NAME].get()) {
			String value = bucket.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// pilot point name
		if (options_[SaveEquivalentStresses.PP_NAME].get()) {
			sheet.addCell(new jxl.write.Label(column, row, stfName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[SaveEquivalentStresses.EID].get()) {
			sheet.addCell(new jxl.write.Label(column, row, eid, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[SaveEquivalentStresses.MAT_NAME].get()) {
			String materialName = stresses.getString("material_name");
			materialName += "/" + stresses.getString("material_specification");
			materialName += "/" + stresses.getString("material_orientation");
			materialName += "/" + stresses.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[SaveEquivalentStresses.MAT_DATA].get()) {

			// p
			double p = stresses.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, p, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// q
			double q = stresses.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, q, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// m
			double m = stresses.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[SaveEquivalentStresses.VALIDITY].get()) {
			int value = (int) stresses.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[SaveEquivalentStresses.OMISSION].get()) {
			double value = stresses.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// fatigue equivalent stress
		if (options_[SaveEquivalentStresses.EQUIVALENT_STRESS].get()) {
			double value = stresses.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param bucket
	 *            STF file bucket.
	 * @param stfName
	 *            STF file name.
	 * @param eid
	 *            Element ID.
	 * @param stresses
	 *            Result set containing stress information.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForFastPreffasEquivalentStress(WritableSheet sheet, STFFileBucket bucket, String stfName, String eid, ResultSet stresses, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[SaveEquivalentStresses.PROGRAM].get()) {
			String value = bucket.getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SaveEquivalentStresses.SECTION].get()) {
			String value = bucket.getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[SaveEquivalentStresses.MISSION].get()) {
			String value = bucket.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// spectrum name
		if (options_[SaveEquivalentStresses.SPEC_NAME].get()) {
			String value = bucket.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// pilot point name
		if (options_[SaveEquivalentStresses.PP_NAME].get()) {
			sheet.addCell(new jxl.write.Label(column, row, stfName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[SaveEquivalentStresses.EID].get()) {
			sheet.addCell(new jxl.write.Label(column, row, eid, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[SaveEquivalentStresses.MAT_NAME].get()) {
			String materialName = stresses.getString("material_name");
			materialName += "/" + stresses.getString("material_specification");
			materialName += "/" + stresses.getString("material_orientation");
			materialName += "/" + stresses.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[SaveEquivalentStresses.MAT_DATA].get()) {

			// Ceff
			double ceff = stresses.getDouble("material_ceff");
			sheet.addCell(new jxl.write.Number(column, row, ceff, getDataFormat(row, CellType.NUMBER, true)));
			column++;

			// m
			double m = stresses.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// A
			double a = stresses.getDouble("material_a");
			sheet.addCell(new jxl.write.Number(column, row, a, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// B
			double b = stresses.getDouble("material_b");
			sheet.addCell(new jxl.write.Number(column, row, b, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// C
			double c = stresses.getDouble("material_c");
			sheet.addCell(new jxl.write.Number(column, row, c, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Ftu
			double ftu = stresses.getDouble("material_ftu");
			sheet.addCell(new jxl.write.Number(column, row, ftu, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Fty
			double fty = stresses.getDouble("material_fty");
			sheet.addCell(new jxl.write.Number(column, row, fty, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[SaveEquivalentStresses.VALIDITY].get()) {
			int value = (int) stresses.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[SaveEquivalentStresses.OMISSION].get()) {
			double value = stresses.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// equivalent stress
		if (options_[SaveEquivalentStresses.EQUIVALENT_STRESS].get()) {
			double value = stresses.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param bucket
	 *            STF file bucket.
	 * @param stfName
	 *            STF file name.
	 * @param eid
	 *            Element ID.
	 * @param stresses
	 *            Result set containing stress information.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForFastLinearEquivalentStress(WritableSheet sheet, STFFileBucket bucket, String stfName, String eid, ResultSet stresses, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[SaveEquivalentStresses.PROGRAM].get()) {
			String value = bucket.getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SaveEquivalentStresses.SECTION].get()) {
			String value = bucket.getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[SaveEquivalentStresses.MISSION].get()) {
			String value = bucket.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// spectrum name
		if (options_[SaveEquivalentStresses.SPEC_NAME].get()) {
			String value = bucket.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// pilot point name
		if (options_[SaveEquivalentStresses.PP_NAME].get()) {
			sheet.addCell(new jxl.write.Label(column, row, stfName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[SaveEquivalentStresses.EID].get()) {
			sheet.addCell(new jxl.write.Label(column, row, eid, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[SaveEquivalentStresses.MAT_NAME].get()) {
			String materialName = stresses.getString("material_name");
			materialName += "/" + stresses.getString("material_specification");
			materialName += "/" + stresses.getString("material_orientation");
			materialName += "/" + stresses.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[SaveEquivalentStresses.MAT_DATA].get()) {

			// Ceff
			double ceff = stresses.getDouble("material_ceff");
			sheet.addCell(new jxl.write.Number(column, row, ceff, getDataFormat(row, CellType.NUMBER, true)));
			column++;

			// m
			double m = stresses.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// A
			double a = stresses.getDouble("material_a");
			sheet.addCell(new jxl.write.Number(column, row, a, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// B
			double b = stresses.getDouble("material_b");
			sheet.addCell(new jxl.write.Number(column, row, b, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// C
			double c = stresses.getDouble("material_c");
			sheet.addCell(new jxl.write.Number(column, row, c, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Ftu
			double ftu = stresses.getDouble("material_ftu");
			sheet.addCell(new jxl.write.Number(column, row, ftu, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Fty
			double fty = stresses.getDouble("material_fty");
			sheet.addCell(new jxl.write.Number(column, row, fty, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[SaveEquivalentStresses.VALIDITY].get()) {
			int value = (int) stresses.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[SaveEquivalentStresses.OMISSION].get()) {
			double value = stresses.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// equivalent stress
		if (options_[SaveEquivalentStresses.EQUIVALENT_STRESS].get()) {
			double value = stresses.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes column headers according selected options.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeHeaders(WritableSheet sheet) throws Exception {

		// initialize column index
		int column = 0;

		// get header format
		WritableCellFormat format = getHeaderFormat();

		// program
		if (options_[SaveEquivalentStresses.PROGRAM].get()) {
			String header = "A/C program";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// section
		if (options_[SaveEquivalentStresses.SECTION].get()) {
			String header = "A/C section";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// mission
		if (options_[SaveEquivalentStresses.MISSION].get()) {
			String header = "Fatigue mission";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// spectrum name
		if (options_[SaveEquivalentStresses.SPEC_NAME].get()) {
			String header = "Spectrum name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// pilot point name
		if (options_[SaveEquivalentStresses.PP_NAME].get()) {
			String header = "Pilot point name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// element ID
		if (options_[SaveEquivalentStresses.EID].get()) {
			String header = "Element ID";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// material name
		if (options_[SaveEquivalentStresses.MAT_NAME].get()) {
			String header = "Material name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// material data
		if (options_[SaveEquivalentStresses.MAT_DATA].get()) {

			// fatigue
			if (stressType_ == SaveEquivalentStressInfoPanel.FATIGUE) {

				// p
				String header = "Material slope (p)";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;

				// q
				header = "Material constant (q)";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;

				// m
				header = "Material constant (m)";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;
			}

			// propagation
			else if ((stressType_ == SaveEquivalentStressInfoPanel.PREFFAS) || (stressType_ == SaveEquivalentStressInfoPanel.LINEAR)) {

				// Ceff
				String header = "Material constant (Ceff)";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;

				// m
				header = "Material constant (m)";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;

				// A
				header = "Material constant (A)";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;

				// B
				header = "Material constant (B)";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;

				// C
				header = "Material constant (C)";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;

				// Ftu
				header = "Ftu";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;

				// Fty
				header = "Fty";
				sheet.addCell(new jxl.write.Label(column, 0, header, format));
				sheet.setColumnView(column, header.length());
				column++;
			}
		}

		// validity
		if (options_[SaveEquivalentStresses.VALIDITY].get()) {
			String header = "Spectrum validity";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// omission level
		if (options_[SaveEquivalentStresses.OMISSION].get()) {
			String header = "Omission level";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// equivalent stress
		if (options_[SaveEquivalentStresses.EQUIVALENT_STRESS].get()) {
			String header = getStressName(stressType_);
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}
	}

	/**
	 * Returns the name of equivalent stress.
	 *
	 * @param stressType
	 *            Equivalent stress type.
	 * @return The name of equivalent stress.
	 */
	private static String getStressName(int stressType) {
		if (stressType == SaveEquivalentStressInfoPanel.FATIGUE)
			return "Fatigue equivalent stress";
		else if (stressType == SaveEquivalentStressInfoPanel.PREFFAS)
			return "Preffas propagation equivalent stress";
		else if (stressType == SaveEquivalentStressInfoPanel.LINEAR)
			return "Linear propagation equivalent stress";
		return null;
	}

	/**
	 * Returns the SQL query for stress.
	 *
	 * @return SQL query.
	 */
	private String getQuery() {
		String sql = null;
		if (stressType_ == SaveEquivalentStressInfoPanel.FATIGUE) {
			sql = "select * from fast_fatigue_equivalent_stresses where stf_id = ?";
		}
		else if (stressType_ == SaveEquivalentStressInfoPanel.PREFFAS) {
			sql = "select * from fast_preffas_equivalent_stresses where stf_id = ?";
		}
		else if (stressType_ == SaveEquivalentStressInfoPanel.LINEAR) {
			sql = "select * from fast_linear_equivalent_stresses where stf_id = ?";
		}
		return sql;
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
		cellFormat.setBackground((rowIndex % 2) == 0 ? Colour.WHITE : Colour.VERY_LIGHT_YELLOW);
		return cellFormat;
	}
}
