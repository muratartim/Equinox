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

import equinox.task.AddSpectrum;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for serializable form of add spectrum task.
 *
 * @author Murat Artim
 * @date Oct 12, 2015
 * @time 1:34:15 PM
 */
public class SerializableAddSpectrum implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Paths to spectrum files. */
	private final File anaFile_, txtFile_, cvtFile_, flsFile_, conversionTable_, specFile_;

	/** The selected conversion table sheet. */
	private final String sheet_;

	/**
	 * Creates add spectrum task for adding individual CDF set files.
	 *
	 * @param anaFile
	 *            Path to ANA file.
	 * @param txtFile
	 *            Path to TXT file. Can be null.
	 * @param cvtFile
	 *            Path to CVT file.
	 * @param flsFile
	 *            Path to FLS file.
	 * @param conversionTable
	 *            Path to conversion table.
	 * @param sheet
	 *            Selected conversion table sheet.
	 */
	public SerializableAddSpectrum(Path anaFile, Path txtFile, Path cvtFile, Path flsFile, Path conversionTable, String sheet) {
		anaFile_ = anaFile == null ? null : anaFile.toFile();
		txtFile_ = txtFile == null ? null : txtFile.toFile();
		cvtFile_ = cvtFile == null ? null : cvtFile.toFile();
		flsFile_ = flsFile == null ? null : flsFile.toFile();
		conversionTable_ = conversionTable == null ? null : conversionTable.toFile();
		sheet_ = sheet;
		specFile_ = null;
	}

	/**
	 * Creates add spectrum task for adding individual CDF set files.
	 *
	 * @param specFile
	 *            Spectrum bundle.
	 */
	public SerializableAddSpectrum(Path specFile) {
		specFile_ = specFile.toFile();
		anaFile_ = null;
		txtFile_ = null;
		cvtFile_ = null;
		flsFile_ = null;
		conversionTable_ = null;
		sheet_ = null;
	}

	@Override
	public AddSpectrum getTask(TreeItem<String> fileTreeRoot) {
		if (specFile_ == null)
			return new AddSpectrum(anaFile_ == null ? null : anaFile_.toPath(), txtFile_ == null ? null : txtFile_.toPath(),
					cvtFile_ == null ? null : cvtFile_.toPath(), flsFile_ == null ? null : flsFile_.toPath(),
					conversionTable_ == null ? null : conversionTable_.toPath(), sheet_, null);
		return new AddSpectrum(specFile_.toPath());
	}
}
