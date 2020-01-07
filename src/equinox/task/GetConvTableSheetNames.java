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
import jxl.Workbook;

/**
 * Class for get conversion table sheet names task.
 *
 * @author Murat Artim
 * @date May 4, 2014
 * @time 10:28:02 AM
 */
public class GetConvTableSheetNames extends TemporaryFileCreatingTask<String[]> implements ShortRunningTask {

	/** UI item that stores the sheet names. */
	private final ConversionTableSheetsRequestingPanel panel_;

	/** Input conversion table file. */
	private final Path inputFile_;

	/**
	 * Creates get conversion table sheets task.
	 *
	 * @param panel
	 *            UI panel.
	 * @param inputFile
	 *            Input conversion table file.
	 */
	public GetConvTableSheetNames(ConversionTableSheetsRequestingPanel panel, Path inputFile) {
		panel_ = panel;
		inputFile_ = inputFile;
	}

	@Override
	public String getTaskTitle() {
		return "Get conversion table sheet names";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected String[] call() throws Exception {

		// update info
		updateTitle("Getting conversion table sheet names");

		// initialize input file
		Path inputFile = inputFile_;

		// ZIP
		if (FileType.getFileType(inputFile_.toFile()).equals(FileType.ZIP)) {
			updateMessage("Extracting zipped conversion table...");
			inputFile = Utility.extractFileFromZIP(inputFile_, this, FileType.XLS, null);
		}

		// initialize workbook
		Workbook workbook = null;

		try {

			// update info
			updateMessage("Reading conversion table...");

			// get workbook
			workbook = Workbook.getWorkbook(inputFile.toFile());

			// get sheet names
			String[] sheetNames = workbook.getSheetNames();

			// return sheet names
			return sheetNames;
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
			panel_.setConversionTableSheetNames(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for conversion table worksheets requesting panel.
	 *
	 * @author Murat Artim
	 * @date Mar 17, 2015
	 * @time 11:19:53 AM
	 */
	public interface ConversionTableSheetsRequestingPanel {

		/**
		 * Sets conversion table sheet names to this panel.
		 *
		 * @param worksheets
		 *            Worksheet names.
		 */
		void setConversionTableSheetNames(String[] worksheets);
	}
}
