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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.Triple;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.fileType.SpectrumItem;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.task.serializableTask.SerializableSaveFlightDamageContributions;
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
 * Class for save typical flight damage contributions task.
 *
 * @author Murat Artim
 * @date 21 Oct 2016
 * @time 17:03:47
 */
public class SaveFlightDamageContributions extends InternalEquinoxTask<Path> implements LongRunningTask, SavableTask, SingleInputTask<Triple<List<SpectrumItem>, List<String>, List<String>>>, AutomaticTaskOwner<Path> {

	/** Option index. */
	public static final int MAT_NAME = 0, FAT_P = 1, FAT_Q = 2, PP_NAME = 3, EID = 4, SPEC_NAME = 5, PROGRAM = 6, SECTION = 7, MISSION = 8, OMISSION = 9;

	/** Damage contributions. */
	private List<SpectrumItem> contributions_;

	/** Contribution names. */
	private List<String> tfNamesWithOccurrences_, tfNamesWithoutOccurrences_;

	/** Options. */
	private final BooleanProperty[] options_;

	/** Output file. */
	private final File output_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save typical flight damage contributions task.
	 *
	 * @param contributions
	 *            Damage contributions. Can be null for automatic execution.
	 * @param tfNamesWithOccurrences
	 *            Typical flight names with flight occurrences. Can be null for automatic execution.
	 * @param tfNamesWithoutOccurrences
	 *            Typical flight names without flight occurrences. Can be null for automatic execution.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SaveFlightDamageContributions(List<SpectrumItem> contributions, List<String> tfNamesWithOccurrences, List<String> tfNamesWithoutOccurrences, BooleanProperty[] options, File output) {
		contributions_ = contributions;
		tfNamesWithOccurrences_ = tfNamesWithOccurrences;
		tfNamesWithoutOccurrences_ = tfNamesWithoutOccurrences;
		options_ = options;
		output_ = output;
	}

	@Override
	public void setAutomaticInput(Triple<List<SpectrumItem>, List<String>, List<String>> input) {
		contributions_ = input.getElement1();
		tfNamesWithOccurrences_ = input.getElement2();
		tfNamesWithoutOccurrences_ = input.getElement3();
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
		return "Save flight damage contributions";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableSaveFlightDamageContributions(contributions_, tfNamesWithOccurrences_, tfNamesWithoutOccurrences_, options_, output_);
	}

	@Override
	protected Path call() throws Exception {

		// update progress info
		updateTitle("Saving damage contributions to '" + output_.getName() + "'");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(output_);

			// create worksheets
			WritableSheet sheetWithOccurrences = workbook.createSheet("With Flight Occurrences", 0);
			WritableSheet sheetWithoutOccurrences = workbook.createSheet("Without Flight Occurrences", 1);

			// write headers
			writeHeaders(sheetWithOccurrences, tfNamesWithOccurrences_);
			writeHeaders(sheetWithoutOccurrences, tfNamesWithoutOccurrences_);

			// get database connection
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {

				// prepare statement to get overall info
				String sql = "select omission_level, material_name, material_specification, material_orientation, material_configuration, ";
				sql += "material_p, material_q, material_m ";
				sql += "from flight_dam_contributions where id = ?";
				try (PreparedStatement getOverallInfo = connection.prepareStatement(sql)) {

					// prepare statement to get contribution info with typical flight occurrences
					sql = "select dam_percent from flight_dam_contribution_with_occurrences where id = ? and flight_name = ?";
					try (PreparedStatement getInfoWithOccurrences = connection.prepareStatement(sql)) {

						// prepare statement to get contribution info without typical flight occurrences
						sql = "select dam_percent from flight_dam_contribution_without_occurrences where id = ? and flight_name = ?";
						try (PreparedStatement getInfoWithoutOccurrences = connection.prepareStatement(sql)) {

							// loop over contributions
							for (int i = 0; i < contributions_.size(); i++) {

								// get contribution
								SpectrumItem contribution = contributions_.get(i);

								// update info
								updateMessage("Writing flight damage contribution " + contribution.getName() + "...");
								updateProgress(i, contributions_.size());

								// set parameters
								getOverallInfo.setInt(1, contribution.getID());
								getInfoWithOccurrences.setInt(1, contribution.getID());
								getInfoWithoutOccurrences.setInt(1, contribution.getID());

								// get overall info
								try (ResultSet overallInfo = getOverallInfo.executeQuery()) {
									while (overallInfo.next()) {
										writeData(sheetWithOccurrences, tfNamesWithOccurrences_, contribution, overallInfo, getInfoWithOccurrences, i + 1);
										writeData(sheetWithoutOccurrences, tfNamesWithoutOccurrences_, contribution, overallInfo, getInfoWithoutOccurrences, i + 1);
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
	 * Writes data rows.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param tfNames
	 *            Typical flight names.
	 * @param item
	 *            Typical flight damage contribution.
	 * @param overallInfo
	 *            Overall contribution info.
	 * @param getInfo
	 *            Statement to get contribution values.
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeData(WritableSheet sheet, List<String> tfNames, SpectrumItem item, ResultSet overallInfo, PreparedStatement getInfo, int row) throws Exception {

		// initialize column index
		int column = 0;

		// cast to flight damage contribution
		FlightDamageContributions contribution = (FlightDamageContributions) item;

		// program
		if (options_[PROGRAM].get()) {
			String value = contribution.getParentItem().getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// section
		if (options_[SECTION].get()) {
			String value = contribution.getParentItem().getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// mission
		if (options_[MISSION].get()) {
			String value = contribution.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME].get()) {
			String value = contribution.getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// pilot point name
		if (options_[PP_NAME].get()) {
			String value = contribution.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// element ID
		if (options_[EID].get()) {
			String value = contribution.getParentItem().getEID();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// material name
		if (options_[MAT_NAME].get()) {
			String materialName = overallInfo.getString("material_name");
			materialName += "/" + overallInfo.getString("material_specification");
			materialName += "/" + overallInfo.getString("material_orientation");
			materialName += "/" + overallInfo.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// fatigue slope p
		if (options_[FAT_P].get()) {
			double value = overallInfo.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// fatigue constant q
		if (options_[FAT_Q].get()) {
			double value = overallInfo.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// omission level
		if (options_[OMISSION].get()) {
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
