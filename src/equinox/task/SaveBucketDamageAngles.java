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
import equinox.data.fileType.STFFileBucket;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableSaveBucketDamageAngles;
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
 * Class for save damage angles form STF file buckets task.
 *
 * @author Murat Artim
 * @date 5 Sep 2016
 * @time 12:16:19
 */
public class SaveBucketDamageAngles extends InternalEquinoxTask<Void> implements LongRunningTask, SavableTask {

	/** STF file buckets. */
	private final ArrayList<STFFileBucket> buckets_;

	/** Options. */
	private final BooleanProperty[] options_;

	/** Output file. */
	private final File output_;

	/**
	 * Creates save damage angles form STF file buckets task.
	 *
	 * @param buckets
	 *            STF file buckets.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SaveBucketDamageAngles(ArrayList<STFFileBucket> buckets, BooleanProperty[] options, File output) {
		buckets_ = buckets;
		options_ = options;
		output_ = output;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Save damage angles";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableSaveBucketDamageAngles(buckets_, options_, output_);
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

				// prepare statement for getting STF files
				String sql = "select file_id, name, eid from stf_files where cdf_id = ? order by name";
				try (PreparedStatement getSTFs = connection.prepareStatement(sql)) {

					// prepare statement for getting damage angles
					sql = "select * from maxdam_angles where stf_id = ?";
					try (PreparedStatement getDamageAngles = connection.prepareStatement(sql)) {

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
									updateMessage("Writing damage angles of STF file '" + stfName + "'...");
									updateProgress(stfCount, numSTFs);
									stfCount++;

									// get damage angles
									getDamageAngles.setInt(1, stfID);
									try (ResultSet damageAngles = getDamageAngles.executeQuery()) {

										// loop over damage angles
										while (damageAngles.next()) {

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

											// write damage angle
											writeData(sheet, bucket, stfName, eid, damageAngles, rowIndex + 1);

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
	 * @param damageAngles
	 *            Result set to get damage angles.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeData(WritableSheet sheet, STFFileBucket bucket, String stfName, String eid, ResultSet damageAngles, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[SaveDamageAngles.PROGRAM].get()) {
			String value = bucket.getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// section
		if (options_[SaveDamageAngles.SECTION].get()) {
			String value = bucket.getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// mission
		if (options_[SaveDamageAngles.MISSION].get()) {
			String value = bucket.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// spectrum name
		if (options_[SaveDamageAngles.SPEC_NAME].get()) {
			String value = bucket.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// pilot point name
		if (options_[SaveDamageAngles.PP_NAME].get()) {
			sheet.addCell(new jxl.write.Label(column, row, stfName, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// element ID
		if (options_[SaveDamageAngles.EID].get()) {
			sheet.addCell(new jxl.write.Label(column, row, eid, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// material name
		if (options_[SaveDamageAngles.MAT_NAME].get()) {
			String materialName = damageAngles.getString("material_name");
			materialName += "/" + damageAngles.getString("material_specification");
			materialName += "/" + damageAngles.getString("material_orientation");
			materialName += "/" + damageAngles.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// fatigue slope p
		if (options_[SaveDamageAngles.FAT_P].get()) {
			double value = damageAngles.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// fatigue constant q
		if (options_[SaveDamageAngles.FAT_Q].get()) {
			double value = damageAngles.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// omission level
		if (options_[SaveDamageAngles.OMISSION].get()) {
			double value = damageAngles.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			}
			column++;
		}

		// damage angle
		if (options_[SaveDamageAngles.DAM_ANGLE].get()) {
			double value = Math.toDegrees(damageAngles.getDouble("angle"));
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// fatigue equivalent stress
		if (options_[SaveDamageAngles.FAT_STRESS].get()) {
			double value = damageAngles.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
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
		if (options_[SaveDamageAngles.PROGRAM].get()) {
			String header = "A/C program";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// section
		if (options_[SaveDamageAngles.SECTION].get()) {
			String header = "A/C section";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// mission
		if (options_[SaveDamageAngles.MISSION].get()) {
			String header = "Fatigue mission";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// spectrum name
		if (options_[SaveDamageAngles.SPEC_NAME].get()) {
			String header = "Spectrum name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// pilot point name
		if (options_[SaveDamageAngles.PP_NAME].get()) {
			String header = "Pilot point name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// element ID
		if (options_[SaveDamageAngles.EID].get()) {
			String header = "Element ID";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// material name
		if (options_[SaveDamageAngles.MAT_NAME].get()) {
			String header = "Material name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue slope p
		if (options_[SaveDamageAngles.FAT_P].get()) {
			String header = "Fatigue material slope (p)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue constant q
		if (options_[SaveDamageAngles.FAT_Q].get()) {
			String header = "Fatigue material constant (q)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// omission level
		if (options_[SaveDamageAngles.OMISSION].get()) {
			String header = "Omission level";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// damage angle
		if (options_[SaveDamageAngles.DAM_ANGLE].get()) {
			String header = "Maximum damage angle";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue equivalent stress
		if (options_[SaveDamageAngles.FAT_STRESS].get()) {
			String header = "Maximum fatigue eq. stress";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
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
		cellFormat.setBackground((rowIndex % 2) == 0 ? Colour.WHITE : Colour.VERY_LIGHT_YELLOW);
		return cellFormat;
	}
}
