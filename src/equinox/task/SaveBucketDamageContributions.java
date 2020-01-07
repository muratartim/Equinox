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
import equinox.dataServer.remote.data.ContributionType;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableSaveBucketDamageContributions;
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
 * Class for save damage contributions from STF file buckets task.
 *
 * @author Murat Artim
 * @date 5 Sep 2016
 * @time 10:17:25
 */
public class SaveBucketDamageContributions extends InternalEquinoxTask<Void> implements LongRunningTask, SavableTask {

	/** STF file buckets. */
	private final List<STFFileBucket> buckets_;

	/** Damage contribution names. */
	private final List<String> contributionNames_;

	/** Options. */
	private final boolean[] options_;

	/** Output file. */
	private final File output_;

	/**
	 * Creates save damage contributions from STF file buckets task.
	 *
	 * @param buckets
	 *            STF file buckets.
	 * @param contributionNames
	 *            Damage contribution names.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SaveBucketDamageContributions(List<STFFileBucket> buckets, List<String> contributionNames, boolean[] options, File output) {
		buckets_ = buckets;
		contributionNames_ = contributionNames;
		options_ = options;
		output_ = output;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Save damage contributions";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableSaveBucketDamageContributions(buckets_, contributionNames_, options_, output_);
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

					// prepare statement to get overall damage contribution info
					sql = "select contributions_id, stress, omission_level, material_name, material_specification, material_orientation, ";
					sql += "material_configuration, material_p, material_q ";
					sql += "from dam_contributions where stf_id = ?";
					try (PreparedStatement getContribuionInfo = connection.prepareStatement(sql)) {

						// prepare statement to get specific contribution
						sql = "select stress from dam_contribution where contributions_id = ? and name = ?";
						try (PreparedStatement getContribution = connection.prepareStatement(sql)) {

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
										updateMessage("Writing damage contributions of STF file '" + stfName + "'...");
										updateProgress(stfCount, numSTFs);
										stfCount++;

										// get contribution info
										getContribuionInfo.setInt(1, stfID);
										try (ResultSet contribuionInfo = getContribuionInfo.executeQuery()) {

											// loop over damage contribution info
											while (contribuionInfo.next()) {

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

												// write contribution info
												getContribution.setInt(1, contribuionInfo.getInt("contributions_id"));
												writeData(sheet, bucket, stfName, eid, contribuionInfo, getContribution, rowIndex + 1);

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
	 * @param contribuionInfo
	 *            Result set to get overall info
	 * @param getContribution
	 *            Statement to get contribution value.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeData(WritableSheet sheet, STFFileBucket bucket, String stfName, String eid, ResultSet contribuionInfo, PreparedStatement getContribution, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[SaveDamageContributions.PROGRAM]) {
			String value = bucket.getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// section
		if (options_[SaveDamageContributions.SECTION]) {
			String value = bucket.getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// mission
		if (options_[SaveDamageContributions.MISSION]) {
			String value = bucket.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// spectrum name
		if (options_[SaveDamageContributions.SPEC_NAME]) {
			String value = bucket.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// pilot point name
		if (options_[SaveDamageContributions.PP_NAME]) {
			sheet.addCell(new jxl.write.Label(column, row, stfName, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// element ID
		if (options_[SaveDamageContributions.EID]) {
			sheet.addCell(new jxl.write.Label(column, row, eid, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// material name
		if (options_[SaveDamageContributions.MAT_NAME]) {
			String materialName = contribuionInfo.getString("material_name");
			materialName += "/" + contribuionInfo.getString("material_specification");
			materialName += "/" + contribuionInfo.getString("material_orientation");
			materialName += "/" + contribuionInfo.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// fatigue slope p
		if (options_[SaveDamageContributions.FAT_P]) {
			double value = contribuionInfo.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// fatigue constant q
		if (options_[SaveDamageContributions.FAT_Q]) {
			double value = contribuionInfo.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// omission level
		if (options_[SaveDamageContributions.OMISSION]) {
			double value = contribuionInfo.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			}
			column++;
		}

		// full damage
		double totalStress = contribuionInfo.getDouble("stress");
		if (options_[SaveDamageContributions.FULL]) {
			sheet.addCell(new jxl.write.Number(column, row, totalStress, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// 1G
		if (options_[SaveDamageContributions.ONEG]) {
			column = writeStress(getContribution, totalStress, sheet, row, column, ContributionType.ONEG.getName(), true);
		}

		// GAG
		if (options_[SaveDamageContributions.GAG]) {
			column = writeStress(getContribution, totalStress, sheet, row, column, ContributionType.GAG.getName(), false);
		}

		// DP
		if (options_[SaveDamageContributions.DP]) {
			column = writeStress(getContribution, totalStress, sheet, row, column, ContributionType.DELTA_P.getName(), true);
		}

		// DT
		if (options_[SaveDamageContributions.DT]) {
			column = writeStress(getContribution, totalStress, sheet, row, column, ContributionType.DELTA_T.getName(), true);
		}

		// increment
		if (options_[SaveDamageContributions.INC]) {

			// loop over contribution names
			for (String contributionName : contributionNames_) {

				// not increment
				if (contributionName.equals(ContributionType.ONEG.getName())) {
					continue;
				}
				else if (contributionName.equals(ContributionType.GAG.getName())) {
					continue;
				}
				else if (contributionName.equals(ContributionType.DELTA_P.getName())) {
					continue;
				}
				else if (contributionName.equals(ContributionType.DELTA_T.getName())) {
					continue;
				}

				// increment
				getContribution.setString(2, contributionName);
				try (ResultSet getContInfo = getContribution.executeQuery()) {
					while (getContInfo.next()) {
						double value = totalStress - getContInfo.getDouble("stress");
						if (options_[SaveDamageContributions.PERCENT]) {
							value = value * 100 / totalStress;
						}
						sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
						column++;
					}
				}
			}
		}
	}

	/**
	 * Writes out damage contribution equivalent stress.
	 *
	 * @param getContribution
	 *            Database statement to get stress.
	 * @param totalStress
	 *            Total stress.
	 * @param sheet
	 *            Worksheet.
	 * @param row
	 *            Row index.
	 * @param column
	 *            Column index.
	 * @param name
	 *            Name of contribution to write.
	 * @param isComplement
	 *            True if complementary stress value should be written.
	 * @return Column index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int writeStress(PreparedStatement getContribution, double totalStress, WritableSheet sheet, int row, int column, String name, boolean isComplement) throws Exception {

		// initialize stress value
		double value = 0.0;

		// loop over contribution names
		for (String contributionName : contributionNames_) {

			// found contribution
			if (contributionName.equals(name)) {

				// execute statement to get stress
				getContribution.setString(2, contributionName);
				try (ResultSet getContInfo = getContribution.executeQuery()) {
					if (getContInfo.next()) {
						value = isComplement ? totalStress - getContInfo.getDouble("stress") : getContInfo.getDouble("stress");
						if (options_[SaveDamageContributions.PERCENT]) {
							value = value * 100 / totalStress;
						}
					}
				}
				break;
			}
		}

		// write
		sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
		column++;

		// return column index
		return column;
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
		if (options_[SaveDamageContributions.PROGRAM]) {
			String header = "A/C program";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// section
		if (options_[SaveDamageContributions.SECTION]) {
			String header = "A/C section";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// mission
		if (options_[SaveDamageContributions.MISSION]) {
			String header = "Fatigue mission";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// spectrum name
		if (options_[SaveDamageContributions.SPEC_NAME]) {
			String header = "Spectrum name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// pilot point name
		if (options_[SaveDamageContributions.PP_NAME]) {
			String header = "Pilot point name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// element ID
		if (options_[SaveDamageContributions.EID]) {
			String header = "Element ID";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// material name
		if (options_[SaveDamageContributions.MAT_NAME]) {
			String header = "Material name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue slope p
		if (options_[SaveDamageContributions.FAT_P]) {
			String header = "Fatigue material slope (p)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue constant q
		if (options_[SaveDamageContributions.FAT_Q]) {
			String header = "Fatigue material constant (q)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// omission level
		if (options_[SaveDamageContributions.OMISSION]) {
			String header = "Omission level";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// full damage
		if (options_[SaveDamageContributions.FULL]) {
			String header = "Total equivalent stress";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// 1g
		if (options_[SaveDamageContributions.ONEG]) {
			String header = "1G contribution";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// gag
		if (options_[SaveDamageContributions.GAG]) {
			String header = "GAG contribution";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// DP
		if (options_[SaveDamageContributions.DP]) {
			String header = "Delta-P contribution";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// DT
		if (options_[SaveDamageContributions.DT]) {
			String header = "Delta-T contribution";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// increment
		if (options_[SaveDamageContributions.INC]) {

			// loop over contribution names
			for (String cont : contributionNames_) {

				// not increment
				if (cont.equals(ContributionType.ONEG.getName())) {
					continue;
				}
				else if (cont.equals(ContributionType.GAG.getName())) {
					continue;
				}
				else if (cont.equals(ContributionType.DELTA_P.getName())) {
					continue;
				}
				else if (cont.equals(ContributionType.DELTA_T.getName())) {
					continue;
				}

				// increment
				sheet.addCell(new jxl.write.Label(column, 0, cont, format));
				sheet.setColumnView(column, cont.length());
				column++;
			}
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
