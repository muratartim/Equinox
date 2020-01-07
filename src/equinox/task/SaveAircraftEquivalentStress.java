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
import java.sql.ResultSet;
import java.sql.Statement;

import equinox.Equinox;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
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
 * Class for save A/C equivalent stress task.
 *
 * @author Murat Artim
 * @date Sep 8, 2015
 * @time 2:36:20 PM
 */
public class SaveAircraftEquivalentStress extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** File item to save. */
	private final AircraftFatigueEquivalentStress equivalentStress_;

	/** Output file. */
	private final File output_;

	/**
	 * Creates save A/C equivalent stress task.
	 *
	 * @param equivalentStress
	 *            File item to save.
	 * @param output
	 *            Output file.
	 */
	public SaveAircraftEquivalentStress(AircraftFatigueEquivalentStress equivalentStress, File output) {
		equivalentStress_ = equivalentStress;
		output_ = output;
	}

	@Override
	public String getTaskTitle() {
		return "Save A/C equivalent stress '" + equivalentStress_.getName() + "' to '" + output_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving equivalent stresses to '" + output_.getName() + "'");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(output_);

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Equivalent Stresses", 0);

			// write headers
			writeHeaders(sheet);

			// update info
			updateMessage("Writing equivalent stresses...");

			// get database connection
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {

				// create statement
				try (Statement statement = connection.createStatement()) {

					// get number of elements
					int numLines = 0;
					String sql = "select count(name) as numlines from ac_eq_stresses_" + equivalentStress_.getID();
					sql += " where name = '" + equivalentStress_.getName() + "'";
					try (ResultSet resultSet = statement.executeQuery(sql)) {
						while (resultSet.next()) {
							numLines = resultSet.getInt("numlines");
						}
					}

					// write stresses
					sql = "select * from ac_eq_stresses_" + equivalentStress_.getID();
					sql += " where name = '" + equivalentStress_.getName() + "' order by mission, eid";
					try (ResultSet resultSet = statement.executeQuery(sql)) {

						// loop over lines
						int count = 0;
						while (resultSet.next()) {

							// update progress
							updateProgress(count, numLines);
							count++;

							// write data
							writeData(sheet, resultSet, count);
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
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void writeData(WritableSheet sheet, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// mission
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("mission"), getDataFormat(row, CellType.LABEL)));
		column++;

		// element ID
		sheet.addCell(new jxl.write.Label(column, row, Integer.toString(resultSet.getInt("eid")), getDataFormat(row, CellType.LABEL)));
		column++;

		// fatigue slope p
		sheet.addCell(new jxl.write.Number(column, row, resultSet.getDouble("fat_p"), getDataFormat(row, CellType.NUMBER)));
		column++;

		// elber m
		sheet.addCell(new jxl.write.Number(column, row, resultSet.getDouble("elber_m"), getDataFormat(row, CellType.NUMBER)));
		column++;

		// fatigue equivalent stress
		sheet.addCell(new jxl.write.Number(column, row, resultSet.getDouble("fat_stress"), getDataFormat(row, CellType.NUMBER)));
		column++;

		// propagation equivalent stress
		sheet.addCell(new jxl.write.Number(column, row, resultSet.getDouble("prop_stress"), getDataFormat(row, CellType.NUMBER)));
		column++;
	}

	/**
	 * Writes column headers according selected options.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void writeHeaders(WritableSheet sheet) throws Exception {

		// initialize column index
		int column = 0;

		// get header format
		WritableCellFormat format = getHeaderFormat();

		// mission
		sheet.addCell(new jxl.write.Label(column, 0, "Fatigue mission", format));
		sheet.setColumnView(column, "Fatigue mission".length());
		column++;

		// element ID
		sheet.addCell(new jxl.write.Label(column, 0, "Element ID", format));
		sheet.setColumnView(column, "Element ID".length());
		column++;

		// fatigue slope p
		sheet.addCell(new jxl.write.Label(column, 0, "Fatigue material slope (p)", format));
		sheet.setColumnView(column, "Fatigue material slope (p)".length());
		column++;

		// elber m
		sheet.addCell(new jxl.write.Label(column, 0, "Elber constant (m)", format));
		sheet.setColumnView(column, "Elber constant (m)".length());
		column++;

		// fatigue equivalent stress
		sheet.addCell(new jxl.write.Label(column, 0, "Fatigue eq. stress", format));
		sheet.setColumnView(column, "Fatigue eq. stress".length());
		column++;

		// propagation equivalent stress
		sheet.addCell(new jxl.write.Label(column, 0, "Propagation eq. stress", format));
		sheet.setColumnView(column, "Propagation eq. stress".length());
		column++;
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
