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
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.fileType.StressSequence;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.PostProcessingTask;
import equinox.task.automation.SingleInputTask;
import jxl.CellType;
import jxl.Workbook;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.VerticalAlignment;
import jxl.write.NumberFormats;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * Class for save mission profile to MS Excel file task.
 *
 * @author Murat Artim
 * @date May 21, 2015
 * @time 2:25:01 PM
 */
public class SaveMissionProfile extends InternalEquinoxTask<Path> implements LongRunningTask, SingleInputTask<StressSequence>, PostProcessingTask, AutomaticTaskOwner<Path> {

	/** Input stress sequence. */
	private StressSequence sequence_;

	/** Output file. */
	private final File output_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save mission profile task.
	 *
	 * @param sequence
	 *            Input stress sequence. Can be null for automatic execution.
	 * @param output
	 *            Output file.
	 */
	public SaveMissionProfile(StressSequence sequence, File output) {
		sequence_ = sequence;
		output_ = output;
	}

	@Override
	public void setAutomaticInput(StressSequence input) {
		sequence_ = input;
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
		return "Save mission profile";
	}

	@Override
	protected Path call() throws Exception {

		// update progress info
		updateTitle("Saving mission profile to '" + output_.getName() + "'");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(output_);

			// create header format
			WritableCellFormat headerFormat = getHeaderFormat();

			// create worksheet
			WritableSheet sheet = createWorksheet(workbook, headerFormat);

			// get database connection
			try (Connection connection = Equinox.DBC_POOL.getConnection()) {

				// create statement
				try (Statement statement = connection.createStatement()) {

					// write mission profile
					writeMissionProfile(sheet, connection, statement);
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
	 * Writes out stress sheet.
	 *
	 * @param sheet
	 *            Sheet.
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeMissionProfile(WritableSheet sheet, Connection connection, Statement statement) throws Exception {

		// update progress info
		updateMessage("Writing segment stress info...");

		// prepare statement to get steady stresses
		String sql = "select oneg_stress, dp_stress, dt_stress from ";
		sql += "segment_steady_stresses_" + sequence_.getID();
		sql += " where segment_id = ?";
		try (PreparedStatement getSteady = connection.prepareStatement(sql)) {

			// prepare statement to get positive increment stresses
			sql = "select stress, factor_num from ";
			sql += "segment_increment_stresses_" + sequence_.getID();
			sql += " where stress >= 0 and segment_id = ? order by factor_num";
			try (PreparedStatement getPosInc = connection.prepareStatement(sql)) {

				// prepare statement to get negative increment stresses
				sql = "select stress, factor_num from ";
				sql += "segment_increment_stresses_" + sequence_.getID();
				sql += " where stress < 0 and segment_id = ? order by factor_num";
				try (PreparedStatement getNegInc = connection.prepareStatement(sql)) {

					// create and execute statement to get segments
					sql = "select segment_id, segment_name, segment_num from ";
					sql += "segments_" + sequence_.getID();
					sql += " order by segment_num asc";
					try (ResultSet getSegment = statement.executeQuery(sql)) {

						// loop over segments
						int row = 1;
						while (getSegment.next()) {

							// task cancelled
							if (isCancelled())
								return;

							// get segment ID
							String segmentName = getSegment.getString("segment_name");
							int segmentID = getSegment.getInt("segment_id");

							// write segment name
							sheet.addCell(new jxl.write.Label(0, row, segmentName, getDataFormat(row, CellType.LABEL)));

							// write steady stresses
							getSteady.setInt(1, segmentID);
							try (ResultSet steadyStresses = getSteady.executeQuery()) {
								while (steadyStresses.next()) {
									sheet.addCell(new jxl.write.Number(1, row, steadyStresses.getDouble("oneg_stress"), getDataFormat(row, CellType.NUMBER)));
									sheet.addCell(new jxl.write.Number(2, row, steadyStresses.getDouble("dp_stress"), getDataFormat(row, CellType.NUMBER)));
									sheet.addCell(new jxl.write.Number(3, row, steadyStresses.getDouble("dt_stress"), getDataFormat(row, CellType.NUMBER)));
								}
							}

							// write positive increment stresses
							getPosInc.setInt(1, segmentID);
							try (ResultSet increment = getPosInc.executeQuery()) {
								while (increment.next()) {
									int facNum = increment.getInt("factor_num");
									sheet.addCell(new jxl.write.Number(3 + facNum, row, increment.getDouble("stress"), getDataFormat(row, CellType.NUMBER)));
								}
							}

							// get negative increment stresses
							getNegInc.setInt(1, segmentID);
							try (ResultSet increment = getNegInc.executeQuery()) {
								while (increment.next()) {
									int facNum = increment.getInt("factor_num");
									sheet.addCell(new jxl.write.Number(11 + facNum, row, increment.getDouble("stress"), getDataFormat(row, CellType.NUMBER)));
								}
							}

							// increment row index
							row++;
						}
					}
				}
			}
		}
	}

	/**
	 * Writes column headers according selected options.
	 *
	 * @param workbook
	 *            Excel workbook.
	 *
	 * @param format
	 *            Header format.
	 * @return The newly created worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static WritableSheet createWorksheet(WritableWorkbook workbook, WritableCellFormat format) throws Exception {

		// create worksheet
		WritableSheet sheet = workbook.createSheet("Mission Profile", 0);

		// segment
		String header = "Segment";
		sheet.addCell(new jxl.write.Label(0, 0, header, format));
		sheet.setColumnView(0, header.length());

		// 1g stress
		header = "1G Stress";
		sheet.addCell(new jxl.write.Label(1, 0, header, format));
		sheet.setColumnView(1, header.length());

		// delta-p stress
		header = "Delta-P Stress";
		sheet.addCell(new jxl.write.Label(2, 0, header, format));
		sheet.setColumnView(2, header.length());

		// delta-t stress
		header = "Delta-T Stress";
		sheet.addCell(new jxl.write.Label(3, 0, header, format));
		sheet.setColumnView(3, header.length());

		// positive incremental stresses
		for (int i = 1; i <= 8; i++) {
			header = "Step +" + i;
			sheet.addCell(new jxl.write.Label(3 + i, 0, header, format));
			sheet.setColumnView(3 + i, header.length());
		}

		// negative incremental stresses
		for (int i = 1; i <= 8; i++) {
			header = "Step -" + i;
			sheet.addCell(new jxl.write.Label(11 + i, 0, header, format));
			sheet.setColumnView(11 + i, header.length());
		}

		// return worksheet
		return sheet;
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
		cellFormat.setWrap(true);
		cellFormat.setVerticalAlignment(VerticalAlignment.CENTRE);
		return cellFormat;
	}
}
