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

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.Utility;
import jxl.Sheet;
import jxl.Workbook;

/**
 * Class for get conversion table info task.
 *
 * @author Murat Artim
 * @date Nov 4, 2015
 * @time 5:50:42 PM
 */
public class GetConvTableInfo extends TemporaryFileCreatingTask<String[]> implements ShortRunningTask {

	/** Requesting panel. */
	private final ConversionTableInfoRequestingPanel panel_;

	/** Input conversion table file. */
	private final Path conversionTableFile_;

	/** Worksheet name. */
	private final String sheet_;

	/**
	 * Creates get conversion table info task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param conversionTableFile
	 *            Conversion table file.
	 * @param sheet
	 *            Worksheet name.
	 */
	public GetConvTableInfo(ConversionTableInfoRequestingPanel panel, Path conversionTableFile, String sheet) {
		panel_ = panel;
		conversionTableFile_ = conversionTableFile;
		sheet_ = sheet;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get conversion table info";
	}

	@Override
	protected String[] call() throws Exception {

		// update info
		updateTitle("Getting conversion table info");

		// initialize input file
		Path inputFile = conversionTableFile_;

		// ZIP
		if (FileType.getFileType(conversionTableFile_.toFile()).equals(FileType.ZIP)) {
			updateMessage("Extracting zipped conversion table...");
			inputFile = Utility.extractFileFromZIP(conversionTableFile_, this, FileType.XLS, null);
		}

		// initialize workbook
		Workbook workbook = null;

		try {

			// update info
			updateMessage("Reading conversion table...");

			// get workbook
			workbook = Workbook.getWorkbook(inputFile.toFile());

			// get sheet
			Sheet sheet = sheet_ == null ? workbook.getSheet(0) : workbook.getSheet(sheet_);

			// null sheet
			if (sheet == null)
				throw new Exception("Cannot find worksheet '" + sheet_ + "' in conversion table excel file '" + inputFile.getFileName() + "'.");

			// get info
			String[] info = new String[7];
			info[ConversionTableInfoRequestingPanel.PROGRAM] = sheet.getCell(1, 1).getContents();
			info[ConversionTableInfoRequestingPanel.SECTION] = sheet.getCell(6, 3).getContents();
			info[ConversionTableInfoRequestingPanel.MISSION] = sheet.getCell(6, 4).getContents();
			info[ConversionTableInfoRequestingPanel.MISSION_ISSUE] = sheet.getCell(6, 2).getContents();
			info[ConversionTableInfoRequestingPanel.FLP_ISSUE] = sheet.getCell(1, 2).getContents();
			info[ConversionTableInfoRequestingPanel.IFLP_ISSUE] = sheet.getCell(1, 3).getContents();
			info[ConversionTableInfoRequestingPanel.CDF_ISSUE] = sheet.getCell(1, 4).getContents();

			// return info
			return info;
		}

		// close workbook
		finally {
			if (workbook != null)
				workbook.close();
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set info
		try {
			panel_.setConversionTableInfo(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for conversion table info requesting panel.
	 *
	 * @author Murat Artim
	 * @date Mar 17, 2015
	 * @time 11:19:53 AM
	 */
	public interface ConversionTableInfoRequestingPanel {

		/** Conversion table info index. */
		int PROGRAM = 0, SECTION = 1, MISSION = 2, MISSION_ISSUE = 3, FLP_ISSUE = 4, IFLP_ISSUE = 5, CDF_ISSUE = 6;

		/**
		 * Sets conversion table info to this panel.
		 *
		 * @param info
		 *            Conversion table info.
		 */
		void setConversionTableInfo(String[] info);
	}
}
