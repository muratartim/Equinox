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
import equinox.data.Pair;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.SpectrumItem;
import equinox.dataServer.remote.data.ContributionType;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.task.serializableTask.SerializableSaveDamageContributions;
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
 * Class for save damage contributions task.
 *
 * @author Murat Artim
 * @date Sep 3, 2015
 * @time 2:21:37 PM
 */
public class SaveDamageContributions extends InternalEquinoxTask<Path> implements LongRunningTask, SavableTask, SingleInputTask<Pair<List<SpectrumItem>, List<String>>>, AutomaticTaskOwner<Path> {

	/** Option index. */
	public static final int PERCENT = 0, FULL = 1, INC = 2, ONEG = 3, GAG = 4, DP = 5, DT = 6, MAT_NAME = 7, FAT_P = 8, FAT_Q = 9, PP_NAME = 10, EID = 11, SPEC_NAME = 12, PROGRAM = 13, SECTION = 14, MISSION = 15, OMISSION = 16;

	/** Damage contributions. */
	private List<SpectrumItem> contributions_;

	/** Damage contribution names. */
	private List<String> contributionNames_;

	/** Options. */
	private final boolean[] options_;

	/** Output file. */
	private final File output_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save damage contributions task.
	 *
	 * @param contributions
	 *            Damage contributions. Can be null for automatic execution.
	 * @param contributionNames
	 *            Damage contribution names. Can be null for automatic execution.
	 * @param options
	 *            Options.
	 * @param output
	 *            Output file.
	 */
	public SaveDamageContributions(List<SpectrumItem> contributions, List<String> contributionNames, boolean[] options, File output) {
		contributions_ = contributions;
		contributionNames_ = contributionNames;
		options_ = options;
		output_ = output;
	}

	@Override
	public void setAutomaticInput(Pair<List<SpectrumItem>, List<String>> input) {
		contributions_ = input.getElement1();
		contributionNames_ = input.getElement2();
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
		return "Save damage contributions";
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableSaveDamageContributions(contributions_, contributionNames_, options_, output_);
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

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Damage Contributions", 0);

			// write headers
			writeHeaders(sheet);

			// get database connection
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {

				// prepare statement to get overall info
				String sql = "select stress, omission_level, material_name, material_specification, ";
				sql += "material_orientation, material_configuration, material_p, material_q ";
				sql += "from dam_contributions where contributions_id = ?";
				try (PreparedStatement statement1 = connection.prepareStatement(sql)) {

					// prepare statement to get contribution info
					sql = "select stress from dam_contribution where contributions_id = ? and name = ?";
					try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

						// loop over contributions
						for (int i = 0; i < contributions_.size(); i++) {

							// get contribution
							LoadcaseDamageContributions contribution = (LoadcaseDamageContributions) contributions_.get(i);

							// update info
							updateMessage("Writing damage contribution " + contribution.getName() + "...");
							updateProgress(i, contributions_.size());

							// set parameters
							statement1.setInt(1, contribution.getID());
							statement2.setInt(1, contribution.getID());
							try (ResultSet getOverallInfo = statement1.executeQuery()) {
								while (getOverallInfo.next()) {
									writeData(sheet, contribution, getOverallInfo, statement2, i + 1);
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
	 * Writes data row.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @param contribution
	 *            Contribution to write.
	 * @param getOverallInfo
	 *            Result set to get overall info
	 * @param statement2
	 *            Statement to get contribution info
	 * @param row
	 *            Row index.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeData(WritableSheet sheet, LoadcaseDamageContributions contribution, ResultSet getOverallInfo, PreparedStatement statement2, int row) throws Exception {

		// initialize column index
		int column = 0;

		// program
		if (options_[PROGRAM]) {
			String value = contribution.getParentItem().getParentItem().getProgram();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// section
		if (options_[SECTION]) {
			String value = contribution.getParentItem().getParentItem().getSection();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// mission
		if (options_[MISSION]) {
			String value = contribution.getParentItem().getMission();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME]) {
			String value = contribution.getParentItem().getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// pilot point name
		if (options_[PP_NAME]) {
			String value = contribution.getParentItem().getName();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// element ID
		if (options_[EID]) {
			String value = contribution.getParentItem().getEID();
			sheet.addCell(new jxl.write.Label(column, row, value, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// material name
		if (options_[MAT_NAME]) {
			String materialName = getOverallInfo.getString("material_name");
			materialName += "/" + getOverallInfo.getString("material_specification");
			materialName += "/" + getOverallInfo.getString("material_orientation");
			materialName += "/" + getOverallInfo.getString("material_configuration");
			sheet.addCell(new jxl.write.Label(column, row, materialName, getDataFormat(row, CellType.LABEL)));
			column++;
		}

		// fatigue slope p
		if (options_[FAT_P]) {
			double value = getOverallInfo.getDouble("material_p");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// fatigue constant q
		if (options_[FAT_Q]) {
			double value = getOverallInfo.getDouble("material_q");
			sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// omission level
		if (options_[OMISSION]) {
			double value = getOverallInfo.getDouble("omission_level");
			if (value == -1) {
				sheet.addCell(new jxl.write.Label(column, row, "N/A", getDataFormat(row, CellType.LABEL)));
			}
			else {
				sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
			}
			column++;
		}

		// full damage
		double totalStress = getOverallInfo.getDouble("stress");
		if (options_[FULL]) {
			sheet.addCell(new jxl.write.Number(column, row, totalStress, getDataFormat(row, CellType.NUMBER)));
			column++;
		}

		// 1G
		if (options_[ONEG]) {
			column = writeStress(statement2, totalStress, sheet, row, column, ContributionType.ONEG.getName(), true);
		}

		// GAG
		if (options_[GAG]) {
			column = writeStress(statement2, totalStress, sheet, row, column, ContributionType.GAG.getName(), false);
		}

		// DP
		if (options_[DP]) {
			column = writeStress(statement2, totalStress, sheet, row, column, ContributionType.DELTA_P.getName(), true);
		}

		// DT
		if (options_[DT]) {
			column = writeStress(statement2, totalStress, sheet, row, column, ContributionType.DELTA_T.getName(), true);
		}

		// increment
		if (options_[INC]) {

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
				statement2.setString(2, contributionName);
				try (ResultSet getContInfo = statement2.executeQuery()) {
					double value = 0.0;
					while (getContInfo.next()) {
						value = totalStress - getContInfo.getDouble("stress");
						if (options_[PERCENT]) {
							value = value * 100 / totalStress;
						}
					}
					sheet.addCell(new jxl.write.Number(column, row, value, getDataFormat(row, CellType.NUMBER)));
					column++;
				}
			}
		}
	}

	/**
	 * Writes out damage contribution equivalent stress.
	 *
	 * @param statement2
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
	private int writeStress(PreparedStatement statement2, double totalStress, WritableSheet sheet, int row, int column, String name, boolean isComplement) throws Exception {

		// initialize stress value
		double value = 0.0;

		// loop over contribution names
		for (String contributionName : contributionNames_) {

			// found contribution
			if (contributionName.equals(name)) {

				// execute statement to get stress
				statement2.setString(2, contributionName);
				try (ResultSet getContInfo = statement2.executeQuery()) {
					if (getContInfo.next()) {
						value = isComplement ? totalStress - getContInfo.getDouble("stress") : getContInfo.getDouble("stress");
						if (options_[PERCENT]) {
							value = value * 100 / totalStress;
						}
					}
				}
				break;
			}
		}

		// increment column number
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
		if (options_[PROGRAM]) {
			String header = "A/C program";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// section
		if (options_[SECTION]) {
			String header = "A/C section";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// mission
		if (options_[MISSION]) {
			String header = "Fatigue mission";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// spectrum name
		if (options_[SPEC_NAME]) {
			String header = "Spectrum name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// pilot point name
		if (options_[PP_NAME]) {
			String header = "Pilot point name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// element ID
		if (options_[EID]) {
			String header = "Element ID";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// material name
		if (options_[MAT_NAME]) {
			String header = "Material name";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue slope p
		if (options_[FAT_P]) {
			String header = "Fatigue material slope (p)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// fatigue constant q
		if (options_[FAT_Q]) {
			String header = "Fatigue material constant (q)";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// omission level
		if (options_[OMISSION]) {
			String header = "Omission level";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// full damage
		if (options_[FULL]) {
			String header = "Total equivalent stress";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// 1g
		if (options_[ONEG]) {
			String header = "1G contribution";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// gag
		if (options_[GAG]) {
			String header = "GAG contribution";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// DP
		if (options_[DP]) {
			String header = "Delta-P contribution";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// DT
		if (options_[DT]) {
			String header = "Delta-T contribution";
			sheet.addCell(new jxl.write.Label(column, 0, header, format));
			sheet.setColumnView(column, header.length());
			column++;
		}

		// increment
		if (options_[INC]) {

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
