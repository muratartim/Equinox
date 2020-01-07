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
package equinox.data.ui;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;

/**
 * Class for MyCheck mission.
 *
 * @author Murat Artim
 * @date Mar 17, 2015
 * @time 10:46:18 AM
 */
public class MyCheckMission implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Enumeration for mission type.
	 *
	 * @author Murat Artim
	 * @date Mar 17, 2015
	 * @time 10:50:25 AM
	 */
	public enum MissionType {

		/** Mission type. */
		SR("Short Range Mission", "SR"), MR("Medium Range Mission", "MR"), LR("Long Range Mission", "LR"), MX("Mix Mission",
				"MX"), MT("Thermal Mission", "MT");

		/** Mission type name. */
		private final String name_, code_;

		/**
		 * Creates mission type.
		 *
		 * @param name
		 *            Mission type name.
		 * @param code
		 *            Mission code.
		 */
		MissionType(String name, String code) {
			name_ = name;
			code_ = code;
		}

		/**
		 * Returns mission type name.
		 *
		 * @return Mission type name.
		 */
		public String getName() {
			return name_;
		}

		/**
		 * Returns mission code.
		 *
		 * @return Mission code.
		 */
		public String getCode() {
			return code_;
		}

		@Override
		public String toString() {
			return name_;
		}
	}

	/** Mission type. */
	private final MissionType type_;

	/** Spectrum files. */
	private final File ana_, txt_, cvt_, conversionTable_, stf_;

	/** Conversion table worksheet. */
	private final String worksheet_;

	/**
	 * Creates MyCheck mission.
	 *
	 * @param type
	 *            Mission type.
	 * @param ana
	 *            Path to ANA file.
	 * @param txt
	 *            Path to TXT file.
	 * @param cvt
	 *            Path to CVT file.
	 * @param conversionTable
	 *            Path to conversion table.
	 * @param worksheet
	 *            Conversion table worksheet.
	 * @param stf
	 *            Path to STF file. This can be null.
	 */
	public MyCheckMission(MissionType type, Path ana, Path txt, Path cvt, Path conversionTable, String worksheet, Path stf) {
		type_ = type;
		ana_ = ana == null ? null : ana.toFile();
		txt_ = txt == null ? null : txt.toFile();
		cvt_ = cvt == null ? null : cvt.toFile();
		conversionTable_ = conversionTable == null ? null : conversionTable.toFile();
		worksheet_ = worksheet;
		stf_ = stf == null ? null : stf.toFile();
	}

	/**
	 * Returns mission type.
	 *
	 * @return Mission type.
	 */
	public MissionType getMissionType() {
		return type_;
	}

	/**
	 * Returns path to ANA file.
	 *
	 * @return Path to ANA file.
	 */
	public Path getANAFile() {
		return ana_ == null ? null : ana_.toPath();
	}

	/**
	 * Returns path to TXT file.
	 *
	 * @return Path to TXT file.
	 */
	public Path getTXTFile() {
		return txt_ == null ? null : txt_.toPath();
	}

	/**
	 * Returns path to CVT file.
	 *
	 * @return Path to CVT file.
	 */
	public Path getCVTFile() {
		return cvt_ == null ? null : cvt_.toPath();
	}

	/**
	 * Returns path to conversion table file.
	 *
	 * @return Path to conversion table file.
	 */
	public Path getConversionTableFile() {
		return conversionTable_ == null ? null : conversionTable_.toPath();
	}

	/**
	 * Returns path to STF file, or null if no STF file supplied.
	 *
	 * @return Path to STF file, or null if no STF file supplied.
	 */
	public Path getSTFFile() {
		return stf_ == null ? null : stf_.toPath();
	}

	/**
	 * Returns conversion table worksheet.
	 *
	 * @return Conversion table worksheet.
	 */
	public String getConversionTableWorksheet() {
		return worksheet_;
	}

	@Override
	public String toString() {
		return type_.toString();
	}
}
