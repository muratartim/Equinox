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
import java.util.List;

import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.serverUtilities.SharedFileInfo;
import equinox.task.InternalEquinoxTask.FileSharingTask;
import equinox.task.automation.SingleInputTask;
import equinox.task.serializableTask.SerializableShareGeneratedItem;
import equinox.utility.Utility;

/**
 * Class for share generated item task.
 *
 * @author Murat Artim
 * @date Sep 23, 2014
 * @time 6:19:36 PM
 */
public class ShareGeneratedItem extends TemporaryFileCreatingTask<Void> implements SavableTask, FileSharingTask, SingleInputTask<Path> {

	/** File to share. */
	private Path file_;

	/** Recipients. */
	private final List<ExchangeUser> recipients_;

	/**
	 * Creates share generated item task.
	 *
	 * @param file
	 *            File to share. Can be null for automatic execution.
	 * @param recipients
	 *            Recipients.
	 */
	public ShareGeneratedItem(Path file, List<ExchangeUser> recipients) {
		file_ = file;
		recipients_ = recipients;
	}

	@Override
	public void setAutomaticInput(Path input) {
		file_ = input;
	}

	@Override
	public String getTaskTitle() {
		return "Share file";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableShareGeneratedItem(file_, recipients_);
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SHARE_FILE);

		// update progress info
		updateTitle("Sharing file...");

		// zip file
		String name = FileType.getNameWithoutExtension(file_);
		Path output = getWorkingDirectory().resolve(FileType.appendExtension(name, FileType.ZIP));
		Utility.zipFile(file_, output.toFile(), this);

		// upload file to filer
		shareFile(output, recipients_, SharedFileInfo.FILE);
		return null;
	}
}
