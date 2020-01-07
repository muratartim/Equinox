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
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.fileType.DamageAngle;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;
import equinox.task.serializableTask.SerializableSaveDamageAngles;
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
 * Class for save damage angles task.
 *
 * @author Murat Artim
 * @date Sep 9, 2015
 * @time 4:00:15 PM
 */
public class SaveDamageAngles extends InternalEquinoxTask<Path> implements LongRunningTask, SavableTask, MultipleInputTask<DamageAngle>, AutomaticTaskOwner<Path> {

	/** Option index. */
	public static final int DAM_ANGLE = 0, FAT_STRESS = 1, MAT_NAME = 2, FAT_P = 3, FAT_Q = 4, PP_NAME = 5, EID = 6, SPEC_NAME = 7, PROGRAM = 8, SECTION = 9, MISSION = 10, OMISSION = 11;

	/** Damage angles. */
	private final List<DamageAngle> angles_;

	/** Options. */
	private final BooleanProperty[] options_;

	/** Output file. */
	private final File output_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save damage angles task.
	 *
	 * @param angles
	 *            Damage angles. Can be null for automatic execution.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SaveDamageAngles(List<DamageAngle> angles, BooleanProperty[] options, File output) {
		angles_ = angles == null ? Collections.synchronizedList(new ArrayList<>()) : angles;
		options_ = options;
		output_ = output;
	}

	@Override
	synchronized public void setInputThreshold(int inputThreshold) {
		inputThreshold_ = inputThreshold;
	}

	@Override
	synchronized public void addAutomaticInput(AutomaticTaskOwner<DamageAngle> task, DamageAngle input, ExecutionMode mode) {
		automaticInputAdded(task, input, mode, angles_, inputThreshold_);
	}

	@Override
	synchronized public void inputFailed(AutomaticTaskOwner<DamageAngle> task, ExecutionMode mode) {
		inputThreshold_ = automaticInputFailed(task, mode, angles_, inputThreshold_);
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
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Save damage angles";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableSaveDamageAngles(angles_, options_, output_);
	}

	@Override
	protected Path call() throws Exception {

		// update progress info
		updateTitle("Saving equivalent stresses to '" + output_.getName() + "'");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(output_);

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Damage Angles", 0);

			// write headers
			writeHeaders(sheet);

			// get database connection
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {

				// prepare statement
				String sql = "select * from maxdam_angles where angle_id = ?";
				try (PreparedStatement statement = connection.prepareStatement(sql)) {

					// loop over angles
					for (int i = 0; i < angles_.size(); i++) {

						// get equivalent stress
						DamageAngle angle = angles_.get(i);

						// update info
						updateMessage("Writing damage angle " + angle.getName() + "...");
						updateProgress(i, angles_.size());

						// execute query
						statement.setInt(1, angle.getID());
						try (ResultSet resultSet = statement.executeQuery()) {
							while (resultSet.next()) {
								writeData(sheet, angle, resultSet, i + 1);
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
		return output_.toPath();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// no automatic task
		if (automaticTasks_ == null)
			return;

		try {

			// get output file
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

		// no automatic task
		if (automaticTasks_ == null)
			return;

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// no automatic task
		if (automaticTasks_ == null)
			return;

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param angle
	 *            Damage angle.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeData(WritableSheet sheet, DamageAngle angle, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = angle.getParentItem().getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = angle.getParentItem().getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = angle.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME].get()) {
			String value = angle.getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// pilot point name
		if (options_[PP_NAME].get()) {
			String value = angle.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = angle.getParentItem().getEID();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// fatigue slope p
		if (options_[FAT_P].get()) {
			double value = resultSet.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// fatigue constant q
		if (options_[FAT_Q].get()) {
			double value = resultSet.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			}
			column++;
		}

		// damage angle
		if (options_[DAM_ANGLE].get()) {
			double value = Math.toDegrees(resultSet.getDouble("angle"));
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// fatigue equivalent stress
		if (options_[FAT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
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
		if (options_[PROGRAM].get()) {
			String header = "A/C program";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String header = "A/C section";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String header = "Fatigue mission";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME].get()) {
			String header = "Spectrum name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// pilot point name
		if (options_[PP_NAME].get()) {
			String header = "Pilot point name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String header = "Element ID";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String header = "Material name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue slope p
		if (options_[FAT_P].get()) {
			String header = "Fatigue material slope (p)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue constant q
		if (options_[FAT_Q].get()) {
			String header = "Fatigue material constant (q)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			String header = "Omission level";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// damage angle
		if (options_[DAM_ANGLE].get()) {
			String header = "Maximum damage angle";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue equivalent stress
		if (options_[FAT_STRESS].get()) {
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
		cellFormat.setBackground(rowIndex % 2 == 0 ? Colour.WHITE : Colour.VERY_LIGHT_YELLOW);
		return cellFormat;
	}
}
