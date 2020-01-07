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
package equinox.task.serializableTask;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.task.SerializableTask;
import equinox.task.ShareGeneratedItem;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of share generated item task.
 *
 * @author Murat Artim
 * @date Oct 13, 2015
 * @time 10:07:05 AM
 */
public class SerializableShareGeneratedItem implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** File to share. */
	private final File file_;

	/** Recipients. */
	private final List<ExchangeUser> recipients_;

	/**
	 * Creates share generated item task.
	 *
	 * @param file
	 *            File to share.
	 * @param recipients
	 *            Recipients.
	 */
	public SerializableShareGeneratedItem(Path file, List<ExchangeUser> recipients) {
		file_ = file.toFile();
		recipients_ = recipients;
	}

	@Override
	public ShareGeneratedItem getTask(TreeItem<String> fileTreeRoot) {
		return new ShareGeneratedItem(file_.toPath(), recipients_);
	}
}
