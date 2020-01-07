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
package equinox.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;

/**
 * Class for conversion table sheet name.
 *
 * @author Murat Artim
 * @date Jun 13, 2016
 * @time 8:25:27 AM
 */
public class ConversionTableSheetName implements Serializable {

	/** File name of the object. */
	public static final String FILE_NAME = "convTableSheet.info";

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Conversion table sheet name. */
	private final String sheetName_;

	/**
	 * Creates conversion table sheet name.
	 *
	 * @param sheetName
	 *            Conversion table sheet name.
	 */
	public ConversionTableSheetName(String sheetName) {
		sheetName_ = sheetName;
	}

	/**
	 * Returns conversion table sheet name.
	 *
	 * @return Conversion table sheet name.
	 */
	public String getSheetName() {
		return sheetName_;
	}

	/**
	 * Saves this object to a file under the given directory.
	 *
	 * @param directory
	 *            Directory to save the object under.
	 * @return Path to output file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public Path write(Path directory) throws Exception {
		Path output = directory.resolve(FILE_NAME);
		try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(output.toFile())))) {
			out.writeObject(this);
		}
		return output;
	}

	/**
	 * Reads conversion table sheet name for the info file within the given directory.
	 *
	 * @param input
	 *            Conversion table info file.
	 * @return Conversion table sheet name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static String read(Path input) throws Exception {
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input.toFile())))) {
			ConversionTableSheetName object = (ConversionTableSheetName) in.readObject();
			return object.getSheetName();
		}
	}
}
