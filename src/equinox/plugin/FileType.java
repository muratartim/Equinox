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
package equinox.plugin;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;

import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Enumeration for file types.
 *
 * @author Murat Artim
 * @date Jan 10, 2014
 * @time 10:11:42 AM
 */
public enum FileType {

	/** File type. */
	// @formatter:off
	STH(".sth", new ExtensionFilter("STH files (*.sth)", "*.sth")), ANA(".ana", new ExtensionFilter("ANA files (*.ana)", "*.ana")), PNG(".png", new ExtensionFilter("PNG files (*.png)", "*.png")),
	XLS(".xls", new ExtensionFilter("Excel 97-2004 workbook files (*.xls)", "*.xls")), ZIP(".zip", new ExtensionFilter("ZIP files (*.zip)", "*.zip")), TXT(".txt", new ExtensionFilter("TXT files (*.txt)", "*.txt")),
	CVT(".cvt", new ExtensionFilter("CVT files (*.cvt)", "*.cvt")), FLS(".fls", new ExtensionFilter("FLS files (*.fls)", "*.fls")), STF(".stf", new ExtensionFilter("STF files (*.stf)", "*.stf")),
	APP(".app", new ExtensionFilter("Applications (*.app)", "*.app")), SIGMA(".sigma", new ExtensionFilter("SIGMA files (*.sigma)", "*.sigma")), RFORT(".rfort", new ExtensionFilter("RFORT files (*.rfort)", "*.rfort")),
	RFLOW(".rflow", new ExtensionFilter("RFLOW files (*.rflow)", "*.rflow")), HTML(".html", new ExtensionFilter("Web pages (*.html)", "*.html")), GZ(".gz", new ExtensionFilter("GZIP files (*.gz)", "*.gz")),
	MP4(".mp4", new ExtensionFilter("Movie files (*.mp4)", "*.mp4")), MOV(".mov", new ExtensionFilter("Movie files (*.mov)", "*.mov")), EQX(".eqx", new ExtensionFilter("Equinox workspaces (*.eqx)", "*.eqx")), MAT(".mat", new ExtensionFilter("SAFE material files (*.mat)", "*.mat")),
	JAR(".jar", new ExtensionFilter("Java archive files (*.jar)", "*.jar")), MUT(".mut", new ExtensionFilter("Loadcase factors files (*.mut)", "*.mut")), F07(".f07", new ExtensionFilter("F07 files (*.f07)", "*.f07")),
	F06(".f06", new ExtensionFilter("F06 files (*.f06)", "*.f06")), GRP(".grp", new ExtensionFilter("Element groups files (*.grp)", "*.grp")), LCS(".lcs", new ExtensionFilter("Load case files (*.lcs)", "*.lcs")),
	PDF(".pdf", new ExtensionFilter("PDF files (*.pdf)", "*.pdf")), EQS(".eqs", new ExtensionFilter("Equivalent stress files (*.eqs)", "*.eqs")), EXE(".exe", new ExtensionFilter("Executable files (*.exe)", "*.exe")),
	DMG(".dmg", new ExtensionFilter("DMG files (*.dmg)", "*.dmg")), SPEC(".spec", new ExtensionFilter("Spectrum bundles (*.spec)", "*.spec")), LOG(".log", new ExtensionFilter("Log files (*.log)", "*.log")),
	ERREURS(".erreurs", new ExtensionFilter("SAFE erreurs files (*.erreurs)", "*.erreurs")), DOSSIER(".dossier", new ExtensionFilter("SAFE dossier files (*.dossier)", "*.dossier")),
	OUT(".out", new ExtensionFilter("OUT files (*.out)", "*.out")), CSV(".csv", new ExtensionFilter("CSV files (*.csv)", "*.csv")), LCK(".lck", new ExtensionFilter("Loadcase keys files (*.lck)", "*.lck")),
	MF(".MF", new ExtensionFilter("Manifest files (*.MF)", "*.MF")), XLSX(".xlsx", new ExtensionFilter("Excel workbook files (*.xlsx)", "*.xlsx")), XML(".xml", new ExtensionFilter("Extensible Markup Language files (*.xml)", "*.xml")),
	JSON(".json", new ExtensionFilter("JavaScript Object Notation files (*.json)", "*.json"));
	// @formatter:on

	/** File extension. */
	private final String extension_;

	/** Extension filter. */
	private final ExtensionFilter extensionFilter_;

	/**
	 * Creates file type.
	 *
	 * @param extension
	 *            File extension.
	 * @param extensionFilter
	 *            Extension filter.
	 */
	FileType(String extension, ExtensionFilter extensionFilter) {
		extension_ = extension;
		extensionFilter_ = extensionFilter;
	}

	/**
	 * Returns file extension.
	 *
	 * @return File extension.
	 */
	public String getExtension() {
		return extension_;
	}

	/**
	 * Returns extension filter.
	 *
	 * @return Extension filter.
	 */
	public ExtensionFilter getExtensionFilter() {
		return extensionFilter_;
	}

	/**
	 * Returns the file type of the given file, or null if file type is not recognized.
	 *
	 * @param file
	 *            File.
	 * @return The file type of the given file, or null if file type is not recognized.
	 */
	public static FileType getFileType(File file) {

		// no file extension found
		int index = file.getName().lastIndexOf(".");
		if (index <= 0)
			return null;

		// get file extension
		String extension = file.getName().substring(index);

		// return file type
		for (FileType fileType : FileType.values())
			if (fileType.getExtension().equalsIgnoreCase(extension))
				return fileType;
		return null;
	}

	/**
	 * Returns the file type of the given file name, or null if file type is not recognized.
	 *
	 * @param fileName
	 *            File name.
	 * @return File type of the given file name, or null if file type is not recognized.
	 */
	public static FileType getFileType(String fileName) {

		// no file extension found
		int index = fileName.lastIndexOf(".");
		if (index <= 0)
			return null;

		// get file extension
		String extension = fileName.substring(index);

		// return file type
		for (FileType fileType : FileType.values())
			if (fileType.getExtension().equalsIgnoreCase(extension))
				return fileType;
		return null;
	}

	/**
	 * Returns the file type for the given extension, or null if unknown extension.
	 *
	 * @param extension
	 *            File extension. Note that, this should include '.' at the beginning.
	 * @return The file type, or null if unknown extension.
	 */
	public static FileType getFileTypeForExtension(String extension) {

		// null extension given
		if (extension == null)
			return null;

		// loop over file types
		for (FileType type : FileType.values())
			if (type.getExtension().equalsIgnoreCase(extension))
				return type;

		// unknown file type
		return null;
	}

	/**
	 * Returns the file name without the extension, or null if file name could not be obtained.
	 *
	 * @param item
	 *            List item.
	 * @return The file name without the extension, or null if file name could not be obtained.
	 */
	public static String getNameWithoutExtension(Path item) {

		// null file name
		Path fileNamePath = item.getFileName();
		if (fileNamePath == null)
			return null;

		// get file name
		String name = fileNamePath.toString();

		// no extension found
		int index = name.lastIndexOf(".");
		if (index <= 0)
			return name;

		// return name without extension
		return name.substring(0, index);
	}

	/**
	 * Returns the file name without the extension.
	 *
	 * @param name
	 *            Name.
	 * @return The file name without the extension.
	 */
	public static String getNameWithoutExtension(String name) {

		// no extension found
		int index = name.lastIndexOf(".");
		if (index <= 0)
			return name;

		// return name without extension
		return name.substring(0, index);
	}

	/**
	 * Appends given extension to file name if necessary.
	 *
	 * @param file
	 *            File.
	 * @param fileType
	 *            File type.
	 * @return The modified file.
	 */
	public static File appendExtension(File file, FileType fileType) {

		// get absolute file path
		String path = file.getAbsolutePath();

		// no extension found
		int index = path.lastIndexOf(".");
		if (index <= 0) {
			path += fileType.getExtension();
			return new File(path);
		}

		// other extension
		if (path.length() >= fileType.getExtension().length() + 1)
			if (fileType.getExtension().equalsIgnoreCase(path.substring(index)) == false) {
				path += fileType.getExtension();
			}

		// return file
		return new File(path);
	}

	/**
	 * Appends given extension to file name if necessary.
	 *
	 * @param name
	 *            File name.
	 * @param fileType
	 *            File type.
	 * @return The modified file.
	 */
	public static String appendExtension(String name, FileType fileType) {

		// no extension found
		int index = name.lastIndexOf(".");
		if (index <= 0) {
			name += fileType.getExtension();
			return name;
		}

		// other extension
		if (name.length() >= fileType.getExtension().length() + 1)
			if (fileType.getExtension().equalsIgnoreCase(name.substring(index)) == false) {
				name += fileType.getExtension();
			}
		return name;
	}

	/**
	 * Returns file extension filter for all spectrum files.
	 *
	 * @param includeSTF
	 *            True to include STF file extension.
	 * @return File extension filter for all spectrum files.
	 */
	public static ExtensionFilter getSpectrumFileFilter(boolean includeSTF) {
		if (!includeSTF)
			return new ExtensionFilter("Spectrum files", "*.spec", "*.ana", "*.gz", "*.zip", "*.cvt", "*.fls", "*.xls", "*.txt");
		return new ExtensionFilter("Spectrum files", "*.spec", "*.ana", "*.gz", "*.zip", "*.cvt", "*.fls", "*.xls", "*.txt", "*.stf");
	}

	/**
	 * Returns file extension filter for given file types.
	 *
	 * @param description
	 *            Description of file filter.
	 * @param types
	 *            File types.
	 * @return File extension filter for given file types.
	 */
	public static ExtensionFilter getCustomFileFilter(String description, FileType... types) {
		String[] extensions = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			extensions[i] = types[i].getExtensionFilter().getExtensions().get(0);
		}
		return new ExtensionFilter(description, extensions);
	}

	/**
	 * Returns file filter for the given file types.
	 *
	 * @param fileTypes
	 *            File types.
	 * @return File filter.
	 */
	public static DirectoryStream.Filter<Path> getFileFilter(FileType... fileTypes) {

		// create file filter
		DirectoryStream.Filter<Path> filter = file -> {
			Path fileNamePath = file.getFileName();
			if (fileNamePath == null)
				return false;
			String fileName = fileNamePath.toString().toLowerCase();
			for (FileType fileType : fileTypes)
				if (fileName.endsWith(fileType.getExtension().toLowerCase()))
					return true;
			return false;
		};

		// return filter
		return filter;
	}
}
