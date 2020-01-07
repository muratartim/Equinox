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
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.SpectrumItem;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;
import equinox.task.serializableTask.SerializableSaveEquivalentStresses;
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
 * Class for save equivalent stresses task.
 *
 * @author Murat Artim
 * @date May 6, 2015
 * @time 1:53:12 PM
 */
public class SaveEquivalentStresses extends InternalEquinoxTask<Path> implements LongRunningTask, SavableTask, MultipleInputTask<SpectrumItem>, AutomaticTaskOwner<Path> {

	/** Option index. */
	public static final int EQUIVALENT_STRESS = 0, MAT_NAME = 1, MAT_DATA = 2, PP_NAME = 3, EID = 4, SEQ_NAME = 5, SPEC_NAME = 6, PROGRAM = 7, SECTION = 8, MISSION = 9, VALIDITY = 10, MAX_STRESS = 11, MIN_STRESS = 12, R_RATIO = 13, OMISSION = 14;

	/** Equivalent stresses. */
	private final List<SpectrumItem> stresses_;

	/** Options. */
	private final BooleanProperty[] options_;

	/** Output file. */
	private final File output_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save equivalent stresses task.
	 *
	 * @param stresses
	 *            Equivalent stresses. Can be null for automatic execution.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SaveEquivalentStresses(List<SpectrumItem> stresses, BooleanProperty[] options, File output) {
		stresses_ = stresses == null ? Collections.synchronizedList(new ArrayList<>()) : stresses;
		options_ = options;
		output_ = output;
	}

	@Override
	synchronized public void setInputThreshold(int inputThreshold) {
		inputThreshold_ = inputThreshold;
	}

	@Override
	synchronized public void addAutomaticInput(AutomaticTaskOwner<SpectrumItem> task, SpectrumItem input, ExecutionMode mode) {
		automaticInputAdded(task, input, mode, stresses_, inputThreshold_);
	}

	@Override
	synchronized public void inputFailed(AutomaticTaskOwner<SpectrumItem> task, ExecutionMode mode) {
		inputThreshold_ = automaticInputFailed(task, mode, stresses_, inputThreshold_);
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
		return "Save equivalent stresses";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableSaveEquivalentStresses(stresses_, options_, output_);
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
			WritableSheet sheet = workbook.createSheet("Page 1", 0);

			// write headers
			writeHeaders(sheet, stresses_.get(0));

			// set table name
			String sql = getQuery(stresses_.get(0));

			// get database connection
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {

				// prepare statement
				try (PreparedStatement statement = connection.prepareStatement(sql)) {

					// loop over equivalent stresses
					for (int i = 0; i < stresses_.size(); i++) {

						// get equivalent stress
						SpectrumItem stress = stresses_.get(i);

						// update info
						updateMessage("Writing equivalent stress " + stress.getName() + "...");
						updateProgress(i, stresses_.size());

						// execute query
						statement.setInt(1, stress.getID());
						try (ResultSet resultSet = statement.executeQuery()) {
							while (resultSet.next()) {
								if (stress instanceof FatigueEquivalentStress) {
									writeDataForFatigueEquivalentStress(sheet, (FatigueEquivalentStress) stress, resultSet, i + 1);
								}
								else if (stress instanceof PreffasEquivalentStress) {
									writeDataForPreffasEquivalentStress(sheet, (PreffasEquivalentStress) stress, resultSet, i + 1);
								}
								else if (stress instanceof LinearEquivalentStress) {
									writeDataForLinearEquivalentStress(sheet, (LinearEquivalentStress) stress, resultSet, i + 1);
								}
								else if (stress instanceof ExternalFatigueEquivalentStress) {
									writeDataForExternalFatigueEquivalentStress(sheet, (ExternalFatigueEquivalentStress) stress, resultSet, i + 1);
								}
								else if (stress instanceof ExternalPreffasEquivalentStress) {
									writeDataForExternalPreffasEquivalentStress(sheet, (ExternalPreffasEquivalentStress) stress, resultSet, i + 1);
								}
								else if (stress instanceof ExternalLinearEquivalentStress) {
									writeDataForExternalLinearEquivalentStress(sheet, (ExternalLinearEquivalentStress) stress, resultSet, i + 1);
								}
								else if (stress instanceof FastFatigueEquivalentStress) {
									writeDataForFastFatigueEquivalentStress(sheet, (FastFatigueEquivalentStress) stress, resultSet, i + 1);
								}
								else if (stress instanceof FastPreffasEquivalentStress) {
									writeDataForFastPreffasEquivalentStress(sheet, (FastPreffasEquivalentStress) stress, resultSet, i + 1);
								}
								else if (stress instanceof FastLinearEquivalentStress) {
									writeDataForFastLinearEquivalentStress(sheet, (FastLinearEquivalentStress) stress, resultSet, i + 1);
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
		return output_.toPath();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get output file
			Path file = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(file, automaticTasks_, taskPanel_);
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
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param stress
	 *            Equivalent stress.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForFastFatigueEquivalentStress(WritableSheet sheet, FastFatigueEquivalentStress stress, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = stress.getParentItem().getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = stress.getParentItem().getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = stress.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME].get()) {
			String value = stress.getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// pilot point name
		if (options_[PP_NAME].get()) {
			String value = stress.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = stress.getParentItem().getEID();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[MAT_DATA].get()) {

			// p
			double p = resultSet.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, p, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// q
			double q = resultSet.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, q, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// m
			double m = resultSet.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[VALIDITY].get()) {
			int value = (int) resultSet.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// fatigue equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param stress
	 *            Equivalent stress.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForFastPreffasEquivalentStress(WritableSheet sheet, FastPreffasEquivalentStress stress, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = stress.getParentItem().getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = stress.getParentItem().getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = stress.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME].get()) {
			String value = stress.getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// pilot point name
		if (options_[PP_NAME].get()) {
			String value = stress.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = stress.getParentItem().getEID();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[MAT_DATA].get()) {

			// Ceff
			double ceff = resultSet.getDouble("material_ceff");
			sheet.addCell(new jxl.write.Number(column, row, ceff, getDataFormat(row, CellType.NUMBER, true)));
			column++;

			// m
			double m = resultSet.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// A
			double a = resultSet.getDouble("material_a");
			sheet.addCell(new jxl.write.Number(column, row, a, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// B
			double b = resultSet.getDouble("material_b");
			sheet.addCell(new jxl.write.Number(column, row, b, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// C
			double c = resultSet.getDouble("material_c");
			sheet.addCell(new jxl.write.Number(column, row, c, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Ftu
			double ftu = resultSet.getDouble("material_ftu");
			sheet.addCell(new jxl.write.Number(column, row, ftu, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Fty
			double fty = resultSet.getDouble("material_fty");
			sheet.addCell(new jxl.write.Number(column, row, fty, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[VALIDITY].get()) {
			int value = (int) resultSet.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param stress
	 *            Equivalent stress.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForFastLinearEquivalentStress(WritableSheet sheet, FastLinearEquivalentStress stress, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = stress.getParentItem().getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = stress.getParentItem().getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = stress.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME].get()) {
			String value = stress.getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// pilot point name
		if (options_[PP_NAME].get()) {
			String value = stress.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = stress.getParentItem().getEID();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[MAT_DATA].get()) {

			// Ceff
			double ceff = resultSet.getDouble("material_ceff");
			sheet.addCell(new jxl.write.Number(column, row, ceff, getDataFormat(row, CellType.NUMBER, true)));
			column++;

			// m
			double m = resultSet.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// A
			double a = resultSet.getDouble("material_a");
			sheet.addCell(new jxl.write.Number(column, row, a, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// B
			double b = resultSet.getDouble("material_b");
			sheet.addCell(new jxl.write.Number(column, row, b, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// C
			double c = resultSet.getDouble("material_c");
			sheet.addCell(new jxl.write.Number(column, row, c, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Ftu
			double ftu = resultSet.getDouble("material_ftu");
			sheet.addCell(new jxl.write.Number(column, row, ftu, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Fty
			double fty = resultSet.getDouble("material_fty");
			sheet.addCell(new jxl.write.Number(column, row, fty, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[VALIDITY].get()) {
			int value = (int) resultSet.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param stress
	 *            Equivalent stress.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForFatigueEquivalentStress(WritableSheet sheet, FatigueEquivalentStress stress, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = stress.getParentItem().getParentItem().getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = stress.getParentItem().getParentItem().getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = stress.getParentItem().getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME].get()) {
			String value = stress.getParentItem().getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// pilot point name
		if (options_[PP_NAME].get()) {
			String value = stress.getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = stress.getParentItem().getParentItem().getEID();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// sequence name
		if (options_[SEQ_NAME].get()) {
			String value = stress.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[MAT_DATA].get()) {

			// p
			double p = resultSet.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, p, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// q
			double q = resultSet.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, q, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// m
			double m = resultSet.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[VALIDITY].get()) {
			int value = (int) resultSet.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// max stress
		if (options_[MAX_STRESS].get()) {
			double value = resultSet.getDouble("max_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// min stress
		if (options_[MIN_STRESS].get()) {
			double value = resultSet.getDouble("min_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// r ratio
		if (options_[R_RATIO].get()) {
			double value = resultSet.getDouble("r_ratio");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// fatigue equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param stress
	 *            Equivalent stress.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForPreffasEquivalentStress(WritableSheet sheet, PreffasEquivalentStress stress, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = stress.getParentItem().getParentItem().getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = stress.getParentItem().getParentItem().getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = stress.getParentItem().getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME].get()) {
			String value = stress.getParentItem().getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// pilot point name
		if (options_[PP_NAME].get()) {
			String value = stress.getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = stress.getParentItem().getParentItem().getEID();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// sequence name
		if (options_[SEQ_NAME].get()) {
			String value = stress.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[MAT_DATA].get()) {

			// Ceff
			double ceff = resultSet.getDouble("material_ceff");
			sheet.addCell(new jxl.write.Number(column, row, ceff, getDataFormat(row, CellType.NUMBER, true)));
			column++;

			// m
			double m = resultSet.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// A
			double a = resultSet.getDouble("material_a");
			sheet.addCell(new jxl.write.Number(column, row, a, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// B
			double b = resultSet.getDouble("material_b");
			sheet.addCell(new jxl.write.Number(column, row, b, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// C
			double c = resultSet.getDouble("material_c");
			sheet.addCell(new jxl.write.Number(column, row, c, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Ftu
			double ftu = resultSet.getDouble("material_ftu");
			sheet.addCell(new jxl.write.Number(column, row, ftu, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Fty
			double fty = resultSet.getDouble("material_fty");
			sheet.addCell(new jxl.write.Number(column, row, fty, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[VALIDITY].get()) {
			int value = (int) resultSet.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// max stress
		if (options_[MAX_STRESS].get()) {
			double value = resultSet.getDouble("max_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// min stress
		if (options_[MIN_STRESS].get()) {
			double value = resultSet.getDouble("min_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// r ratio
		if (options_[R_RATIO].get()) {
			double value = resultSet.getDouble("r_ratio");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param stress
	 *            Equivalent stress.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForLinearEquivalentStress(WritableSheet sheet, LinearEquivalentStress stress, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = stress.getParentItem().getParentItem().getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = stress.getParentItem().getParentItem().getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = stress.getParentItem().getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME].get()) {
			String value = stress.getParentItem().getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// pilot point name
		if (options_[PP_NAME].get()) {
			String value = stress.getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = stress.getParentItem().getParentItem().getEID();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// sequence name
		if (options_[SEQ_NAME].get()) {
			String value = stress.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[MAT_DATA].get()) {

			// Ceff
			double ceff = resultSet.getDouble("material_ceff");
			sheet.addCell(new jxl.write.Number(column, row, ceff, getDataFormat(row, CellType.NUMBER, true)));
			column++;

			// m
			double m = resultSet.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// A
			double a = resultSet.getDouble("material_a");
			sheet.addCell(new jxl.write.Number(column, row, a, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// B
			double b = resultSet.getDouble("material_b");
			sheet.addCell(new jxl.write.Number(column, row, b, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// C
			double c = resultSet.getDouble("material_c");
			sheet.addCell(new jxl.write.Number(column, row, c, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Ftu
			double ftu = resultSet.getDouble("material_ftu");
			sheet.addCell(new jxl.write.Number(column, row, ftu, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Fty
			double fty = resultSet.getDouble("material_fty");
			sheet.addCell(new jxl.write.Number(column, row, fty, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[VALIDITY].get()) {
			int value = (int) resultSet.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// max stress
		if (options_[MAX_STRESS].get()) {
			double value = resultSet.getDouble("max_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// min stress
		if (options_[MIN_STRESS].get()) {
			double value = resultSet.getDouble("min_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// r ratio
		if (options_[R_RATIO].get()) {
			double value = resultSet.getDouble("r_ratio");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param stress
	 *            Equivalent stress.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForExternalFatigueEquivalentStress(WritableSheet sheet, ExternalFatigueEquivalentStress stress, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = stress.getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = stress.getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = stress.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = ExternalStressSequence.getEID(stress.getParentItem().getName());
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// sequence name
		if (options_[SEQ_NAME].get()) {
			String value = stress.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[MAT_DATA].get()) {

			// p
			double p = resultSet.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, p, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// q
			double q = resultSet.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, q, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// m
			double m = resultSet.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[VALIDITY].get()) {
			int value = (int) resultSet.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// max stress
		if (options_[MAX_STRESS].get()) {
			double value = resultSet.getDouble("max_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// min stress
		if (options_[MIN_STRESS].get()) {
			double value = resultSet.getDouble("min_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// r ratio
		if (options_[R_RATIO].get()) {
			double value = resultSet.getDouble("r_ratio");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// fatigue equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param stress
	 *            Equivalent stress.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForExternalPreffasEquivalentStress(WritableSheet sheet, ExternalPreffasEquivalentStress stress, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = stress.getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = stress.getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = stress.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = ExternalStressSequence.getEID(stress.getParentItem().getName());
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// sequence name
		if (options_[SEQ_NAME].get()) {
			String value = stress.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[MAT_DATA].get()) {

			// Ceff
			double ceff = resultSet.getDouble("material_ceff");
			sheet.addCell(new jxl.write.Number(column, row, ceff, getDataFormat(row, CellType.NUMBER, true)));
			column++;

			// m
			double m = resultSet.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// A
			double a = resultSet.getDouble("material_a");
			sheet.addCell(new jxl.write.Number(column, row, a, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// B
			double b = resultSet.getDouble("material_b");
			sheet.addCell(new jxl.write.Number(column, row, b, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// C
			double c = resultSet.getDouble("material_c");
			sheet.addCell(new jxl.write.Number(column, row, c, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Ftu
			double ftu = resultSet.getDouble("material_ftu");
			sheet.addCell(new jxl.write.Number(column, row, ftu, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Fty
			double fty = resultSet.getDouble("material_fty");
			sheet.addCell(new jxl.write.Number(column, row, fty, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[VALIDITY].get()) {
			int value = (int) resultSet.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// max stress
		if (options_[MAX_STRESS].get()) {
			double value = resultSet.getDouble("max_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// min stress
		if (options_[MIN_STRESS].get()) {
			double value = resultSet.getDouble("min_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// r ratio
		if (options_[R_RATIO].get()) {
			double value = resultSet.getDouble("r_ratio");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param stress
	 *            Equivalent stress.
	 * @param resultSet
	 *            Result set.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataForExternalLinearEquivalentStress(WritableSheet sheet, ExternalLinearEquivalentStress stress, ResultSet resultSet, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM].get()) {
			String value = stress.getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = stress.getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = stress.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = ExternalStressSequence.getEID(stress.getParentItem().getName());
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// sequence name
		if (options_[SEQ_NAME].get()) {
			String value = stress.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = resultSet.getString("material_name");
			materialName += "/" + resultSet.getString("material_specification");
			materialName += "/" + resultSet.getString("material_orientation");
			materialName += "/" + resultSet.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL, false)));
			column++;
		}

		// material data
		if (options_[MAT_DATA].get()) {

			// Ceff
			double ceff = resultSet.getDouble("material_ceff");
			sheet.addCell(new jxl.write.Number(column, row, ceff, getDataFormat(row, CellType.NUMBER, true)));
			column++;

			// m
			double m = resultSet.getDouble("material_m");
			sheet.addCell(new jxl.write.Number(column, row, m, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// A
			double a = resultSet.getDouble("material_a");
			sheet.addCell(new jxl.write.Number(column, row, a, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// B
			double b = resultSet.getDouble("material_b");
			sheet.addCell(new jxl.write.Number(column, row, b, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// C
			double c = resultSet.getDouble("material_c");
			sheet.addCell(new jxl.write.Number(column, row, c, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Ftu
			double ftu = resultSet.getDouble("material_ftu");
			sheet.addCell(new jxl.write.Number(column, row, ftu, getDataFormat(row, CellType.NUMBER, false)));
			column++;

			// Fty
			double fty = resultSet.getDouble("material_fty");
			sheet.addCell(new jxl.write.Number(column, row, fty, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// validity
		if (options_[VALIDITY].get()) {
			int value = (int) resultSet.getDouble("validity");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// max stress
		if (options_[MAX_STRESS].get()) {
			double value = resultSet.getDouble("max_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// min stress
		if (options_[MIN_STRESS].get()) {
			double value = resultSet.getDouble("min_stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// r ratio
		if (options_[R_RATIO].get()) {
			double value = resultSet.getDouble("r_ratio");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
			double value = resultSet.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL, false)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			}
			column++;
		}

		// equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			double value = resultSet.getDouble("stress");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER, false)));
			column++;
		}
	}

	/**
	 * Writes column headers according selected options.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param item
	 *            Spectrum item.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeHeaders(WritableSheet sheet, SpectrumItem item) throws Exception {

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

		// sequence name
		if (options_[SEQ_NAME].get()) {
			String header = "Stress sequence name";
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

		// material data
		if (options_[MAT_DATA].get()) {

			// fatigue
			if (item instanceof FatigueEquivalentStress || item instanceof ExternalFatigueEquivalentStress || item instanceof FastFatigueEquivalentStress) {

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
			else if (item instanceof PreffasEquivalentStress || item instanceof LinearEquivalentStress || item instanceof ExternalPreffasEquivalentStress || item instanceof ExternalLinearEquivalentStress || item instanceof FastPreffasEquivalentStress || item instanceof FastLinearEquivalentStress) {

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
		if (options_[VALIDITY].get()) {
			String header = "Spectrum validity";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// max stress
		if (options_[MAX_STRESS].get()) {
			String header = "Maximum stress";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// min stress
		if (options_[MIN_STRESS].get()) {
			String header = "Minimum stress";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// r ratio
		if (options_[R_RATIO].get()) {
			String header = "R-ratio";
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

		// equivalent stress
		if (options_[EQUIVALENT_STRESS].get()) {
			String stressName = getStressName(stresses_.get(0));
			sheet.addCell(new jxl.write.Label(column, 0, stressName, format));
			sheet.setColumnView(column, stressName.length());
			column++;
		}
	}

	/**
	 * Returns the name of equivalent stress.
	 *
	 * @param stress
	 *            Equivalent stress.
	 * @return The name of equivalent stress.
	 */
	private static String getStressName(SpectrumItem stress) {
		if (stress instanceof FatigueEquivalentStress || stress instanceof ExternalFatigueEquivalentStress || stress instanceof FastFatigueEquivalentStress)
			return "Fatigue equivalent stress";
		else if (stress instanceof PreffasEquivalentStress || stress instanceof ExternalPreffasEquivalentStress || stress instanceof FastPreffasEquivalentStress)
			return "Preffas propagation equivalent stress";
		else if (stress instanceof LinearEquivalentStress || stress instanceof ExternalLinearEquivalentStress || stress instanceof FastLinearEquivalentStress)
			return "Linear propagation equivalent stress";
		return null;
	}

	/**
	 * Returns the SQL query.
	 *
	 * @param item
	 *            Spectrum item.
	 * @return SQL query.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getQuery(SpectrumItem item) throws Exception {
		String sql = null;
		if (item instanceof FatigueEquivalentStress) {
			sql = "select * from fatigue_equivalent_stresses where id = ?";
		}
		else if (item instanceof PreffasEquivalentStress) {
			sql = "select * from preffas_equivalent_stresses where id = ?";
		}
		else if (item instanceof LinearEquivalentStress) {
			sql = "select * from linear_equivalent_stresses where id = ?";
		}
		else if (item instanceof ExternalFatigueEquivalentStress) {
			sql = "select * from ext_fatigue_equivalent_stresses where id = ?";
		}
		else if (item instanceof ExternalPreffasEquivalentStress) {
			sql = "select * from ext_preffas_equivalent_stresses where id = ?";
		}
		else if (item instanceof ExternalLinearEquivalentStress) {
			sql = "select * from ext_linear_equivalent_stresses where id = ?";
		}
		else if (item instanceof FastFatigueEquivalentStress) {
			sql = "select * from fast_fatigue_equivalent_stresses where id = ?";
		}
		else if (item instanceof FastPreffasEquivalentStress) {
			sql = "select * from fast_preffas_equivalent_stresses where id = ?";
		}
		else if (item instanceof FastLinearEquivalentStress) {
			sql = "select * from fast_linear_equivalent_stresses where id = ?";
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
		cellFormat.setBackground(rowIndex % 2 == 0 ? Colour.WHITE : Colour.VERY_LIGHT_YELLOW);
		return cellFormat;
	}
}
