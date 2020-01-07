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
import java.sql.Statement;

import org.controlsfx.control.ToggleSwitch;

import equinox.Equinox;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableSaveAircraftLifeFactors;
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
 * Class for save A/C model life factors task.
 *
 * @author Murat Artim
 * @date Oct 1, 2015
 * @time 4:32:41 PM
 */
public class SaveAircraftLifeFactors extends InternalEquinoxTask<Void> implements LongRunningTask, SavableTask {

	/** Option index. */
	public static final int FAT_FACTOR = 0, PROP_FACTOR = 1, FAT_P = 2, ELBER_M = 3, EID = 4, MISSION = 5;

	/** Equivalent stress. */
	private final AircraftFatigueEquivalentStress eqStress_;

	/** Options. */
	private final BooleanProperty[] options_;

	/** Output file. */
	private final File output_;

	/** Basis mission. */
	private final String basisMission_;

	/**
	 * Creates save A/C model life factors task.
	 *
	 * @param eqStress
	 *            Equivalent stress.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 * @param basisMission
	 *            Basis mission.
	 */
	public SaveAircraftLifeFactors(AircraftFatigueEquivalentStress eqStress, BooleanProperty[] options, File output, String basisMission) {
		eqStress_ = eqStress;
		options_ = options;
		output_ = output;
		basisMission_ = basisMission;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Save aircraft life factors";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableSaveAircraftLifeFactors(eqStress_, options_, output_, basisMission_);
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Saving aircraft life factors to '" + output_.getName() + "'");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(output_);

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Life Factors", 0);

			// write headers
			writeHeaders(sheet);

			// get database connection
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {
				writeLifeFactors(connection, sheet);
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
	 * Writes out life factors.
	 *
	 * @param connection
	 *            Database connection.
	 * @param sheet
	 *            Worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeLifeFactors(Connection connection, WritableSheet sheet) throws Exception {

		// initialize row index
		int row = 1;

		// prepare statement to get other mission stresses
		try (PreparedStatement statement1 = connection.prepareStatement(createOtherMissionQuery())) {

			// create and execute query for basis mission
			try (Statement statement2 = connection.createStatement()) {
				try (ResultSet getBasisMission = statement2.executeQuery(createBasisMissionQuery())) {

					// loop over basis mission stresses
					while (getBasisMission.next()) {

						// get data
						int eid = getBasisMission.getInt("eid");
						double fatP = getBasisMission.getDouble("fat_p");
						double elberM = getBasisMission.getDouble("elber_m");

						// execute query for other missions
						statement1.setDouble(1, getBasisMission.getDouble("fat_stress"));
						statement1.setDouble(2, fatP);
						statement1.setDouble(3, getBasisMission.getDouble("prop_stress"));
						statement1.setDouble(4, elberM);
						statement1.setInt(5, eid);
						try (ResultSet getOtherMission = statement1.executeQuery()) {

							// loop over other missions
							while (getOtherMission.next()) {

								// get mission
								String mission = getOtherMission.getString("mission");
								double fatLF = getOtherMission.getDouble("fatLF");
								double propLF = getOtherMission.getDouble("propLF");

								// write data row
								writeDataRow(sheet, eid, mission, fatLF, propLF, fatP, elberM, row);
								row++;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param eid
	 *            Element ID.
	 * @param mission
	 *            Fatigue mission.
	 * @param fatLF
	 *            Fatigue life factor.
	 * @param propLF
	 *            Propagation life factor.
	 * @param fatP
	 *            Fatigue material slope (p).
	 * @param elberM
	 *            Elber constant (m).
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataRow(WritableSheet sheet, int eid, String mission, double fatLF, double propLF, double fatP, double elberM, int row) throws Exception {

		// initialize column index
		int column = 0;

		// mission
		if (options_[MISSION].get()) {
			sheet.addCell(new jxl.write.Label(column, row, mission, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			sheet.addCell(new jxl.write.Label(column, row, Integer.toString(eid), getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// fatigue slope p
		if (options_[FAT_P].get()) {
			sheet.addCell(new jxl.write.Number(column, row, fatP, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// elber m
		if (options_[ELBER_M].get()) {
			sheet.addCell(new jxl.write.Number(column, row, elberM, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// fatigue life factor
		if (options_[FAT_FACTOR].get()) {
			sheet.addCell(new jxl.write.Number(column, row, fatLF, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// propagation life factor
		if (options_[PROP_FACTOR].get()) {
			sheet.addCell(new jxl.write.Number(column, row, propLF, getDataFormat(row, CellType.NUMBER)));
			column++;
		}
	}

	/**
	 * Creates query with element grouping.
	 *
	 * @return Query.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String createBasisMissionQuery() throws Exception {

		// get table names
		int modelID = eqStress_.getParentItem().getParentItem().getID();
		String stressTable = "ac_eq_stresses_" + modelID;

		// select stresses
		String sql = "select eid, ";
		sql += "fat_stress, prop_stress, fat_p, elber_m";
		sql += " from " + stressTable;

		// add equivalent stress name and mission criteria
		sql += " where name = '" + eqStress_.getName() + "'";
		sql += " and mission = '" + basisMission_ + "'";

		// return query
		return sql;
	}

	/**
	 * Creates query with element grouping.
	 *
	 * @return Query.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String createOtherMissionQuery() throws Exception {

		// get table names
		int modelID = eqStress_.getParentItem().getParentItem().getID();
		String stressTable = "ac_eq_stresses_" + modelID;

		// select stresses
		String sql = "select mission, ";
		sql += "(power(?/fat_stress, ?)) as fatLF, ";
		sql += "(power(?/prop_stress, ?)) as propLF";
		sql += " from " + stressTable;

		// add equivalent stress name and mission criteria
		sql += " where name = '" + eqStress_.getName() + "'";
		sql += " and eid = ?";

		// return query
		return sql;
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

		// mission
		if (options_[MISSION].get()) {
			String header = ((ToggleSwitch) options_[MISSION].getBean()).getText();
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String header = ((ToggleSwitch) options_[EID].getBean()).getText();
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue slope p
		if (options_[FAT_P].get()) {
			String header = ((ToggleSwitch) options_[FAT_P].getBean()).getText();
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// elber m
		if (options_[ELBER_M].get()) {
			String header = ((ToggleSwitch) options_[ELBER_M].getBean()).getText();
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue life factor
		if (options_[FAT_FACTOR].get()) {
			String header = ((ToggleSwitch) options_[FAT_FACTOR].getBean()).getText();
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// propagation life factor
		if (options_[PROP_FACTOR].get()) {
			String header = ((ToggleSwitch) options_[PROP_FACTOR].getBean()).getText();
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
