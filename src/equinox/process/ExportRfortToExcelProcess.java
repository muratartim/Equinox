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

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import equinox.data.fileType.Rfort;
import equinox.task.InternalEquinoxTask;
import equinox.task.SaveRfortInfo;
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
 * Class for export RFORT results to MS Excel process.
 *
 * @author Murat Artim
 * @date Apr 8, 2016
 * @time 4:07:42 PM
 */
public class ExportRfortToExcelProcess implements EquinoxProcess<Void> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** RFORT file. */
	private final Rfort rfort_;

	/** Output Excel file. */
	private final Path outputFile_;

	/** Selected pilot points and omissions. */
	private final ArrayList<String> pilotPoints_, omissions_;

	/**
	 * Creates export RFORT results to MS Excel process.
	 *
	 * @param task
	 *            The owner task.
	 * @param rfort
	 *            RFORT file.
	 * @param outputFile
	 *            Output Excel file.
	 * @param pilotPoints
	 *            Selected pilot points. Can be null for all.
	 * @param omissions
	 *            Selected omissions. Can be null for all.
	 */
	public ExportRfortToExcelProcess(InternalEquinoxTask<?> task, Rfort rfort, Path outputFile, ArrayList<String> pilotPoints, ArrayList<String> omissions) {
		task_ = task;
		rfort_ = rfort;
		outputFile_ = outputFile;
		pilotPoints_ = pilotPoints;
		omissions_ = omissions;
	}

	@Override
	public Void start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress
		task_.updateMessage("Export RFORT results to '" + outputFile_.getFileName() + "'");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(outputFile_.toFile());

			// create worksheet
			WritableSheet sheet = workbook.createSheet("RFORT Analysis", 0);

			// write headers
			writeHeaders(sheet);

			// initialize row index
			int rowIndex = 1;

			// prepare statement for getting equivalent stresses
			String sql = "select eq_stress from rfort_outputs where analysis_id = " + rfort_.getID();
			sql += " and pp_name = ? and omission_name = ? and stress_type = ?";
			try (PreparedStatement getEquivalentStress = connection.prepareStatement(sql)) {

				// prepare statement for getting pilot point info
				sql = "select pp_name, included_in_rfort, num_peaks, stress_amp, omission_value from rfort_outputs where ";
				sql += "analysis_id = " + rfort_.getID() + " and stress_type = '" + SaveRfortInfo.FATIGUE + "' ";
				sql += "and omission_name = ?";
				try (PreparedStatement getPPInfo = connection.prepareStatement(sql)) {

					// get omission levels
					try (Statement getOmissionLevels = connection.createStatement()) {
						sql = "select distinct omission_name, num_peaks from rfort_outputs where analysis_id = " + rfort_.getID() + " order by num_peaks desc";
						try (ResultSet omissions = getOmissionLevels.executeQuery(sql)) {

							// loop over omissions
							while (omissions.next()) {

								// get omission name
								String omissionName = omissions.getString("omission_name");

								// not selected
								if (omissions_ != null && !omissions_.contains(omissionName))
									continue;

								// get pilot point info
								getPPInfo.setString(1, omissionName);
								try (ResultSet ppInfo = getPPInfo.executeQuery()) {

									// loop over pilot points
									while (ppInfo.next()) {

										// get pilot point name
										String ppName = ppInfo.getString("pp_name");

										// not selected
										if (pilotPoints_ != null && !pilotPoints_.contains(ppName))
											continue;

										// get pilot point info
										boolean includedInRfort = ppInfo.getBoolean("included_in_rfort");
										int numPeaks = ppInfo.getInt("num_peaks");
										double stressAmp = ppInfo.getDouble("stress_amp");
										double omissionValue = ppInfo.getDouble("omission_value");
										double fatStress = -1.0;
										double prefStress = -1.0;
										double linStress = -1.0;

										// set inputs for getting equivalent stresses
										getEquivalentStress.setString(1, ppName);
										getEquivalentStress.setString(2, omissionName);

										// get fatigue equivalent stress
										if (rfort_.isFatigue()) {
											getEquivalentStress.setString(3, SaveRfortInfo.FATIGUE);
											try (ResultSet eqStress = getEquivalentStress.executeQuery()) {
												while (eqStress.next())
													fatStress = eqStress.getDouble("eq_stress");
											}
										}

										// get preffas equivalent stress
										if (rfort_.isPreffas()) {
											getEquivalentStress.setString(3, SaveRfortInfo.PREFFAS);
											try (ResultSet eqStress = getEquivalentStress.executeQuery()) {
												while (eqStress.next())
													prefStress = eqStress.getDouble("eq_stress");
											}
										}

										// get linear equivalent stress
										if (rfort_.isLinear()) {
											getEquivalentStress.setString(3, SaveRfortInfo.LINEAR);
											try (ResultSet eqStress = getEquivalentStress.executeQuery()) {
												while (eqStress.next())
													linStress = eqStress.getDouble("eq_stress");
											}
										}

										// write data
										writeData(sheet, ppName, includedInRfort, omissionName, numPeaks, stressAmp, omissionValue, fatStress, prefStress, linStress, rowIndex);
										rowIndex++;
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
			if (workbook != null)
				workbook.close();
		}

		// return
		return null;
	}

	/**
	 * Writes pilot point data.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param ppName
	 *            Pilot point name.
	 * @param includedInRfort
	 *            True if pilot point was included in RFORT analysis.
	 * @param omissionName
	 *            Omission name.
	 * @param numPeaks
	 *            Number of peaks.
	 * @param stressAmp
	 *            Stress amplitude.
	 * @param omissionValue
	 *            Omission value.
	 * @param fatStress
	 *            Fatigue eq. stress.
	 * @param prefStress
	 *            Preffas eq. stress.
	 * @param linStress
	 *            Linear eq. stress.
	 * @param rowIndex
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeData(WritableSheet sheet, String ppName, boolean includedInRfort, String omissionName, int numPeaks, double stressAmp, double omissionValue, double fatStress, double prefStress, double linStress, int rowIndex) throws Exception {

		// initialize column index
		int column = 0;

		// pilot point name
		sheet.addCell(new jxl.write.Label(column, rowIndex, ppName, getDataFormat(rowIndex, CellType.LABEL, false)));
		column++;

		// included in RFORT
		sheet.addCell(new jxl.write.Label(column, rowIndex, includedInRfort ? "Yes" : "No", getDataFormat(rowIndex, CellType.LABEL, false)));
		column++;

		// omission name
		sheet.addCell(new jxl.write.Label(column, rowIndex, omissionName, getDataFormat(rowIndex, CellType.LABEL, false)));
		column++;

		// number of peaks
		sheet.addCell(new jxl.write.Number(column, rowIndex, numPeaks, getDataFormat(rowIndex, CellType.NUMBER, false)));
		column++;

		// stress amplitude
		sheet.addCell(new jxl.write.Number(column, rowIndex, stressAmp, getDataFormat(rowIndex, CellType.NUMBER, false)));
		column++;

		// omission value
		sheet.addCell(new jxl.write.Number(column, rowIndex, omissionValue, getDataFormat(rowIndex, CellType.NUMBER, false)));
		column++;

		// fatigue equivalent stress
		if (rfort_.isFatigue()) {
			sheet.addCell(new jxl.write.Number(column, rowIndex, fatStress, getDataFormat(rowIndex, CellType.NUMBER, false)));
			column++;
		}

		// preffas equivalent stress
		if (rfort_.isPreffas()) {
			sheet.addCell(new jxl.write.Number(column, rowIndex, prefStress, getDataFormat(rowIndex, CellType.NUMBER, false)));
			column++;
		}

		// linear equivalent stress
		if (rfort_.isLinear()) {
			sheet.addCell(new jxl.write.Number(column, rowIndex, linStress, getDataFormat(rowIndex, CellType.NUMBER, false)));
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

		// pilot point
		String header = "Pilot Point";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// included in RFORT
		header = "Included in RFORT";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// omission
		header = "Omission";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// number of peaks
		header = "Average Number of Peaks per Flight";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// stress amplitude
		header = "Max. Stress Amplitude";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// omission value
		header = "Omission Value";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// fatigue equivalent stress
		if (rfort_.isFatigue()) {
			header = "Fatigue Eq. Stress";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// preffas equivalent stress
		if (rfort_.isPreffas()) {
			header = "Preffas Eq. Stress";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// linear equivalent stress
		if (rfort_.isLinear()) {
			header = "Linear Prop. Eq. Stress";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
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
