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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;

import equinox.data.EmbeddedTask;
import equinox.data.Pair;
import equinox.dataServer.remote.data.SpectrumInfo;
import equinox.dataServer.remote.data.SpectrumInfo.SpectrumInfoType;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.Utility;

/**
 * Class for download spectrum task.
 *
 * @author Murat Artim
 * @date Jul 29, 2014
 * @time 11:43:43 AM
 */
public class DownloadSpectrum extends TemporaryFileCreatingTask<AddSpectrum> implements LongRunningTask, SingleInputTask<SpectrumInfo>, AutomaticTaskOwner<Pair<Path, SpectrumInfo>> {

	/** CDF set info. */
	private SpectrumInfo info_;

	/** Output file. */
	private Path output_;

	/** True if the downloaded spectrum should be added to local database. */
	private final boolean addToDatabase_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<Path, SpectrumInfo>>> automaticTasks_ = null;

	/**
	 * Creates download spectrum task.
	 *
	 * @param info
	 *            CDF set info. Can be null for automatic execution.
	 * @param output
	 *            Output file. Null should be given if the downloaded spectrum should be added to local database.
	 * @param addToDatabase
	 *            True if the downloaded spectrum should be added to local database.
	 */
	public DownloadSpectrum(SpectrumInfo info, Path output, boolean addToDatabase) {
		info_ = info;
		output_ = output;
		addToDatabase_ = addToDatabase;
	}

	@Override
	public String getTaskTitle() {
		return "Download spectrum";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public void setAutomaticInput(SpectrumInfo info) {
		info_ = info;
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Pair<Path, SpectrumInfo>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Pair<Path, SpectrumInfo>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	protected AddSpectrum call() throws Exception {

		// check permission
		checkPermission(Permission.DOWNLOAD_SPECTRUM);

		// update progress info
		updateTitle("Downloading spectrum");

		// check output file
		if (output_ == null) {
			String name = (String) info_.getInfo(SpectrumInfoType.NAME);
			output_ = getWorkingDirectory().resolve(name + FileType.ZIP.getExtension());
		}

		// progress info
		updateMessage("Downloading spectrum '" + (String) info_.getInfo(SpectrumInfoType.NAME) + "' from database...");

		// get data URL
		String url = (String) info_.getInfo(SpectrumInfoType.DATA_URL);

		// download
		if (url != null) {
			try (FilerConnection filer = getFilerConnection()) {
				if (filer.fileExists(url)) {
					filer.getSftpChannel().get(url, output_.toString());
				}
			}
		}

		// add to database
		if (addToDatabase_) {

			// extract archive
			ArrayList<Path> files = Utility.extractAllFilesFromZIP(output_, this, getWorkingDirectory());

			// get spectrum files
			Path ana = getSpectrumFile(files, FileType.ANA);
			Path txt = getSpectrumFile(files, FileType.TXT);
			Path cvt = getSpectrumFile(files, FileType.CVT);
			Path fls = getSpectrumFile(files, FileType.FLS);
			Path xls = getSpectrumFile(files, FileType.XLS);

			// get paths to file names
			Path anaFileName = ana.getFileName();
			Path txtFileName = txt.getFileName();
			Path cvtFileName = cvt.getFileName();
			Path flsFileName = fls.getFileName();
			Path xlsFileName = xls.getFileName();
			if (anaFileName == null || txtFileName == null || cvtFileName == null || flsFileName == null || xlsFileName == null)
				throw new Exception("Cannot find CDF set files in the spectrum archive.");

			// create task
			AddSpectrum task = new AddSpectrum(null, null, null, null, null, null, info_);

			// copy spectrum files
			Path anaCopy = task.getWorkingDirectory().resolve(anaFileName.toString());
			Path txtCopy = task.getWorkingDirectory().resolve(txtFileName.toString());
			Path cvtCopy = task.getWorkingDirectory().resolve(cvtFileName.toString());
			Path flsCopy = task.getWorkingDirectory().resolve(flsFileName.toString());
			Path xlsCopy = task.getWorkingDirectory().resolve(xlsFileName.toString());
			Files.copy(ana, anaCopy, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(txt, txtCopy, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(cvt, cvtCopy, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(fls, flsCopy, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(xls, xlsCopy, StandardCopyOption.REPLACE_EXISTING);

			// get mission name (will be used as conversion table sheet)
			String mission = (String) info_.getInfo(SpectrumInfoType.FAT_MISSION);

			// set copied files to task
			task.setSpectrumFiles(anaCopy, txtCopy, cvtCopy, flsCopy, xlsCopy, mission);

			// return task
			return task;
		}

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// add to database
			if (addToDatabase_) {
				taskPanel_.getOwner().runTaskInParallel(get());
			}

			// manage automatic tasks
			automaticTaskOwnerSucceeded(new Pair<>(output_, info_), automaticTasks_, taskPanel_);
		}

		// exception occurred
		catch (Exception e) {
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
	 * Returns requested spectrum file from the given list.
	 *
	 * @param files
	 *            List of spectrum files.
	 * @param fileType
	 *            File type.
	 * @return Requested spectrum file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Path getSpectrumFile(ArrayList<Path> files, FileType fileType) throws Exception {
		for (Path file : files) {
			if (FileType.getFileType(file.toFile()).equals(fileType))
				return file;
		}
		throw new Exception("Downloaded spectrum archive does NOT contain the spectrum file with extension '" + fileType.getExtension() + "'.");
	}
}
